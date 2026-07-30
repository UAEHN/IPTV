#!/usr/bin/env python3
"""
Downloads the IBM Plex Sans Arabic weights the app bundles.

IBM Plex Sans Arabic (SIL OFL 1.1) is used instead of the system default because
Roboto's Arabic fallback (Noto Naskh) mixes badly with Latin channel names, and
Plex Arabic was drawn as a matched Arabic/Latin pair. Weights are pulled from the
Google Fonts CSS API so the URLs stay valid as the family is revved.

Usage:  python3 tools/fetch_fonts.py
"""
from __future__ import annotations

import os
import re
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "app", "src", "main", "res", "font")

FAMILY = "IBM+Plex+Sans+Arabic"
# res/font/ filenames must be lowercase with underscores for aapt.
WEIGHTS = {400: "regular", 500: "medium", 600: "semibold", 700: "bold"}
# The CSS API picks a font format from the User-Agent. A bare "Mozilla/5.0" is old
# enough that it is served plain TTF (what Android wants) rather than woff2, but
# new enough that it is served the full family rather than an empty stylesheet.
UA = "Mozilla/5.0"


def fetch(url: str, ua: str = UA) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": ua})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read()


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    css_url = (f"https://fonts.googleapis.com/css2?family={FAMILY}:wght@"
               + ";".join(str(w) for w in WEIGHTS) + "&display=swap")
    css = fetch(css_url).decode("utf-8")

    blocks = css.split("@font-face")
    found = {}
    for block in blocks:
        m_w = re.search(r"font-weight:\s*(\d+)", block)
        m_u = re.search(r"src:\s*url\((https://[^)]+\.ttf)\)", block)
        if m_w and m_u:
            found[int(m_w.group(1))] = m_u.group(1)

    missing = set(WEIGHTS) - set(found)
    if missing:
        raise SystemExit(f"Google Fonts CSS did not offer TTF for weights {sorted(missing)}")

    for weight, name in WEIGHTS.items():
        data = fetch(found[weight])
        path = os.path.join(OUT, f"plex_arabic_{name}.ttf")
        with open(path, "wb") as f:
            f.write(data)
        print(f"  {os.path.relpath(path, ROOT)}  {len(data) // 1024} KB")


if __name__ == "__main__":
    main()
