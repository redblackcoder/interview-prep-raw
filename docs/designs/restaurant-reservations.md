# Restaurant reservations

Find and reserve a table at one restaurant without double-booking it.

## Worked design

Represent each booking by table, start and end. A conflict exists when an existing start precedes the requested end and its end follows the requested start. Adjacent bookings can coexist.

Search suitable tables by capacity, then lock a candidate table, recheck for overlap and insert in one transaction. Retry another table after contention. Reuse a booking request ID when the caller retries after a timeout.
