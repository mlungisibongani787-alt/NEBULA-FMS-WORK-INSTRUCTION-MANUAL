# RFID SYSTEM

Production-oriented modular RFID inventory system for the Zebra RFD8500.

## Run the web system
From this folder:

`python -m http.server 8080`

Open `http://localhost:8080`

## Run the bridge server
From `bridge-server`:

`npm install`
`npm start`

Default bridge: `ws://127.0.0.1:8787`

## RFD8500 hardware
The RFD8500 connects to an Android host through Zebra's RFID SDK. The Android bridge forwards real reader events to the web dashboard. Zebra's official SDK documents RFD8500 Bluetooth connection, inventory and tag location.

The vendor AAR binaries are intentionally not included in this repository. Obtain them from Zebra and place them in `android-bridge/app/libs/` using the filenames in that folder's README.

## ASN
The supplied ASN sample contains: ASN/Receipt, Line #, Owner, Item, Pack, UOM, Hold Code, LPN, Location, PO/SO, Status, Expected Qty and Received Qty. The Receiving module maps these fields and creates item-level EPC records from the expected quantity until real EPC data is received from the WMS/reader integration.

## Direct laptop + RFD8500
For a Windows laptop, use `windows-bridge/`. Zebra's Windows RFID SDK explicitly supports the RFD8500 over Bluetooth. Pair the sled in Windows first, run the bridge, then connect the dashboard to `ws://127.0.0.1:8787/rfid/`.
