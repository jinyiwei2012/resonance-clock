package dev.resonance.clock.client;

import dev.resonance.clock.ResonanceClock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Clears synchronized state when the client leaves a server or local world. */
@Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ResonanceClientLifecycleEvents {
  @SubscribeEvent
  public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
    ResonanceClientStore.clear();
    if (Minecraft.getInstance().screen instanceof ResonanceClockScreen screen) {
      screen.onClose();
    }
  }

  @SubscribeEvent
  public static void onLevelLoad(LevelEvent.Load event) {
    if (event.getLevel().isClientSide()) {
      ResonanceClientStore.observeClientLevel(event.getLevel());
    }
  }

  @SubscribeEvent
  public static void onLevelUnload(LevelEvent.Unload event) {
    if (event.getLevel().isClientSide()) {
      ResonanceClientStore.clear();
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.screen instanceof ResonanceClockScreen) {
        minecraft.setScreen(null);
      }
    }
  }

  @SubscribeEvent
  public static void onEntityLeave(EntityLeaveLevelEvent event) {
    if (event.getLevel().isClientSide()) {
      ResonanceClientStore.removeVisual(event.getEntity().getId());
    }
  }

  private ResonanceClientLifecycleEvents() {}
}
