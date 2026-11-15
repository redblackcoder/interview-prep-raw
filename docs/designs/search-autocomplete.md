# Search autocomplete

Return the ten highest-ranked keyword suggestions while a user types.

## Worked design

Reference design for a requirements-only sketch. Normalize search terms consistently at ingest and lookup. Aggregate frequencies and insert terms in a trie, caching the ten best completions at each visited prefix. At request time traverse the normalized prefix and read its cached list.

Publish index versions atomically. Include stable tie-breaking and bounded refresh intervals. Have the client discard responses for older prefixes so a slow lookup cannot overwrite newer suggestions.
