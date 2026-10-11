package dev.resonance.clock.network;

import dev.resonance.clock.api.ClientUiSink;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class OpenRelicPanelPacket {
  public static void encode(OpenRelicPanelPacket packet, FriendlyByteBuf buffer) {}

  public static OpenRelicPanelPacket decode(FriendlyByteBuf buffer) {
    return new OpenRelicPanelPacket();
  }

  public static void handle(OpenRelicPanelPacket packet,
      Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(ClientUiSink::openRelicPanel);
    context.setPacketHandled(true);
  }
}
