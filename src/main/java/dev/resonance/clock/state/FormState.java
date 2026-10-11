package dev.resonance.clock.state;

import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.api.FormMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/** Independent persistent relic form state. Invalid or absent data safely resolves to BASE. */
public final class FormState {
  static final String ROOT = ResonanceClock.MOD_ID + ":form";
  static final int DATA_VERSION = 1;

  public static FormMode get(ServerPlayer player) {
    return read(player.getPersistentData());
  }

  static FormMode read(CompoundTag persistent) {
    if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
      return FormMode.BASE;
    }
    CompoundTag state = persistent.getCompound(ROOT);
    if (!state.contains("version", Tag.TAG_INT) || state.getInt("version") != DATA_VERSION
        || !state.contains("mode", Tag.TAG_INT)) {
      return FormMode.BASE;
    }
    return FormMode.decode(state.getInt("mode")).orElse(FormMode.BASE);
  }

  public static void set(ServerPlayer player, FormMode mode) {
    write(player.getPersistentData(), mode);
  }

  static void write(CompoundTag persistent, FormMode mode) {
    CompoundTag state = new CompoundTag();
    state.putInt("version", DATA_VERSION);
    state.putInt("mode", mode.ordinal());
    persistent.put(ROOT, state);
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

  private FormState() {}
}
