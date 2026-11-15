# Price-triggered purchases

Buy from a seller when a quoted price falls within a customer's limit before expiry.

## Worked design

Persist pending orders with deadlines and a next-check time. Claim due orders with expiring leases, group by item and obtain one current quote per seller and item. Skip expired orders and submit qualifying purchases with the quoted price and a stable idempotency key.

A changed price reschedules the check within the remaining window. A lost purchase response requires seller-side idempotency or reconciliation before retry. Repeated checks throughout the window avoid waiting until just before expiration.
