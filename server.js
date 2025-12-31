const http = require('http');
const fs = require('fs');
const path = require('path');
const os = require('os');

const PORT = 3000;
const FILE_PATH = path.join(__dirname, 'playlist.m3u');

function getLocalIP() {
    const interfaces = os.networkInterfaces();
    for (const name of Object.keys(interfaces)) {
        for (const iface of interfaces[name]) {
            if (iface.family === 'IPv4' && !iface.internal) {
                return iface.address;
            }
        }
    }
    return 'localhost';
}

const server = http.createServer((req, res) => {
    // Determine content type
    const ext = path.extname(req.url);

    // Serve playlist.m3u
    if (req.url === '/playlist.m3u' || req.url === '/') {
        fs.readFile(FILE_PATH, (err, data) => {
            if (err) {
                res.writeHead(500);
                res.end('Error loading playlist.');
                return;
            }
            res.writeHead(200, {
                'Content-Type': 'application/x-mpegurl', // Standard M3U MIME type
                'Access-Control-Allow-Origin': '*', // Allow any player to access (CORS)
                'Cache-Control': 'no-cache' // Ensure players always get the latest version
            });
            res.end(data);
        });
    } else {
        res.writeHead(404);
        res.end('Not found. Use /playlist.m3u');
    }
});

server.listen(PORT, () => {
    const ip = getLocalIP();
    console.log('---------------------------------------------------');
    console.log('🚀 Playlist Server is Running!');
    console.log(`📡 Local Link:   http://localhost:${PORT}/playlist.m3u`);
    console.log(`🌍 Network Link: http://${ip}:${PORT}/playlist.m3u`);
    console.log('---------------------------------------------------');
    console.log('👉 Put the "Network Link" into your IPTV Player.');
    console.log('👉 When you re-run generate_playlist.js, the link updates automatically.');
});
