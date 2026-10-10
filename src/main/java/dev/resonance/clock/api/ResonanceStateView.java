package dev.resonance.clock.api;

import dev.resonance.clock.state.TemporalState;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

/** Stable read-only state seam for client synchronization and future integrations. */
public interface ResonanceStateView {
  boolean isRelicEquipped(ServerPlayer player);

  Optional<TemporalState.PublicState> temporalState(ServerPlayer player);

  long cooldownTicksRemaining(ServerPlayer player);
}
