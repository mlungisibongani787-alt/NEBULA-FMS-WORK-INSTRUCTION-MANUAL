# RFID SYSTEM Bridge Server

This lightweight WebSocket relay lets the web dashboard and the Android RFD8500 bridge communicate over the same LAN.

## Start
1. Install Node.js 20+
2. `npm install`
3. `npm start`
4. Set the dashboard bridge URL to `ws://LAPTOP-IP:8787`

For a public HTTPS deployment, use a TLS reverse proxy and set the dashboard URL to `wss://...`.
