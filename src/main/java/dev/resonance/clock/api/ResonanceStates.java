package dev.resonance.clock.api;

import dev.resonance.clock.state.RelicState;
import dev.resonance.clock.state.TemporalState;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;

public final class ResonanceStates {
  public static final ResonanceStateView SERVER = new ResonanceStateView() {
    @Override
    public boolean isRelicEquipped(ServerPlayer player) {
      return RelicState.isEquipped(player);
    }

    @Override
    public Optional<TemporalState.PublicState> temporalState(ServerPlayer player) {
      return Optional.of(TemporalState.publicState(player));
    }

    @Override
    public long cooldownTicksRemaining(ServerPlayer player) {
      return TemporalState.publicState(player).cooldownTicks();
    }
  };

  private ResonanceStates() {}
}
