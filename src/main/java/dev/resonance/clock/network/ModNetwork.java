package dev.resonance.clock.network;

import dev.resonance.clock.ResonanceClock;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import dev.resonance.clock.api.ClientActionSink;
import dev.resonance.clock.state.TemporalTwistService;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import dev.resonance.clock.state.RelicState;
import dev.resonance.clock.state.TemporalState;
import dev.resonance.clock.state.TemporalCurseState;
import dev.resonance.clock.state.FormState;
import dev.resonance.clock.api.FormMode;
import dev.resonance.clock.config.ServerConfig;
import net.minecraft.world.item.ItemStack;

public final class ModNetwork {
  private static final String VERSION = "2";
  public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
       new ResourceLocation(ResonanceClock.MOD_ID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);
  private static final int REQUEST_INTERVAL_TICKS = 10;
  private static final RequestThrottle TEMPORAL_TWIST_THROTTLE =
      new RequestThrottle(REQUEST_INTERVAL_TICKS);
  private static final RequestThrottle FORM_THROTTLE = new RequestThrottle(REQUEST_INTERVAL_TICKS);
  private static final RequestThrottle OPEN_PANEL_THROTTLE = new RequestThrottle(REQUEST_INTERVAL_TICKS);
  private static final RequestThrottle REJECTED_ACK_THROTTLE = new RequestThrottle(REQUEST_INTERVAL_TICKS);
  private static final AtomicInteger REQUEST_SEQUENCE = new AtomicInteger();

