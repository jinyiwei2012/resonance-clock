package dev.resonance.clock.state;

import dev.resonance.clock.config.ServerConfig;
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
      state.put("snapshot", TemporalSnapshot.capture(player).save());
      state.putInt("remainingTicks", ServerConfig.TWIST_DURATION_TICKS.get());
      state.putBoolean("executing", false);
      TemporalCurseService.clear(player, false);
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.started",
          ServerConfig.TWIST_DURATION_TICKS.get() / 20), true);
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
    TemporalRestoreExecutor.Result result = TemporalRestoreExecutor.restore(player, snapshot);
    if (result != TemporalRestoreExecutor.Result.RESTORED) {
      fail(player, state, switch (result) {
        case DIMENSION_UNAVAILABLE -> "message.resonance_clock.twist.dimension";
        case UNSAFE_DESTINATION -> "message.resonance_clock.twist.unsafe";
        case INVALID_SNAPSHOT -> "message.resonance_clock.twist.invalid_snapshot";
        case TELEPORT_FAILED -> "message.resonance_clock.twist.teleport_failed";
        case RESTORED -> throw new IllegalStateException("Unexpected successful failure result");
      });
      return;
    }
    TemporalState.clearAnchor(state);
    state.putInt("cooldownTicks", ServerConfig.TWIST_COOLDOWN_TICKS.get());
    player.displayClientMessage(Component.translatable("message.resonance_clock.twist.restored"), true);
  }

  private static void fail(ServerPlayer player, CompoundTag state, String messageKey) {
    TemporalState.clearAnchor(state);
    state.putInt("cooldownTicks", ServerConfig.TWIST_COOLDOWN_TICKS.get());
    player.displayClientMessage(Component.translatable(messageKey), true);
  }

  private static void notifyInvalidOnce(ServerPlayer player) {
    CompoundTag persistent = player.getPersistentData();
    if (!persistent.getBoolean(TemporalState.INVALID_NOTIFIED)) {
      persistent.putBoolean(TemporalState.INVALID_NOTIFIED, true);
      player.displayClientMessage(Component.translatable("message.resonance_clock.twist.invalid_snapshot"), true);
    }
  }

  private TemporalTwistService() {}
}
