package dev.resonance.clock.state;

import dev.resonance.clock.config.ServerConfig;
import dev.resonance.clock.api.FormMode;
import dev.resonance.clock.debug.ResonanceDebug;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
    synchronized (player.getPersistentData()) {
      if (TemporalState.validate(player.getPersistentData()) == TemporalState.Validation.INVALID) {
        notifyInvalidOnce(player);
        return StartResult.ACTIVE;
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
      int resolvedDuration = ServerConfig.TWIST_DURATION_TICKS.get();
      int resolvedCooldown = resolveCooldownTicks(FormState.get(player),
          ServerConfig.TWIST_COOLDOWN_TICKS.get(), ServerConfig.RESONANT_COOLDOWN_MULTIPLIER.get());
      state.put("snapshot", TemporalSnapshot.capture(player).save());
      state.putInt("remainingTicks", resolvedDuration);
      state.putInt("resolvedDurationTicks", resolvedDuration);
      state.putInt("resolvedCooldownTicks", resolvedCooldown);
      state.putBoolean("executing", false);
      TemporalCurseService.clear(player, false);
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.started",
          resolvedDuration / 20), true);
      return StartResult.STARTED;
    }
  }

  public static void tick(ServerPlayer player) {
    synchronized (player.getPersistentData()) {
      TemporalState.Validation validation = TemporalState.validate(player.getPersistentData());
      if (validation == TemporalState.Validation.INVALID) {
        notifyInvalidOnce(player);
        return;
      }
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
  }

  /** Revalidates persisted anchors before a newly connected player can use them. */
  public static void revalidateOnLogin(ServerPlayer player) {
    if (TemporalState.validate(player.getPersistentData()) == TemporalState.Validation.INVALID) {
      notifyInvalidOnce(player);
      return;
    }
    CompoundTag state = TemporalState.get(player);
    if (state.contains("snapshot")
        && (!ServerConfig.TWIST_ENABLED.get() || !RelicState.isEquipped(player))) {
      cancel(player, "disabled or relic unequipped");
    }
  }

  public static void cancel(ServerPlayer player, String reason) {
    if (TemporalState.validate(player.getPersistentData()) == TemporalState.Validation.INVALID) {
      TemporalState.clearPersistent(player.getPersistentData());
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.invalid_snapshot"), true);
      return;
    }
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
    TemporalRestoreExecutor.Result result;
    try {
      result = TemporalRestoreExecutor.restore(player, snapshot);
    } catch (RuntimeException exception) {
      ResonanceDebug.logException("restore-exception", player, exception);
      throw exception;
    }
    if (result != TemporalRestoreExecutor.Result.RESTORED) {
      fail(player, state, switch (result) {
        case DIMENSION_UNAVAILABLE -> "message.resonance_clock.twist.dimension";
        case UNSAFE_DESTINATION -> "message.resonance_clock.twist.unsafe";
        case INVALID_SNAPSHOT -> "message.resonance_clock.twist.invalid_snapshot";
        case TELEPORT_FAILED -> "message.resonance_clock.twist.teleport_failed";
        case RESTORED -> throw new IllegalStateException("Unexpected successful failure result");
      });
      ResonanceDebug.logRestoreResult(player, "twist", result);
      return;
    }
    int resolvedCooldown = TemporalState.resolvedCooldownTicks(state);
    TemporalState.clearAnchor(state);
    state.putInt("cooldownTicks", resolvedCooldown);
    player.displayClientMessage(Component.translatable("message.resonance_clock.twist.restored"), true);
    ResonanceDebug.logRestoreResult(player, "twist", result);
  }

  private static void fail(ServerPlayer player, CompoundTag state, String messageKey) {
    int resolvedCooldown = TemporalState.resolvedCooldownTicks(state);
    TemporalState.clearAnchor(state);
    state.putInt("cooldownTicks", resolvedCooldown);
    player.displayClientMessage(Component.translatable(messageKey), true);
  }

  private static void notifyInvalidOnce(ServerPlayer player) {
    CompoundTag persistent = player.getPersistentData();
    if (!persistent.getBoolean(TemporalState.INVALID_NOTIFIED)) {
      persistent.putBoolean(TemporalState.INVALID_NOTIFIED, true);
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.invalid_snapshot"), true);
    }
  }

  static int resolveCooldownTicks(FormMode mode, int baseCooldownTicks, double resonantMultiplier) {
    double multiplier = mode == FormMode.RESONANT ? resonantMultiplier : 1.0D;
    return Math.max(0, (int) Math.round(baseCooldownTicks * multiplier));
  }

  private TemporalTwistService() {}
}
