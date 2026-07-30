#!/usr/bin/env python3
"""
Renders the Shasha brand assets that have to exist as bitmaps:

  * legacy launcher icons (mipmap-*/ic_launcher.png + ic_launcher_round.png)
    for devices below API 26, which cannot use the adaptive vector icon
  * the Android TV home-screen banner (drawable-xhdpi/tv_banner.png), which
    Leanback requires as a 320x180 raster

Everything is drawn from the same primitives as ic_launcher_foreground.xml:
the letter ش reduced to three teeth on a connecting stroke with three dots
above. Single brass tone on ink. No gradients, no glow, no bevels.

Usage:  python3 tools/gen_brand_assets.py
"""
from __future__ import annotations

import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")

INK = (14, 16, 19, 255)
BRASS = (200, 164, 93, 255)
LINE = (39, 43, 49, 255)

# Supersampling factor: draw big, downscale with LANCZOS for clean edges.
SS = 8


def rounded_bar(d: ImageDraw.ImageDraw, x0, y0, x1, y1, r, fill):
    d.rounded_rectangle([x0, y0, x1, y1], radius=r, fill=fill)


def draw_mark(d: ImageDraw.ImageDraw, cx: float, cy: float, size: float, color=BRASS):
    """Draw the ش mark centred on (cx, cy) fitting a `size` box.

    Geometry mirrors app/src/main/res/drawable/ic_launcher_foreground.xml.
    """
    # Work in the same 108-unit space as the vector drawable, then map.
    u = size / 108.0

    def X(v):
        return cx + (v - 55.5) * u

    def Y(v):
        # drawn content spans y 18..74 in the vector; centre that band on cy
        return cy + (v - 46.0) * u

    # connecting stroke
    rounded_bar(d, X(29), Y(66), X(83), Y(74), 4 * u, color)
    # three teeth
    for tx in (30, 51, 72):
        rounded_bar(d, X(tx), Y(44), X(tx + 10), Y(70), 4 * u, color)
    # three dots
    for dx, dy in ((56, 23), (43, 37), (69, 37)):
        r = 5 * u
        d.ellipse([X(dx) - r, Y(dy) - r, X(dx) + r, Y(dy) + r], fill=color)


def launcher_icon(px: int, round_icon: bool) -> Image.Image:
    n = px * SS
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if round_icon:
        d.ellipse([0, 0, n - 1, n - 1], fill=INK)
    else:
        d.rounded_rectangle([0, 0, n - 1, n - 1], radius=int(n * 0.22), fill=INK)
    draw_mark(d, n / 2, n / 2, n * 0.68)
    return img.resize((px, px), Image.LANCZOS)


def pick_font(size: int) -> ImageFont.FreeTypeFont | None:
    for name in ("plex_arabic_semibold.ttf", "plex_arabic_bold.ttf",
                 "plex_arabic_regular.ttf"):
        p = os.path.join(FONT_DIR, name)
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return None


def tv_banner() -> Image.Image:
    """320x180 Leanback banner: mark, hairline rule, Arabic + Latin wordmark."""
    w, h = 320 * SS, 180 * SS
    img = Image.new("RGBA", (w, h), INK)
    d = ImageDraw.Draw(img)

    # mark on the right (RTL reading order), rule, wordmark to its left
    draw_mark(d, w * 0.78, h * 0.5, h * 0.50)
    d.rectangle([w * 0.615, h * 0.28, w * 0.615 + SS, h * 0.72], fill=LINE)

    ar = pick_font(int(46 * SS))
    la = pick_font(int(15 * SS))
    if ar:
        # `direction`/`language` require raqm; harfbuzz shapes the Arabic joins.
        d.text((w * 0.545, h * 0.46), "شاشة", font=ar, fill=BRASS,
               anchor="rm", direction="rtl", language="ar")
    if la:
        d.text((w * 0.545, h * 0.66), "A R A B I C   L I V E   T V", font=la,
               fill=(152, 160, 168, 255), anchor="rm")

    return img.resize((320, 180), Image.LANCZOS)


def main():
    # Legacy launcher icons. mdpi=48 through xxxhdpi=192.
    for bucket, px in (("mdpi", 48), ("hdpi", 72), ("xhdpi", 96),
                       ("xxhdpi", 144), ("xxxhdpi", 192)):
        out = os.path.join(RES, f"mipmap-{bucket}")
        os.makedirs(out, exist_ok=True)
        launcher_icon(px, False).save(os.path.join(out, "ic_launcher.png"))
        launcher_icon(px, True).save(os.path.join(out, "ic_launcher_round.png"))
        print(f"  mipmap-{bucket}/ic_launcher.png  {px}x{px}")

    out = os.path.join(RES, "drawable-xhdpi")
    os.makedirs(out, exist_ok=True)
    tv_banner().save(os.path.join(out, "tv_banner.png"))
    print("  drawable-xhdpi/tv_banner.png  320x180")


if __name__ == "__main__":
    main()
