package dev.resonance.clock.client;

import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.api.ClientStateSink;
import dev.resonance.clock.api.ClientUiSink;
import dev.resonance.clock.api.ClientActionSink;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

/** Client-only setup and lifecycle cleanup. */
@Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ResonanceClientSetup {
  private static boolean configRegistered;

  @SubscribeEvent
  public static void clientSetup(FMLClientSetupEvent event) {
    event.enqueueWork(() -> {
      ClientStateSink.register(ResonanceClockScreen::acceptState);
      ClientStateSink.registerVisual(ResonanceClientStore::acceptVisual);
      ClientActionSink.register(ResonanceClockScreen::acceptAction);
      ClientUiSink.registerOpenRelicPanel(ResonanceClockScreen::openOrRefresh);
    });
  }

  public static void registerConfig() {
    if (!configRegistered) {
      ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ResonanceVisualSettings.SPEC);
      configRegistered = true;
    }
  }

  private ResonanceClientSetup() {}
}
