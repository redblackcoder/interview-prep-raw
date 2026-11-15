# Ride dispatch

Match an on-demand ride to an available driver with the requested vehicle type.

## Worked design

Index fresh driver positions by cell and tier. Search nearby cells, expand the radius if necessary, and rank candidates by estimated pickup time. Send an expiring offer and store its assignment version.

On acceptance conditionally claim both the open ride and available driver in one transaction. Competing accepts or stale offers lose. Expire unanswered offers and try the next candidate. Location updates can lag because the assignment record enforces exclusivity.
