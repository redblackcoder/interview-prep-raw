# Contest judging service

Accept contest submissions and rank contestants by accepted problems, failed attempts and time.

## Worked design

At contest start publish a fixed problem set. Timestamp and persist each source submission before enqueuing it. Run it in an isolated worker with CPU, memory and time limits against the contest's versioned tests. Persist one verdict per submission.

Score the first accepted attempt per contestant and problem using submission time and prior failures. Sort by problems solved then penalty. Rejudge by rebuilding standings from versioned verdicts to avoid counting a result twice.
