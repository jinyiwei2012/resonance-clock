package dev.resonance.clock.network;

import dev.resonance.clock.api.ClientStateSink;
import dev.resonance.clock.api.FormMode;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncFormVisualPacket(int entityId, boolean equipped, FormMode formMode) {
  public static void encode(SyncFormVisualPacket packet, FriendlyByteBuf buffer) {
    buffer.writeVarInt(packet.entityId);
    buffer.writeBoolean(packet.equipped);
    buffer.writeVarInt(packet.formMode.ordinal());
  }

  public static SyncFormVisualPacket decode(FriendlyByteBuf buffer) {
    int entityId = buffer.readVarInt();
    boolean equipped = buffer.readBoolean();
    FormMode mode = FormMode.decode(buffer.readVarInt()).orElse(FormMode.BASE);
    return new SyncFormVisualPacket(entityId, equipped, mode);
  }

  public static void handle(SyncFormVisualPacket packet,
      Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> ClientStateSink.acceptVisual(new ClientStateSink.VisualState(
        packet.entityId, packet.equipped, packet.formMode)));
    context.setPacketHandled(true);
  }
}
