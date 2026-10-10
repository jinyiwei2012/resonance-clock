package dev.resonance.clock.client;

import dev.resonance.clock.ResonanceClock;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

/** Client-only registration for the player visual layer. */
@Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ResonanceClientEvents {
  @SubscribeEvent
  public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
    for (String skin : event.getSkins()) {
      PlayerRenderer renderer = event.getSkin(skin);
      if (renderer != null) {
        renderer.addLayer(new ResonanceRenderLayer(renderer));
      }
    }
  }

  private ResonanceClientEvents() {}
}
