package dev.resonance.clock.state;

import dev.resonance.clock.ResonanceClock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalState {
  private static final String ROOT = ResonanceClock.MOD_ID + ":temporal";
  private static final int DATA_VERSION = 1;

  public record PublicState(boolean active, int remainingTicks, int cooldownTicks) {}

  public static PublicState publicState(ServerPlayer player) {
    CompoundTag state = get(player);
    return new PublicState(state.contains("snapshot"), Math.max(0, state.getInt("remainingTicks")),
        Math.max(0, state.getInt("cooldownTicks")));
  }

  public static CompoundTag get(ServerPlayer player) {
    CompoundTag persistent = player.getPersistentData();
    if (!persistent.contains(ROOT)) {
      CompoundTag state = new CompoundTag();
      state.putInt("version", DATA_VERSION);
      persistent.put(ROOT, state);
    }
    return persistent.getCompound(ROOT);
  }

  public static void copyOnRespawn(ServerPlayer original, ServerPlayer replacement) {
    if (original.getPersistentData().contains(ROOT)) {
      replacement.getPersistentData().put(ROOT, original.getPersistentData().getCompound(ROOT).copy());
    }
  }

  public static void clearAnchor(CompoundTag state) {
    state.remove("snapshot");
    state.remove("remainingTicks");
    state.remove("executing");
  }

  private TemporalState() {}
}
