# Notification delivery

Deliver social events to an inbox and through users' enabled channels.

## Worked design

Resolve event recipients and preferences. Insert a notification keyed by event and recipient together with an outbox entry. Project it into the inbox and enqueue enabled channels separately. Push a live update for online recipients and retain unread state for offline readers.

Retry transient channel failures independently with stable delivery IDs. Track provider acknowledgments and dead-letter exhausted attempts. A provider that cannot deduplicate retries may deliver twice.

For live delivery, partition the durable stream by recipient and maintain a registry mapping active sessions to connection servers. Forward to the current server, then resume from the last acknowledged sequence after reconnect. A direct push and a replay can race, so the client deduplicates by notification ID.
