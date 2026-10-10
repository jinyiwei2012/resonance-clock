package dev.resonance.clock.state;

import dev.resonance.clock.ResonanceClock;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalCurseState {
  static final String ROOT = ResonanceClock.MOD_ID + ":temporal_disorder";
  static final String INVALID_NOTIFIED = ROOT + "_invalid_notified";
  static final int DATA_VERSION = 1;

  public enum Validation { INACTIVE, ACTIVE, INVALID }

  public static CompoundTag get(ServerPlayer player) {
    if (player.getPersistentData().contains(ROOT)
        && !player.getPersistentData().contains(ROOT, Tag.TAG_COMPOUND)) {
      return new CompoundTag();
    }
    return getOrCreate(player.getPersistentData());
  }

  /** Returns a defensive copy of existing validly typed state without creating or migrating a root. */
  public static Optional<CompoundTag> readExisting(ServerPlayer player) {
    CompoundTag persistent = player.getPersistentData();
    if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
      return Optional.empty();
    }
    return Optional.of(persistent.getCompound(ROOT).copy());
  }

  static CompoundTag getOrCreate(CompoundTag persistent) {
    if (!persistent.contains(ROOT)) {
      CompoundTag state = new CompoundTag();
      state.putInt("version", DATA_VERSION);
      persistent.put(ROOT, state);
    } else if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
      throw new IllegalStateException("Temporal Disorder root has the wrong NBT type");
    }
    return persistent.getCompound(ROOT);
  }

  public static boolean isActive(ServerPlayer player) {
    return validate(player.getPersistentData()) == Validation.ACTIVE;
  }

  static Validation validate(CompoundTag persistent) {
    if (!persistent.contains(ROOT)) {
      return Validation.INACTIVE;
    }
    if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
      return Validation.INVALID;
    }
    CompoundTag state = persistent.getCompound(ROOT);
    if (!state.contains("version", Tag.TAG_INT) || state.getInt("version") != DATA_VERSION) {
      return Validation.INVALID;
    }
    if (!state.contains("snapshot")) {
      return Validation.INACTIVE;
    }
    if (!state.contains("snapshot", Tag.TAG_COMPOUND)
        || !state.contains("remainingTicks", Tag.TAG_INT)
        || state.getInt("remainingTicks") < 0
        || !state.contains("executing", Tag.TAG_BYTE)) {
      return Validation.INVALID;
    }
    try {
      TemporalSnapshot.load(state.getCompound("snapshot"));
      return Validation.ACTIVE;
    } catch (RuntimeException exception) {
      return Validation.INVALID;
    }
  }

  public static void copyOnRespawn(ServerPlayer original, ServerPlayer replacement) {
    copyPersistentData(original.getPersistentData(), replacement.getPersistentData());
  }

  static void copyPersistentData(CompoundTag original, CompoundTag replacement) {
    Tag state = original.get(ROOT);
    if (state != null) {
      replacement.put(ROOT, state.copy());
    }
  }

  static boolean clearPersistent(CompoundTag persistent) {
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

  public static void clear(CompoundTag state) {
    state.remove("snapshot");
    state.remove("remainingTicks");
    state.remove("executing");
  }

  private TemporalCurseState() {}
}
