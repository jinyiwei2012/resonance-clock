package dev.resonance.clock.network;

import dev.resonance.clock.ResonanceClock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
  private static final Map<UUID, Integer> LAST_REQUEST_TICK = new HashMap<>();

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
    int currentTick = player.tickCount;
    Integer previousTick = LAST_REQUEST_TICK.get(player.getUUID());
    if (previousTick != null && currentTick - previousTick < REQUEST_INTERVAL_TICKS) {
      return false;
    }
    LAST_REQUEST_TICK.put(player.getUUID(), currentTick);
    return true;
  }

  public static void clearPlayerRequestState(ServerPlayer player) {
    LAST_REQUEST_TICK.remove(player.getUUID());
  }

  private ModNetwork() {}
}
