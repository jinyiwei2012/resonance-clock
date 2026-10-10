package dev.resonance.clock.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.network.ModNetwork;
import dev.resonance.clock.network.RequestTemporalTwistPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import org.lwjgl.glfw.GLFW;

/** Client input for the server-authoritative temporal twist intent. */
@Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ResonanceKeyMappings {
  public static final String CATEGORY = "key.categories.resonance_clock";
  public static final KeyMapping TEMPORAL_TWIST = new KeyMapping(
      "key.resonance_clock.temporal_twist",
      KeyConflictContext.IN_GAME,
      InputConstants.Type.KEYSYM,
      GLFW.GLFW_KEY_V,
      CATEGORY);

  @SubscribeEvent
  public static void register(RegisterKeyMappingsEvent event) {
    event.register(TEMPORAL_TWIST);
  }

  @Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
  public static final class ForgeEvents {
    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
      if (event.phase == TickEvent.Phase.END && TEMPORAL_TWIST.consumeClick()) {
        sendRequest();
      }
    }

    private static void sendRequest() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
        ModNetwork.CHANNEL.sendToServer(new RequestTemporalTwistPacket());
      }
    }
  }

  private ResonanceKeyMappings() {}
}
