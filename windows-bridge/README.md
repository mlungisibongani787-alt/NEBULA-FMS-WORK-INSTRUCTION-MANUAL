# RFID SYSTEM — Windows RFD8500 Bridge

This is the preferred direct-to-laptop bridge for the Zebra RFD8500. Zebra's current Windows RFID SDK documentation supports RFD8500 over Bluetooth and documents the `IRfidReader` API for discovery, connection, inventory, tag events, tag locationing, beeper and antenna power configuration.

## Required Zebra SDK files
Download the official Zebra RFID SDK for Windows and copy the SDK DLLs into `vendor/`. The official guide lists these runtime assemblies:

- InTheHand.Net.Personal.dll
- RFIDCommandLib.dll
- Symbol.Extensions.Compatibility.dll
- Symbol.RFID.SDK.Connectivity.Windows.dll
- Symbol.RFID.SDK.Discovery.Windows.dll
- Symbol.RFID.SDK.dll
- Symbol.RFID.SDK.Domain.Reader.dll
- Symbol.RFID.SDK.Domain.Reader.Infrastructure.dll
- Symbol.RFID.SDK.Domain.Reader.Infrastructure.Management.dll
- Symbol.RFID.SDK.IP.dll
- Symbol.RFID.SDK.Logger.dll
- Symbol.RFID.SDK.USB.dll
- HostService.dll

The project references the main SDK assemblies directly; the remaining runtime DLLs must also be beside the compiled executable as specified by Zebra's App.Config.

## Build
1. Pair the RFD8500 in Windows Bluetooth settings.
2. Install Visual Studio 2019/2022 with .NET Framework 4.8 desktop development.
3. Add the Zebra SDK DLLs to `vendor/`.
4. Build `RFIDSystem.WindowsBridge.csproj`.
5. Run the bridge as administrator if Windows blocks the HTTP listener.
6. Open the RFID SYSTEM dashboard locally and set Devices → Bridge URL to `ws://127.0.0.1:8787/rfid/`.

## What the bridge controls
- RFD8500 Bluetooth discovery and connection
- RFID inventory start/stop
- EPC/RSSI/tag-seen event forwarding
- RFD8500 tag locationing and proximity percentage
- Beeper volume
- Antenna transmit power index

Do not publish Zebra proprietary SDK binaries to GitHub unless your Zebra license permits it.
