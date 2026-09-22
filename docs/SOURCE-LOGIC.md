# Source Logic Applied

## Supplied ASN sample
The supplied spreadsheet contains the columns ASN/Receipt, Line #, Owner, Item, Pack, UOM, Hold Code, LPN, Location, PO / SO, Status, Expected Qty and Received Qty. The receiving importer maps these fields.

## Supplied RFID/ASN integration guide
The supplied integration guide describes a hierarchy of shipment/ASN, container/LPN and item/SGTIN-96 EPC data. It also describes ASN ingestion, LPN-based receiving, SKU discrepancy search and handheld tag locating with audible feedback and relative signal behaviour.

Where the spreadsheet contains only SKU and quantity, the system does not invent EPCs. It waits for real EPCs from the WMS/ASN EPC source or the physical RFD8500 receiving scan.
