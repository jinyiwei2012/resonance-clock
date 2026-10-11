package dev.resonance.clock.debug;

import com.mojang.logging.LogUtils;
import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.network.ModNetwork;
import dev.resonance.clock.state.RelicState;
import dev.resonance.clock.state.TemporalCurseState;
import dev.resonance.clock.state.TemporalState;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Runtime-only, session-scoped diagnostics that cannot alter gameplay outcomes. */
@Mod.EventBusSubscriber(modid = ResonanceClock.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ResonanceDebug {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String TEMPORAL_ROOT = ResonanceClock.MOD_ID + ":temporal";
  private static final String CURSE_ROOT = ResonanceClock.MOD_ID + ":temporal_disorder";
  private static final long DETAIL_INTERVAL_TICKS = 20L;
  private static final DiagnosticSampling DETAIL_SAMPLING = new DiagnosticSampling(DETAIL_INTERVAL_TICKS);
  private static final AtomicLong REQUEST_IDS = new AtomicLong();
  private static volatile boolean enabled;

  public static boolean isEnabled() {
    return enabled;
  }

  public static void setEnabled(boolean value) {
    enabled = value;
    DiagnosticGuard.runBestEffort(() -> {
      DETAIL_SAMPLING.clear();
      LOGGER.info("RC-DEBUG mode={}", value ? "enabled" : "disabled");
    });
  }

  /** Called on the server work queue; this is not a client or cross-thread correlation ID. */
  public static long beginServerRequest() {
    return enabled ? REQUEST_IDS.incrementAndGet() : 0L;
  }

  public static void logRequestResult(ServerPlayer player, long requestId, boolean accepted,
      Object startResult) {
    if (requestId == 0L) {
      return;
    }
    DiagnosticGuard.runBestEffort(() -> logSampled(player, accepted ? "request-accepted" : "request-rejected",
        "requestId=" + requestId + " stage=server-enqueued-result thread="
            + Thread.currentThread().getName() + " entityId=" + player.getId() + " accepted=" + accepted
            + " startResult=" + (startResult == null ? "NOT_INVOKED" : startResult),
        "request diagnostic snapshot stack"));
  }

  public static void logRestoreResult(ServerPlayer player, String feature, Object result) {
    DiagnosticGuard.runBestEffort(() -> logSampled(player, "restore-" + feature,
        "stage=restore-result feature=" + feature + " thread=" + Thread.currentThread().getName()
            + " entityId=" + player.getId() + " restoreResult=" + result,
        "restore diagnostic snapshot stack"));
  }

  /** Logs the original exception object so its cause and suppressed chains remain intact. */
  public static void logException(String stage, ServerPlayer player, RuntimeException exception) {
    DiagnosticGuard.runBestEffort(() -> {
      if (!enabled) {
        return;
      }
      DiagnosticSampling.Decision decision = DETAIL_SAMPLING.allow(player.getUUID(), "exception-" + stage,
          ModNetwork.temporalTwistRequestClock(player));
      if (!decision.allowed()) {
        return;
      }
      if (decision.skippedCount() > 0) {
        LOGGER.info("RC-DEBUG exception-summary entityId={} stage={} skipped={} windowTicks={}", player.getId(),
            stage, decision.skippedCount(), DETAIL_INTERVAL_TICKS);
      }
      LOGGER.error("RC-DEBUG stage={} thread={} entityId={} exception follows", stage,
          Thread.currentThread().getName(), player.getId(), exception);
    });
  }

  /** Reads existing roots only; on diagnostic failure returns an unavailable snapshot. */
  public static Snapshot snapshot(ServerPlayer player) {
    try {
      CompoundTag persistent = player.getPersistentData();
      CompoundTag temporal = compoundIfPresent(persistent, TEMPORAL_ROOT);
      CompoundTag curse = compoundIfPresent(persistent, CURSE_ROOT);
      boolean anchor = temporal != null && temporal.contains("snapshot", Tag.TAG_COMPOUND);
      boolean curseActive = curse != null && curse.contains("snapshot", Tag.TAG_COMPOUND);
      TemporalState.ValidationDetail temporalDetail = TemporalState.validateDetailed(persistent);
      TemporalCurseState.ValidationDetail curseDetail = TemporalCurseState.validateDetailed(persistent);
      return new Snapshot(true, RelicState.isEquipped(player), ModNetwork.temporalTwistRequestClock(player),
          player.tickCount, ModNetwork.lastAcceptedTemporalTwistRequestClock(player),
          rootType(persistent, TEMPORAL_ROOT), rootType(persistent, CURSE_ROOT),
          temporalDetail.validation(), curseDetail.validation(), temporalDetail.reason(), temporalDetail.field(),
          temporalDetail.expected(), temporalDetail.actual(), curseDetail.reason(), curseDetail.field(),
          curseDetail.expected(), curseDetail.actual(), anchor,
          anchor ? Math.max(0, temporal.getInt("remainingTicks")) : 0,
          temporal != null && temporal.getBoolean("executing"),
          temporal == null ? 0 : Math.max(0, temporal.getInt("cooldownTicks")), curseActive,
          curseActive ? Math.max(0, curse.getInt("remainingTicks")) : 0,
          curse != null && curse.getBoolean("executing"), temporal == null ? -1 : temporal.getInt("version"),
          curse == null ? -1 : curse.getInt("version"));
    } catch (RuntimeException ignored) {
      return Snapshot.unavailable();
    }
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
      logLifecycle("logout", player);
      DiagnosticGuard.runBestEffort(() -> DETAIL_SAMPLING.clear(player.getUUID()));
    }
  }

  @SubscribeEvent
  public static void onClone(PlayerEvent.Clone event) {
    if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
      DiagnosticGuard.runBestEffort(() -> {
        if (enabled) {
          logStack("lifecycle=clone oldEntityId=" + original.getId() + " newEntityId=" + replacement.getId()
              + " globalTick=" + ModNetwork.temporalTwistRequestClock(replacement) + " "
              + snapshot(replacement).describe(), "clone diagnostic snapshot stack");
        }
        DETAIL_SAMPLING.clear(replacement.getUUID());
      });
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
    DiagnosticGuard.runBestEffort(DETAIL_SAMPLING::clear);
  }

  private static void logSampled(ServerPlayer player, String stage, String prefix, String stackLabel) {
    if (!enabled) {
      return;
    }
    DiagnosticSampling.Decision decision = DETAIL_SAMPLING.allow(player.getUUID(), stage,
        ModNetwork.temporalTwistRequestClock(player));
    if (!decision.allowed()) {
      return;
    }
    if (decision.skippedCount() > 0) {
      LOGGER.info("RC-DEBUG detail-summary entityId={} stage={} skipped={} windowTicks={}", player.getId(),
          stage, decision.skippedCount(), DETAIL_INTERVAL_TICKS);
    }
    logStack(prefix + " " + snapshot(player).describe(), stackLabel);
  }

  private static CompoundTag compoundIfPresent(CompoundTag persistent, String key) {
    return persistent.contains(key, Tag.TAG_COMPOUND) ? persistent.getCompound(key) : null;
  }

  private static String rootType(CompoundTag persistent, String key) {
    Tag value = persistent.get(key);
    if (value == null) {
      return "absent";
    }
    return switch (value.getId()) {
      case Tag.TAG_END -> "end";
      case Tag.TAG_BYTE -> "byte";
      case Tag.TAG_SHORT -> "short";
      case Tag.TAG_INT -> "int";
      case Tag.TAG_LONG -> "long";
      case Tag.TAG_FLOAT -> "float";
      case Tag.TAG_DOUBLE -> "double";
      case Tag.TAG_BYTE_ARRAY -> "byte_array";
      case Tag.TAG_STRING -> "string";
      case Tag.TAG_LIST -> "list";
      case Tag.TAG_COMPOUND -> "compound";
      case Tag.TAG_INT_ARRAY -> "int_array";
      case Tag.TAG_LONG_ARRAY -> "long_array";
      default -> "unknown(" + value.getId() + ")";
    };
  }

  private static void logLifecycle(String event, ServerPlayer player) {
    DiagnosticGuard.runBestEffort(() -> logSampled(player, "lifecycle-" + event,
        "lifecycle=" + event + " thread=" + Thread.currentThread().getName() + " entityId=" + player.getId(),
        event + " diagnostic snapshot stack"));
  }

  private static void logStack(String message, String stackLabel) {
    LOGGER.info("RC-DEBUG " + message, new Throwable(stackLabel));
  }

  public record Snapshot(boolean available, boolean equipped, long globalTick, int entityTick,
                         long throttleLast, String temporalRootType, String curseRootType,
                          TemporalState.Validation temporalValidation,
                          TemporalCurseState.Validation curseValidation, String temporalReason,
                          String temporalField, String temporalExpected, String temporalActual,
                          String curseReason, String curseField, String curseExpected, String curseActual,
                          boolean anchor,
                         int remainingTicks, boolean executing, int cooldownTicks,
                         boolean curseActive, int curseRemainingTicks, boolean curseExecuting,
                         int temporalVersion, int curseVersion) {
    private static Snapshot unavailable() {
        return new Snapshot(false, false, -1L, -1, -1L, "unavailable", "unavailable", null, null,
            "unavailable", "unavailable", "unavailable", "unavailable", "unavailable", "unavailable",
            "unavailable", "unavailable", false, 0, false, 0, false, 0, false, -1, -1);
    }

    public String describe() {
      return "snapshotAvailable=" + available + " globalTick=" + globalTick + " entityTick=" + entityTick
          + " throttleLast=" + throttleLast + " equipped=" + equipped + " temporalRootType=" + temporalRootType
           + " curseRootType=" + curseRootType + " temporalValidation=" + temporalValidation
           + " temporalReason=" + temporalReason + " temporalField=" + temporalField
           + " temporalExpected=" + temporalExpected + " temporalActual=" + temporalActual
           + " curseValidation=" + curseValidation + " curseReason=" + curseReason + " curseField=" + curseField
           + " curseExpected=" + curseExpected + " curseActual=" + curseActual + " anchor=" + anchor + " remaining=" + remainingTicks
          + " executing=" + executing + " cooldown=" + cooldownTicks + " curseActive=" + curseActive
          + " curseRemaining=" + curseRemainingTicks + " curseExecuting=" + curseExecuting
          + " temporalVersion=" + temporalVersion + " curseVersion=" + curseVersion;
    }

    public boolean enabled() {
      return ResonanceDebug.enabled;
    }
  }

  private ResonanceDebug() {}
}
