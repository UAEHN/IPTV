// Hand-curated channels.
//
// These entries bypass the keyword / blocked-region filters in
// generate_playlist.js (see the `_custom` flag), because they are vetted one by
// one instead of coming from an automated upstream list. They are still subject
// to the liveness check in validate_streams.js.
//
// Most of the block below is Gulf and Levant channels whose streams are missing
// from the upstream iptv-org lists: state broadcasters (Abu Dhabi Media, Dubai
// Media Incorporated, Sharjah Media, Kuwait Ministry of Information,
// JRTV/Al Mamlaka, Tele Liban) plus a few private Gulf channels.

const CUSTOM_CHANNELS = [
    // ---------------------------------------------------------------
    // 🇦🇪 UAE — Sharjah (Sharjah Broadcasting Authority)
    // ---------------------------------------------------------------
    {
        url: 'https://ythls.armelin.one/channel/UCn8lMRYDANs_1yAL3iuw7_g.m3u8',
        id: 'SharjahQuran.ae',
        logo: 'https://i.imgur.com/Gkx79Bq.png',
        name: 'Sharjah Quran TV',
        _country: 'ae',
        _catId: 'religious'
    },
    {
        url: 'https://linkastream.co/headless?url=https://youtube.com/channel/UCn8lMRYDANs_1yAL3iuw7_g/live',
        id: 'SharjahQuran.ae',
        logo: 'https://i.imgur.com/Gkx79Bq.png',
        name: 'Sharjah Quran TV - Backup',
        _country: 'ae',
        _catId: 'religious'
    },
    {
        // Name is expanded to Arabic by CHANNEL_NAME_MAP ('Sharjah Sports').
        url: 'https://svs.itworkscdn.net/smc4sportslive/smc4.smil/playlist.m3u8',
        id: 'SharjahSports.ae',
        logo: 'https://i.imgur.com/CsEElsJ.png',
        name: 'Sharjah Sports',
        _country: 'ae',
        _catId: 'sports'
    },

    // ---------------------------------------------------------------
    // 🇦🇪 UAE — Abu Dhabi Media (government of Abu Dhabi)
    // ---------------------------------------------------------------
    {
        url: 'https://admdn2.cdn.mangomolo.com/adtv/smil:adtv.stream.smil/chunklist.m3u8',
        id: 'AbuDhabiTV.ae',
        logo: 'https://i.imgur.com/8DAriPs.png',
        name: 'Abu Dhabi TV | قناة أبوظبي',
        _country: 'ae',
        _catId: 'general'
    },
    {
        url: 'https://admdn3.cdn.mangomolo.com/emarat/smil:emarat.stream.smil/playlist.m3u8',
        id: 'AlEmarat.ae',
        logo: 'https://www.emarattv.ae/public/img/branding/logo.png',
        name: 'Al Emarat TV | قناة الإمارات',
        _country: 'ae',
        _catId: 'general'
    },
    {
        // Name is expanded to Arabic by CHANNEL_NAME_MAP ('Abu Dhabi Sports').
        url: 'https://vo-live.cdb.cdn.orange.com/Content/Channel/AbuDhabiSportsChannel1/HLS/index.m3u8',
        id: 'AbuDhabiSports1.ae',
        logo: 'https://i.imgur.com/6BVWk8z.png',
        name: 'Abu Dhabi Sports 1',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://vo-live.cdb.cdn.orange.com/Content/Channel/AbuDhabiSportsChannel2/HLS/index.m3u8',
        id: 'AbuDhabiSports2.ae',
        logo: 'https://i.imgur.com/y1I2jFK.png',
        name: 'Abu Dhabi Sports 2',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://admdn3.cdn.mangomolo.com/adsports3/smil:adsports3.stream.smil/playlist.m3u8',
        id: 'AbuDhabiSports3.ae',
        logo: 'https://i.imgur.com/LfBQkeq.png',
        name: 'Abu Dhabi Sports 3',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://admdn4ta.cdn.mgmlcdn.com/adsports4/smil:adsports4.stream.smil/playlist.m3u8',
        id: 'AbuDhabiSports4.ae',
        logo: 'https://i.imgur.com/65IINZo.png',
        name: 'Abu Dhabi Sports 4',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://admdn1.cdn.mangomolo.com/yastv/smil:yastv.stream.smil/playlist.m3u8',
        id: 'YasTV.ae',
        logo: 'https://i.imgur.com/CSHQhP4.png',
        name: 'Yas TV | ياس',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        // 'Majid' on its own is swapped for the bilingual name by CHANNEL_NAME_MAP.
        url: 'https://admdn4.cdn.mangomolo.com/majid/smil:majid.stream.smil/playlist.m3u8',
        id: 'Majid.ae',
        logo: 'https://i.imgur.com/TzOKMMy.png',
        name: 'Majid',
        _country: 'ae',
        _catId: 'kids'
    },
    {
        url: 'https://admdn2.cdn.mangomolo.com/nagtv/smil:nagtv.stream.smil/playlist.m3u8',
        id: 'NationalGeographicAbuDhabi.ae',
        logo: 'https://i.imgur.com/fNA00VF.png',
        name: 'National Geographic Abu Dhabi',
        _country: 'ae',
        _catId: 'documentary'
    },
    {
        url: 'https://vo-live.cdb.cdn.orange.com/Content/Channel/Baynounah/HLS/index.m3u8',
        id: 'BaynounahTV.ae',
        logo: 'https://i.imgur.com/uVmHT97.png',
        name: 'Baynounah TV | بينونة',
        _country: 'ae',
        _catId: 'general'
    },

    // ---------------------------------------------------------------
    // 🇦🇪 UAE — Dubai Media Incorporated (government of Dubai)
    // ---------------------------------------------------------------
    {
        url: 'https://dmisxthvll.cdn.mgmlcdn.com/dubaitvht/smil:dubaitv.stream.smil/playlist.m3u8',
        id: 'DubaiTV.ae',
        logo: 'https://i.imgur.com/UMmbyHv.png',
        name: 'Dubai TV | قناة دبي',
        _country: 'ae',
        _catId: 'general'
    },
    {
        url: 'https://dminnvll.cdn.mangomolo.com/dubaione/smil:dubaione.stream.smil/playlist.m3u8',
        id: 'DubaiOne.ae',
        logo: 'https://i.imgur.com/Dj16oKL.png',
        name: 'Dubai One | دبي وان',
        _country: 'ae',
        _catId: 'general'
    },
    {
        url: 'https://dmieigthvll.cdn.mgmlcdn.com/samadubaiht/smil:samadubai.stream.smil/playlist.m3u8',
        id: 'SamaDubai.ae',
        logo: 'https://i.imgur.com/F5g3ymW.png',
        name: 'Sama Dubai | سما دبي',
        _country: 'ae',
        _catId: 'general'
    },
    {
        // 'Zaman' is expanded to '| زمان' by CHANNEL_NAME_MAP.
        url: 'https://dmiffthvll.cdn.mangomolo.com/dubaizaman/smil:dubaizaman.stream.smil/playlist.m3u8',
        id: 'DubaiZaman.ae',
        logo: 'https://i.imgur.com/5Q5qMzP.png',
        name: 'Dubai Zaman',
        _country: 'ae',
        _catId: 'classic'
    },
    {
        // Name is expanded to Arabic by CHANNEL_NAME_MAP ('Dubai Sports').
        url: 'https://dmitnthfr.cdn.mgmlcdn.com/dubaisports/smil:dubaisports.stream.smil/chunklist.m3u8',
        id: 'DubaiSports1.ae',
        logo: 'https://i.imgur.com/Poxw8lG.png',
        name: 'Dubai Sports 1',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://dmitwlvvll.cdn.mangomolo.com/dubaisportshd/smil:dubaisportshd.smil/index.m3u8',
        id: 'DubaiSports2.ae',
        logo: 'https://i.imgur.com/PMJ7Zmo.png',
        name: 'Dubai Sports 2',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://dmitwlvvll.cdn.mangomolo.com/dubaisportshd5/smil:dubaisportshd5.smil/index.m3u8',
        id: 'DubaiSports3.ae',
        logo: 'https://i.imgur.com/U0A8Gex.png',
        name: 'Dubai Sports 3',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://dmisvthvll.cdn.mgmlcdn.com/events/smil:events.stream.smil/playlist.m3u8',
        id: 'DubaiRacing.ae',
        logo: 'https://i.imgur.com/CJ1dagP.png',
        name: 'Dubai Racing 1 | دبي ريسينج 1',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://dmithrvll.cdn.mangomolo.com/dubairacing/smil:dubairacing.smil/playlist.m3u8',
        id: 'DubaiRacing2.ae',
        logo: 'https://i.imgur.com/H2TTn7t.png',
        name: 'Dubai Racing 2 | دبي ريسينج 2',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://dmithrvll.cdn.mangomolo.com/dubaimubasher/smil:dubaimubasher.smil/playlist.m3u8',
        id: 'DubaiRacing3.ae',
        logo: 'https://i.imgur.com/Vg4fxmc.png',
        name: 'Dubai Mubasher | دبي مباشر',
        _country: 'ae',
        _catId: 'sports'
    },
    {
        url: 'https://dmiffthvll.cdn.mangomolo.com/noordubaitv/smil:noordubaitv.smil/playlist.m3u8',
        id: 'NoorDubai.ae',
        logo: 'https://i.imgur.com/Nwl8Y7K.png',
        name: 'Noor Dubai | نور دبي',
        _country: 'ae',
        _catId: 'religious'
    },

    // ---------------------------------------------------------------
    // 🇦🇪 UAE — other Emirati broadcasters
    // ---------------------------------------------------------------
    {
        url: 'https://shls-live-ak.akamaized.net/out/v1/dfbdea4c1bf149629764e58c6ff314c8/index.m3u8',
        id: 'AlAanTV.ae',
        logo: 'https://i.imgur.com/Jl9Uw8N.png',
        name: 'Al Aan TV | الآن',
        _country: 'ae',
        _catId: 'news'
    },
    {
        url: 'http://live.aldafrah.tv:1935/live/myStream3/playlist.m3u8',
        id: 'AlDafrahTV.ae',
        logo: 'https://i.imgur.com/sOMOe9t.png',
        name: 'Al Dafrah TV | الظفرة',
        _country: 'ae',
        _catId: 'general'
    },
    {
        // MENA feed, distributed out of Dubai.
        url: 'https://shls-cartoon-net-prod-dub.shahid.net/out/v1/dc4aa87372374325a66be458f29eab0f/index.m3u8',
        id: 'CartoonNetworkArabic.ae',
        logo: 'https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Cartoon_Network_Arabic_logo.png/960px-Cartoon_Network_Arabic_logo.png',
        name: 'Cartoon Network Arabic',
        _country: 'ae',
        _catId: 'kids'
    },

    // ---------------------------------------------------------------
    // 🇸🇦 Saudi Arabia
    // ---------------------------------------------------------------
    {
        url: 'https://mn-nl.mncdn.com/khalij/khalij/playlist.m3u8',
        id: 'AlKhalijTV.sa',
        logo: 'https://i.imgur.com/Knd2lAQ.png',
        name: 'Al Khalij TV | الخليج',
        _country: 'sa',
        _catId: 'general'
    },
    {
        // Name is expanded to Arabic by CHANNEL_NAME_MAP ('MBC Max').
        url: 'https://shls-mbcmax-prod-dub.shahid.net/out/v1/13815a7cda864c249a88c38e66a2e653/index.m3u8',
        id: 'MBCMax.sa',
        logo: 'https://i.imgur.com/A02CptP.png',
        name: 'MBC Max',
        _country: 'sa',
        _catId: 'movies'
    },
    {
        url: 'https://59355e6c6462b.streamlock.net/quran/ngrp:ahul_all/playlist.m3u8',
        id: 'AhlAlquranTV.sa',
        logo: 'https://i.imgur.com/xeQvi2z.png',
        name: 'Ahl Al Quran TV',
        _country: 'sa',
        _catId: 'religious'
    },

    // ---------------------------------------------------------------
    // 🇰🇼 Kuwait
    // ---------------------------------------------------------------
    {
        // Broadcasts the sessions of the National Assembly (مجلس الأمة).
        url: 'https://svs.itworkscdn.net/ktvalmajlislive/kalmajlis.smil/playlist.m3u8',
        id: 'KTVAlMajlis.kw',
        logo: 'https://i.imgur.com/AVEyGig.png',
        name: 'KTV Al Majlis | تلفزيون الكويت المجلس',
        _country: 'kw',
        _catId: 'news'
    },
    {
        url: 'https://svs.itworkscdn.net/alraitvlive/alraitv.smil/playlist.m3u8',
        id: 'AlraiTV.kw',
        logo: 'https://i.imgur.com/ZohZFuq.png',
        name: 'Alrai TV | الراي',
        _country: 'kw',
        _catId: 'general'
    },

    // ---------------------------------------------------------------
    // 🇯🇴 Jordan — public broadcasting
    // ---------------------------------------------------------------
    {
        url: 'https://almamlka-live.ercdn.net/almamlka/almamlka.m3u8',
        id: 'AlMamlakaTV.jo',
        logo: 'https://i.imgur.com/UeTseMV.png',
        name: 'Al Mamlaka TV | المملكة',
        _country: 'jo',
        _catId: 'news'
    },

    // ---------------------------------------------------------------
    // 🇱🇧 Lebanon — Tele Liban (state broadcaster)
    //
    // '.lb' is in BLOCKED_REGIONS, so the only Lebanese channel here is the
    // state one, added explicitly.
    // ---------------------------------------------------------------
    {
        url: 'https://cdn.catiacast.video/abr/ed8f807e2548db4507d2a6f4ba0c4a06/playlist.m3u8',
        id: 'TeleLiban.lb',
        logo: 'https://i.imgur.com/5VPYxJi.png',
        name: 'Tele Liban | تلفزيون لبنان',
        _country: 'lb',
        _catId: 'general'
    }
];

// Every entry here is curated, so tag them all rather than repeating the flag.
CUSTOM_CHANNELS.forEach(ch => {
    ch._isArab = true;
    ch._custom = true;
    ch.group = ch.group || '';
});

module.exports = { CUSTOM_CHANNELS };
