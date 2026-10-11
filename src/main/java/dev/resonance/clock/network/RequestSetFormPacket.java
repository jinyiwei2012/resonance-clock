package dev.resonance.clock.network;

import dev.resonance.clock.api.FormMode;
import dev.resonance.clock.api.ServerActions;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public record RequestSetFormPacket(FormMode mode, int requestSeq) {
  public RequestSetFormPacket(FormMode mode) {
    this(mode, ModNetwork.nextRequestSeq());
  }

  public static void encode(RequestSetFormPacket packet, FriendlyByteBuf buffer) {
    buffer.writeVarInt(packet.mode.ordinal());
    buffer.writeVarInt(packet.requestSeq);
  }

  public static RequestSetFormPacket decode(FriendlyByteBuf buffer) {
    Optional<FormMode> mode = FormMode.decode(buffer.readVarInt());
    return new RequestSetFormPacket(mode.orElse(null), buffer.readVarInt());
  }

  public static void handle(RequestSetFormPacket packet,
      Supplier<NetworkEvent.Context> contextSupplier) {
    NetworkEvent.Context context = contextSupplier.get();
    context.enqueueWork(() -> {
      ServerPlayer sender = context.getSender();
      if (sender == null || packet.requestSeq <= 0) {
        return;
      }
      if (packet.mode == null) {
        ModNetwork.syncOwner(sender);
        ModNetwork.sendActionResult(sender, dev.resonance.clock.api.ClientActionSink.Action.FORM,
            packet.requestSeq, dev.resonance.clock.api.ClientActionSink.Result.INVALID_FORM);
      } else if (!ModNetwork.acceptFormRequest(sender)) {
        ModNetwork.sendThrottledResult(sender,
            dev.resonance.clock.api.ClientActionSink.Action.FORM, packet.requestSeq);
      } else {
        ServerActions.SetFormResult result = ServerActions.requestSetForm(sender, packet.mode);
        ModNetwork.syncOwner(sender);
        if (result != ServerActions.SetFormResult.NOT_EQUIPPED) {
          ModNetwork.syncVisual(sender);
        }
        ModNetwork.sendActionResult(sender, dev.resonance.clock.api.ClientActionSink.Action.FORM,
            packet.requestSeq, result == ServerActions.SetFormResult.NOT_EQUIPPED
                ? dev.resonance.clock.api.ClientActionSink.Result.NOT_EQUIPPED
                : dev.resonance.clock.api.ClientActionSink.Result.ACCEPTED);
      }
    });
    context.setPacketHandled(true);
  }
}
