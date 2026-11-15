# Digital evidence archive

Ingest large evidence files with integrity verification, access control and an auditable chain of custody.

## Worked design

Upload chunks, verify their hashes against a signed manifest, and publish the evidence version after verification. Keep immutable object versions and record which key attested to the content.

In the transaction authorizing a protected operation, write its audit record or outbox event. Export audit events in tenant order to retention-protected storage. Authorize searches and downloads independently so a search hit alone cannot grant access.
