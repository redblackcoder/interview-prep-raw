# Maximum stock profit

Find the maximum gain from one buy followed by one sale.

## Worked solution

Initialize best profit to zero and minimum to infinity. For each price, update the best profit with price minus the prior minimum, then lower the minimum if appropriate. Return zero on an empty or falling sequence. Time is linear and auxiliary space is constant.
