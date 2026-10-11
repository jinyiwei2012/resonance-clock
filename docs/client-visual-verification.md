# 0.3.0 Client Visual Verification

## Scope

This pass is limited to client Java under `client/**`, assets under
`assets/resonance_clock/**`, and this document. No Gradle task or game visual
capture is claimed here.

## Static checks

- `FormMode.BASE` keeps the existing I–XII numeric dial path.
- `FormMode.RESONANT` uses a separate moon-phase orbit with twelve simplified
  phase markers and restrained leaf/flower geometry; it does not reuse the
  BASE tick dial and does not load reference JPGs or character clothing.
- The owner panel reads private timers and server rules only from
  `ClientStateSink.State`; the UI exposes `twistEnabled` as an availability
  gate and shows configured duration, configured rule cooldown, resonant multiplier,
  curse enable/interval, and the conservative flight boolean as read-only.
- Live cooldown status uses `cooldownTicks`. The rule summary uses
  `configuredCooldownTicks * resonantCooldownMultiplier`; it does not use
  `resolvedCooldownTicks` while the anchor is inactive, because that field can
  correctly be zero outside an active anchor.
  Other-player packets are stored as `VisualState(entityId, equipped, mode)`.
- Form buttons send `ModNetwork.sendSetForm` and remain pending until a
  server state packet confirms the mode. No optimistic mode commit is sent.
- Action acknowledgements are matched by action and positive request sequence;
  stale or late replies do not clear a newer page request. Pending requests use
  a five-second monotonic timeout and report network unresponsiveness.
- The screen has one pending action slot. While occupied, both form buttons,
  cast, and refresh are disabled and their send handlers reject new actions;
  local visual preference controls and close remain available. Matching
  accepted/rejected ACKs and timeout clear the slot and rebuild the widgets.
- Owner state updates repaint the screen without consuming or replacing the
  pending action. Form completion requires both the matching accepted ACK and
  the matching confirmed owner mode, regardless of arrival order. An accepted
  open-panel response refreshes the existing screen rather than replacing it.
- Twist remains the existing `V` action. The reserved panel key is registered
  with `GLFW_KEY_UNKNOWN` and therefore has no default binding.
- Visual preferences are client-only Forge config values and do not expose or
  write server multipliers.
- The render layer skips entities beyond 48 blocks and reuses arithmetic
  geometry paths rather than allocating per-frame geometry objects.
- Client caches are cleared when there is no client level/player, preventing
  stale entity IDs from surviving disconnect/world changes.
- Client logout, client level load identity changes, client level unload, and
  client entity leave remove the appropriate cache entries. Owner private state
  is projected to a separate owner visual value and is never read from the
  tracked-player map.

## Not yet visually verified

- No in-game screenshot, FPS measurement, dedicated-server UI test, or actual
  packet round-trip is claimed in this pass. The parent session must run its
  assigned integration/build checks after core changes settle.

## Contract boundary

The panel does not edit any server rule. `resonantCooldownMultiplier`, curse
settings, and flight state are displayed as synchronized read-only values. A
future rule not present in the frozen contract must not be inferred from these
fields.
