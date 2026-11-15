# Block-aligned range cache

Serve arbitrary byte ranges using a local cache of fixed-size remote blocks.

## Worked solution

For a nonempty read [a,b), visit block IDs from floor(a/B) through floor((b−1)/B). For each block starting at s, copy the intersection [max(a,s),min(b,s+B)) to the destination beginning at max(a,s)−a.

Store one in-flight future per block using atomic insertion. Its owner fetches the complete block, publishes it only after success and completes the future. Other readers await the same future. Remove failed futures so later reads can retry. Join all copies before returning.
