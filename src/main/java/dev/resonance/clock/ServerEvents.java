package dev.resonance.clock;

import dev.resonance.clock.config.ServerConfig;
import dev.resonance.clock.flight.FlightController;
import dev.resonance.clock.state.TemporalState;
import dev.resonance.clock.state.TemporalTwistService;
import dev.resonance.clock.state.TemporalCurseService;
import dev.resonance.clock.state.TemporalCurseState;
import dev.resonance.clock.state.RelicState;
import dev.resonance.clock.state.FormState;
import dev.resonance.clock.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import dev.resonance.clock.api.FormMode;

public final class ServerEvents {
  private final Map<UUID, VisualState> lastVisualStates = new HashMap<>();
  private final Map<UUID, Integer> lastCooldownTicks = new HashMap<>();

  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent event) {
    dev.resonance.clock.command.ResonanceCommands.register(event.getDispatcher());
  }

  @SubscribeEvent
  public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
      FlightController.tick(player);
      TemporalTwistService.tick(player);
      TemporalCurseService.tick(player);
      syncChangedState(player);
    }
  }

  @SubscribeEvent
  public void onStarvationDamage(LivingDamageEvent event) {
    if (event.getEntity() instanceof ServerPlayer player
        && RelicState.isEquipped(player)
        && event.getSource().is(DamageTypes.STARVE)) {
      event.setAmount((float) (event.getAmount() * ServerConfig.STARVATION_MULTIPLIER.get()));
    }
  }

  @SubscribeEvent
  public void onDeath(LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      TemporalTwistService.cancel(player, "death");
      FlightController.revokeOwnedLease(player);
    }
  }

  @SubscribeEvent
  public void onClone(PlayerEvent.Clone event) {
    if (event.getOriginal() instanceof ServerPlayer original && event.getEntity() instanceof ServerPlayer replacement) {
      event.getOriginal().reviveCaps();
      TemporalState.copyOnRespawn(original, replacement);
      TemporalCurseState.copyOnRespawn(original, replacement);
      FormState.copyOnRespawn(original, replacement);
      event.getOriginal().invalidateCaps();
      TemporalTwistService.cancel(replacement, "death");
      FlightController.clearResidualLease(replacement);
    }
  }

  @SubscribeEvent
  public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      FlightController.revokeOwnedLease(player);
      ModNetwork.clearPlayerRequestState(player);
      lastVisualStates.remove(player.getUUID());
      lastCooldownTicks.remove(player.getUUID());
    }
  }

  @SubscribeEvent
  public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      FlightController.clearResidualLease(player);
      TemporalTwistService.revalidateOnLogin(player);
      FlightController.tick(player);
      ModNetwork.syncOwner(player);
      ModNetwork.syncVisual(player);
      lastCooldownTicks.put(player.getUUID(), TemporalState.publicState(player).cooldownTicks());
    }
  }

  @SubscribeEvent
  public void onStartTracking(PlayerEvent.StartTracking event) {
    if (event.getEntity() instanceof ServerPlayer viewer
        && event.getTarget() instanceof ServerPlayer tracked) {
      ModNetwork.syncVisualTo(tracked, viewer);
    }
  }

  @SubscribeEvent
  public void onServerStopping(ServerStoppingEvent event) {
    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
      FlightController.revokeOwnedLease(player);
      ModNetwork.clearPlayerRequestState(player);
    }
  }

  private void syncChangedState(ServerPlayer player) {
    VisualState current = new VisualState(RelicState.isEquipped(player), FormState.get(player));
    VisualState previous = lastVisualStates.put(player.getUUID(), current);
    if (!current.equals(previous)) {
      ModNetwork.syncVisual(player);
      ModNetwork.syncOwner(player);
      return;
    }
    TemporalState.PublicState twist = TemporalState.publicState(player);
    boolean curseActive = TemporalCurseState.isActive(player);
    int previousCooldown = lastCooldownTicks.getOrDefault(player.getUUID(), twist.cooldownTicks());
    lastCooldownTicks.put(player.getUUID(), twist.cooldownTicks());
    if (shouldSyncOwnerPeriodic(current.equipped, twist.active(), curseActive,
        twist.cooldownTicks(), previousCooldown, player.tickCount)) {
      ModNetwork.syncOwner(player);
    }
  }

  public static boolean shouldSyncOwnerPeriodic(boolean equipped, boolean twistActive,
      boolean curseActive, int cooldownTicks, int previousCooldownTicks, int playerTickCount) {
    return previousCooldownTicks > 0 && cooldownTicks == 0
        || playerTickCount % 20 == 0
            && (equipped || twistActive || curseActive || cooldownTicks > 0);
  }

  private record VisualState(boolean equipped, FormMode mode) {}
}
