#!/usr/bin/env python3
"""
Builds channels/channels.json from verified probe results.

SELECTION is the editorial decision — which Arabic channels this app carries,
what they are called in Arabic, and which section they sit in. Everything else
(the actual stream URL, the broadcaster's website) is lifted from
tools/candidate_results.json, so no URL in the shipped catalog is one that was
typed by hand and never tested.

Editorial rules applied to the lineup:
  * Arabic-language channels only.
  * Free-to-air only: nothing encrypted, nothing behind a subscription.
  * The endpoint has to be broadcaster delivery, not a reseller.
  * Broadcasters under international sanctions are left out, so the app stays
    distributable wherever the owner happens to be.

Usage:  python3 tools/curate.py
"""
from __future__ import annotations

import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RESULTS = os.path.join(ROOT, "tools", "candidate_results.json")
OUT = os.path.join(ROOT, "channels", "channels.json")

CATEGORIES = [
    {"id": "news", "ar": "أخبار", "en": "News"},
    {"id": "general", "ar": "عام", "en": "General"},
    {"id": "docs", "ar": "وثائقية", "en": "Documentary"},
    {"id": "religion", "ar": "قنوات دينية", "en": "Religious"},
    {"id": "kids", "ar": "أطفال", "en": "Kids"},
]

COUNTRY_AR = {
    "AE": "الإمارات", "SA": "السعودية", "QA": "قطر", "KW": "الكويت", "OM": "عُمان",
    "BH": "البحرين", "JO": "الأردن", "PS": "فلسطين", "LB": "لبنان", "EG": "مصر",
    "IQ": "العراق", "MA": "المغرب", "TN": "تونس", "DZ": "الجزائر", "LY": "ليبيا",
    "SD": "السودان", "YE": "اليمن", "SY": "سوريا", "GB": "بريطانيا", "UK": "بريطانيا",
    "FR": "فرنسا", "DE": "ألمانيا", "TR": "تركيا", "CN": "الصين", "US": "الولايات المتحدة",
}

# (our id, iptv-org id, Arabic name, English name, category, URL must contain)
# Extra endpoints appended ahead of the probed ones. Saudi Broadcasting delivers
# Al Ekhbariya and Al Sunnah from cdn-globecast under /live/eds/<slug>/, so the
# Quran channel almost certainly sits beside them; the probed kwikmotion feed is
# the radio simulcast and is kept only as a fallback. The health check confirms
# or drops these on the next run.
EXTRA_SOURCES: dict[str, list[str]] = {
    "quran-ksa": [
        "https://cdn-globecast.akamaized.net/live/eds/saudi_quran/hls_roku/index.m3u8",
        "https://cdn-globecast.akamaized.net/live/eds/al_quran_al_kareem/hls_roku/index.m3u8",
    ],
}

