# Document sharing and sync

Share binary files with users and groups and synchronize changes across devices.

## Worked design

Upload bytes to temporary storage and verify integrity before committing a file record. Store owner, group grants and comment/read permissions with metadata. A directory change appends a monotonic entry to its scoped change feed.

Devices track cursors and lazily fetch bytes when opened. Recheck effective permissions on downloads and sync results. Revocation blocks subsequent reads and invalidates cached access grants.
