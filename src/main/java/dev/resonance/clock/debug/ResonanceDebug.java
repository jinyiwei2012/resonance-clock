package dev.resonance.clock.debug;

import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.network.ModNetwork;
import dev.resonance.clock.state.RelicState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Runtime-only diagnostics for temporal request delivery and player state. */
@Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ResonanceDebug {
  private static final String TEMPORAL_ROOT = ResonanceClock.MOD_ID + ":temporal";
  private static final String CURSE_ROOT = ResonanceClock.MOD_ID + ":temporal_disorder";
  private static final long DETAIL_INTERVAL_TICKS = 20L;
  private static final Map<UUID, Long> LAST_ACCEPTED_DETAIL_TICKS = new HashMap<>();
  private static final Map<UUID, Long> LAST_REJECTED_DETAIL_TICKS = new HashMap<>();
  private static volatile boolean enabled;

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean value) {
    enabled = value;
    LAST_ACCEPTED_DETAIL_TICKS.clear();
    LAST_REJECTED_DETAIL_TICKS.clear();
    log("mode=" + (value ? "enabled" : "disabled"));
  }

  public static Snapshot snapshot(ServerPlayer player) {
    CompoundTag persistent = player.getPersistentData();
    CompoundTag temporal = compoundIfPresent(persistent, TEMPORAL_ROOT);
    CompoundTag curse = compoundIfPresent(persistent, CURSE_ROOT);
    boolean anchor = temporal != null && temporal.contains("snapshot", Tag.TAG_COMPOUND);
    boolean curseActive = curse != null && curse.contains("snapshot", Tag.TAG_COMPOUND);
    return new Snapshot(
        enabled,
        RelicState.isEquipped(player),
        ModNetwork.temporalTwistRequestClock(player),
        player.tickCount,
        ModNetwork.lastAcceptedTemporalTwistRequestClock(player),
        anchor,
        anchor ? Math.max(0, temporal.getInt("remainingTicks")) : 0,
        temporal != null && temporal.getBoolean("executing"),
        temporal == null ? 0 : Math.max(0, temporal.getInt("cooldownTicks")),
        curseActive,
        curseActive ? Math.max(0, curse.getInt("remainingTicks")) : 0,
        temporal == null ? -1 : temporal.getInt("version"),
        curse == null ? -1 : curse.getInt("version"));
  }

  public static void logTemporalTwistRequest(ServerPlayer player, boolean accepted) {
    if (!enabled || !allowDetail(player, accepted)) {
      return;
    }
    Snapshot snapshot = snapshot(player);
    log("request entityId=" + player.getId()
        + " received=true throttle=" + (accepted ? "accepted" : "rejected")
        + " globalTick=" + snapshot.globalTick()
        + " entityTick=" + snapshot.entityTick()
        + " throttleLast=" + snapshot.throttleLast()
        + " equipped=" + snapshot.equipped()
        + " anchor=" + snapshot.anchor()
        + " executing=" + snapshot.executing()
        + " cooldown=" + snapshot.cooldownTicks()
        + " curseActive=" + snapshot.curseActive());
  }

  @SubscribeEvent
  public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      logLifecycle("login", player);
    }
  }

  @SubscribeEvent
  public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      clearDetailState(player);
      logLifecycle("logout", player);
    }
  }

  @SubscribeEvent
  public static void onClone(PlayerEvent.Clone event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      clearDetailState(player);
      logLifecycle("clone", player);
    }
  }

  @SubscribeEvent
  public static void onDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      logLifecycle("death", player);
    }
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    enabled = false;
    LAST_ACCEPTED_DETAIL_TICKS.clear();
    LAST_REJECTED_DETAIL_TICKS.clear();
  }

  private static CompoundTag compoundIfPresent(CompoundTag persistent, String key) {
    return persistent.contains(key, Tag.TAG_COMPOUND) ? persistent.getCompound(key).copy() : null;
  }

  private static boolean allowDetail(ServerPlayer player, boolean accepted) {
    Map<UUID, Long> detailTicks = accepted ? LAST_ACCEPTED_DETAIL_TICKS : LAST_REJECTED_DETAIL_TICKS;
    long currentTick = ModNetwork.temporalTwistRequestClock(player);
    Long previousTick = detailTicks.get(player.getUUID());
    if (previousTick != null && currentTick >= previousTick && currentTick - previousTick < DETAIL_INTERVAL_TICKS) {
      return false;
    }
    detailTicks.put(player.getUUID(), currentTick);
    return true;
  }

  private static void clearDetailState(ServerPlayer player) {
    LAST_ACCEPTED_DETAIL_TICKS.remove(player.getUUID());
    LAST_REJECTED_DETAIL_TICKS.remove(player.getUUID());
  }

  private static void logLifecycle(String event, ServerPlayer player) {
    if (enabled) {
      log("lifecycle=" + event + " entityId=" + player.getId());
    }
  }

  private static void log(String message) {
    if (enabled || message.startsWith("mode=")) {
      System.getLogger(ResonanceDebug.class.getName()).log(System.Logger.Level.INFO, "RC-DEBUG " + message);
    }
  }

  public record Snapshot(boolean enabled, boolean equipped, long globalTick, int entityTick,
                         long throttleLast, boolean anchor, int remainingTicks, boolean executing,
                         int cooldownTicks, boolean curseActive, int curseRemainingTicks,
                         int temporalVersion, int curseVersion) {}

  private ResonanceDebug() {}
}
