package dev.resonance.clock.state;

import dev.resonance.clock.config.ServerConfig;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public final class TemporalTwistService {
  public enum StartResult { STARTED, DISABLED, NOT_EQUIPPED, ACTIVE, COOLDOWN }

  public static StartResult start(ServerPlayer player) {
    if (!ServerConfig.TWIST_ENABLED.get()) {
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.disabled"), true);
      return StartResult.DISABLED;
    }
    if (!RelicState.isEquipped(player)) {
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.not_equipped"), true);
      return StartResult.NOT_EQUIPPED;
    }
    CompoundTag state = TemporalState.get(player);
    if (state.contains("snapshot")) {
      return StartResult.ACTIVE;
    }
    int cooldown = state.getInt("cooldownTicks");
    if (cooldown > 0) {
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.cooldown", (cooldown + 19) / 20), true);
      return StartResult.COOLDOWN;
    }
    state.put("snapshot", TemporalSnapshot.capture(player).save());
    state.putInt("remainingTicks", ServerConfig.TWIST_DURATION_TICKS.get());
    state.putBoolean("executing", false);
    player.displayClientMessage(Component.translatable("message.resonance_clock.twist.started",
        ServerConfig.TWIST_DURATION_TICKS.get() / 20), true);
    return StartResult.STARTED;
  }

  public static void tick(ServerPlayer player) {
    CompoundTag state = TemporalState.get(player);
    if (state.getInt("cooldownTicks") > 0) {
      state.putInt("cooldownTicks", state.getInt("cooldownTicks") - 1);
    }
    if (!state.contains("snapshot")) {
      return;
    }
    if (!ServerConfig.TWIST_ENABLED.get() || !RelicState.isEquipped(player)) {
      cancel(player, "disabled or relic unequipped");
      return;
    }
    int remaining = state.getInt("remainingTicks") - 1;
    state.putInt("remainingTicks", remaining);
    if (remaining <= 0) {
      restore(player, state);
    }
  }

  /** Revalidates persisted anchors before a newly connected player can use them. */
  public static void revalidateOnLogin(ServerPlayer player) {
    CompoundTag state = TemporalState.get(player);
    if (state.contains("snapshot")
        && (!ServerConfig.TWIST_ENABLED.get() || !RelicState.isEquipped(player))) {
      cancel(player, "disabled or relic unequipped");
    }
  }

  public static void cancel(ServerPlayer player, String reason) {
    CompoundTag state = TemporalState.get(player);
    if (state.contains("snapshot")) {
      TemporalState.clearAnchor(state);
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.cancelled", reason), true);
    }
  }

  private static void restore(ServerPlayer player, CompoundTag state) {
    if (state.getBoolean("executing")) {
      return;
    }
    state.putBoolean("executing", true);
    TemporalSnapshot snapshot;
    try {
      snapshot = TemporalSnapshot.load(state.getCompound("snapshot"));
    } catch (RuntimeException exception) {
      fail(player, state, "message.resonance_clock.twist.invalid_snapshot");
      return;
    }
    ServerLevel target = player.server.getLevel(snapshot.dimension());
    if (target == null) {
      fail(player, state, "message.resonance_clock.twist.dimension");
      return;
    }
    BlockPos requested = BlockPos.containing(snapshot.x(), snapshot.y(), snapshot.z());
    ChunkPos chunk = new ChunkPos(requested);
    target.getChunk(chunk.x, chunk.z); // One target chunk is the hard loading budget per activation.
    Optional<BlockPos> safe = findSafePosition(target, player, requested);
    if (safe.isEmpty()) {
      fail(player, state, "message.resonance_clock.twist.unsafe");
      return;
    }
    BlockPos destination = safe.get();
    player.teleportTo(target, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
        snapshot.yaw(), snapshot.pitch());
    if (!player.isAlive() || player.connection == null || player.level() != target
        || player.position().distanceToSqr(destination.getX() + 0.5D, destination.getY(),
            destination.getZ() + 0.5D) > 1.0E-6D) {
      fail(player, state, "message.resonance_clock.twist.teleport_failed");
      return;
    }
    restoreManagedEffects(player, snapshot);
    player.setHealth(Math.min(snapshot.health(), player.getMaxHealth()));
    player.getFoodData().setFoodLevel(snapshot.food());
    player.getFoodData().setSaturation(snapshot.saturation());
    player.setAirSupply(Math.min(snapshot.air(), player.getMaxAirSupply()));
    player.setRemainingFireTicks(snapshot.fireTicks());
    player.setAbsorptionAmount(snapshot.absorption());
    TemporalState.clearAnchor(state);
    state.putInt("cooldownTicks", ServerConfig.TWIST_COOLDOWN_TICKS.get());
    player.displayClientMessage(Component.translatable("message.resonance_clock.twist.restored"), true);
  }

  private static Optional<BlockPos> findSafePosition(ServerLevel level, ServerPlayer player, BlockPos origin) {
    for (int offset = 0; offset <= 4; offset++) {
      for (int direction : offset == 0 ? new int[] {0} : new int[] {offset, -offset}) {
        BlockPos candidate = origin.offset(0, direction, 0);
        if (candidate.getY() <= level.getMinBuildHeight()
            || candidate.getY() + player.getDimensions(player.getPose()).height >= level.getMaxBuildHeight()
            || !level.getWorldBorder().isWithinBounds(candidate)
            || !level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
          continue;
        }
        AABB bounds = player.getDimensions(player.getPose()).makeBoundingBox(
            candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
        if (level.noCollision(player, bounds) && !level.containsAnyLiquid(bounds)) {
          return Optional.of(candidate);
        }
      }
    }
    return Optional.empty();
  }

  private static void restoreManagedEffects(ServerPlayer player, TemporalSnapshot snapshot) {
    player.getActiveEffects().stream()
        .filter(TemporalSnapshot::isManagedEffect)
        .map(MobEffectInstance::getEffect)
        .toList()
        .forEach(player::removeEffect);
    for (MobEffectInstance effect : snapshot.effects()) {
      if (TemporalSnapshot.isManagedEffect(effect)) {
        player.addEffect(new MobEffectInstance(effect));
      }
    }
  }

  private static void fail(ServerPlayer player, CompoundTag state, String messageKey) {
    TemporalState.clearAnchor(state);
    state.putInt("cooldownTicks", ServerConfig.TWIST_COOLDOWN_TICKS.get());
    player.displayClientMessage(Component.translatable(messageKey), true);
  }

  private TemporalTwistService() {}
}
