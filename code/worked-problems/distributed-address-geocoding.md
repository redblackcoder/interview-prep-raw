# Distributed address geocoding

Replace unreliable coordinates in a large file while sharing geocoder results across duplicate addresses.

## Worked solution

Tag each row with its original ID and derive a structured key from street, city, region and postal code. Shuffle by key. Each partition groups identical addresses and calls the geocoder once per distinct key in a successful run. Persist coordinates by key and join them to the original IDs.

Bound concurrent calls and checkpoint successful keys before retrying failed partitions. If a worker dies between the external call and saving its result, an absolute one-call guarantee requires idempotency or result lookup at the provider.
