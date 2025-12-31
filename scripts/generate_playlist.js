const fs = require('fs');
const https = require('https');
const path = require('path');

// Target Arab Countries (Excluding Maghreb)
const COUNTRY_CODES = [
    'sa', 'ae', 'eg', 'kw', 'qa', 'bh', 'om', 'iq', 'jo', 'lb', 'ps', 'sy', 'ye', 'sd'
];

// Sources
const SRC_ARABIC_LANG = 'https://iptv-org.github.io/iptv/languages/ara.m3u';
const SRC_DOC = 'https://iptv-org.github.io/iptv/categories/documentary.m3u';
const SRC_MOVIES = 'https://iptv-org.github.io/iptv/categories/movies.m3u';
const SRC_SERIES = 'https://iptv-org.github.io/iptv/categories/series.m3u';
const SRC_COMEDY = 'https://iptv-org.github.io/iptv/categories/comedy.m3u';
const SRC_CLASSIC = 'https://iptv-org.github.io/iptv/categories/classic.m3u';
const SRC_RELIGIOUS = 'https://iptv-org.github.io/iptv/categories/religious.m3u';
const SRC_EDUCATION = 'https://iptv-org.github.io/iptv/categories/education.m3u';
const SRC_FAMILY = 'https://iptv-org.github.io/iptv/categories/family.m3u';
const SRC_LIFESTYLE = 'https://iptv-org.github.io/iptv/categories/lifestyle.m3u';
const SRC_NEWS = 'https://iptv-org.github.io/iptv/categories/news.m3u';
const SRC_KIDS = 'https://iptv-org.github.io/iptv/categories/kids.m3u';
const SRC_ANIMATION = 'https://iptv-org.github.io/iptv/categories/animation.m3u';
const SRC_SPORTS = 'https://iptv-org.github.io/iptv/categories/sports.m3u';
const SRC_AUTO = 'https://iptv-org.github.io/iptv/categories/auto.m3u';
const SRC_BUSINESS = 'https://iptv-org.github.io/iptv/categories/business.m3u';

const OUTPUT_FILE = path.join(__dirname, '../playlist.m3u');

// Explicit exclusions (Global)
const EXCLUDED_KEYWORDS = [
    'Rotana', 'Fann', 'Watar', 'Radio', 'Audio', 'FM', 'Test'
];

// Force Include Specific Show Keywords (Override exclusions if matches)
const FORCE_INCLUDE = [
    'Maraya', 'Tash', 'Comedy', 'Classic', 'Zaman', 'Drama', 'Spacetoon', 'MBC', 'Cartoon'
];

// Block content from these regions (unless it's Arab)
// This strictly targets "Non-Arab Asian" and other unwanted regions to keep the list clean.
const BLOCKED_REGIONS = [
    '.ir', '.cn', '.ru', '.in', '.pk', '.tr', '.vn', '.th', '.kr', '.jp', '.br', '.es', '.fr', '.it', '.de', '.pl', '.ua', '.id', '.il', '.et', '.er', '.so'
];

async function fetchM3U(url) {
    return new Promise((resolve, reject) => {
        const options = { timeout: 20000 };
        const req = https.get(url, options, (res) => {
            if (res.statusCode !== 200) {
                res.resume();
                resolve('');
                return;
            }
            let data = '';
            res.on('data', (chunk) => data += chunk);
            res.on('end', () => resolve(data));
        });

        req.on('timeout', () => {
            req.destroy();
            resolve('');
        });

        req.on('error', (err) => resolve(''));
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
        } else if (line.startsWith('http')) {
            if (currentInf) {
                const idMatch = currentInf.match(/tvg-id="([^"]*)"/);
                const logoMatch = currentInf.match(/tvg-logo="([^"]*)"/);
                const groupMatch = currentInf.match(/group-title="([^"]*)"/);
                const nameMatch = currentInf.match(/,([^,]*)$/);

                const channel = {
                    inf: currentInf,
                    url: line,
                    id: idMatch ? idMatch[1] : '',
                    logo: logoMatch ? logoMatch[1] : '',
                    group: groupMatch ? groupMatch[1] : 'Undefined',
                    name: nameMatch ? nameMatch[1].trim() : 'Unknown Channel'
                };
                channels.push(channel);
                currentInf = null;
            }
        }
    });
    return channels;
}

