const fs = require('fs');
const https = require('https');
const path = require('path');
const { CATEGORY_MAP, COUNTRY_MAP, CHANNEL_NAME_MAP } = require('./mappings');

// Output File
const OUTPUT_FILE = path.join(__dirname, '../playlist.m3u');

// Sources
const SRC_ARABIC_LANG = 'https://iptv-org.github.io/iptv/languages/ara.m3u';
const CATEGORY_SOURCES = [
    { id: 'documentary', url: 'https://iptv-org.github.io/iptv/categories/documentary.m3u' },
    { id: 'movies', url: 'https://iptv-org.github.io/iptv/categories/movies.m3u' },
    { id: 'series', url: 'https://iptv-org.github.io/iptv/categories/series.m3u' },
    { id: 'comedy', url: 'https://iptv-org.github.io/iptv/categories/comedy.m3u' },
    { id: 'classic', url: 'https://iptv-org.github.io/iptv/categories/classic.m3u' },
    { id: 'religious', url: 'https://iptv-org.github.io/iptv/categories/religious.m3u' },
    { id: 'education', url: 'https://iptv-org.github.io/iptv/categories/education.m3u' },
    { id: 'family', url: 'https://iptv-org.github.io/iptv/categories/family.m3u' },
    { id: 'lifestyle', url: 'https://iptv-org.github.io/iptv/categories/lifestyle.m3u' },
    { id: 'news', url: 'https://iptv-org.github.io/iptv/categories/news.m3u' },
    { id: 'kids', url: 'https://iptv-org.github.io/iptv/categories/kids.m3u' },
    { id: 'animation', url: 'https://iptv-org.github.io/iptv/categories/animation.m3u' },
    { id: 'sports', url: 'https://iptv-org.github.io/iptv/categories/sports.m3u' },
    { id: 'auto', url: 'https://iptv-org.github.io/iptv/categories/auto.m3u' },
    { id: 'business', url: 'https://iptv-org.github.io/iptv/categories/business.m3u' },
    { id: 'music', url: 'https://iptv-org.github.io/iptv/categories/music.m3u' }
];

// Exclusion & Filtering
// Added: IR, TR, CY, NG, SE, TM, etc. strictly as requested.
const BLOCKED_REGIONS = [
    '.ir', '.cn', '.ru', '.in', '.pk', '.tr', '.vn', '.th', '.kr', '.jp', '.br', '.es', '.fr', '.it', '.de', '.pl', '.ua', '.id', '.il', '.et', '.er', '.so',
    '.se', '.ng', '.cy', '.tm', '.mr', '.eh', '.dj', '.td', '.ly', '.tn', '.sd', '.dz', '.ma', '.ye', '.lb'
];
const EXCLUDED_KEYWORDS = ['Rotana', 'Fann', 'Watar', 'Radio', 'Audio', 'FM', 'Test', 'Kurd', 'Rudaw', 'NRT', 'Zagros', 'Waar', 'Afarin', 'Mixkurdy', 'Payam', 'Speda', 'NUBAR', 'Rojava', 'Ronahi'];

// Force Include Specific Show Keywords (Override exclusions if matches)
const FORCE_INCLUDE = [
    'Maraya', 'Tash', 'Comedy', 'Classic', 'Zaman', 'Drama', 'Spacetoon', 'MBC', 'Cartoon', 'WLD'
];

// ---- HELPERS ----

async function fetchM3U(url) {
    return new Promise((resolve) => {
        const options = { timeout: 15000 };
        const req = https.get(url, options, (res) => {
            if (res.statusCode !== 200) { res.resume(); return resolve(''); }
            let data = '';
            res.on('data', c => data += c);
            res.on('end', () => resolve(data));
        });
        req.on('timeout', () => { req.destroy(); resolve(''); });
        req.on('error', () => resolve(''));
    });
}

function parseM3U(content) {
    if (!content) return [];
    const lines = content.split('\n');
    const channels = [];
    let currentInf = null;

    lines.forEach(line => {
        line = line.trim();
        if (line.startsWith('#EXTINF:')) {
            currentInf = line;
        } else if (line.startsWith('http') && currentInf) {
            const idMatch = currentInf.match(/tvg-id="([^"]*)"/);
            const logoMatch = currentInf.match(/tvg-logo="([^"]*)"/);
            const groupMatch = currentInf.match(/group-title="([^"]*)"/);
            const nameMatch = currentInf.match(/,([^,]*)$/);

            channels.push({
                inf: currentInf, // Raw INF line (will be rebuilt)
                url: line,
                id: idMatch ? idMatch[1] : '',
                logo: logoMatch ? logoMatch[1] : '',
                group: groupMatch ? groupMatch[1] : '',
                name: nameMatch ? nameMatch[1].trim() : 'Unknown Name',
                origGroup: groupMatch ? groupMatch[1] : ''
            });
            currentInf = null;
        }
    });
    return channels;
}

