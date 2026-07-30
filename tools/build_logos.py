#!/usr/bin/env python3
"""
Builds app/src/main/assets/logos/<id>.webp — one tile per channel, so the grid
never shows a hole and the app needs no network to render a logo.

Two paths, both producing the same 480x300 transparent canvas:

  1. Channels in LOGO_SOURCES get their real broadcaster logo, fetched from the
     tv-logo/tv-logos collection, trimmed to its ink and scaled to a common
     optical size. A logo whose artwork is dark (drawn for white backgrounds)
     is set on a paper-coloured slab so it stays legible on the app's dark
     plate, instead of disappearing into it.

  2. Every other channel gets a typographic wordmark: the Arabic name set in
     IBM Plex Sans Arabic, a short brass rule, and the Latin name beneath. The
     point is that a channel without artwork should look deliberate rather than
     broken, and should sit at the same optical weight as a real logo.

Requires network. Run it when the catalog changes:
    python3 tools/build_logos.py [--repo /path/to/tv-logos]
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import tempfile
import urllib.request

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "channels", "channels.json")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "logos")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")

LOGOS_REPO = "https://github.com/tv-logo/tv-logos.git"
RAW = "https://raw.githubusercontent.com/tv-logo/tv-logos/main/countries/"

TILE = (480, 300)
# Real logos are given a smaller box than wordmarks: broadcaster marks carry
# their own padding, so matching the raw bounding boxes would make them read
# larger than the type-set tiles sitting next to them.
LOGO_BOX = (348, 186)
WORD_BOX = (400, 210)

PAPER = (243, 241, 236, 255)
PAPER_DIM = (154, 161, 169, 255)
BRASS = (200, 164, 93, 255)

# channel id -> path under countries/ in the tv-logos collection
LOGO_SOURCES = {
    "aljazeera": "united-kingdom/aljazeera-uk.png",
    "aljazeera-mubasher": "united-kingdom/aljazeera-uk.png",
    "alarabiya": "united-arab-emirates/al-arabiya-ae.png",
    "alhadath": "united-arab-emirates/al-arabiya-al-hadath-ae.png",
    "skynewsarabia": "world-middle-east/sky-news-arabia-mea.png",
    "bbcarabic": "international/bbc-news-arabic-int.png",
    "france24-ar": "international/france-24-int.png",
    "dw-arabic": "international/dw-int.png",
    "trt-arabi": "turkey/trt-arabi-tr.png",
    "cgtn-arabic": "international/cgtn-arabic-int.png",
    "cnbc-arabia": "world-middle-east/cnbc-arabiya-mea.png",
    "mtv-lebanon": "lebanon/mtv-lebanon-lb.png",
    "otv-lebanon": "lebanon/otv-lb.png",
    "sharjah-tv": "united-arab-emirates/sharjah-tv-ae.png",
    "sharjah-quran": "united-arab-emirates/sharjah-tv-ae.png",
    "qatar-tv": "united-arab-emirates/qatar-tv-ae.png",
    "alsaudiya": "united-arab-emirates/al-saudiya-ae.png",
    "sbc": "united-arab-emirates/sbc-ae.png",
    "mbc1": "united-arab-emirates/mbc-1-ae.png",
    "mbc3": "united-arab-emirates/mbc-3-ae.png",
    "mbc5": "united-arab-emirates/mbc-5-ae.png",
    "mbc-masr": "united-arab-emirates/mbc-maser-ae.png",
    "mbc-iraq": "united-arab-emirates/mbc-iraq-ae.png",
}


# ---------------------------------------------------------------- fetching

def fetch_bytes(url: str) -> bytes | None:
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.read()
    except Exception as e:
        print(f"    ! {url}: {e}", file=sys.stderr)
        return None


def from_repo(repo: str, rel: str) -> bytes | None:
    """Read a blob out of a local (possibly blobless) clone."""
    try:
        return subprocess.run(
            ["git", "-C", repo, "show", f"HEAD:countries/{rel}"],
            capture_output=True, check=True).stdout
    except subprocess.CalledProcessError:
        return None


# ---------------------------------------------------------------- drawing

def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(os.path.join(FONT_DIR, name), size)


def fit(img: Image.Image, box: tuple[int, int]) -> Image.Image:
    w, h = img.size
    scale = min(box[0] / w, box[1] / h)
    return img.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)


def mean_luma(img: Image.Image) -> float:
    """Average luminance of the pixels that are actually painted."""
    small = img.resize((64, 64), Image.LANCZOS)
    total = weight = 0.0
    for r, g, b, a in list(small.convert("RGBA").getdata()):
        if a < 24:
            continue
        w = a / 255.0
        total += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0 * w
        weight += w
    return total / weight if weight else 1.0


def logo_tile(raw: bytes) -> Image.Image:
    src = Image.open(__import__("io").BytesIO(raw)).convert("RGBA")
    bbox = src.getchannel("A").getbbox()
    if bbox:
        src = src.crop(bbox)

    tile = Image.new("RGBA", TILE, (0, 0, 0, 0))

    if mean_luma(src) < 0.42:
        # Dark artwork: seat it on a paper slab, sized from the logo itself so
        # the slab reads as part of the mark rather than as a card background.
        art = fit(src, (LOGO_BOX[0] - 44, LOGO_BOX[1] - 44))
        pad = 22
        slab = Image.new("RGBA", (art.width + pad * 2, art.height + pad * 2), (0, 0, 0, 0))
        ImageDraw.Draw(slab).rounded_rectangle(
            [0, 0, slab.width - 1, slab.height - 1], radius=10, fill=PAPER)
        slab.alpha_composite(art, (pad, pad))
        art = slab
    else:
        art = fit(src, LOGO_BOX)

    tile.alpha_composite(art, ((TILE[0] - art.width) // 2, (TILE[1] - art.height) // 2))
    return tile


RULE_W, RULE_H = 34, 3
GAP_NAME_RULE = 18
GAP_RULE_LATIN = 17


def wordmark_tile(arabic: str, latin: str) -> Image.Image:
    tile = Image.new("RGBA", TILE, (0, 0, 0, 0))
    d = ImageDraw.Draw(tile)

    # Shrink the Arabic until it fits the box; long names get smaller type
    # rather than a truncated or squashed mark.
    size = 68
    while size > 26:
        f_ar = font("plex_arabic_semibold.ttf", size)
        if d.textlength(arabic, font=f_ar, direction="rtl", language="ar") <= WORD_BOX[0]:
            break
        size -= 2
    f_ar = font("plex_arabic_semibold.ttf", size)

    latin = latin.upper()
    f_la = font("plex_arabic_medium.ttf", 21 if len(latin) <= 22 else 17)
    tracking = 2.4  # Latin only: tracking an Arabic string would break its joins.
    glyphs = [(ch, d.textlength(ch, font=f_la)) for ch in latin]
    latin_w = sum(w for _, w in glyphs) + tracking * (len(glyphs) - 1)

    # Measure the real inked bounds rather than the font's line box. Plex
    # Arabic carries deep descenders, and laying the rule out from the line box
    # puts it inside the word instead of below it.
    ar_box = d.textbbox((0, 0), arabic, font=f_ar, anchor="ls",
                        direction="rtl", language="ar")
    ar_top, ar_bottom = ar_box[1], ar_box[3]
    la_box = d.textbbox((0, 0), latin, font=f_la, anchor="ls")
    la_top, la_bottom = la_box[1], la_box[3]

    ar_h = ar_bottom - ar_top
    la_h = la_bottom - la_top
    stack_h = ar_h + GAP_NAME_RULE + RULE_H + GAP_RULE_LATIN + la_h

    cx = TILE[0] / 2
    top = (TILE[1] - stack_h) / 2

    # Place each element by its inked top, converting back to a baseline.
    d.text((cx, top - ar_top), arabic, font=f_ar, fill=PAPER,
           anchor="ms", direction="rtl", language="ar")

    rule_y = top + ar_h + GAP_NAME_RULE
    d.rounded_rectangle([cx - RULE_W / 2, rule_y, cx + RULE_W / 2, rule_y + RULE_H],
                        radius=RULE_H / 2, fill=BRASS)

    la_baseline = rule_y + RULE_H + GAP_RULE_LATIN - la_top
    x = cx - latin_w / 2
    for ch, w in glyphs:
        d.text((x, la_baseline), ch, font=f_la, fill=PAPER_DIM, anchor="ls")
        x += w + tracking

    return tile


# ---------------------------------------------------------------- main

def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", help="path to an existing tv-logos clone")
    args = ap.parse_args()

    catalog = json.load(open(CATALOG, encoding="utf-8"))
    os.makedirs(OUT, exist_ok=True)

    repo = args.repo
    tmp = None
    if repo is None and LOGO_SOURCES:
        tmp = tempfile.mkdtemp(prefix="tvlogos-")
        repo = os.path.join(tmp, "tv-logos")
        print(f"  cloning {LOGOS_REPO}")
        subprocess.run(["git", "clone", "--depth", "1", "--filter=blob:none",
                        "--no-checkout", LOGOS_REPO, repo], check=True,
                       stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

    real = words = 0
    for c in catalog["channels"]:
        cid = c["id"]
        raw = None
        rel = LOGO_SOURCES.get(cid)
        if rel:
            raw = from_repo(repo, rel) if repo else None
            if not raw:
                raw = fetch_bytes(RAW + rel)
            if not raw:
                print(f"    ! {cid}: {rel} unavailable, falling back to a wordmark")

        if raw:
            tile = logo_tile(raw)
            real += 1
            kind = "logo"
        else:
            tile = wordmark_tile(c["ar"], c["en"])
            words += 1
            kind = "word"

        tile.save(os.path.join(OUT, f"{cid}.webp"), "WEBP", quality=92, method=6)
        print(f"  {kind}  {cid}")

    if tmp:
        subprocess.run(["rm", "-rf", tmp])

    total = sum(os.path.getsize(os.path.join(OUT, f)) for f in os.listdir(OUT))
    print(f"\n  {real} broadcaster logos, {words} wordmarks, {total // 1024} KB total")


if __name__ == "__main__":
    main()
