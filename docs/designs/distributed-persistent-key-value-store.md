# Distributed persistent key-value store

Provide durable point reads and partition-local range scans across storage nodes.

## Worked design

Hash partition keys into movable shards. Within a shard store rows ordered by sort key so a range scan stays local. Append updates to the shard log, replicate to a quorum and acknowledge after commit. Apply committed entries to the local read index.

A leader change resumes from the committed prefix. A stale follower read trades freshness for latency. Transfer a snapshot and subsequent log entries before moving ownership to a replacement replica.
