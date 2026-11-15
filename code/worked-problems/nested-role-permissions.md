# Nested role permissions

Resolve a permission for a user with multiple roles and inherited policies.

## Worked solution

For each directly assigned role, examine its explicit decision for the requested permission first. If absent, recursively examine its inherited roles and grant if any grants. Missing rules deny by default. Use a recursion-stack set to reject cycles and memoize completed role results during the query.

An explicit deny in a child suppresses an inherited allow on that path. Another directly assigned role can still grant. In the priority variant, collect matching rules and choose the highest priority with an explicit tie policy.