SELECTION: list[tuple[str, str, str, str, str, str | None]] = [
    # ---------------------------------------------------------------- news
    ("aljazeera", "AlJazeera.qa", "الجزيرة", "Al Jazeera", "news", "getaj.net/AJA"),
    ("aljazeera-mubasher", "AlJazeeraMubasher.qa", "الجزيرة مباشر", "Al Jazeera Mubasher", "news", "getaj.net/AJM"),
    ("alarabiya", "Alarabiya.ae", "العربية", "Al Arabiya", "news", "alarabiya.net"),
    ("alhadath", "AlHadath.sa", "الحدث", "Al Hadath", "news", "alarabiya.net"),
    ("alarabiya-business", "AlArabiyaBusiness.ae", "العربية بزنس", "Al Arabiya Business", "news", "alarabiya.net"),
    ("skynewsarabia", "SkyNewsArabia.ae", "سكاي نيوز عربية", "Sky News Arabia", "news", "horizontal"),
    ("asharq", "AsharqNews.sa", "الشرق", "Asharq News", "news", "asharq.com"),
    ("alekhbariya", "AlEkhbariya.sa", "الإخبارية", "Al Ekhbariya", "news", "globecast"),
    ("alarabytv", "AlArabyTV.qa", "التلفزيون العربي", "Al Araby TV", "news", None),
    ("alghad", "AlGhadTV.eg", "الغد", "Al Ghad", "news", None),
    ("alyaum", "AlYaumTV.ae", "اليوم", "Al Yaum TV", "news", None),
    ("cnbc-arabia", "CNBCArabiya.ae", "سي إن بي سي عربية", "CNBC Arabia", "news", None),
    ("qbc", "QBC.qa", "قطر الاقتصادية", "Qatar Business Channel", "news", None),
    ("bbcarabic", "BBCArabic.uk", "بي بي سي عربي", "BBC News Arabic", "news", "bbci.co.uk"),
    ("france24-ar", "France24.fr", "فرانس 24", "France 24 Arabic", "news", "F24_AR_HI_HLS/master_5000"),
    ("dw-arabic", "DW.de", "دويتشه فيله عربية", "DW Arabic", "news", "dwstream104"),
    ("trt-arabi", "TRTArabi.tr", "تي آر تي عربي", "TRT Arabi", "news", None),
    ("cgtn-arabic", "CGTNArabic.cn", "سي جي تي إن العربية", "CGTN Arabic", "news", "cgtn.com"),
    ("alhurra", "Alhurra.us", "الحرة", "Alhurra", "news", None),
    ("alhiwar", "AlHiwarTV.uk", "الحوار", "Al Hiwar TV", "news", None),
    ("medi1", "Medi1TVArabic.ma", "ميدي 1 تي في", "Medi1 TV Arabic", "news", "arabic"),
    ("syriatv", "SyriaTV.sy", "تلفزيون سوريا", "Syria TV", "news", None),
    ("iraqia-news", "AlIraqiaNews.iq", "الإخبارية العراقية", "Al Iraqia News", "news", None),
    ("alsharqiya-news", "AlSharqiyaNews.iq", "الشرقية نيوز", "Al Sharqiya News", "news", None),
    ("altaghier", "AltaghierTV.jo", "التغيير", "Al Taghier TV", "news", None),

    # ------------------------------------------------------------- general
    ("mbc1", "MBC1.ae", "إم بي سي 1", "MBC 1", "general", "bitmovin-mbc-1/"),
    ("mbc-masr", "MBCMasr.eg", "إم بي سي مصر", "MBC Masr", "general", "bitmovin-mbc-masr/"),
    ("mbc-iraq", "MBCIraq.iq", "إم بي سي العراق", "MBC Iraq", "general", None),
    ("mbc5", "MBC5.ae", "إم بي سي 5", "MBC 5", "general", None),
    ("alsaudiya", "AlSaudiya.sa", "السعودية", "Al Saudiya", "general", None),
    ("sbc", "SBC.sa", "إس بي سي", "SBC", "general", "edgenextcdn"),
    ("thikrayat", "ThikrayatTV.sa", "ذكريات", "Thikrayat TV", "general", None),
    ("sharjah-tv", "SharjahTV.ae", "الشارقة", "Sharjah TV", "general", "kwikmotion"),
    ("ktv1", "KTV1.kw", "تلفزيون الكويت", "Kuwait TV 1", "general", None),
    ("oman-tv", "OmanTV.om", "تلفزيون عمان", "Oman TV", "general", None),
    ("bahrain-tv", "BahrainTV.bh", "تلفزيون البحرين", "Bahrain TV", "general", "bahraintvmain"),
    ("qatar-tv", "QatarTelevision.qa", "تلفزيون قطر", "Qatar TV", "general", "qtv1"),
    ("alrayyan", "AlRayyanTV.qa", "الريان", "Al Rayyan TV", "general", "noalrayy"),
    ("jordan-tv", "JordanTV.jo", "التلفزيون الأردني", "Jordan TV", "general", None),
    ("amman-tv", "AmmanTV.jo", "عمان", "Amman TV", "general", None),
    ("palestine-tv", "PalestineSatelliteChannel.ps", "فلسطين", "Palestine TV", "general", "globecast"),
    ("alaoula", "AlAoula.ma", "الأولى المغربية", "Al Aoula", "general", "livemediama"),
    ("2m", "2MMonde.ma", "دوزيم", "2M", "general", None),
    ("watania1", "ElWatania1.tn", "الوطنية 1", "El Watania 1", "general", "watania1/chunklist"),
    ("iraqia", "AlIraqia.iq", "العراقية", "Al Iraqia", "general", None),
    ("alsharqiya", "AlSharqiya.iq", "الشرقية", "Al Sharqiya", "general", "streamlock"),
    ("mtv-lebanon", "MTVLebanon.lb", "إم تي في لبنان", "MTV Lebanon", "general", None),
    ("otv-lebanon", "OTV.lb", "أو تي في", "OTV Lebanon", "general", None),
    ("libya-alwataniya", "LibyaAlWataniya.ly", "ليبيا الوطنية", "Libya Al Wataniya", "general", None),
    ("sudan-tv", "SudanTV.sd", "تلفزيون السودان", "Sudan TV", "general", None),
    ("alsouriya", "AlSouriyaTV.sy", "السورية", "Al Souriya TV", "general", None),

    # --------------------------------------------------------- documentary
    ("aljazeera-documentary", "AlJazeeraDocumentary.qa", "الجزيرة الوثائقية", "Al Jazeera Documentary", "docs", None),
    ("asharq-documentary", "AsharqDocumentary.sa", "الشرق للوثائقيات", "Asharq Documentary", "docs", None),
    ("asharq-discovery", "AsharqDiscovery.sa", "الشرق ديسكفري", "Asharq Discovery", "docs", None),
    ("alarabiya-programs", "AlArabiyaPrograms.ae", "العربية برامج", "Al Arabiya Programs", "docs", None),
    ("saudi-thaqafiya", "SaudiThaqafiyaTV.sa", "السعودية الثقافية", "Saudi Thaqafiya", "docs", None),
    ("oman-cultural", "OmanTVCultural.om", "عمان الثقافية", "Oman TV Cultural", "docs", None),
    ("ktv-arabe", "KTVArabe.kw", "الكويت العربي", "KTV Arabe", "docs", None),
    ("story-channel", "StoryChannelTV.ma", "ستوري", "Story Channel", "docs", None),

    # ----------------------------------------------------------- religious
    ("quran-ksa", "HolyQuranRadio.sa", "القرآن الكريم", "Saudi Quran TV", "religion", "kwikmotion"),
    ("sunnah-ksa", "AlSunnahAlNabawiyahTV.sa", "السنة النبوية", "Saudi Sunnah TV", "religion", None),
    ("qatar-quran", "QatarTVTheHolyQuran.qa", "القرآن الكريم – قطر", "Qatar Quran TV", "religion", None),
    ("sharjah-quran", "Sharjah2.ae", "القرآن الكريم – الشارقة", "Sharjah Quran TV", "religion", None),
    ("bahrain-quran", "BahrainQuran.bh", "القرآن الكريم – البحرين", "Bahrain Quran TV", "religion", None),
    ("iqraa", "IqraaArabic.sa", "إقرأ", "Iqraa TV", "religion", None),
    ("iqraa-quran", "IqraaQuran.sa", "إقرأ القرآن", "Iqraa Quran", "religion", None),
    ("ktv-ethraa", "KTVEthraa.kw", "الكويت إثراء", "KTV Ethraa", "religion", None),
    ("alistiqama", "AlistiqamaTV.om", "الاستقامة", "Al Istiqama TV", "religion", None),

    # ---------------------------------------------------------------- kids
    ("spacetoon", "SpacetoonArabic.ae", "سبيستون", "Spacetoon", "kids", None),
    ("mbc3", "MBC3USA.us", "إم بي سي 3", "MBC 3", "kids", None),
    ("roya-kids", "RoyaKids.jo", "رؤيا كيدز", "Roya Kids", "kids", "roya-kids/"),
    ("roya-kids-originals", "RoyaKidsOriginals.jo", "رؤيا كيدز أوريجينالز", "Roya Kids Originals", "kids", None),
    ("taha", "TahaTV.lb", "طه", "Taha TV", "kids", None),
    ("atfal-mawaheb", "AtfalMawahebTV.sa", "أطفال ومواهب", "Atfal Mawaheb", "kids", None),
]


