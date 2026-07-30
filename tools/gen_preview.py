#!/usr/bin/env python3
"""
Renders docs/preview-phone.png and docs/preview-tv.png.

These are LAYOUT MOCKUPS, not screenshots. They are drawn from the same inputs
the app uses — the real palette values from Theme.kt, the bundled IBM Plex Sans
Arabic, the real artwork tiles from assets/logos, the real channel names from
channels.json, and the spacing from Metrics — so they show what the design
actually looks like. They cannot show live video, focus animation, or anything
the platform draws.

Usage:  python3 tools/gen_preview.py
"""
from __future__ import annotations

import json
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CATALOG = os.path.join(ROOT, "channels", "channels.json")
LOGOS = os.path.join(ROOT, "app", "src", "main", "assets", "logos")
FONTS = os.path.join(ROOT, "app", "src", "main", "res", "font")
OUT = os.path.join(ROOT, "docs")

# Straight from Palette in Theme.kt.
INK = (10, 11, 13, 255)
INK_RAISED = (19, 21, 25, 255)
PLATE = (27, 30, 35, 255)
PLATE_HI = (35, 39, 46, 255)
HAIRLINE = (38, 42, 49, 255)
HAIRLINE_HI = (58, 65, 74, 255)
PAPER = (243, 241, 236, 255)
PAPER_DIM = (154, 161, 169, 255)
PAPER_FAINT = (106, 114, 122, 255)
BRASS = (200, 164, 93, 255)
BRASS_BRIGHT = (231, 200, 146, 255)
SIGNAL = (217, 80, 63, 255)

SS = 2  # supersample


