# Bid distribution

Distribute changing bid values to several independent serving clusters.

## Worked design

Encode changed bid values with advertiser key and source version. Batch them into a log and give each cluster its own consumer group. A consumer accepts only newer versions and checkpoints after applying the batch.

Bootstrap from a snapshot labeled with its source offset, then replay later deltas. Measure consumer lag and elapsed time since production because fresh offsets alone cannot detect a stalled producer.
