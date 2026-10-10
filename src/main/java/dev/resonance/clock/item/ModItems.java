package dev.resonance.clock.item;

import dev.resonance.clock.ResonanceClock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
  private static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, ResonanceClock.MOD_ID);

  public static final RegistryObject<Item> RESONANCE_RELIC =
      ITEMS.register("resonance_relic", () -> new ResonanceRelicItem(new Item.Properties().stacksTo(1)));
  public static final RegistryObject<Item> TEMPORAL_PURIFIER =
      ITEMS.register("temporal_purifier", () -> new TemporalPurifierItem(new Item.Properties().stacksTo(16)));

  public static void register(IEventBus bus) {
    ITEMS.register(bus);
    bus.addListener(ModItems::addCreativeItems);
  }

  private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
      event.accept(RESONANCE_RELIC);
      event.accept(TEMPORAL_PURIFIER);
    }
  }

  private ModItems() {}
}
