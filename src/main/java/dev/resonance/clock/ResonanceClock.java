package dev.resonance.clock;

import dev.resonance.clock.config.ServerConfig;
import dev.resonance.clock.item.ModItems;
import dev.resonance.clock.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

@Mod(ResonanceClock.MOD_ID)
public final class ResonanceClock {
  public static final String MOD_ID = "resonance_clock";

  public ResonanceClock() {
    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    ModItems.register(modBus);
    modBus.addListener(this::registerGameTests);
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    DistExecutor.safeRunWhenOn(Dist.CLIENT,
        () -> dev.resonance.clock.client.ResonanceClientSetup::registerConfig);
    ModNetwork.register();
    MinecraftForge.EVENT_BUS.register(new ServerEvents());
  }

  private void registerGameTests(RegisterGameTestsEvent event) {
    try {
      Class<?> tests = Class.forName("dev.resonance.clock.state.TemporalStateGameTests");
      event.register(tests);
    } catch (ClassNotFoundException exception) {
      System.getLogger(ResonanceClock.class.getName()).log(System.Logger.Level.DEBUG,
          "GameTest source is absent from this runtime", exception);
    }
  }
}
