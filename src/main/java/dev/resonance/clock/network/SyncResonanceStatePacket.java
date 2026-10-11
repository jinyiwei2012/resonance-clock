package dev.resonance.clock.network;

import dev.resonance.clock.api.ClientStateSink;
import dev.resonance.clock.api.FormMode;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record SyncResonanceStatePacket(int entityId, boolean equipped, FormMode formMode,
                                       boolean twistActive, int remainingTicks, int cooldownTicks,
                                       boolean curseActive, int curseRemainingTicks,
                                       int resolvedDurationTicks, int resolvedCooldownTicks,
                                       boolean twistEnabled, int configuredDurationTicks,
                                       int configuredCooldownTicks, double resonantCooldownMultiplier,
                                       boolean curseEnabled, int curseIntervalTicks,
                                       boolean flightEnabled) {
  public static void encode(SyncResonanceStatePacket packet, FriendlyByteBuf buffer) {
    buffer.writeVarInt(packet.entityId);
    buffer.writeBoolean(packet.equipped);
    buffer.writeVarInt(packet.formMode.ordinal());
    buffer.writeBoolean(packet.twistActive);
    buffer.writeVarInt(packet.remainingTicks);
    buffer.writeVarInt(packet.cooldownTicks);
    buffer.writeBoolean(packet.curseActive);
    buffer.writeVarInt(packet.curseRemainingTicks);
    buffer.writeVarInt(packet.resolvedDurationTicks);
    buffer.writeVarInt(packet.resolvedCooldownTicks);
    buffer.writeBoolean(packet.twistEnabled);
    buffer.writeVarInt(packet.configuredDurationTicks);
    buffer.writeVarInt(packet.configuredCooldownTicks);
    buffer.writeDouble(packet.resonantCooldownMultiplier);
    buffer.writeBoolean(packet.curseEnabled);
    buffer.writeVarInt(packet.curseIntervalTicks);
    buffer.writeBoolean(packet.flightEnabled);
  }

  public static SyncResonanceStatePacket decode(FriendlyByteBuf buffer) {
    int entityId = buffer.readVarInt();
    boolean equipped = buffer.readBoolean();
    FormMode mode = FormMode.decode(buffer.readVarInt()).orElse(FormMode.BASE);
    return new SyncResonanceStatePacket(entityId, equipped, mode, buffer.readBoolean(),
        buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
        buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
        buffer.readVarInt(), buffer.readDouble(), buffer.readBoolean(), buffer.readVarInt(),
        buffer.readBoolean());
  }

  public static void handle(SyncResonanceStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> ClientStateSink.accept(new ClientStateSink.State(packet.entityId,
        packet.equipped, packet.formMode, packet.twistActive, packet.remainingTicks,
        packet.cooldownTicks, packet.curseActive, packet.curseRemainingTicks,
        packet.resolvedDurationTicks, packet.resolvedCooldownTicks, packet.twistEnabled,
        packet.configuredDurationTicks, packet.configuredCooldownTicks,
        packet.resonantCooldownMultiplier, packet.curseEnabled, packet.curseIntervalTicks,
        packet.flightEnabled)));
    context.setPacketHandled(true);
  }
}
