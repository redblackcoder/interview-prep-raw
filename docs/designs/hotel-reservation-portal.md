# Hotel reservation portal

Ingest hotel inventory, search by location and dates, and reserve a continuous stay.

## Worked design

Ingest partner availability updates with a version per inventory item. Index names, location and approximate availability for browsing. For booking validate every night in the half-open stay interval against authoritative inventory.

Lock the room-night rows in a deterministic order, decrement availability and insert the booking in one transaction. A failed night rolls back the stay. Deduplicate client retries and propagate committed changes back to search asynchronously.
