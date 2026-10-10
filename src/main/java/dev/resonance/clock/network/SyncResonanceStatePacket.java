package dev.resonance.clock.network;

import dev.resonance.clock.api.ClientStateSink;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncResonanceStatePacket(int entityId, boolean equipped, boolean twistActive,
                                       int remainingTicks, int cooldownTicks) {
  public static void encode(SyncResonanceStatePacket packet, FriendlyByteBuf buffer) {
    buffer.writeVarInt(packet.entityId);
    buffer.writeBoolean(packet.equipped);
    buffer.writeBoolean(packet.twistActive);
    buffer.writeVarInt(packet.remainingTicks);
    buffer.writeVarInt(packet.cooldownTicks);
  }

  public static SyncResonanceStatePacket decode(FriendlyByteBuf buffer) {
    return new SyncResonanceStatePacket(buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(),
        buffer.readVarInt(), buffer.readVarInt());
  }

  public static void handle(SyncResonanceStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> ClientStateSink.accept(new ClientStateSink.State(packet.entityId,
        packet.equipped, packet.twistActive, packet.remainingTicks, packet.cooldownTicks)));
    context.setPacketHandled(true);
  }
}
