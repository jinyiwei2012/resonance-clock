package dev.resonance.clock.state;

import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.config.ServerConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalState {
  static final String ROOT = ResonanceClock.MOD_ID + ":temporal";
  static final String INVALID_NOTIFIED = ROOT + "_invalid_notified";
  static final int DATA_VERSION = 1;

  public enum Validation { INACTIVE, ACTIVE, INVALID }

  public record ValidationDetail(Validation validation, String reason, String field,
                                 String expected, String actual) {}

  public record PublicState(boolean active, int remainingTicks, int cooldownTicks,
                            int resolvedDurationTicks, int resolvedCooldownTicks) {}

  public static PublicState publicState(ServerPlayer player) {
    Validation validation = validate(player.getPersistentData());
    if (validation == Validation.INVALID) {
      return new PublicState(false, 0, 0, 0, 0);
    }
    CompoundTag state = get(player);
    boolean active = state.contains("snapshot");
    return new PublicState(active, Math.max(0, state.getInt("remainingTicks")),
        Math.max(0, state.getInt("cooldownTicks")),
        active ? resolvedDurationTicks(state) : 0, active ? resolvedCooldownTicks(state) : 0);
  }

  public static CompoundTag get(ServerPlayer player) {
    CompoundTag persistent = player.getPersistentData();
    if (!persistent.contains(ROOT)) {
      CompoundTag state = new CompoundTag();
      state.putInt("version", DATA_VERSION);
      persistent.put(ROOT, state);
    } else if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
      throw new IllegalStateException("Temporal Twist root has the wrong NBT type");
    }
    return persistent.getCompound(ROOT);
  }

  public static Validation validate(CompoundTag persistent) {
    return validateDetailed(persistent).validation();
  }

  public static ValidationDetail validateDetailed(CompoundTag persistent) {
    if (!persistent.contains(ROOT)) {
      return detail(Validation.INACTIVE, "absent", "root", "absent", "absent");
    }
    if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
      return detail(Validation.INVALID, "wrong_type", "root", "compound", typeName(persistent.get(ROOT)));
    }
    CompoundTag state = persistent.getCompound(ROOT);
    if (!state.contains("version", Tag.TAG_INT) || state.getInt("version") != DATA_VERSION) {
      return detail(Validation.INVALID, "version", "version", "int " + DATA_VERSION, "unsupported");
    }
    if (!state.contains("snapshot")) {
      return state.contains("cooldownTicks") && !state.contains("cooldownTicks", Tag.TAG_INT)
          ? detail(Validation.INVALID, "wrong_type", "cooldownTicks", "int",
              typeName(state.get("cooldownTicks")))
          : detail(Validation.INACTIVE, "inactive", "snapshot", "absent", "absent");
    }
    if ((state.contains("resolvedDurationTicks")
            && (!state.contains("resolvedDurationTicks", Tag.TAG_INT)
                || state.getInt("resolvedDurationTicks") < 0))
        || (state.contains("resolvedCooldownTicks")
            && (!state.contains("resolvedCooldownTicks", Tag.TAG_INT)
                || state.getInt("resolvedCooldownTicks") < 0))) {
      return detail(Validation.INVALID, "wrong_type", "resolvedTiming", "non-negative ints",
          "rejected");
    }
    if (!state.contains("snapshot", Tag.TAG_COMPOUND)
        || !state.contains("remainingTicks", Tag.TAG_INT)
        || state.getInt("remainingTicks") < 0
        || !state.contains("executing", Tag.TAG_BYTE)) {
      return detail(Validation.INVALID, "required_field", "anchor", "typed snapshot and state fields", "rejected");
    }
    try {
      TemporalSnapshot.load(state.getCompound("snapshot"));
      return detail(Validation.ACTIVE, "active", "snapshot", "valid", "valid");
    } catch (SnapshotValidationException exception) {
      return detail(Validation.INVALID, exception.reason().name().toLowerCase(), exception.field(),
          exception.expected(), exception.actual());
    } catch (RuntimeException exception) {
      return detail(Validation.INVALID, "invalid_snapshot", "snapshot", "valid", "rejected");
    }
  }

  private static ValidationDetail detail(Validation validation, String reason, String field,
      String expected, String actual) {
    return new ValidationDetail(validation, reason, field, expected, actual);
  }

  private static String typeName(Tag tag) {
    return tag == null ? "absent" : "tag(" + tag.getId() + ")";
  }

  public static void copyOnRespawn(ServerPlayer original, ServerPlayer replacement) {
    Tag state = original.getPersistentData().get(ROOT);
    if (state != null) {
      replacement.getPersistentData().put(ROOT, state.copy());
    }
  }

  public static boolean clearPersistent(CompoundTag persistent) {
    if (!persistent.contains(ROOT)) {
      return false;
    }
    persistent.remove(ROOT);
    persistent.remove(INVALID_NOTIFIED);
    CompoundTag state = new CompoundTag();
    state.putInt("version", DATA_VERSION);
    persistent.put(ROOT, state);
    return true;
  }

  public static void clearAnchor(CompoundTag state) {
    state.remove("snapshot");
    state.remove("remainingTicks");
    state.remove("executing");
    state.remove("resolvedDurationTicks");
    state.remove("resolvedCooldownTicks");
  }

  static int resolvedDurationTicks(CompoundTag state) {
    return state.contains("resolvedDurationTicks", Tag.TAG_INT)
        ? Math.max(0, state.getInt("resolvedDurationTicks"))
        : ServerConfig.TWIST_DURATION_TICKS.get();
  }

  static int resolvedCooldownTicks(CompoundTag state) {
    return state.contains("resolvedCooldownTicks", Tag.TAG_INT)
        ? Math.max(0, state.getInt("resolvedCooldownTicks"))
        : ServerConfig.TWIST_COOLDOWN_TICKS.get();
  }

  private TemporalState() {}
}
