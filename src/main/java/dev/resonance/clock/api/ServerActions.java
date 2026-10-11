package dev.resonance.clock.api;

import dev.resonance.clock.state.TemporalTwistService;
import dev.resonance.clock.state.FormState;
import dev.resonance.clock.state.RelicState;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative action entry point. Network handlers must enqueue calls onto the server thread. */
public final class ServerActions {
  public enum SetFormResult { CHANGED, UNCHANGED, NOT_EQUIPPED }

  public static TemporalTwistService.StartResult requestTemporalTwist(ServerPlayer player) {
    return TemporalTwistService.start(player);
  }

  public static SetFormResult requestSetForm(ServerPlayer player, FormMode mode) {
    if (!RelicState.isEquipped(player)) {
      return SetFormResult.NOT_EQUIPPED;
    }
    if (FormState.get(player) == mode) {
      return SetFormResult.UNCHANGED;
    }
    FormState.set(player, mode);
    return SetFormResult.CHANGED;
  }

  private ServerActions() {}
}
