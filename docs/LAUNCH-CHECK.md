# RFID SYSTEM Launch Check

## Verified in build environment
- Modular route/page architecture
- Shared navigation and hash routing
- Responsive desktop/tablet/mobile CSS
- ASN CSV/XLS/XLSX import path (XLS/XLSX via SheetJS CDN)
- ASN validation and local persistence
- RFID ledger, cycle-count, audit, locate, investigations and reports UI flows
- WebSocket bridge client/server message path
- Android bridge source targets Zebra RFID SDK APIs for RFD8500 Bluetooth, inventory and event reporting
- No UI element claims an RFD8500 is connected until the bridge reports it

## Hardware verification required on the target setup
A physical RFD8500 cannot be radio-tested by this build environment. Final hardware acceptance requires pairing the RFD8500 to the Android host, installing the Zebra SDK AARs, starting the laptop bridge, and performing a live inventory/locate scan.
