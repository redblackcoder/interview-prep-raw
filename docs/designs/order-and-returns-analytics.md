# Order and returns analytics

Rank products and customers and report order value and returns by period.

## Worked design

Store an order header and immutable line facts containing order ID, customer, product, sale timestamp, unit price and quantity. Store return quantities against the original order line with their own return timestamp.

To rank products by sales, filter sale timestamps to a half-open period, group by product, sum quantity times unit price, order by amount and take k. Count orders using distinct order IDs. Aggregate returns independently before joining them to sales so multiple return events cannot multiply revenue.
