#!/usr/bin/env python3
"""
Probes every source URL in channels/channels.json and writes channels/health.json.

This has to run somewhere with unrestricted outbound network (a GitHub Actions
runner), which is why it is a CI job rather than something the app does.

Two things this deliberately does NOT do:

  * It does not treat a runner-side failure as proof a channel is dead. GitHub
    runners sit in US/EU datacentres and several Gulf broadcasters refuse
    datacentre ranges outright, so a stream that is fine on a phone in Dubai can
    fail here. Those are classified `blocked` rather than `dead`.
  * It does not fail a channel just because a variant playlist inside a master
    would not load. ExoPlayer negotiates variants differently; a master that
    parses is recorded as healthy with `variantUnverified` noted.

Each source is tried twice: once bare, and once with a Referer/Origin derived
from the channel's own site, because a few CDNs gate on it. When the second
attempt is the one that works, the report says so and the catalog should carry
`"referer": true` for that channel so the app sends the same header.

Usage:
  python3 tools/check_streams.py [--diagnose] [--timeout 20] [--jobs 8]
"""
from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import re
import socket
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "channels", "channels.json")
HEALTH = os.path.join(ROOT, "channels", "health.json")

UA = ("Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
BASE_HEADERS = {
    "User-Agent": UA,
    "Accept": "application/vnd.apple.mpegurl,application/x-mpegURL,*/*",
    "Accept-Language": "ar,en;q=0.8",
}

STREAM_INF = re.compile(r"#EXT-X-STREAM-INF")
EXTINF = re.compile(r"#EXTINF")
# Wording that shows up on the interstitials CDNs serve instead of a 403.
GEO_WORDS = re.compile(
    r"not available in your|geo|region|country|restricted|forbidden|blocked",
    re.I)


class Fetch:
    __slots__ = ("status", "body", "final_url", "ctype", "error", "kind")

    def __init__(self, status=0, body="", final_url="", ctype="", error="", kind=""):
        self.status, self.body, self.final_url = status, body, final_url
        self.ctype, self.error, self.kind = ctype, error, kind


def classify(exc: BaseException) -> tuple[str, str]:
    """Return (kind, human detail). urllib buries the real cause in .reason."""
    reason = getattr(exc, "reason", exc)
    if isinstance(reason, socket.gaierror):
        return "dns", f"DNS: {reason}"
    if isinstance(reason, ssl.SSLCertVerificationError):
        return "tls", f"TLS cert: {reason.verify_message or reason}"
    if isinstance(reason, ssl.SSLError):
        return "tls", f"TLS: {reason}"
    if isinstance(reason, (TimeoutError, socket.timeout)):
        return "timeout", "timed out"
    if isinstance(reason, ConnectionResetError):
        return "reset", "connection reset by peer"
    if isinstance(reason, ConnectionRefusedError):
        return "refused", "connection refused"
    return "error", f"{type(reason).__name__}: {reason}"


def fetch(url: str, timeout: float, referer: str | None = None) -> Fetch:
    headers = dict(BASE_HEADERS)
    if referer:
        parts = urllib.parse.urlsplit(referer)
        headers["Referer"] = referer
        headers["Origin"] = f"{parts.scheme}://{parts.netloc}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return Fetch(status=r.status, body=r.read(262144).decode("utf-8", "replace"),
                         final_url=r.url, ctype=r.headers.get("Content-Type", ""))
    except urllib.error.HTTPError as e:
        body = ""
        try:
            body = e.read(4096).decode("utf-8", "replace")
        except Exception:
            pass
        return Fetch(status=e.code, body=body, final_url=e.url or url,
                     ctype=e.headers.get("Content-Type", "") if e.headers else "",
                     error=f"http {e.code}", kind="http")
    except Exception as e:
        kind, detail = classify(e)
        return Fetch(error=detail, kind=kind, final_url=url)


