# Recent event counter

Count events in a rolling five-minute window.

## Worked solution

Assume monotonically increasing add and query times. Append one timestamp per event. At query time t, discard head entries at or before t minus five minutes for the half-open window (t−5 minutes,t]. The remaining deque size is the count.

Each event enters and leaves once. Equal timestamps count separately. Out-of-order input needs an ordered index or a stated lateness bound before eviction is safe.

## Reference implementation

Reconstructed Python example.

```python
from collections import deque


class RecentEvents:
    def __init__(self, window_ms=300_000):
        self.events = deque()
        self.window = window_ms
        self.last = None

    def count(self, now):
        if self.last is not None and now < self.last:
            raise ValueError("time must be monotone")
        self.last = now
        while self.events and self.events[0] <= now - self.window:
            self.events.popleft()
        return len(self.events)

    def add(self, now):
        self.count(now)
        self.events.append(now)


events = RecentEvents()
for t in [0, 1, 1]:
    events.add(t)
assert events.count(300_000) == 2
```