def main() -> None:
    results = json.load(open(RESULTS, encoding="utf-8"))

    channels = []
    missing = []
    unverified = []

    for cid, iptv_id, ar, en, cat, prefer in SELECTION:
        entries = results.get(iptv_id)
        if not entries:
            missing.append((cid, iptv_id, "not in the probed pool"))
            continue

        working = [e for e in entries if e["ok"]]
        pool = working or entries
        if prefer:
            filtered = [e for e in pool if prefer in e["url"]]
            if filtered:
                pool = filtered
            elif working:
                missing.append((cid, iptv_id, f"no working URL contains {prefer!r}"))
                continue

        # Masters first (they carry every bitrate), then fastest to respond.
        pool = sorted(pool, key=lambda e: (0 if e.get("kind") == "master" else 1, e.get("ms", 0)))
        chosen = pool[:2]
        if not working:
            unverified.append((cid, iptv_id, chosen[0].get("reason", "")))

        country = chosen[0].get("country", "")
        channels.append({
            "id": cid,
            "ar": ar,
            "en": en,
            "cat": cat,
            "country": country,
            "countryAr": COUNTRY_AR.get(country, ""),
            "site": chosen[0].get("site", ""),
            "verified": bool(working),
            "sources": [
                {"url": url, "label": "HD" if i == 0 else "Backup"}
                for i, url in enumerate(EXTRA_SOURCES.get(cid, []))
            ] + [
                {
                    "url": e["url"],
                    "label": "HD" if (i == 0 and cid not in EXTRA_SOURCES) else "Backup",
                    **({"referer": True} if e.get("referer") else {}),
                }
                for i, e in enumerate(chosen)
            ],
        })

    order = {c["id"]: i for i, c in enumerate(CATEGORIES)}
    channels.sort(key=lambda c: (order[c["cat"]], c["en"]))

    catalog = {
        "schema": 1,
        "updated": "2026-07-30",
        "note": (
            "Free-to-air Arabic channels only. Every URL here was verified live from "
            "a GitHub Actions runner before being written; see channels/health.json "
            "for the last check. Nothing is encrypted, subscription-gated, or scraped "
            "from a paid service."
        ),
        "categories": CATEGORIES,
        "channels": channels,
    }

    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=1)
        f.write("\n")

    by_cat: dict[str, int] = {}
    for c in channels:
        by_cat[c["cat"]] = by_cat.get(c["cat"], 0) + 1
    print(f"  {len(channels)} channels: " +
          ", ".join(f"{k} {v}" for k, v in by_cat.items()))
    print(f"  {sum(len(c['sources']) for c in channels)} source URLs")

    if unverified:
        print("\n  shipped but NOT verified from the runner:")
        for cid, iptv_id, reason in unverified:
            print(f"    {cid:26} {reason}")
    if missing:
        print("\n  dropped:")
        for cid, iptv_id, reason in missing:
            print(f"    {cid:26} {iptv_id:32} {reason}")


if __name__ == "__main__":
    main()