async function main() {
    try {
        console.log('--- STARTING GENERATION (ULTIMATE DIG: ALL CATEGORIES) ---');
        let allChannels = [];

        // 1. Fetch Arab Countries (Trusted Core)
        console.log('1. Fetching Specific Arab Country Lists (Trusted)...');
        for (const code of COUNTRY_CODES) {
            const url = `https://iptv-org.github.io/iptv/countries/${code}.m3u`;
            process.stdout.write(`   > [${code.toUpperCase()}] Requesting... `);
            const content = await fetchM3U(url);
            if (!content) {
                console.log('EMPTY');
            } else {
                const channels = parseM3U(content);
                console.log(`OK (${channels.length})`);
                channels.forEach(c => c._source = 'arab_core');
                allChannels = [...allChannels, ...channels];
            }
        }

        // 2. Fetch Arabic Language List (Broad Search)
        console.log('2. Deep Search: Arabic Language List...');
        const araContent = await fetchM3U(SRC_ARABIC_LANG);
        const araChannels = parseM3U(araContent);
        console.log(`   > Fetched ${araChannels.length} raw Arabic channels.`);

        // Filter Language list immediately
        const cleanAra = araChannels.filter(ch => {
            const id = (ch.id || '').toLowerCase();
            if (BLOCKED_REGIONS.some(r => id.includes(r))) return false; // Block Iran, Israel, etc.
            return true;
        });
        console.log(`   > Kept ${cleanAra.length} potentially valid Arabic channels.`);
        cleanAra.forEach(c => c._source = 'arab_lang');
        allChannels = [...allChannels, ...cleanAra];

        // 3. Fetch Western/Global Categories + SHOWS Specific
        const categories = [
            { name: 'News', url: SRC_NEWS },
            { name: 'Kids', url: SRC_KIDS },
            { name: 'Animation', url: SRC_ANIMATION },
            { name: 'Sports', url: SRC_SPORTS },
            { name: 'Comedy', url: SRC_COMEDY },
            { name: 'Classic', url: SRC_CLASSIC },
            { name: 'Documentaries', url: SRC_DOC },
            { name: 'Movies', url: SRC_MOVIES },
            { name: 'Series', url: SRC_SERIES },
            { name: 'Family', url: SRC_FAMILY },
            { name: 'Lifestyle', url: SRC_LIFESTYLE },
            { name: 'Auto', url: SRC_AUTO },
            { name: 'Business', url: SRC_BUSINESS }
        ];

        console.log('3. Deep Search: Global Categories & Shows...');
        for (const cat of categories) {
            console.log(`   > Fetching ${cat.name}...`);
            const content = await fetchM3U(cat.url);
            const channels = parseM3U(content);
            console.log(`     -> Found ${channels.length}.`);

            // Intelligent Filter for Global Content
            const cleanCat = channels.filter(ch => {
                const id = (ch.id || '').toLowerCase();
                const name = (ch.name || '').toLowerCase();

                // FORCE INCLUDE Check
                if (FORCE_INCLUDE.some(k => name.includes(k.toLowerCase()))) return true;

                // A) Keep if it matches our Arab Country List
                if (COUNTRY_CODES.some(c => id.includes(`.${c}`))) return true;

                // B) Keep if it is "Western Premium" (US/UK/CA)
                if (id.includes('.us') || id.includes('.uk') || id.includes('.ca')) return true;

                // C) Block defaults
                return false;
            });
            console.log(`     -> Kept ${cleanCat.length}.`);

            cleanCat.forEach(c => c._source = 'global_cat');
            allChannels = [...allChannels, ...cleanCat];
        }

        console.log(`4. Total Candidates Breakdown: ${allChannels.length}`);

        // Deduplicate by URL
        const seenUrls = new Set();
        allChannels = allChannels.filter(ch => {
            if (seenUrls.has(ch.url)) return false;
            seenUrls.add(ch.url);
            return true;
        });

        // Final Safety Filter
        console.log('5. Applying Final Safety & Keyword Filters...');
        const filteredChannels = allChannels.filter(ch => {
            const name = (ch.name || '').toLowerCase();
            const group = (ch.group || '').toLowerCase();
            const id = (ch.id || '').toLowerCase();

            // 0. Force Include Overrides (If it matches "Maraya" or "Tash", keep it even if it says "Rotana" maybe? User said "No Rotana" earlier, but asked for "Tash". Let's assume User Priority: Content > Network Name)
            // But let's be safe: If it strictly says "Rotana", we still ban it unless it's specifically "Tash" or "Maraya" channel (unlikely to be named Rotana Tash).
            // Actually, keep Rotana ban strict for "Rotana Cinema" etc.

            // 1. Keyword Exclusions
            if (EXCLUDED_KEYWORDS.some(k => name.includes(k.toLowerCase()) || group.includes(k.toLowerCase()))) {
                // EXCEPTION: If the user wants specific shows, and they happen to be here. 
                // But generally "Rotana" channels are channels, not shows. "Maraya" channel is fine.
                return false;
            }
            if (group === 'radio' || group === 'audio') return false;

            // 2. Region Safety (Double check in case something slipped through)
            if (BLOCKED_REGIONS.some(r => id.includes(r))) return false;

            // 3. Name Safety (Explicit "Iran" check again)
            if (name.includes('iran') || name.includes('persian') || name.includes('al alam')) return false;

            return true;
        });

        // Sorting Strategy: Smart Category Grouping
        // 1. Arab Core (General/National)
        // 2. Kids/Cartoons (High priority for families)
        // 3. Movies & Series & Comedy
        // 4. Documentaries & Education
        // 5. Sports
        // 6. News
        // 7. Others
        filteredChannels.sort((a, b) => {
            const scoreA = getSmartSortScore(a);
            const scoreB = getSmartSortScore(b);
            if (scoreA !== scoreB) return scoreA - scoreB;
            return a.name.localeCompare(b.name);
        });

        console.log(`   > Final count: ${filteredChannels.length}`);

        // Generate output
        let output = '#EXTM3U\n';
        filteredChannels.forEach(ch => {
            output += `${ch.inf}\n${ch.url}\n`;
        });

        fs.writeFileSync(OUTPUT_FILE, output);
        console.log(`6. Playlist saved to: ${OUTPUT_FILE}`);
        console.log('--- GENERATION COMPLETE ---');

    } catch (error) {
        console.error('!!! ERROR !!!:', error);
    }
}

function getSmartSortScore(ch) {
    const group = (ch.group || '').toLowerCase();
    const name = (ch.name || '').toLowerCase();

    // 1. Kids/Animation
    if (group.includes('kids') || group.includes('animation') || name.includes('cartoon') || name.includes('spacetoon')) return 1;

    // 2. Movies & Series & Comedy & Classics (Entertainment)
    if (group.includes('movie') || group.includes('series') || group.includes('comedy') || group.includes('classic') || group.includes('drama')) return 2;

    // 3. Documentaries & Education
    if (group.includes('documentary') || group.includes('education') || group.includes('science')) return 3;

    // 4. Sports
    if (group.includes('sport')) return 4;

    // 5. News
    if (group.includes('news')) return 5;

    // 6. Arab Core (General/Family when not matching above)
    if (ch._source === 'arab_core' || ch._source === 'arab_lang') return 6;

    // 7. Others (Lifestyle, Auto, Business...)
    return 7;
}

main();
