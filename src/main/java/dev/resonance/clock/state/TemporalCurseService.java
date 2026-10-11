package dev.resonance.clock.state;

import dev.resonance.clock.config.ServerConfig;
import dev.resonance.clock.debug.ResonanceDebug;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class TemporalCurseService {
  public enum ApplyResult { APPLIED, TWIST_ACTIVE, ALREADY_CURSED }

  enum CycleAction { FROZEN, WAITING, RESTORE }

  public static ApplyResult apply(ServerPlayer player) {
    synchronized (player.getPersistentData()) {
      if (TemporalState.validate(player.getPersistentData()) != TemporalState.Validation.INACTIVE) {
        return ApplyResult.TWIST_ACTIVE;
      }
      if (TemporalCurseState.validate(player.getPersistentData()) == TemporalCurseState.Validation.INVALID) {
        return ApplyResult.ALREADY_CURSED;
      }
      CompoundTag state = TemporalCurseState.get(player);
      if (state.contains("snapshot")) {
        return ApplyResult.ALREADY_CURSED;
      }
      state.put("snapshot", TemporalSnapshot.capture(player).save());
      restartCycle(state);
      player.displayClientMessage(Component.translatable("message.resonance_clock.curse.applied"), false);
      return ApplyResult.APPLIED;
    }
  }

  public static void tick(ServerPlayer player) {
    if (!ServerConfig.CURSE_ENABLED.get() || !player.isAlive()) {
      return;
    }
    synchronized (player.getPersistentData()) {
      TemporalCurseState.Validation validation = TemporalCurseState.validate(player.getPersistentData());
      if (validation == TemporalCurseState.Validation.INVALID) {
        notifyInvalidOnce(player);
        return;
      }
      if (validation != TemporalCurseState.Validation.ACTIVE
          || TemporalState.validate(player.getPersistentData()) != TemporalState.Validation.INACTIVE) {
        return;
      }
      CompoundTag state = TemporalCurseState.get(player);
      CycleAction action = advanceCycle(state, true, true);
      if (action != CycleAction.RESTORE) {
        return;
      }
      state.putBoolean("executing", true);
      TemporalRestoreExecutor.Result result;
      try {
        result = TemporalRestoreExecutor.restore(player, TemporalSnapshot.load(state.getCompound("snapshot")));
      } catch (RuntimeException exception) {
        ResonanceDebug.logException("curse-restore-exception", player, exception);
        result = TemporalRestoreExecutor.Result.INVALID_SNAPSHOT;
      }
      restartCycle(state);
      player.displayClientMessage(Component.translatable(
          result == TemporalRestoreExecutor.Result.RESTORED
              ? "message.resonance_clock.curse.restored"
              : "message.resonance_clock.curse.restore_failed"), false);
      ResonanceDebug.logRestoreResult(player, "curse", result);
    }
  }

  public static boolean clear(ServerPlayer player, boolean notify) {
    if (TemporalCurseState.validate(player.getPersistentData()) == TemporalCurseState.Validation.INACTIVE) {
      if (notify) {
        player.displayClientMessage(Component.translatable("message.resonance_clock.curse.none"), true);
      }
      return false;
    }
    synchronized (player.getPersistentData()) {
      TemporalCurseState.clearPersistent(player.getPersistentData());
      if (notify) {
        player.displayClientMessage(Component.translatable("message.resonance_clock.curse.cleared"), false);
      }
      return true;
    }
  }

  private static void restartCycle(CompoundTag state) {
    resetCycle(state, ServerConfig.CURSE_INTERVAL_TICKS.get());
  }

  static CycleAction advanceCycle(CompoundTag state, boolean enabled, boolean alive) {
    if (!enabled || !alive || state.getBoolean("executing")) {
      return CycleAction.FROZEN;
    }
    int remaining = state.getInt("remainingTicks") - 1;
    state.putInt("remainingTicks", remaining);
    return remaining <= 0 ? CycleAction.RESTORE : CycleAction.WAITING;
  }

  static void resetCycle(CompoundTag state, int interval) {
    state.putInt("remainingTicks", interval);
    state.putBoolean("executing", false);
  }

  private static void notifyInvalidOnce(ServerPlayer player) {
    CompoundTag persistent = player.getPersistentData();
    if (!persistent.getBoolean(TemporalCurseState.INVALID_NOTIFIED)) {
      persistent.putBoolean(TemporalCurseState.INVALID_NOTIFIED, true);
      player.displayClientMessage(Component.translatable("message.resonance_clock.curse.restore_failed"), false);
    }
  }

  private TemporalCurseService() {}
}
