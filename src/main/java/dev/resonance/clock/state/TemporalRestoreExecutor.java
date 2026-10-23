package dev.resonance.clock.state;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

/** Stateless snapshot restoration shared by all temporal features. */
public final class TemporalRestoreExecutor {
  public enum Result { RESTORED, INVALID_SNAPSHOT, DIMENSION_UNAVAILABLE, UNSAFE_DESTINATION, TELEPORT_FAILED }

  public static Result restore(ServerPlayer player, TemporalSnapshot snapshot) {
    if (snapshot == null || !Double.isFinite(snapshot.x()) || !Double.isFinite(snapshot.y())
        || !Double.isFinite(snapshot.z()) || Math.abs(snapshot.x()) > 29_999_984.0D
        || Math.abs(snapshot.z()) > 29_999_984.0D) {
      return Result.INVALID_SNAPSHOT;
    }
    ServerLevel target = player.server.getLevel(snapshot.dimension());
    if (target == null) {
      return Result.DIMENSION_UNAVAILABLE;
    }
    if (snapshot.y() <= target.getMinBuildHeight()
        || snapshot.y() + player.getDimensions(player.getPose()).height >= target.getMaxBuildHeight()) {
      return Result.UNSAFE_DESTINATION;
    }
    BlockPos requested = BlockPos.containing(snapshot.x(), snapshot.y(), snapshot.z());
    if (!target.getWorldBorder().isWithinBounds(requested)) {
      return Result.UNSAFE_DESTINATION;
    }
    ChunkPos chunk = new ChunkPos(requested);
    target.getChunk(chunk.x, chunk.z);
    Optional<BlockPos> safe = findSafePosition(target, player, requested);
    if (safe.isEmpty()) {
      return Result.UNSAFE_DESTINATION;
    }
    BlockPos destination = safe.get();
    player.teleportTo(target, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
        snapshot.yaw(), snapshot.pitch());
    if (!player.isAlive() || player.connection == null || player.level() != target
        || player.position().distanceToSqr(destination.getX() + 0.5D, destination.getY(),
            destination.getZ() + 0.5D) > 1.0E-6D) {
      return Result.TELEPORT_FAILED;
    }
    restoreManagedEffects(player, snapshot);
    player.setHealth(Math.min(snapshot.health(), player.getMaxHealth()));
    player.getFoodData().setFoodLevel(snapshot.food());
    player.getFoodData().setSaturation(snapshot.saturation());
    player.setAirSupply(Math.min(snapshot.air(), player.getMaxAirSupply()));
    player.setRemainingFireTicks(snapshot.fireTicks());
    player.setAbsorptionAmount(snapshot.absorption());
    return Result.RESTORED;
  }

  /** Package-private so GameTests can verify destination resolution without a networked player. */
  static Optional<BlockPos> findSafePosition(ServerLevel level, ServerPlayer player, BlockPos origin) {
    // First pass: try the exact snapshot position and nearby vertical offsets,
    // preferring positions above solid ground.
    for (int offset = 0; offset <= 4; offset++) {
      for (int direction : offset == 0 ? new int[] {0} : new int[] {offset, -offset}) {
        BlockPos candidate = origin.offset(0, direction, 0);
        if (candidate.getY() <= level.getMinBuildHeight()
            || candidate.getY() + player.getDimensions(player.getPose()).height >= level.getMaxBuildHeight()
            || !level.getWorldBorder().isWithinBounds(candidate)) {
          continue;
        }
        if (!level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
          continue;
        }
        AABB bounds = player.getDimensions(player.getPose()).makeBoundingBox(
            candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
        if (level.noCollision(player, bounds) && !level.containsAnyLiquid(bounds)) {
          return Optional.of(candidate);
        }
      }
    }
    // Second pass: expand the search horizontally and vertically for solid ground.
    // This handles the case where the player triggered the twist while suspended
    // in air (e.g. over water, lava, or floating) and the snapshot position is
    // far from any walkable surface.
    for (int radius = 1; radius <= 8; radius++) {
      for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
          if (dx == 0 && dz == 0) {
            continue;
          }
          double distanceSq = dx * dx + dz * dz;
          if (distanceSq > radius * radius) {
            continue;
          }
          for (int yOffset = -8; yOffset <= 8; yOffset++) {
            BlockPos candidate = origin.offset(dx, yOffset, dz);
            if (candidate.getY() <= level.getMinBuildHeight()
                || candidate.getY() + player.getDimensions(player.getPose()).height >= level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(candidate)) {
              continue;
            }
            if (!level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP)) {
              continue;
            }
            AABB bounds = player.getDimensions(player.getPose()).makeBoundingBox(
                candidate.getX() + 0.5D, candidate.getY(), candidate.getZ() + 0.5D);
            if (level.noCollision(player, bounds) && !level.containsAnyLiquid(bounds)) {
              return Optional.of(candidate);
            }
          }
        }
      }
    }
    // Third pass: if no solid ground was found, fall back to the original snapshot
    // position as long as it is not inside a solid block or liquid. This allows
    // temporal twist to function when triggered from mid-air over large bodies of
    // water or other non-solid terrain.
    AABB originBounds = player.getDimensions(player.getPose()).makeBoundingBox(
        origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
    if (level.noCollision(player, originBounds) && !level.containsAnyLiquid(originBounds)) {
      return Optional.of(origin);
    }
    return Optional.empty();
  }

  private static void restoreManagedEffects(ServerPlayer player, TemporalSnapshot snapshot) {
    player.getActiveEffects().stream()
        .filter(TemporalSnapshot::isManagedEffect)
        .map(MobEffectInstance::getEffect)
        .toList()
        .forEach(player::removeEffect);
    snapshot.effects().stream()
        .filter(TemporalSnapshot::isManagedEffect)
        .map(MobEffectInstance::new)
        .forEach(player::addEffect);
  }

  private TemporalRestoreExecutor() {}
}
