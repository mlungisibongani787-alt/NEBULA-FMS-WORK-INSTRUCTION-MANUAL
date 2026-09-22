# RFID SYSTEM Android RFD8500 Bridge

This is the production hardware adapter for the Zebra RFD8500. It uses Zebra's Android RFID SDK over Bluetooth and forwards reader status/tag events to the RFID SYSTEM WebSocket bridge server.

## Required vendor files
Place the Zebra-provided AARs in `app/libs/` with these names:
- API3_READER-release.aar
- API3_CMN-release.aar
- API3_INTERFACE-release.aar
- API3_ASCII-release.aar
- API3_TRANSPORT-release.aar

Do not publish vendor SDK binaries unless your Zebra license permits it. Zebra's official Android SDK supports RFD8500 and exposes Bluetooth reader management, inventory and tag location APIs.

## Configure laptop bridge
Change `bridgeUrl` in `MainActivity.java` to the laptop's LAN IP, e.g. `ws://192.168.1.20:8787`. The Android device and laptop must be on the same network.
