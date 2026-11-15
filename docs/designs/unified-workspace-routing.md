# Unified workspace routing

Route an authenticated user from one landing page to the chosen workspace.

## Worked design

Authenticate at the landing service and list only authorized workspaces. Resolve the selected workspace ID in a versioned directory mapping IDs to regional backends. Pass a narrowly scoped identity context and recheck workspace membership at the destination.

Cache routing records with bounded lifetimes and invalidate on migration. Fail closed if a workspace's current owner cannot be resolved.
