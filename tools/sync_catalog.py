#!/usr/bin/env python3
"""
Copies channels/channels.json into the app's assets, minified.

channels/channels.json is the human-edited source of truth. The app ships a
compact copy so it works on first launch with no network, and can later pull a
newer catalog from the repo at runtime.

Usage:  python3 tools/sync_catalog.py
"""
from __future__ import annotations

import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "channels", "channels.json")
DST = os.path.join(ROOT, "app", "src", "main", "assets", "channels.json")


def main() -> None:
    catalog = json.load(open(SRC, encoding="utf-8"))

    ids = [c["id"] for c in catalog["channels"]]
    if len(ids) != len(set(ids)):
        dupes = sorted({i for i in ids if ids.count(i) > 1})
        raise SystemExit(f"duplicate channel ids: {dupes}")

    cats = {c["id"] for c in catalog["categories"]}
    for c in catalog["channels"]:
        if c["cat"] not in cats:
            raise SystemExit(f'{c["id"]}: unknown category {c["cat"]!r}')
        if not c["sources"]:
            raise SystemExit(f'{c["id"]}: no sources')
        for s in c["sources"]:
            if not s["url"].startswith("https://"):
                raise SystemExit(f'{c["id"]}: non-https source {s["url"]!r}')

    os.makedirs(os.path.dirname(DST), exist_ok=True)
    with open(DST, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")

    size = os.path.getsize(DST)
    print(f"  {os.path.relpath(DST, ROOT)}  {len(catalog['channels'])} channels, {size // 1024} KB")


if __name__ == "__main__":
    main()
