# Authorization for internal queries

Preserve entitlement checks across a query gateway and downstream services.

## Worked design

Validate the incoming identity once and issue short-lived, audience-bound context for service hops. Each resolver identifies the resource and requested action. The resource service checks tenant, subject, operation and current policy before returning data.

Batch repeated checks within one request and bound policy cache lifetimes. Include policy version and resource scope in cache keys to prevent an earlier permission decision being reused across tenants.
