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
    ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("dimension"));
    if (dimensionId == null) {
      throw new IllegalArgumentException("Invalid snapshot dimension");
    }
    List<MobEffectInstance> effects = new ArrayList<>();
    for (Tag effectTag : tag.getList("effects", Tag.TAG_COMPOUND)) {
      MobEffectInstance effect = MobEffectInstance.load((CompoundTag) effectTag);
      if (effect != null && isManagedEffect(effect)) {
        effects.add(effect);
      }
    }
    return new TemporalSnapshot(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionId),
        tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getFloat("yaw"),
        tag.getFloat("pitch"), tag.getFloat("health"), tag.getInt("food"), tag.getFloat("saturation"),
        tag.getInt("air"), tag.getInt("fireTicks"), tag.getFloat("absorption"), effects);
  }
}
