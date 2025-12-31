const fs = require('fs');
const path = require('path');
const https = require('https');
const http = require('http');

const PLAYLIST_FILE = path.join(__dirname, '../playlist.m3u');

// Strict stream checker
function checkStream(url) {
    return new Promise((resolve) => {
        const urlParsed = new URL(url);
        const options = {
            method: 'HEAD', // We will try HEAD first, if fast and good, great.
            // If we wanted to be super strict about content, we'd do GET with range.
            // For now, let's enforce a strict timeout.
            timeout: 2500, // 2.5s Strict Timeout
            headers: {
                'User-Agent': 'VLC/3.0.18 LibVLC/3.0.18' // Mimic a real player
            }
        };

        const req = (urlParsed.protocol === 'https:' ? https : http).request(url, options, (res) => {
            if (res.statusCode >= 200 && res.statusCode < 400) {
                // Determine if it looks like a media stream
                // content-type is often 'application/vnd.apple.mpegurl' or 'video/...'
                // application/octet-stream is also common.
                // We'll trust the status code if it returns quickly.
                resolve({ ok: true, code: res.statusCode });
            } else {
                resolve({ ok: false, code: res.statusCode });
            }
        });

        req.on('timeout', () => {
            req.destroy();
            resolve({ ok: false, code: 'TIMEOUT' });
        });

        req.on('error', (err) => {
            resolve({ ok: false, code: err.code || 'ERROR' });
        });

        req.end();
    });
}

function parseM3U(content) {
    const lines = content.split('\n');
    const channels = [];
    let currentName = '';

    lines.forEach(line => {
        line = line.trim();
        if (line.startsWith('#EXTINF:')) {
            const nameMatch = line.match(/,([^,]*)$/);
            currentName = nameMatch ? nameMatch[1].trim() : "Unknown";
        } else if (line.startsWith('http')) {
            if (currentName) {
                channels.push({ name: currentName, url: line });
                currentName = '';
            }
        }
    });
    return channels;
}

// Improved Parser to keep original lines
function parseM3U_Full(content) {
    const lines = content.split('\n');
    const channels = [];
    let currentInf = '';

    lines.forEach(line => {
        line = line.trim();
        if (line.startsWith('#EXTINF:')) {
            currentInf = line;
        } else if (line.startsWith('http')) {
            if (currentInf) {
                const nameMatch = currentInf.match(/,([^,]*)$/);
                const name = nameMatch ? nameMatch[1].trim() : "Unknown";
                channels.push({ name, url: line, inf: currentInf });
                currentInf = '';
            }
        }
    });
    return channels;
}

// Overwrite main with better logic
main = async function () {
    if (!fs.existsSync(PLAYLIST_FILE)) {
        console.error('Playlist file not found!');
        return;
    }

    const content = fs.readFileSync(PLAYLIST_FILE, 'utf-8');
    const channels = parseM3U_Full(content); // Use better parser

    console.log(`Validating ${channels.length} channels...`);

    const validChannels = [];
    const BATCH_SIZE = 10;

    for (let i = 0; i < channels.length; i += BATCH_SIZE) {
        const batch = channels.slice(i, i + BATCH_SIZE);
        const results = await Promise.all(batch.map(async ch => {
            const status = await checkStream(ch.url);
            return { ...ch, status };
        }));

        results.forEach(res => {
            if (res.status.ok) {
                console.log(`[PASS] ${res.name}`);
                validChannels.push(res);
            } else {
                console.log(`[FAIL] ${res.name} (${res.status.code}) - REMOVED`);
            }
        });
    }

    // Write back
    let newContent = '#EXTM3U\n';
    validChannels.forEach(ch => {
        newContent += `${ch.inf}\n${ch.url}\n`;
    });

    fs.writeFileSync(PLAYLIST_FILE, newContent);
    console.log(`\nPlaylist updated! Removed ${channels.length - validChannels.length} broken streams.`);
    console.log(`Final Clean Count: ${validChannels.length}`);
};

main();