function normalizeChannelName(name) {
    let cleanName = name.replace(/\s*\(.*?\)/g, '').replace(/\s*\[.*?\]/g, '').trim();
    for (const [key, replacement] of Object.entries(CHANNEL_NAME_MAP)) {
        const regex = new RegExp(`\\b${key}\\b`, 'i');
        if (regex.test(cleanName)) {
            if (cleanName.toLowerCase() === key.toLowerCase()) return replacement;
            cleanName = cleanName.replace(regex, replacement);
            break;
        }
    }
    return cleanName;
}

function getCountryFromId(id) {
    if (!id) return null;
    const parts = id.split('.');
    const tld = parts[parts.length - 1];
    return tld && tld.length === 2 ? tld.toLowerCase() : null;
}

// ---- MAIN LOGIC ----

async function main() {
    console.log('🚀 Starting Professional Playlist Generation...');

    // 1. Fetch Arab Core (Trusted)
    let arabChannels = [];
    console.log('📥 Fetching Arab Countries...');
    for (const code of Object.keys(COUNTRY_MAP)) {
        const url = `https://iptv-org.github.io/iptv/countries/${code}.m3u`;
        const data = await fetchM3U(url);
        const parsed = parseM3U(data);
        parsed.forEach(c => { c._country = code; c._isArab = true; });
        arabChannels = [...arabChannels, ...parsed];
    }
    console.log(`   > Found ${arabChannels.length} Arab Core channels.`);

    // 2. Fetch Arabic Language (Broad)
    console.log('📥 Fetching Arabic Language List...');
    const langData = await fetchM3U(SRC_ARABIC_LANG);
    let langChannels = parseM3U(langData);
    langChannels.forEach(c => { c._isArab = true; c._country = getCountryFromId(c.id); });
    console.log(`   > Found ${langChannels.length} additional Arabic channels.`);

    // 3. Fetch Categories (Global)
    let globalChannels = [];
    console.log('📥 Fetching Global Categories...');
    for (const cat of CATEGORY_SOURCES) {
        const data = await fetchM3U(cat.url);
        const parsed = parseM3U(data);
        parsed.forEach(c => {
            c._catId = cat.id;
            c._country = getCountryFromId(c.id);
        });
        globalChannels = [...globalChannels, ...parsed];
    }
    console.log(`   > Found ${globalChannels.length} Global Category channels.`);

    // 4. Load Custom Channels
    const { CUSTOM_CHANNELS } = require('./custom_channels');
    console.log(`   > Loaded ${CUSTOM_CHANNELS.length} Custom/Manual channels.`);

    // --- PROCESSING & MERGING ---

    // Master List
    const streamMap = new Map();

    const addToMap = (list) => {
        list.forEach(ch => {
            // Filter by Keywords (Rotana, etc.)
            if (EXCLUDED_KEYWORDS.some(k => ch.name.includes(k) || ch.group.includes(k))) return;

            // Filter by Blocked Regions (IR, TR, etc.)
            const idLower = (ch.id || '').toLowerCase();
            if (BLOCKED_REGIONS.some(r => idLower.includes(r))) return;

            // SPECIAL FILTER: Remove "Al Iraqia" (General/Drama/Sports) but KEEP "Al Iraqia News"
            if (ch.name.includes('Al Iraqia') && !ch.name.includes('News')) return;

            if (!streamMap.has(ch.url)) {
                streamMap.set(ch.url, ch);
            } else {
                const existing = streamMap.get(ch.url);
                if (!existing._country && ch._country) existing._country = ch._country;
                if (!existing._catId && ch._catId) existing._catId = ch._catId;
                if (ch._isArab) existing._isArab = true;
            }
        });
    };

    addToMap(arabChannels);
    addToMap(langChannels);
    addToMap(CUSTOM_CHANNELS);

    // For Global channels
    globalChannels.forEach(ch => {
        if (EXCLUDED_KEYWORDS.some(k => ch.name.includes(k) || ch.group.includes(k))) return;

        // Filter by Blocked Regions
        const idLower = (ch.id || '').toLowerCase();
        if (BLOCKED_REGIONS.some(r => idLower.includes(r))) return;

        const cc = ch._country;
        const isArab = COUNTRY_MAP[cc];
        const isWestern = ['us', 'uk', 'ca'].includes(cc);

        if (isArab || isWestern) {
            if (!streamMap.has(ch.url)) {
                streamMap.set(ch.url, ch);
            } else {
                const existing = streamMap.get(ch.url);
                if (!existing._catId && ch._catId) existing._catId = ch._catId; // Tag category
            }
        }
    });

    console.log(`📊 Unique Streams Database: ${streamMap.size} streams.`);

    // --- GENERATING THE FINAL PLAYLIST ENTRIES ---
    let finalEntries = [];

    streamMap.forEach(ch => {
        const cc = ch._country;
        const catId = ch._catId;
        const isArab = ch._isArab || COUNTRY_MAP[cc];

        // Refine Name
        const prettyName = normalizeChannelName(ch.name);

        // 1. Determine Categories assignments
        const assignedCategories = new Set();

        // A. Country Category (If valid Arab country)
        if (COUNTRY_MAP[cc]) {
            assignedCategories.add(COUNTRY_MAP[cc]);
        }

        // B. Genre Category
        if (catId && CATEGORY_MAP[catId]) {
            assignedCategories.add(CATEGORY_MAP[catId]);
        }

        // If no explicit catId, try to guess
        if (!catId) {
            const rawGroup = (ch.origGroup || '').toLowerCase();
            const lowerName = prettyName.toLowerCase();

            if (rawGroup.includes('sport') || lowerName.includes('sport')) assignedCategories.add(CATEGORY_MAP['sports']);
            else if (rawGroup.includes('movie') || rawGroup.includes('cinema') || lowerName.includes('cinema')) assignedCategories.add(CATEGORY_MAP['movies']);
            else if (rawGroup.includes('news')) assignedCategories.add(CATEGORY_MAP['news']);
            else if (rawGroup.includes('kids') || lowerName.includes('cartoon')) assignedCategories.add(CATEGORY_MAP['kids']);
            else if (rawGroup.includes('religion') || rawGroup.includes('islam') || lowerName.includes('quran')) assignedCategories.add(CATEGORY_MAP['religious']);
            else if (lowerName.includes('docu') || rawGroup.includes('docu')) assignedCategories.add(CATEGORY_MAP['documentary']);
            else if (isArab) assignedCategories.add(CATEGORY_MAP['general']);
        }

        assignedCategories.forEach(catName => {
            const newInf = `#EXTINF:-1 tvg-id="${ch.id}" tvg-logo="${ch.logo}" group-title="${catName}",${prettyName}`;

            finalEntries.push({
                inf: newInf,
                url: ch.url,
                group: catName,
                name: prettyName,
                origObj: ch
            });
        });
    });

    console.log(`📝 Generated ${finalEntries.length} playlist entries (including duplicates).`);

    // --- SORTING ---
    const GROUP_ORDER = [
        CATEGORY_MAP['premium'],
        CATEGORY_MAP['sports'],
        CATEGORY_MAP['movies'],
        CATEGORY_MAP['series'],
        CATEGORY_MAP['comedy'],
        CATEGORY_MAP['kids'],
        CATEGORY_MAP['documentary'],
        CATEGORY_MAP['news'],
        CATEGORY_MAP['religious'],
        CATEGORY_MAP['general'],
        COUNTRY_MAP['sa'],
        COUNTRY_MAP['ae'],
        COUNTRY_MAP['eg'],
        COUNTRY_MAP['kw'],
    ];

    finalEntries.sort((a, b) => {
        const idxA = GROUP_ORDER.indexOf(a.group);
        const idxB = GROUP_ORDER.indexOf(b.group);

        if (idxA !== -1 && idxB !== -1) return idxA - idxB;
        if (idxA !== -1) return -1;
        if (idxB !== -1) return 1;
        if (a.group !== b.group) return a.group.localeCompare(b.group);
        return a.name.localeCompare(b.name);
    });

    const content = '#EXTM3U\n' + finalEntries.map(e => `${e.inf}\n${e.url}`).join('\n');
    fs.writeFileSync(OUTPUT_FILE, content);
    console.log(`✅ Playlist saved to: ${OUTPUT_FILE}`);
}

main();
