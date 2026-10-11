package dev.resonance.clock.network;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record RequestOpenRelicPanelPacket(int requestSeq) {
  public RequestOpenRelicPanelPacket() {
    this(ModNetwork.nextRequestSeq());
  }

  public static void encode(RequestOpenRelicPanelPacket packet, FriendlyByteBuf buffer) {
    buffer.writeVarInt(packet.requestSeq);
  }

  public static RequestOpenRelicPanelPacket decode(FriendlyByteBuf buffer) {
    return new RequestOpenRelicPanelPacket(buffer.readVarInt());
  }

  public static void handle(RequestOpenRelicPanelPacket packet,
      Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> {
      ServerPlayer sender = context.getSender();
      if (sender == null || packet.requestSeq <= 0) {
        return;
      }
      if (!ModNetwork.acceptOpenPanelRequest(sender)) {
        ModNetwork.sendThrottledResult(sender,
            dev.resonance.clock.api.ClientActionSink.Action.OPEN_PANEL, packet.requestSeq);
      } else if (ModNetwork.mayOpenRelicPanel(sender)) {
        ModNetwork.openRelicPanel(sender);
        ModNetwork.sendActionResult(sender,
            dev.resonance.clock.api.ClientActionSink.Action.OPEN_PANEL, packet.requestSeq,
            dev.resonance.clock.api.ClientActionSink.Result.ACCEPTED);
      } else {
        ModNetwork.syncOwner(sender);
        ModNetwork.sendActionResult(sender,
            dev.resonance.clock.api.ClientActionSink.Action.OPEN_PANEL, packet.requestSeq,
            dev.resonance.clock.api.ClientActionSink.Result.UNAVAILABLE);
      }
    });
    context.setPacketHandled(true);
  }
}
