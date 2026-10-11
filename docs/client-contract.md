# 0.3.0 Client Contract

Common/server code exposes only side-safe API types. Client code registers sinks during client setup and is never referenced by dedicated-server classes.

## Types

- `api.FormMode`: `BASE` (I-XII dial) and `RESONANT` (moon-phase dial).
- `api.ClientStateSink.State`: owner-only state with `entityId`, `equipped`, `formMode`, `twistActive`, `remainingTicks`, `cooldownTicks`, `curseActive`, `curseRemainingTicks`, `resolvedDurationTicks`, `resolvedCooldownTicks`, `twistEnabled`, `configuredDurationTicks`, `configuredCooldownTicks`, `resonantCooldownMultiplier`, `curseEnabled`, `curseIntervalTicks`, `flightEnabled`.
- `api.ClientStateSink.VisualState`: tracking-safe state with only `entityId`, `equipped`, and `formMode`.
- `api.ClientUiSink`: register the panel-opening `Runnable` with `registerOpenRelicPanel(Runnable)`.
- `api.ClientActionSink`: register `Consumer<ActionResult>` with `register`. `ActionResult` contains `action`, `requestSeq`, and `result`.

Register owner state with `ClientStateSink.register(Consumer<State>)` and tracked-player visuals with `ClientStateSink.registerVisual(Consumer<VisualState>)`. Both sinks default to no-op, so packets may safely arrive before client registration. `SyncResonanceStatePacket` calls only the owner sink; `SyncFormVisualPacket` calls only the visual sink and cannot erase owner timers.

The original 5-argument and prior 10-argument `State` constructors remain source-compatible for in-progress client code. Network decode always uses the full schema above. Added configuration fields are read-only server rules. `flightEnabled` communicates only the conservative global boolean and does not assert ownership of another flight grant.

## Network helpers

- `int ModNetwork.requestTemporalTwist()` preserves the existing V-key action and returns its request sequence.
- `int ModNetwork.sendSetForm(FormMode)` is the stable panel helper (`requestSetForm` remains an equivalent alias). It returns a request sequence; only the server may commit a mode, and only while the genuine relic is equipped.
- `int ModNetwork.requestOpenPanel()` is the stable helper for a future unbound key (`requestOpenRelicPanel` remains an equivalent alias). The server permits opening only while the genuine relic is equipped or held in either hand.
- `ModNetwork.openRelicPanel(ServerPlayer)`, `syncOwner`, `syncVisual`, and `syncVisualTo` are server helpers.

The channel protocol is `2`; 0.2.x peers are intentionally rejected before packets can be decoded with the wrong schema. Invalid enum values decode to a rejected set-form intent or a safe `BASE` visual fallback.

## Action acknowledgement

Every intent carries a positive client-local `requestSeq`. The server derives player identity only from the network sender; the sequence is correlation data, not authentication. `Action` is `FORM`, `TWIST`, or `OPEN_PANEL`. `Result` is `ACCEPTED`, `THROTTLED`, `NOT_EQUIPPED`, `DISABLED`, `ACTIVE`, `COOLDOWN`, `INVALID_FORM`, or `UNAVAILABLE`.

For processed requests, the server sends refreshed owner state before `ActionResultPacket`. Invalid form enums receive `INVALID_FORM`. Unauthorized panel opening receives `UNAVAILABLE`. Twist start results map directly to the corresponding result. Burst requests may receive `THROTTLED`, but rejected ACKs are themselves limited to one per player per 10 server ticks; therefore the UI must also clear pending state with a short local timeout. A missing sender or an unexpected server exception produces no ACK, and the existing exception propagation/debug instrumentation remains authoritative.

## UI behavior

After server authorization, both held-item right-click and optional-key requests send the owner state first and then the S2C open intent, which invokes `ClientUiSink.openRelicPanel()` on queued client work. The panel may open for a held but unequipped genuine relic, but must show that casting and form switching require equipment. `BASE` must retain the I-XII dial and `RESONANT` must use moon phases. A confirmed server form affects the next Temporal Twist cooldown resolution; it is not a local visual toggle. Do not expose controls that directly edit cooldown multipliers: `resonantCooldownMultiplier` is a server rule. Only visual preferences belong in a separate client config.

Calling an intent helper must not optimistically mutate authoritative state. Pending UI state is correlated by both `action` and `requestSeq`; only that matching ACK or the timeout clears it. A FORM `ACCEPTED` ACK follows the owner state packet and confirms the server decision; rejection results leave the synchronized mode unchanged.

No new localization key is required by the common ACK protocol. The client may reuse existing unavailable/pending text; if it displays specific result reasons, the client writer owns those optional keys.

No key is assigned to panel opening in 0.3.0. Existing V remains Temporal Twist.

## Localization placeholders for the client writer

Assets are outside this implementation lane. Add these keys in both `en_us.json` and `zh_cn.json` when implementing the screen:

- `screen.resonance_clock.relic.title`
- `screen.resonance_clock.relic.form.base`
- `screen.resonance_clock.relic.form.resonant`
- `screen.resonance_clock.relic.form.base.description`
- `screen.resonance_clock.relic.form.resonant.description`
- `screen.resonance_clock.relic.not_equipped`
- `screen.resonance_clock.relic.twist_unavailable`
- `screen.resonance_clock.relic.form_switch_unavailable`
- `key.resonance_clock.open_relic_panel` (reserved, unbound in this version)
