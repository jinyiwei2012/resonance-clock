package dev.resonance.clock.network;

import dev.resonance.clock.api.ServerActions;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Empty intent packet. The server derives identity and validates all state after enqueueing. */
public final class RequestTemporalTwistPacket {
  public static void encode(RequestTemporalTwistPacket packet, FriendlyByteBuf buffer) {}

  public static RequestTemporalTwistPacket decode(FriendlyByteBuf buffer) {
    return new RequestTemporalTwistPacket();
  }

  public static void handle(RequestTemporalTwistPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> {
      ServerPlayer sender = context.getSender();
      if (sender != null && ModNetwork.acceptTemporalTwistRequest(sender)) {
        ServerActions.requestTemporalTwist(sender);
      }
    });
    context.setPacketHandled(true);
  }
}
