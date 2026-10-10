package dev.resonance.clock.item;

import dev.resonance.clock.ResonanceClock;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
  private static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, ResonanceClock.MOD_ID);

  public static final RegistryObject<Item> RESONANCE_RELIC =
      ITEMS.register("resonance_relic", () -> new ResonanceRelicItem(new Item.Properties().stacksTo(1)));

  public static void register(IEventBus bus) {
    ITEMS.register(bus);
  }

  private ModItems() {}
}
