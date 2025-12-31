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
const BLOCKED_REGIONS = ['.ir', '.cn', '.ru', '.in', '.pk', '.tr', '.vn', '.th', '.kr', '.jp', '.br', '.es', '.fr', '.it', '.de', '.pl', '.ua', '.id', '.il', '.et', '.er', '.so'];
const EXCLUDED_KEYWORDS = ['Radio', 'Audio', 'FM', 'Test']; // Keep strict "Rotana" ban out for now, handle by logic if needed, but user seems to want quality layout first.

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
    // 1. Clean up " (1080p)", " [Geo-blocked]", etc.
    let cleanName = name.replace(/\s*\(.*?\)/g, '').replace(/\s*\[.*?\]/g, '').trim();

    // 2. Map to Bilingual if exists
    // Iterate specific keys to find partial matches or exact matches?
    // Let's do exact-ish match (case insensitive inclusion)
    for (const [key, replacement] of Object.entries(CHANNEL_NAME_MAP)) {
        // If the key is found standalone in the name
        const regex = new RegExp(`\\b${key}\\b`, 'i');
        if (regex.test(cleanName)) {
            // Check if we should replace the whole name or just part
            // For "MBC 1", replacement is "MBC 1 | ..."
            // If name is "MBC 1 HD", we might want "MBC 1 | ... HD".
            // For simplicity, if we match a major network key, let's try to map it intelligently or just swap.
            // Let's just return the replacement if it's a strong match, otherwise modify.

            // If the key is almost the whole name
            if (cleanName.toLowerCase() === key.toLowerCase()) return replacement;

            // If name contains it, replace the match
            cleanName = cleanName.replace(regex, replacement);
            break; // One replacement per channel usually enough
        }
    }
    return cleanName;
}

function getCountryFromId(id) {
    if (!id) return null;
    const parts = id.split('.');
    const tld = parts[parts.length - 1]; // "sa", "us", etc.
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
    // Filter non-blocked regions
    langChannels = langChannels.filter(c => {
        const cc = getCountryFromId(c.id);
        return !BLOCKED_REGIONS.some(r => c.id.includes(r)) && !COUNTRY_MAP[cc]; // Exclude if already in COUNTRY_MAP (avoid duplicates with step 1?) - Actually Step 1 is by country file, Step 2 is by language file. They overlap. We will dedupe later.
    });
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

    // --- PROCESSING & MERGING ---

    // Master List of unique entries (by URL) to build our "Database" of available streams
    const streamMap = new Map(); // URL -> Channel Object candidates

    const addToMap = (list) => {
        list.forEach(ch => {
            if (EXCLUDED_KEYWORDS.some(k => ch.name.includes(k) || ch.group.includes(k))) return;
            if (!streamMap.has(ch.url)) {
                streamMap.set(ch.url, ch);
            } else {
                // Merge info if needed? e.g. if one source has better ID
                const existing = streamMap.get(ch.url);
                if (!existing._country && ch._country) existing._country = ch._country;
                if (!existing._catId && ch._catId) existing._catId = ch._catId;
                if (ch._isArab) existing._isArab = true;
            }
        });
    };

    addToMap(arabChannels);
    addToMap(langChannels);

    // For Global channels, we only want to add them if they are:
    // A. From an Arab country (missed by other lists?)
    // B. From specific trusted Western regions (US, UK, CA) AND are in our Categories of interest
    globalChannels.forEach(ch => {
        if (EXCLUDED_KEYWORDS.some(k => ch.name.includes(k) || ch.group.includes(k))) return;

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
    // Rule: A stream can be outputted MULTIPLE times with DIFFERENT Group Titles.

    let finalEntries = [];

    streamMap.forEach(ch => {
        const cc = ch._country;
        const catId = ch._catId; // e.g., 'sports', 'movies'
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
        // If we have an explicit category ID from source
        if (catId && CATEGORY_MAP[catId]) {
            assignedCategories.add(CATEGORY_MAP[catId]);
        }

        // If no explicit catId, try to guess from Group Title or Name
        if (!catId) {
            const rawGroup = (ch.origGroup || '').toLowerCase();
            const lowerName = prettyName.toLowerCase();

            if (rawGroup.includes('sport') || lowerName.includes('sport')) assignedCategories.add(CATEGORY_MAP['sports']);
            else if (rawGroup.includes('movie') || rawGroup.includes('cinema') || lowerName.includes('cinema')) assignedCategories.add(CATEGORY_MAP['movies']);
            else if (rawGroup.includes('news')) assignedCategories.add(CATEGORY_MAP['news']);
            else if (rawGroup.includes('kids') || lowerName.includes('cartoon')) assignedCategories.add(CATEGORY_MAP['kids']);
            else if (rawGroup.includes('religion') || rawGroup.includes('islam') || lowerName.includes('quran')) assignedCategories.add(CATEGORY_MAP['religious']);
            else if (lowerName.includes('docu') || rawGroup.includes('docu')) assignedCategories.add(CATEGORY_MAP['documentary']);
            else if (isArab) assignedCategories.add(CATEGORY_MAP['general']); // Fallback for Arab
        }

        // 2. Create an entry for each assigned category
        assignedCategories.forEach(catName => {
            // Rebuild EXTINF line
            // #EXTINF:-1 tvg-id="..." tvg-logo="..." group-title="New Group",Pretty Name
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
    // Priority: 
    // 1. Premium/Sports/Movies/Kids (Genres)
    // 2. Countries (Arab Core)
    // 3. Alphabetical within group

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
        // Then Countries...
        COUNTRY_MAP['sa'], // KSA First
        COUNTRY_MAP['ae'],
        COUNTRY_MAP['eg'],
        COUNTRY_MAP['kw'],
        // Others...
    ];

    finalEntries.sort((a, b) => {
        const idxA = GROUP_ORDER.indexOf(a.group);
        const idxB = GROUP_ORDER.indexOf(b.group);

        // If both are KEY Sort groups
        if (idxA !== -1 && idxB !== -1) return idxA - idxB;

        // If one is key sort, it comes first
        if (idxA !== -1) return -1;
        if (idxB !== -1) return 1;

        // If neither, sort by Group Name string
        if (a.group !== b.group) return a.group.localeCompare(b.group);

        // Internal Sort: Name
        return a.name.localeCompare(b.name);
    });

    // Write File
    const content = '#EXTM3U\n' + finalEntries.map(e => `${e.inf}\n${e.url}`).join('\n');
    fs.writeFileSync(OUTPUT_FILE, content);
    console.log(`✅ Playlist saved to: ${OUTPUT_FILE}`);
}

main();
