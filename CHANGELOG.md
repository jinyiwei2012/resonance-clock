# Changelog

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
