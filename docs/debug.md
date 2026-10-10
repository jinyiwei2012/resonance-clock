# Runtime debugging

## Session switch

Server operators with permission level 2 can use:

```text
/resonance_clock debug on
/resonance_clock debug off
/resonance_clock debug status [target]
```

Debug logging is disabled by default and exists only for the running server session. It is reset when the server stops; it does not write or modify a configuration file, player cooldown, anchor, curse, or any other gameplay state.

## What it observes

While enabled, the server logs `RC-DEBUG` entries for temporal-twist request delivery and throttle acceptance or rejection. Accepted and rejected details have separate per-player 20-server-tick sampling windows, so an accepted request cannot hide the immediately following rejected request. They identify the player only by entity ID and report the global request clock, entity tick counter, last accepted throttle clock, equipment and relevant state flags. Lifecycle entries cover login, logout, clone, and death. The production request throttle remains 10 ticks; the 20-tick value applies only to diagnostic log sampling.

`debug status` reads the existing player persistent-data roots without creating, migrating, clearing, or serializing them. It reports only boolean state, tick counters, and stored data-version fields. It does not print raw NBT, UUIDs, world coordinates, dimensions, snapshots, or inventory data.

## Diagnosing a key press

After enabling debug mode, a server entry beginning with `RC-DEBUG request` confirms that the temporal-twist request packet reached the server. It also shows whether the request throttle accepted it. A missing server entry does not by itself prove that the client received no key input; client-side input diagnostics are not implemented here.

This tool is observational only. Do not use it as a reset or recovery command. Back up a test world before reproducing a problem.
