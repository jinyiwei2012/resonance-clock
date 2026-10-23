# Changelog

## Unreleased — Issue #1 follow-up

- Raised the back-mounted clock ring to shoulder-blade height, enlarged both BASE and RESONANT radii, and added a real I–XII Roman numeral dial to the BASE form.
- Temporal Twist restore now searches nearby standable ground horizontally (8-block radius) and vertically (±8 blocks) before falling back to the original snapshot position, so mid-air triggers restore instead of failing as `UNSAFE_DESTINATION`.
- Added four GameTests for safe-destination resolution (open-air fallback, nearest-floor addressing, liquid-embedded and block-embedded rejection); full suite now 23/23 with clean exit `0`.
- Decision record for remaining roadmap work added to [docs/roadmap.md](docs/roadmap.md); evidence in [docs/verification.md](docs/verification.md).

## 0.3.0 — Relic forms and stable client contract

- Added persistent BASE and RESONANT relic forms with server-authoritative, equipped-only switching.
- RESONANT freezes a configurable cooldown multiplier (default `0.5`, range `0.1..1.0`) when Temporal Twist starts; duration and Temporal Disorder are unchanged.
- Added owner-private state synchronization, tracking-safe form visuals, an authenticated panel-open intent, and protocol version `2`.
- Added held-relic right-click opening, an optional unbound panel key, local-only visual preferences, and distinct I–XII BASE / moon-phase RESONANT rendering.
- Default rules remain 10-second duration and 30-second BASE cooldown; RESONANT freezes the next started twist cooldown at the default `0.5` multiplier (15 seconds) until completion or failure.
- Kept flight disabled by default and retained backward handling for valid 0.2.2 anchors without resolved timing fields.
- This follows reported 0.2.2 user testing; it does not claim every gameplay or multiplayer scenario is verified.
- Final ForgeGradle GameTest evidence discovered and passed 19/19 required tests with GameTestServer self-shutdown, `BUILD SUCCESSFUL`, and clean exit `0`.
- This remains a pre-release test build: human GUI and ACK round trips, two-client tracking visuals, screenshots/FPS, and third-party modpack interoperability remain unverified. Apollon adaptation, time stop, and finisher abilities are not included.

## 0.2.2 — Pre-release snapshot and Curios compatibility repair

- Fixed valid vanilla `fireTicks = -1` and negative air snapshots being rejected after a Temporal Twist starts.
- Kept strict NBT typing and bounded-value validation while allowing legal modded food and saturation values above vanilla's normal cap.
- Added field-level, non-sensitive validation diagnostics for Temporal Twist and Temporal Disorder debug snapshots.
- Added a size-one, relic-only `resonance_clock` Curios slot for players, without replacing the existing `charm` compatibility path.
- The 0.2.0 death/respawn timing fix remains unchanged.

## 0.2.1 — Diagnostic trace patch

- Expanded the session-only operator diagnostics to record sampled full local stack snapshots at key server request, lifecycle, and restore-result boundaries.
- Added correlated request IDs, explicit accepted/rejected throttle results, actual Temporal Twist start results, state-root types, validation outcomes, and restore results.
- Preserved complete exception causes and suppressed exceptions through SLF4J throwable logging without changing exception propagation or gameplay behavior.
- Kept the 0.2.0 death/135-second regression fix intact; this release is observational only.

## 0.2.0 — Temporal Disorder

- Added persistent Temporal Disorder curses with configurable online-tick rewind cycles.
- Added operator curse/status/clear commands and a self-use Temporal Purifier.
- Extracted guarded snapshot restoration for reuse without storage, cooldown, or message coupling.
- Preserved curses across death and paused them during death, logout, or feature disablement.
- A successfully created Temporal Twist anchor now atomically purifies the curse.
- Added strict snapshot/root validation and pre-chunk coordinate, border, and build-height checks.
- Moved request throttling to the server overworld clock so entity tick resets cannot extend the throttle indefinitely.
- Added permission-level-2, session-only read-only diagnostics for temporal request and state investigation.

## 0.1.0 — Initial test release

- Added the Resonance Relic Curios `charm` item.
- Added server-authoritative temporal-twist state capture and guarded restoration.
- Added conservative, opt-in flight authorization and configurable starvation-damage multiplier.
- Added a rebindable temporal-twist key binding and an original ring/wing render layer.
- Added Forge server configuration defaults and Chinese/English localization.

This is a test release. See [docs/verification.md](docs/verification.md) for the evidence boundary and untested areas.
