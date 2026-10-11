# Runtime debugging

## Session switch

Server operators with permission level 2 can use:

```text
/resonance_clock debug on
/resonance_clock debug off
/resonance_clock debug status [target]
```

`debug on` enables the detailed diagnostic mode. Debug logging is disabled by default, is reset when the server stops, and does not write a configuration file or modify cooldown, anchor, curse, or other gameplay state.

## What it observes

While enabled, `RC-DEBUG` server log entries record a monotonically assigned diagnostic request ID when an intent packet is received, then record the result after it has run on the enqueued server thread. The latter includes the actual throttle acceptance/rejection and, for accepted requests, the actual `TemporalTwistService.StartResult`; rejected requests report `NOT_INVOKED`.

The detailed record includes the current server/request clock, entity tick, last accepted throttle tick, equipment state, temporal and curse root NBT types, root validation outcomes, versions, active/remaining/executing/cooldown counters, and actual restore result. The diagnostics themselves do not intentionally collect raw NBT, inventory, token, chat, player name, UUID, coordinates, dimension, or unrelated persistent-data roots of other mods. Original exception messages are passed through unchanged and can still contain such data.

Each accepted and rejected result has an independent per-player 20-server-tick sampling window. Omitted results are summarized when the next same-category detail record is emitted. This diagnostic sampling does not alter the production request throttle, which remains 10 ticks.

Sampled request results, lifecycle boundaries (login, logout, death, clone), and restoration outcomes include a complete **local snapshot stack**. It is a Java `Throwable` created only after diagnostic sampling accepts the record, and no stack frames are intentionally trimmed. Exception logging passes the original throwable to SLF4J, preserving its exception stack, complete cause chain, and suppressed exceptions. Normal exception propagation is unchanged.

The clone entry records old/new entity IDs and server time. `debug status` reads existing player persistent-data roots without creating, migrating, clearing, or serializing them; it remains a concise in-game status response.

## Scope and privacy

The trace is server-side only. It can prove that the server network handler or its enqueued work executed, but it cannot prove a client key-binding call chain or bridge client/server asynchronous stacks into a single call chain. No client diagnostic packet or protocol change was added.

Server logs can still contain server paths and, depending on the surrounding platform logging configuration, identifying information. Log renderers can abbreviate shared common stack frames using `... N more`; this is normal Throwable rendering rather than diagnostic frame removal. Review and redact paths, player identity, and exception messages before sharing logs publicly.

## Diagnosing a key press

After enabling debug mode, an `RC-DEBUG requestId=... stage=server-enqueued-result` entry confirms that the intent reached and ran on the server work queue. Compare its `accepted` and `startResult` values with the state fields, then use its stack snapshot to identify the local server boundary. A missing server entry does not by itself prove that the client received no key input.

This tool is observational only. Do not use it as a reset or recovery command. Back up a test world before reproducing a problem.
