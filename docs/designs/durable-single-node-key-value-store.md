# Durable single-node key-value store

Keep concurrent point reads and writes recoverable after a process or machine crash.

## Worked design

Serialize writes through a log writer. Encode length-delimited records with checksums so recovery can reject a torn trailing record. Force the log, apply records to memory in log order, then release waiting callers. A reader can use the concurrent map.

Group commit lets multiple waiting writers share a force. Use a time bound as well as a size threshold so an isolated write completes. Recovery loads a checkpoint, replays later complete records and drops the incomplete tail.