def inspect(f: Fetch, url: str, timeout: float) -> dict:
    """Turn a fetched body into a verdict about the playlist."""
    out = {"ok": False, "reason": f.error, "kind": "", "variants": 0,
           "class": f.kind or "", "finalUrl": f.final_url if f.final_url != url else "",
           "contentType": f.ctype, "snippet": ""}

    if f.status == 0:
        out["class"] = f.kind or "error"
        return out

    if f.status != 200:
        out["reason"] = f"http {f.status}"
        out["class"] = "blocked" if f.status in (401, 403, 451) else "http"
        out["snippet"] = " ".join(f.body.split())[:160]
        return out

    if "#EXTM3U" not in f.body:
        flat = " ".join(f.body.split())
        out["reason"] = "not a playlist"
        out["class"] = "blocked" if GEO_WORDS.search(flat[:600]) else "notplaylist"
        out["snippet"] = flat[:160]
        return out

    if "#EXT-X-ENDLIST" in f.body:
        out["reason"] = "recorded, not live"
        out["class"] = "vod"
        return out

    variants = len(STREAM_INF.findall(f.body))
    out["variants"] = variants

    if variants:
        out["kind"] = "master"
        out["ok"] = True
        child = None
        lines = [ln.strip() for ln in f.body.splitlines()]
        for i, ln in enumerate(lines):
            if ln.startswith("#EXT-X-STREAM-INF"):
                for nxt in lines[i + 1:]:
                    if nxt and not nxt.startswith("#"):
                        child = urllib.parse.urljoin(f.final_url or url, nxt)
                        break
                break
        if child:
            cf = fetch(child, timeout)
            if cf.status == 200 and EXTINF.search(cf.body):
                out["reason"] = ""
            else:
                # Recorded, not fatal: players negotiate variants differently.
                out["reason"] = f"variantUnverified ({cf.error or 'no segments'})"
        else:
            out["reason"] = "variantUnverified (no variant URI)"
        return out

    if EXTINF.search(f.body):
        out["kind"] = "media"
        out["ok"] = True
        return out

    out["reason"] = "playlist has no segments"
    out["class"] = "empty"
    return out


def probe(url: str, site: str | None, timeout: float) -> dict:
    started = time.time()
    result = inspect(fetch(url, timeout), url, timeout)
    result["referer"] = False

    if not result["ok"] and site:
        # A few CDNs answer only when the request looks like it came from the
        # broadcaster's own player page.
        retry = inspect(fetch(url, timeout, referer=site), url, timeout)
        if retry["ok"]:
            retry["referer"] = True
            result = retry

    result["url"] = url
    result["ms"] = int((time.time() - started) * 1000)
    return result


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--timeout", type=float, default=20.0)
    ap.add_argument("--jobs", type=int, default=8)
    ap.add_argument("--diagnose", action="store_true",
                    help="print full detail for every failing source")
    args = ap.parse_args()

    catalog = json.load(open(CATALOG, encoding="utf-8"))
    channels = catalog["channels"]

    work = [(c, s) for c in channels for s in c["sources"]]
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.jobs) as ex:
        probes = list(ex.map(
            lambda cs: probe(cs[1]["url"], cs[0].get("site"), args.timeout), work))

    by_channel: dict[str, list[dict]] = {}
    for (c, _s), p in zip(work, probes):
        by_channel.setdefault(c["id"], []).append(p)

    report = {"checked": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "note": "Probed from a GitHub runner. `blocked` means the CDN "
                      "refused this datacentre, not that the channel is dead.",
              "channels": {}}

    healthy = blocked = dead = 0
    for c in channels:
        results = by_channel[c["id"]]
        ok = any(r["ok"] for r in results)
        only_blocked = not ok and all(
            r["class"] in ("blocked", "dns", "tls", "timeout", "reset", "refused")
            for r in results)
        state = "ok" if ok else ("blocked" if only_blocked else "dead")
        healthy += state == "ok"
        blocked += state == "blocked"
        dead += state == "dead"
        report["channels"][c["id"]] = {"state": state, "sources": results}

    report["summary"] = {"total": len(channels), "ok": healthy,
                         "blocked": blocked, "dead": dead}

    os.makedirs(os.path.dirname(HEALTH), exist_ok=True)
    with open(HEALTH, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=1, ensure_ascii=False)
        f.write("\n")

    mark = {"ok": "OK", "blocked": "~~", "dead": "XX"}
    for c in sorted(channels, key=lambda x: (x["cat"], x["en"])):
        entry = report["channels"][c["id"]]
        detail = " | ".join(
            (f'{r["kind"]}/{r["variants"]}v {r["ms"]}ms'
             + (" +referer" if r["referer"] else "")
             + (f' [{r["reason"]}]' if r["reason"] else "")) if r["ok"]
            else f'{r["class"]}: {r["reason"]}'
            for r in entry["sources"])
        print(f'{mark[entry["state"]]} {c["en"][:32]:32} {c["cat"]:9} {detail}')

    s = report["summary"]
    print("-" * 96)
    print(f'{s["ok"]} ok, {s["blocked"]} blocked from this runner, {s["dead"]} dead '
          f'(of {s["total"]})')

    if args.diagnose:
        print("\n" + "=" * 96)
        print("DIAGNOSTICS for every non-ok source")
        print("=" * 96)
        for c in channels:
            entry = report["channels"][c["id"]]
            for r in entry["sources"]:
                if r["ok"]:
                    continue
                print(f'\n{c["id"]}  [{r["class"]}] {r["reason"]}')
                print(f'  url   {r["url"]}')
                if r["finalUrl"]:
                    print(f'  final {r["finalUrl"]}')
                if r["contentType"]:
                    print(f'  ctype {r["contentType"]}')
                if r["snippet"]:
                    print(f'  body  {r["snippet"]}')
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
