package dev.resonance.clock.state;

import dev.resonance.clock.config.ServerConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;

public record TemporalSnapshot(
    ResourceKey<Level> dimension,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    float health,
    int food,
    float saturation,
    int air,
    int fireTicks,
    float absorption,
    List<MobEffectInstance> effects) {
  private static final double MAX_HORIZONTAL_COORDINATE = 29_999_984.0D;
  private static final double MAX_ABSOLUTE_STORED_Y = 20_000_000.0D;
  private static final float MAX_STORED_HEALTH = 1_000_000.0F;
  private static final int MAX_STORED_AIR = 1_000_000;
  private static final int MAX_STORED_FIRE_TICKS = 1_728_000;
  private static final float MAX_STORED_ABSORPTION = 1_000_000.0F;

  public static TemporalSnapshot capture(ServerPlayer player) {
    Set<ResourceLocation> exclusions = ServerConfig.EXCLUDED_EFFECTS.get().stream()
        .map(ResourceLocation::tryParse)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toSet());
    List<MobEffectInstance> effects = player.getActiveEffects().stream()
        .filter(effect -> isManagedEffect(effect, exclusions))
        .map(MobEffectInstance::new)
        .toList();
    return new TemporalSnapshot(player.level().dimension(), player.getX(), player.getY(), player.getZ(),
        player.getYRot(), player.getXRot(), player.getHealth(), player.getFoodData().getFoodLevel(),
        player.getFoodData().getSaturationLevel(), player.getAirSupply(), player.getRemainingFireTicks(),
        player.getAbsorptionAmount(), effects);
  }

  /**
   * Only vanilla effects are restored by default. Third-party effects remain untouched unless a
   * future explicit compatibility policy is added; configured IDs are always left untouched.
   */
  public static boolean isManagedEffect(MobEffectInstance effect) {
    Set<ResourceLocation> exclusions = ServerConfig.EXCLUDED_EFFECTS.get().stream()
        .map(ResourceLocation::tryParse)
        .filter(java.util.Objects::nonNull)
        .collect(Collectors.toSet());
    return isManagedEffect(effect, exclusions);
  }

  private static boolean isManagedEffect(MobEffectInstance effect, Set<ResourceLocation> exclusions) {
    ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
    return id != null && "minecraft".equals(id.getNamespace()) && !exclusions.contains(id);
  }

  public CompoundTag save() {
    CompoundTag tag = new CompoundTag();
    tag.putString("dimension", dimension.location().toString());
    tag.putDouble("x", x);
    tag.putDouble("y", y);
    tag.putDouble("z", z);
    tag.putFloat("yaw", yaw);
    tag.putFloat("pitch", pitch);
    tag.putFloat("health", health);
    tag.putInt("food", food);
    tag.putFloat("saturation", saturation);
    tag.putInt("air", air);
    tag.putInt("fireTicks", fireTicks);
    tag.putFloat("absorption", absorption);
    ListTag effectTags = new ListTag();
    effects.forEach(effect -> effectTags.add(effect.save(new CompoundTag())));
    tag.put("effects", effectTags);
    return tag;
  }

  public static TemporalSnapshot load(CompoundTag tag) {
    requireType(tag, "dimension", Tag.TAG_STRING);
    requireType(tag, "x", Tag.TAG_DOUBLE);
    requireType(tag, "y", Tag.TAG_DOUBLE);
    requireType(tag, "z", Tag.TAG_DOUBLE);
    requireType(tag, "yaw", Tag.TAG_FLOAT);
    requireType(tag, "pitch", Tag.TAG_FLOAT);
    requireType(tag, "health", Tag.TAG_FLOAT);
    requireType(tag, "food", Tag.TAG_INT);
    requireType(tag, "saturation", Tag.TAG_FLOAT);
    requireType(tag, "air", Tag.TAG_INT);
    requireType(tag, "fireTicks", Tag.TAG_INT);
    requireType(tag, "absorption", Tag.TAG_FLOAT);
    requireType(tag, "effects", Tag.TAG_LIST);
    ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
    double x = tag.getDouble("x");
    double y = tag.getDouble("y");
    double z = tag.getDouble("z");
    float yaw = tag.getFloat("yaw");
    float pitch = tag.getFloat("pitch");
    float health = tag.getFloat("health");
    int food = tag.getInt("food");
    float saturation = tag.getFloat("saturation");
    int air = tag.getInt("air");
    int fireTicks = tag.getInt("fireTicks");
    float absorption = tag.getFloat("absorption");
    if (dimensionId == null || !finite(x) || !finite(y) || !finite(z) || !Float.isFinite(yaw)
        || !Float.isFinite(pitch) || !Float.isFinite(health) || !Float.isFinite(saturation)
        || !Float.isFinite(absorption) || Math.abs(x) > MAX_HORIZONTAL_COORDINATE
        || Math.abs(z) > MAX_HORIZONTAL_COORDINATE || Math.abs(y) > MAX_ABSOLUTE_STORED_Y
        || pitch < -90.0F || pitch > 90.0F || health <= 0.0F || health > MAX_STORED_HEALTH
        || food < 0 || food > 20 || saturation < 0.0F || saturation > 20.0F
        || air < 0 || air > MAX_STORED_AIR || fireTicks < 0 || fireTicks > MAX_STORED_FIRE_TICKS
        || absorption < 0.0F || absorption > MAX_STORED_ABSORPTION) {
      throw new IllegalArgumentException("Invalid snapshot dimension");
    }
    ListTag storedEffects = (ListTag) tag.get("effects");
    if (!storedEffects.isEmpty() && storedEffects.getElementType() != Tag.TAG_COMPOUND) {
      throw new IllegalArgumentException("Snapshot effects must be compounds");
    }
    List<MobEffectInstance> effects = new ArrayList<>();
    for (Tag effectTag : storedEffects) {
      MobEffectInstance effect;
      try {
        effect = MobEffectInstance.load((CompoundTag) effectTag);
      } catch (RuntimeException exception) {
        throw new IllegalArgumentException("Invalid snapshot effect", exception);
      }
      if (effect != null && isManagedEffect(effect)) {
        effects.add(effect);
      }
    }
    return new TemporalSnapshot(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId),
        x, y, z, yaw, pitch, health, food, saturation, air, fireTicks, absorption, effects);
  }

  private static void requireType(CompoundTag tag, String key, int type) {
    if (!tag.contains(key, type)) {
      throw new IllegalArgumentException("Missing or incorrectly typed snapshot field: " + key);
    }
  }

  private static boolean finite(double value) {
    return Double.isFinite(value);
  }
}
