package dev.resonance.clock.state;

import dev.resonance.clock.item.ModItems;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;

public final class RelicState {
  public static boolean isEquipped(LivingEntity entity) {
    return CuriosApi.getCuriosInventory(entity)
        .map(handler -> handler.isEquipped(ModItems.RESONANCE_RELIC.get()))
        .orElse(false);
  }

  private RelicState() {}
}
