package dev.resonance.clock.item;

import dev.resonance.clock.state.TemporalCurseService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TemporalPurifierItem extends Item {
  public TemporalPurifierItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    ItemStack stack = player.getItemInHand(hand);
    if (!(player instanceof ServerPlayer serverPlayer)) {
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
    if (!TemporalCurseService.clear(serverPlayer, true)) {
      return InteractionResultHolder.fail(stack);
    }
    if (!player.getAbilities().instabuild) {
      stack.shrink(1);
    }
    return InteractionResultHolder.consume(stack);
  }
}
