# RFD8500 connection design

Hardware target: **Zebra RFD8500 RFID reader sled**.

The RFD8500 documentation describes Bluetooth SPP/custom transports and Zebra's RFID SDK/ZETI path. Zebra's current Android RFID SDK documentation continues to list the RFD8500 as a supported handheld reader and documents inventory and tag-location operations.

For this reason the project separates:

1. **Web dashboard** — GitHub Pages/static web UI, inventory workflows, audit logic, reporting UI and browser diagnostics.
2. **Android bridge** — Zebra SDK connection layer for actual RFD8500 RFID operations.

The web dashboard must not falsely report that a normal desktop browser has production RFID control. Browser Bluetooth support varies and does not replace Zebra's RFID SDK transport.

## RFD8500 functions mapped by this project

- Reader connection / reconnection
- RFID inventory
- Tag location / Geiger-style UI
- Target tag isolation
- Relative signal/proximity display
- Beeper/volume UI
- RFID/barcode mode concept
- Two-reader dashboard slots
- Offline bridge architecture

## Zebra references

- Zebra RFID Android developer guide: https://techdocs.zebra.com/dcs/rfid/android/4-0-0-9/tutorials/rfiddevguide/
- Zebra RFD8500 developer guide: https://prod-www.zebra.com/content/dam/support-dam/en/documentation/unrestricted/guide/software/rfd8500-dg-en.pdf
- Zebra RFID Android SDK: https://techdocs.zebra.com/dcs/rfid/android/2-0-5-275/guide/about/
