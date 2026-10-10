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

@Mod(ResonanceClock.MOD_ID)
public final class ResonanceClock {
  public static final String MOD_ID = "resonance_clock";

  public ResonanceClock() {
    IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
    ModItems.register(modBus);
    ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    ModNetwork.register();
    MinecraftForge.EVENT_BUS.register(new ServerEvents());
  }
}
