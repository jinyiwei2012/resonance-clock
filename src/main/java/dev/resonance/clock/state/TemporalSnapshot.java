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
  private static final int MAX_STORED_FOOD = 1_000_000;
  private static final float MAX_STORED_SATURATION = 1_000_000.0F;
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
    require(dimensionId != null, "dimension", "valid resource location");
    require(finite(x) && Math.abs(x) <= MAX_HORIZONTAL_COORDINATE, "x", "finite world coordinate");
    require(finite(y) && Math.abs(y) <= MAX_ABSOLUTE_STORED_Y, "y", "finite world coordinate");
    require(finite(z) && Math.abs(z) <= MAX_HORIZONTAL_COORDINATE, "z", "finite world coordinate");
    require(Float.isFinite(yaw), "yaw", "finite float");
    require(Float.isFinite(pitch), "pitch", "finite float");
    require(Float.isFinite(health) && health > 0.0F && health <= MAX_STORED_HEALTH, "health", "positive bounded float");
    require(food >= 0 && food <= MAX_STORED_FOOD, "food", "non-negative bounded int");
    require(Float.isFinite(saturation) && saturation >= 0.0F && saturation <= MAX_STORED_SATURATION,
        "saturation", "non-negative bounded float");
    require(Math.abs((long) air) <= MAX_STORED_AIR, "air", "bounded int");
    require(Math.abs((long) fireTicks) <= MAX_STORED_FIRE_TICKS, "fireTicks", "bounded int");
    require(Float.isFinite(absorption) && absorption >= 0.0F && absorption <= MAX_STORED_ABSORPTION,
        "absorption", "non-negative bounded float");
    ListTag storedEffects = (ListTag) tag.get("effects");
    if (!storedEffects.isEmpty() && storedEffects.getElementType() != Tag.TAG_COMPOUND) {
      throw invalid("effects", "list of compounds");
    }
    List<MobEffectInstance> effects = new ArrayList<>();
    for (Tag effectTag : storedEffects) {
      MobEffectInstance effect;
      try {
        effect = MobEffectInstance.load((CompoundTag) effectTag);
      } catch (RuntimeException exception) {
        throw new SnapshotValidationException(SnapshotValidationException.Reason.INVALID_EFFECT,
            "effects", "valid effect compound", "invalid compound");
      }
      if (effect != null && isManagedEffect(effect)) {
        effects.add(effect);
      }
    }
    return new TemporalSnapshot(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId),
        x, y, z, yaw, pitch, health, food, saturation, air, fireTicks, absorption, effects);
  }

  private static void requireType(CompoundTag tag, String key, int type) {
    if (!tag.contains(key)) {
      throw new SnapshotValidationException(SnapshotValidationException.Reason.MISSING_FIELD,
          key, tagTypeName(type), "absent");
    }
    if (!tag.contains(key, type)) {
      throw new SnapshotValidationException(SnapshotValidationException.Reason.WRONG_TYPE,
          key, tagTypeName(type), tagTypeName(tag.get(key).getId()));
    }
  }

  private static void require(boolean valid, String field, String expected) {
    if (!valid) {
      throw invalid(field, expected);
    }
  }

  private static SnapshotValidationException invalid(String field, String expected) {
    return new SnapshotValidationException(SnapshotValidationException.Reason.INVALID_VALUE,
        field, expected, "rejected");
  }

  private static String tagTypeName(int type) {
    return switch (type) {
      case Tag.TAG_BYTE -> "byte";
      case Tag.TAG_INT -> "int";
      case Tag.TAG_FLOAT -> "float";
      case Tag.TAG_DOUBLE -> "double";
      case Tag.TAG_STRING -> "string";
      case Tag.TAG_LIST -> "list";
      case Tag.TAG_COMPOUND -> "compound";
      default -> "tag(" + type + ")";
    };
  }

  private static boolean finite(double value) {
    return Double.isFinite(value);
  }
}
