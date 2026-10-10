package dev.resonance.clock.flight;

import dev.resonance.clock.config.ServerConfig;
import dev.resonance.clock.state.RelicState;
import dev.resonance.clock.ResonanceClock;
import net.minecraft.server.level.ServerPlayer;

/**
 * Exclusive vanilla-flight lease.
 *
 * <p>Vanilla's mayfly flag has no source attribution. The persistent marker therefore means this
 * mod owns the flag and must only be enabled when no other source is expected to grant mayfly.
 */
public final class FlightController {
  private static final String LEASE_MARKER = ResonanceClock.MOD_ID + ":flight_lease";

  public static void tick(ServerPlayer player) {
    boolean shouldGrant = ServerConfig.FLIGHT_ENABLED.get() && RelicState.isEquipped(player)
        && !player.isCreative() && !player.isSpectator();
    if (shouldGrant) {
      if (!player.getAbilities().mayfly) {
        player.getAbilities().mayfly = true;
        player.getPersistentData().putBoolean(LEASE_MARKER, true);
        player.onUpdateAbilities();
      }
      return;
    }
    revokeOwnedLease(player);
  }

  /** Revokes a lease previously granted by this mod and clears its durable ownership marker. */
  public static void revokeOwnedLease(ServerPlayer player) {
    boolean owned = player.getPersistentData().getBoolean(LEASE_MARKER);
    player.getPersistentData().remove(LEASE_MARKER);
    if (owned && !player.isCreative() && !player.isSpectator()) {
      player.getAbilities().mayfly = false;
      player.getAbilities().flying = false;
      player.onUpdateAbilities();
    }
  }

  /** Clears a stale durable lease before login or player replacement re-evaluates the equipment. */
  public static void clearResidualLease(ServerPlayer player) {
    revokeOwnedLease(player);
  }

  private FlightController() {}
}
