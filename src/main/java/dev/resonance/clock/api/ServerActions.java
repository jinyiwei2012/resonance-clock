package dev.resonance.clock.api;

import dev.resonance.clock.state.TemporalTwistService;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative action entry point. Network handlers must enqueue calls onto the server thread. */
public final class ServerActions {
  public static TemporalTwistService.StartResult requestTemporalTwist(ServerPlayer player) {
    return TemporalTwistService.start(player);
  }

  private ServerActions() {}
}