  public static void register() {
    CHANNEL.registerMessage(0, RequestTemporalTwistPacket.class, RequestTemporalTwistPacket::encode,
        RequestTemporalTwistPacket::decode, RequestTemporalTwistPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_SERVER));
    CHANNEL.registerMessage(1, SyncResonanceStatePacket.class, SyncResonanceStatePacket::encode,
        SyncResonanceStatePacket::decode, SyncResonanceStatePacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    CHANNEL.registerMessage(2, RequestSetFormPacket.class, RequestSetFormPacket::encode,
        RequestSetFormPacket::decode, RequestSetFormPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_SERVER));
    CHANNEL.registerMessage(3, RequestOpenRelicPanelPacket.class,
        RequestOpenRelicPanelPacket::encode, RequestOpenRelicPanelPacket::decode,
        RequestOpenRelicPanelPacket::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    CHANNEL.registerMessage(4, OpenRelicPanelPacket.class, OpenRelicPanelPacket::encode,
        OpenRelicPanelPacket::decode, OpenRelicPanelPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    CHANNEL.registerMessage(5, SyncFormVisualPacket.class, SyncFormVisualPacket::encode,
        SyncFormVisualPacket::decode, SyncFormVisualPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    CHANNEL.registerMessage(6, ActionResultPacket.class, ActionResultPacket::encode,
        ActionResultPacket::decode, ActionResultPacket::handle,
        Optional.of(NetworkDirection.PLAY_TO_CLIENT));
  }

  public static int requestTemporalTwist() {
    int requestSeq = nextRequestSeq();
    CHANNEL.sendToServer(new RequestTemporalTwistPacket(requestSeq));
    return requestSeq;
  }

  public static int requestSetForm(FormMode mode) {
    int requestSeq = nextRequestSeq();
    CHANNEL.sendToServer(new RequestSetFormPacket(Objects.requireNonNull(mode), requestSeq));
    return requestSeq;
  }

  /** Stable client-facing alias used by the relic panel. */
  public static int sendSetForm(FormMode mode) {
    return requestSetForm(mode);
  }

  public static int requestOpenRelicPanel() {
    int requestSeq = nextRequestSeq();
    CHANNEL.sendToServer(new RequestOpenRelicPanelPacket(requestSeq));
    return requestSeq;
  }

  /** Stable client-facing alias for a future optional key binding. */
  public static int requestOpenPanel() {
    return requestOpenRelicPanel();
  }

  static int nextRequestSeq() {
    return REQUEST_SEQUENCE.updateAndGet(value -> value == Integer.MAX_VALUE ? 1 : value + 1);
  }

  public static void syncOwner(ServerPlayer player) {
    TemporalState.PublicState state = TemporalState.publicState(player);
    var curse = TemporalCurseState.readExisting(player);
    boolean curseActive = TemporalCurseState.validate(player.getPersistentData())
        == TemporalCurseState.Validation.ACTIVE;
    int curseRemaining = curseActive ? Math.max(0, curse.orElseThrow().getInt("remainingTicks")) : 0;
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncResonanceStatePacket(player.getId(),
        RelicState.isEquipped(player), FormState.get(player), state.active(), state.remainingTicks(),
        state.cooldownTicks(), curseActive, curseRemaining, state.resolvedDurationTicks(),
        state.resolvedCooldownTicks(), ServerConfig.TWIST_ENABLED.get(),
        ServerConfig.TWIST_DURATION_TICKS.get(), ServerConfig.TWIST_COOLDOWN_TICKS.get(),
        ServerConfig.RESONANT_COOLDOWN_MULTIPLIER.get(), ServerConfig.CURSE_ENABLED.get(),
        ServerConfig.CURSE_INTERVAL_TICKS.get(), ServerConfig.FLIGHT_ENABLED.get()));
  }

  /** Compatibility alias retained for existing common callers. */
  public static void syncToSelf(ServerPlayer player) {
    syncOwner(player);
  }

  public static void syncVisual(ServerPlayer player) {
    CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player),
        new SyncFormVisualPacket(player.getId(), RelicState.isEquipped(player), FormState.get(player)));
  }

  public static void syncVisualTo(ServerPlayer tracked, ServerPlayer viewer) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer),
        new SyncFormVisualPacket(tracked.getId(), RelicState.isEquipped(tracked),
            FormState.get(tracked)));
  }

  public static void openRelicPanel(ServerPlayer player) {
    syncOwner(player);
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenRelicPanelPacket());
  }

  public static void sendActionResult(ServerPlayer player, ClientActionSink.Action action,
      int requestSeq, ClientActionSink.Result result) {
    CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
        new ActionResultPacket(action, requestSeq, result));
  }

  public static void sendThrottledResult(ServerPlayer player, ClientActionSink.Action action,
      int requestSeq) {
    if (tryAccept(player, REJECTED_ACK_THROTTLE)) {
      syncOwner(player);
      sendActionResult(player, action, requestSeq, ClientActionSink.Result.THROTTLED);
    }
  }

  public static ClientActionSink.Result mapTwistResult(TemporalTwistService.StartResult result) {
    return switch (result) {
      case STARTED -> ClientActionSink.Result.ACCEPTED;
      case DISABLED -> ClientActionSink.Result.DISABLED;
      case NOT_EQUIPPED -> ClientActionSink.Result.NOT_EQUIPPED;
      case ACTIVE -> ClientActionSink.Result.ACTIVE;
      case COOLDOWN -> ClientActionSink.Result.COOLDOWN;
    };
  }

  public static boolean mayOpenRelicPanel(ServerPlayer player) {
    if (RelicState.isEquipped(player)) {
      return true;
    }
    return isGenuineRelic(player.getMainHandItem()) || isGenuineRelic(player.getOffhandItem());
  }

  private static boolean isGenuineRelic(ItemStack stack) {
    return stack.is(dev.resonance.clock.item.ModItems.RESONANCE_RELIC.get());
  }

  /** Rejects burst requests before they enter the temporal action path. */
  public static boolean acceptTemporalTwistRequest(ServerPlayer player) {
    if (player.getServer() == null) {
      return false;
    }
    long currentTick = player.getServer().overworld().getGameTime();
    return TEMPORAL_TWIST_THROTTLE.tryAccept(player.getUUID(), currentTick);
  }

  public static boolean acceptFormRequest(ServerPlayer player) {
    return tryAccept(player, FORM_THROTTLE);
  }

  public static boolean acceptOpenPanelRequest(ServerPlayer player) {
    return tryAccept(player, OPEN_PANEL_THROTTLE);
  }

  private static boolean tryAccept(ServerPlayer player, RequestThrottle throttle) {
    if (player.getServer() == null) {
      return false;
    }
    return throttle.tryAccept(player.getUUID(), player.getServer().overworld().getGameTime());
  }

  /** Returns the current global server clock used by the temporal request throttle. */
  public static long temporalTwistRequestClock(ServerPlayer player) {
    return player.getServer() == null ? -1L : player.getServer().overworld().getGameTime();
  }

  /** Returns the last accepted request clock for diagnostics without changing throttle state. */
  public static long lastAcceptedTemporalTwistRequestClock(ServerPlayer player) {
    Long tick = TEMPORAL_TWIST_THROTTLE.getLastAcceptedTick(player.getUUID());
    return tick == null ? -1L : tick;
  }

  public static void clearPlayerRequestState(ServerPlayer player) {
    TEMPORAL_TWIST_THROTTLE.clear(player.getUUID());
    FORM_THROTTLE.clear(player.getUUID());
    OPEN_PANEL_THROTTLE.clear(player.getUUID());
    REJECTED_ACK_THROTTLE.clear(player.getUUID());
  }

  private ModNetwork() {}
}
