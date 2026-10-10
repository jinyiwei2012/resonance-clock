package dev.resonance.clock.network;

import dev.resonance.clock.ResonanceClock;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import dev.resonance.clock.state.RelicState;
import dev.resonance.clock.state.TemporalState;

public final class ModNetwork {
  private static final String VERSION = "1";
  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
       new ResourceLocation(ResonanceClock.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
  private static final int REQUEST_INTERVAL_TICKS = 10;
  private static final RequestThrottle TEMPORAL_TWIST_THROTTLE =
      new RequestThrottle(REQUEST_INTERVAL_TICKS);

  public static void register() {
    CHANNEL.registerMessage(0, RequestTemporalTwistPacket.class, RequestTemporalTwistPacket::encode,
        RequestTemporalTwistPacket::decode, RequestTemporalTwistPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_SERVER));
    CHANNEL.registerMessage(1, SyncResonanceStatePacket.class, SyncResonanceStatePacket::encode,
        SyncResonanceStatePacket::decode, SyncResonanceStatePacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
  }

  public static void requestTemporalTwist() {
    CHANNEL.sendToServer(new RequestTemporalTwistPacket());
  }

  public static void syncToSelf(ServerPlayer player) {
    TemporalState.PublicState state = TemporalState.publicState(player);
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncResonanceStatePacket(player.getId(),
        RelicState.isEquipped(player), state.active(), state.remainingTicks(), state.cooldownTicks()));
  }

  /** Rejects burst requests before they enter the temporal action path. */
  public static boolean acceptTemporalTwistRequest(ServerPlayer player) {
    if (player.getServer() == null) {
      return false;
    }
    long currentTick = player.getServer().overworld().getGameTime();
    return TEMPORAL_TWIST_THROTTLE.tryAccept(player.getUUID(), currentTick);
  }

  /** Returns the current global server clock used by the temporal request throttle. */
  public static long temporalTwistRequestClock(ServerPlayer player) {
    return player.getServer() == null ? -1L : player.getServer().overworld().getGameTime();
  }

  /** Returns the last accepted request clock for diagnostics without changing throttle state. */
  public static long lastAcceptedTemporalTwistRequestClock(ServerPlayer player) {
    Long tick = TEMPORAL_TWIST_THROTTLE.getLastAcceptedTick(player.getUUID());
    return tick == null ? -1L : tick;
  }

  public static void clearPlayerRequestState(ServerPlayer player) {
    TEMPORAL_TWIST_THROTTLE.clear(player.getUUID());
  }

  private ModNetwork() {}
}
