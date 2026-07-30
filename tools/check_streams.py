#!/usr/bin/env python3
"""
Probes every source URL in channels/channels.json and writes channels/health.json.

This has to run somewhere with unrestricted outbound network (a GitHub Actions
runner), which is why it is a CI job rather than something the app does.

A source counts as healthy when:
  * the playlist responds 200 and starts with #EXTM3U, and
  * it is either a master playlist with at least one #EXT-X-STREAM-INF variant,
    or a media playlist with at least one #EXTINF segment, and
  * for a master playlist, the first variant also resolves and yields segments
    (catches CDNs that keep serving a stale master for a retired channel), and
  * the playlist is live, i.e. it does not carry #EXT-X-ENDLIST.

Exit code is 0 unless --fail-under is given and the healthy-channel ratio is
below it. A channel is healthy if any one of its sources is.

Usage:
  python3 tools/check_streams.py [--fail-under 0.6] [--timeout 20] [--jobs 8]
"""
from __future__ import annotations

import argparse
import concurrent.futures
import json
import os
import re
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "channels", "channels.json")
HEALTH = os.path.join(ROOT, "channels", "health.json")

# Some broadcaster CDNs 403 anything that does not look like a browser or a
# set-top box. ExoPlayer sends a similar UA, so probing with one keeps the
# check honest rather than optimistic.
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
    "Accept": "application/vnd.apple.mpegurl,application/x-mpegURL,*/*",
    "Accept-Language": "ar,en;q=0.8",
}

STREAM_INF = re.compile(r"#EXT-X-STREAM-INF")
EXTINF = re.compile(r"#EXTINF")


def get(url: str, timeout: float) -> tuple[int, str]:
    ctx = ssl.create_default_context()
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=ctx) as r:
            # A live playlist is small; cap the read so a misconfigured URL that
            # returns a video file cannot stall the job.
            return r.status, r.read(262144).decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, ""
    except Exception as e:  # DNS, TLS, timeout, connection reset
        return 0, type(e).__name__


def first_variant(body: str, base: str) -> str | None:
    lines = [ln.strip() for ln in body.splitlines()]
    for i, ln in enumerate(lines):
        if ln.startswith("#EXT-X-STREAM-INF"):
            for nxt in lines[i + 1:]:
                if nxt and not nxt.startswith("#"):
                    return urllib.parse.urljoin(base, nxt)
    return None


def probe(url: str, timeout: float) -> dict:
    started = time.time()
    status, body = get(url, timeout)
    result = {"url": url, "status": status, "ok": False, "reason": "", "kind": "",
              "variants": 0, "ms": 0}

    if status != 200:
        result["reason"] = f"http {status}" if status else f"unreachable ({body})"
    elif "#EXTM3U" not in body:
        result["reason"] = "not an m3u8"
    else:
        variants = len(STREAM_INF.findall(body))
        segments = len(EXTINF.findall(body))
        result["variants"] = variants
        if "#EXT-X-ENDLIST" in body:
            result["reason"] = "vod, not live"
        elif variants:
            result["kind"] = "master"
            child = first_variant(body, url)
            if not child:
                result["reason"] = "master with no usable variant"
            else:
                c_status, c_body = get(child, timeout)
                if c_status != 200:
                    result["reason"] = f"variant http {c_status}"
                elif not EXTINF.search(c_body):
                    result["reason"] = "variant has no segments"
                else:
                    result["ok"] = True
        elif segments:
            result["kind"] = "media"
            result["ok"] = True
        else:
            result["reason"] = "playlist has no segments"

    result["ms"] = int((time.time() - started) * 1000)
    return result


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--timeout", type=float, default=20.0)
    ap.add_argument("--jobs", type=int, default=8)
    ap.add_argument("--fail-under", type=float, default=None,
                    help="exit 1 if the healthy-channel ratio is below this")
    args = ap.parse_args()

    catalog = json.load(open(CATALOG, encoding="utf-8"))
    channels = catalog["channels"]

    work = [(c, s) for c in channels for s in c["sources"]]
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.jobs) as ex:
        probes = list(ex.map(lambda cs: probe(cs[1]["url"], args.timeout), work))

    by_channel: dict[str, list[dict]] = {}
    for (c, _s), p in zip(work, probes):
        by_channel.setdefault(c["id"], []).append(p)

    report = {"checked": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
              "channels": {}}
    healthy = 0
    for c in channels:
        results = by_channel[c["id"]]
        ok = any(r["ok"] for r in results)
        healthy += ok
        report["channels"][c["id"]] = {"ok": ok, "sources": results}

    report["summary"] = {"total": len(channels), "healthy": healthy,
                         "dead": len(channels) - healthy}

    os.makedirs(os.path.dirname(HEALTH), exist_ok=True)
    with open(HEALTH, "w", encoding="utf-8") as f:
        json.dump(report, f, indent=1, ensure_ascii=False)
        f.write("\n")

    order = {c["id"]: (c["cat"], c["en"]) for c in channels}
    print(f"{'':2} {'channel':34} {'cat':9} source result")
    print("-" * 92)
    for c in sorted(channels, key=lambda x: (x["cat"], x["en"])):
        entry = report["channels"][c["id"]]
        mark = "OK" if entry["ok"] else "XX"
        detail = " | ".join(
            (f'{r["kind"] or "-"}/{r["variants"]}v {r["ms"]}ms' if r["ok"]
             else r["reason"]) for r in entry["sources"])
        print(f'{mark:2} {c["en"][:34]:34} {order[c["id"]][0]:9} {detail}')

    s = report["summary"]
    print("-" * 92)
    print(f'{s["healthy"]}/{s["total"]} channels reachable, {s["dead"]} dead')

    if args.fail_under is not None and s["total"]:
        ratio = s["healthy"] / s["total"]
        if ratio < args.fail_under:
            print(f"FAIL: {ratio:.0%} healthy is below the {args.fail_under:.0%} floor")
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