def font(name: str, px: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(os.path.join(FONTS, name), px * SS)


def ar(d, xy, text, f, fill, anchor="ra"):
    d.text(xy, text, font=f, fill=fill, anchor=anchor, direction="rtl", language="ar")


def tile(d, img, box, logo_id, focused=False, radius=10):
    x0, y0, x1, y1 = box
    d.rounded_rectangle(box, radius=radius * SS,
                        fill=PLATE_HI if focused else PLATE,
                        outline=BRASS if focused else HAIRLINE,
                        width=(2 if focused else 1) * SS)
    path = os.path.join(LOGOS, f"{logo_id}.webp")
    if not os.path.exists(path):
        return
    art = Image.open(path).convert("RGBA")
    pad_x, pad_y = 14 * SS, 12 * SS
    bw, bh = (x1 - x0) - pad_x * 2, (y1 - y0) - pad_y * 2
    s = min(bw / art.width, bh / art.height)
    art = art.resize((max(1, int(art.width * s)), max(1, int(art.height * s))), Image.LANCZOS)
    img.alpha_composite(art, (int(x0 + ((x1 - x0) - art.width) / 2),
                              int(y0 + ((y1 - y0) - art.height) / 2)))


def section_header(d, x_right, x_left, y, title, f_sec, f_meta, count):
    """Label at the leading (right) edge, rule running to the trailing edge."""
    ar(d, (x_right, y), title, f_sec, PAPER, anchor="rm")
    w = d.textlength(title, font=f_sec, direction="rtl", language="ar")
    d.text((x_left, y), str(count), font=f_meta, fill=PAPER_FAINT, anchor="lm")
    cw = d.textlength(str(count), font=f_meta)
    d.rectangle([x_left + cw + 14 * SS, y, x_right - w - 14 * SS, y + SS], fill=HAIRLINE)


def phone(catalog) -> Image.Image:
    W, H = 390 * SS, 844 * SS
    img = Image.new("RGBA", (W, H), INK)
    d = ImageDraw.Draw(img)

    f_display = font("plex_arabic_bold.ttf", 30)
    f_title = font("plex_arabic_semibold.ttf", 20)
    f_sec = font("plex_arabic_semibold.ttf", 15)
    f_label = font("plex_arabic_medium.ttf", 14)
    f_meta = font("plex_arabic_regular.ttf", 12)
    f_over = font("plex_arabic_medium.ttf", 11)

    gut = 20 * SS
    right, left = W - gut, gut
    y = 58 * SS

    # Header: wordmark at the leading edge, channel count opposite.
    ar(d, (right, y), "شاشة", f_display, PAPER)
    d.text((left, y + 22 * SS), str(len(catalog["channels"])), font=f_meta,
           fill=PAPER_FAINT, anchor="la")
    ar(d, (right, y + 40 * SS), "قنوات عربية مباشرة", f_meta, PAPER_FAINT)
    y += 84 * SS

    # Resume panel.
    ph = 100 * SS
    d.rounded_rectangle([left, y, right, y + ph], radius=10 * SS,
                        fill=INK_RAISED, outline=HAIRLINE, width=SS)
    art_w, art_h = 128 * SS, 80 * SS
    tile(d, img, (right - 12 * SS - art_w, y + 10 * SS, right - 12 * SS, y + 10 * SS + art_h),
         "aljazeera", radius=8)
    tx = right - 12 * SS - art_w - 16 * SS
    # LIVE chip: dot plus label in a dark chip, never a band.
    chip_w = 62 * SS
    d.rounded_rectangle([tx - chip_w, y + 18 * SS, tx, y + 36 * SS], radius=4 * SS,
                        fill=(10, 11, 13, 200))
    d.ellipse([tx - 14 * SS, y + 24 * SS, tx - 8 * SS, y + 30 * SS], fill=SIGNAL)
    ar(d, (tx - 20 * SS, y + 27 * SS), "مباشر", f_over, PAPER, anchor="rm")
    ar(d, (tx, y + 55 * SS), "الجزيرة", f_title, PAPER, anchor="rm")
    ar(d, (tx, y + 76 * SS), "أخبار  ·  قطر", f_meta, PAPER_FAINT, anchor="rm")
    # Play affordance: an outline, not a filled block.
    bx0, by0 = left + 10 * SS, y + ph / 2 - 22 * SS
    d.rounded_rectangle([bx0, by0, bx0 + 44 * SS, by0 + 44 * SS], radius=9 * SS,
                        outline=HAIRLINE_HI, width=int(1.5 * SS))
    d.polygon([(bx0 + 17 * SS, by0 + 14 * SS), (bx0 + 30 * SS, by0 + 22 * SS),
               (bx0 + 17 * SS, by0 + 30 * SS)], fill=PAPER)
    y += ph + 32 * SS

    tw, th = 150 * SS, int(150 * SS / 1.6)
    gap = 12 * SS
    for label, cat, focus_first in (("أخبار", "news", True), ("عام", "general", False),
                                    ("أطفال", "kids", False)):
        chans = [c for c in catalog["channels"] if c["cat"] == cat]
        section_header(d, right, left, y, label, f_sec, f_meta, len(chans))
        y += 24 * SS
        x = right
        for i, c in enumerate(chans):
            if x - tw < left - tw:
                break
            tile(d, img, (x - tw, y, x, y + th), c["id"], focused=(focus_first and i == 0))
            x -= tw + gap
            if x < 0:
                break
        y += th + 34 * SS

    # Bottom bar.
    bh = 74 * SS
    d.rectangle([0, H - bh, W, H], fill=INK_RAISED)
    d.rectangle([0, H - bh, W, H - bh + SS], fill=HAIRLINE)
    tabs = ["الرئيسية", "القنوات", "بحث", "المفضلة", "الإعدادات"]
    step = W / len(tabs)
    for i, t in enumerate(tabs):
        cx = W - (i + 0.5) * step
        active = i == 0
        col = BRASS if active else PAPER_FAINT
        d.rectangle([cx - 8 * SS, H - bh + 20 * SS, cx + 8 * SS, H - bh + 22 * SS], fill=col)
        ar(d, (cx, H - bh + 44 * SS), t, f_meta, col, anchor="mm")
        if active:
            d.rectangle([cx - 8 * SS, H - 16 * SS, cx + 8 * SS, H - 14 * SS], fill=BRASS)

    return img.resize((W // SS, H // SS), Image.LANCZOS)


def tv(catalog) -> Image.Image:
    W, H = 1280 * SS, 720 * SS
    img = Image.new("RGBA", (W, H), INK)
    d = ImageDraw.Draw(img)

    f_title = font("plex_arabic_semibold.ttf", 22)
    f_sec = font("plex_arabic_semibold.ttf", 17)
    f_label = font("plex_arabic_medium.ttf", 16)
    f_meta = font("plex_arabic_regular.ttf", 14)

    # Nav rail on the leading (right) edge, expanded as it is when focused.
    rail = 232 * SS
    d.rectangle([W - rail, 0, W, H], fill=INK_RAISED)
    d.rectangle([W - rail - SS, 0, W - rail, H], fill=HAIRLINE)
    ar(d, (W - 22 * SS, 40 * SS), "شاشة", font("plex_arabic_bold.ttf", 34), PAPER, anchor="rm")
    ar(d, (W - 22 * SS, 68 * SS), "قنوات عربية مباشرة", f_meta, PAPER_FAINT, anchor="rm")

    items = ["الرئيسية", "القنوات", "بحث", "المفضلة", "الإعدادات"]
    y = 130 * SS
    for i, t in enumerate(items):
        selected, focused = i == 1, i == 1
        if focused:
            d.rounded_rectangle([W - rail + 14 * SS, y, W - 14 * SS, y + 50 * SS],
                                radius=9 * SS, fill=PLATE_HI, outline=BRASS, width=SS)
        if selected:
            d.rounded_rectangle([W - 28 * SS, y + 15 * SS, W - 25 * SS, y + 35 * SS],
                                radius=2 * SS, fill=BRASS)
        ar(d, (W - 46 * SS, y + 25 * SS), t,
           f_label, BRASS_BRIGHT if focused else PAPER_DIM, anchor="rm")
        y += 56 * SS

    gut = 48 * SS
    right, left = W - rail - gut, gut
    y = 44 * SS
    chans = catalog["channels"]
    section_header(d, right, left, y, "كل القنوات", f_sec, f_meta, len(chans))
    y += 34 * SS

    # Neutral outline chips: no filled colour pills.
    chips = ["الكل", "أخبار", "عام", "وثائقية", "قنوات دينية", "أطفال"]
    x = right
    for i, c in enumerate(chips):
        w = d.textlength(c, font=f_label, direction="rtl", language="ar") + 30 * SS
        on = i == 0
        d.rounded_rectangle([x - w, y, x, y + 38 * SS], radius=7 * SS,
                            outline=BRASS if on else HAIRLINE,
                            width=int((1.5 if on else 1) * SS))
        ar(d, (x - 15 * SS, y + 19 * SS), c, f_label,
           BRASS_BRIGHT if on else PAPER_DIM, anchor="rm")
        x -= w + 9 * SS
    y += 62 * SS

    tw, th, gap = 208 * SS, 130 * SS, 20 * SS
    cols = int((right - left + gap) // (tw + gap))
    i = 0
    while y + th < H - 20 * SS and i < len(chans):
        x = right
        for _ in range(cols):
            if i >= len(chans):
                break
            tile(d, img, (x - tw, y, x, y + th), chans[i]["id"],
                 focused=(i == 2), radius=12)
            x -= tw + gap
            i += 1
        y += th + gap

    return img.resize((W // SS, H // SS), Image.LANCZOS)


def main() -> None:
    catalog = json.load(open(CATALOG, encoding="utf-8"))
    os.makedirs(OUT, exist_ok=True)
    phone(catalog).convert("RGB").save(os.path.join(OUT, "preview-phone.png"))
    tv(catalog).convert("RGB").save(os.path.join(OUT, "preview-tv.png"))
    for n in ("preview-phone.png", "preview-tv.png"):
        p = os.path.join(OUT, n)
        print(f"  docs/{n}  {os.path.getsize(p) // 1024} KB")


if __name__ == "__main__":
    main()
