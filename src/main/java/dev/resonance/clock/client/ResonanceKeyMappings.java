package dev.resonance.clock.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.network.ModNetwork;
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
  /** Reserved for a future panel key; intentionally unbound in 0.3.0. */
  public static final KeyMapping OPEN_RELIC_PANEL = new KeyMapping(
      "key.resonance_clock.open_relic_panel",
      KeyConflictContext.IN_GAME,
      InputConstants.Type.KEYSYM,
      GLFW.GLFW_KEY_UNKNOWN,
      CATEGORY);

  @SubscribeEvent
  public static void register(RegisterKeyMappingsEvent event) {
    event.register(TEMPORAL_TWIST);
    event.register(OPEN_RELIC_PANEL);
  }

  @Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
  public static final class ForgeEvents {
    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
      if (event.phase == TickEvent.Phase.END) {
        if (TEMPORAL_TWIST.consumeClick()) {
          sendTwistRequest();
        }
        if (OPEN_RELIC_PANEL.consumeClick()) {
          sendPanelRequest();
        }
        ResonanceClockScreen.tickPending();
      }
    }

    private static void sendTwistRequest() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
        ModNetwork.requestTemporalTwist();
      }
    }

    private static void sendPanelRequest() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null && minecraft.level != null && minecraft.screen == null) {
        ModNetwork.requestOpenPanel();
      }
    }
  }

  private ResonanceKeyMappings() {}
}
