package dev.resonance.clock.network;

import dev.resonance.clock.api.ServerActions;
import dev.resonance.clock.debug.ResonanceDebug;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Empty intent packet. The server derives identity and validates all state after enqueueing. */
public record RequestTemporalTwistPacket(int requestSeq) {
  public RequestTemporalTwistPacket() {
    this(ModNetwork.nextRequestSeq());
  }

  public static void encode(RequestTemporalTwistPacket packet, FriendlyByteBuf buffer) {
    buffer.writeVarInt(packet.requestSeq);
  }

  public static RequestTemporalTwistPacket decode(FriendlyByteBuf buffer) {
    return new RequestTemporalTwistPacket(buffer.readVarInt());
  }

  public static void handle(RequestTemporalTwistPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> {
      ServerPlayer sender = context.getSender();
      if (sender != null && packet.requestSeq > 0) {
        long requestId = ResonanceDebug.beginServerRequest();
        try {
          boolean accepted = ModNetwork.acceptTemporalTwistRequest(sender);
          Object startResult = null;
          if (accepted) {
            startResult = ServerActions.requestTemporalTwist(sender);
            ModNetwork.syncOwner(sender);
            ModNetwork.sendActionResult(sender, dev.resonance.clock.api.ClientActionSink.Action.TWIST,
                packet.requestSeq, ModNetwork.mapTwistResult(
                    (dev.resonance.clock.state.TemporalTwistService.StartResult) startResult));
          } else {
            ModNetwork.sendThrottledResult(sender,
                dev.resonance.clock.api.ClientActionSink.Action.TWIST, packet.requestSeq);
          }
          ResonanceDebug.logRequestResult(sender, requestId, accepted, startResult);
        } catch (RuntimeException exception) {
          ResonanceDebug.logException("request-server-enqueued", sender, exception);
          throw exception;
        }
      }
    });
    context.setPacketHandled(true);
  }
}
