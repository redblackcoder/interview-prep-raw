# Top shared articles

Find the most shared articles over several rolling time windows.

## Worked design

Partition events by article ID and count by article and minute. As a window advances, add the newest bucket and subtract the expired one from per-article totals. Rank these totals at query time or maintain an ordered index.

Merge the local top k from every article shard to obtain the global top k with a deterministic tie-break. Checkpoint buckets with source offsets and recover by replay. Minute resolution defines when displayed results change.
