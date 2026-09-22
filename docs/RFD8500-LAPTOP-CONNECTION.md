# RFD8500 → Windows Laptop Connection

The RFD8500 can be paired to a Windows PC and controlled through Zebra's Windows RFID SDK. Zebra's current Windows developer guide explicitly lists RFD8500 as Bluetooth-supported and documents discovery, connection, inventory tag events and tag locationing.

## Physical pairing
1. Turn on the RFD8500.
2. Press its Bluetooth button for about one second so it becomes discoverable.
3. In Windows Bluetooth / Devices, choose the RFD8500.
4. Complete pairing as directed by the RFD8500 guide.

## Software path
RFD8500 → Windows Bluetooth → RFID SYSTEM Windows Bridge → WebSocket → RFID SYSTEM dashboard.

## Dashboard URL
When the bridge is running, set:

`ws://127.0.0.1:8787/rfid/`

## Important
The dashboard must never display a physical reader as connected until the bridge reports a real connection. If the reader is not paired, the Devices page shows Offline and no RFID inventory is generated.
