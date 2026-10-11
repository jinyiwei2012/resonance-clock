package dev.resonance.clock.network;

import dev.resonance.clock.api.ClientActionSink;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ActionResultPacket(ClientActionSink.Action action, int requestSeq,
                                 ClientActionSink.Result result) {
  public static void encode(ActionResultPacket packet, FriendlyByteBuf buffer) {
    buffer.writeVarInt(packet.action.ordinal());
    buffer.writeVarInt(packet.requestSeq);
    buffer.writeVarInt(packet.result.ordinal());
  }

  public static ActionResultPacket decode(FriendlyByteBuf buffer) {
    ClientActionSink.Action action = ClientActionSink.Action.decode(buffer.readVarInt()).orElse(null);
    int requestSeq = buffer.readVarInt();
    ClientActionSink.Result result = ClientActionSink.Result.decode(buffer.readVarInt()).orElse(null);
    return new ActionResultPacket(action, requestSeq, result);
  }

  public static void handle(ActionResultPacket packet,
      Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    if (packet.action != null && packet.result != null && packet.requestSeq > 0) {
      context.enqueueWork(() -> ClientActionSink.accept(new ClientActionSink.ActionResult(
          packet.action, packet.requestSeq, packet.result)));
    }
    context.setPacketHandled(true);
  }
}
