# Lakehouse file storage

Build an immutable-file service with a mutable namespace on ordinary storage nodes.

## Worked design

Resolve a logical path to a versioned list of extents. For each extent, reserve offsets in large append-only storage files on nodes in different failure domains. Clients write in parallel and collect acknowledgments. Commit the namespace entry atomically once the durability threshold is met.

Translate read ranges to extent slices and fall back to another copy on failure. Reclaim abandoned reservations and repair under-replicated extents from healthy replicas. Packing extents into larger files reduces per-file metadata.
