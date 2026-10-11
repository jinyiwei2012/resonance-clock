# Changelog

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
