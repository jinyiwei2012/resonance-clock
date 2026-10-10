package dev.resonance.clock;

import dev.resonance.clock.config.ServerConfig;
import dev.resonance.clock.flight.FlightController;
import dev.resonance.clock.state.TemporalState;
import dev.resonance.clock.state.TemporalTwistService;
import dev.resonance.clock.state.RelicState;
import dev.resonance.clock.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ServerEvents {
  @SubscribeEvent
  public void onPlayerTick(TickEvent.PlayerTickEvent event) {
    if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
      FlightController.tick(player);
      TemporalTwistService.tick(player);
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
    }
  }

  @SubscribeEvent
  public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player) {
      FlightController.clearResidualLease(player);
      TemporalTwistService.revalidateOnLogin(player);
      FlightController.tick(player);
    }
  }

  @SubscribeEvent
  public void onServerStopping(ServerStoppingEvent event) {
    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
      FlightController.revokeOwnedLease(player);
      ModNetwork.clearPlayerRequestState(player);
    }
  }
}
