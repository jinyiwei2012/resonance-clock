package dev.resonance.clock.command;

import com.mojang.brigadier.CommandDispatcher;
import dev.resonance.clock.debug.ResonanceDebug;
import dev.resonance.clock.state.TemporalCurseService;
import dev.resonance.clock.state.TemporalCurseState;
import java.util.Collection;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ResonanceCommands {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("resonance_clock")
        .then(Commands.literal("curse").requires(source -> source.hasPermission(2))
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(context -> curse(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
        .then(Commands.literal("status")
            .executes(context -> status(context.getSource(), context.getSource().getPlayerOrException()))
            .then(Commands.argument("target", EntityArgument.player()).requires(source -> source.hasPermission(2))
                .executes(context -> status(context.getSource(), EntityArgument.getPlayer(context, "target")))))
        .then(Commands.literal("clear").requires(source -> source.hasPermission(2))
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(context -> clear(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
        .then(Commands.literal("debug").requires(source -> source.hasPermission(2))
            .then(Commands.literal("on").executes(context -> setDebug(context.getSource(), true)))
            .then(Commands.literal("off").executes(context -> setDebug(context.getSource(), false)))
            .then(Commands.literal("status")
                .executes(context -> debugStatus(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> debugStatus(context.getSource(), EntityArgument.getPlayer(context, "target")))))));
  }

  private static int curse(CommandSourceStack source, Collection<ServerPlayer> targets) {
    int applied = 0;
    for (ServerPlayer target : targets) {
      TemporalCurseService.ApplyResult result = TemporalCurseService.apply(target);
      source.sendSuccess(() -> Component.translatable("command.resonance_clock.curse.result",
          target.getDisplayName(), Component.translatable(curseResultKey(result))), true);
      if (result == TemporalCurseService.ApplyResult.APPLIED) {
        applied++;
      }
    }
    return applied;
  }

  private static int status(CommandSourceStack source, ServerPlayer target) {
    var state = TemporalCurseState.readExisting(target);
    boolean active = state.map(value -> value.contains("snapshot")).orElse(false);
    int remainingTicks = active ? Math.max(0, state.orElseThrow().getInt("remainingTicks")) : 0;
    source.sendSuccess(() -> Component.translatable("command.resonance_clock.status",
        target.getDisplayName(), Component.translatable(active
            ? "command.resonance_clock.status.active"
            : "command.resonance_clock.status.inactive"),
        remainingTicks), false);
    return active ? 1 : 0;
  }

  private static int clear(CommandSourceStack source, Collection<ServerPlayer> targets) {
    int cleared = 0;
    for (ServerPlayer target : targets) {
      if (TemporalCurseService.clear(target, true)) {
        cleared++;
      }
    }
    int result = cleared;
    source.sendSuccess(() -> Component.translatable("command.resonance_clock.clear.result", result), true);
    return cleared;
  }

  private static String curseResultKey(TemporalCurseService.ApplyResult result) {
    return switch (result) {
      case APPLIED -> "command.resonance_clock.curse.result.applied";
      case TWIST_ACTIVE -> "command.resonance_clock.curse.result.twist_active";
      case ALREADY_CURSED -> "command.resonance_clock.curse.result.already_cursed";
    };
  }

  private static int setDebug(CommandSourceStack source, boolean enabled) {
    ResonanceDebug.setEnabled(enabled);
    source.sendSuccess(() -> Component.translatable(enabled
        ? "command.resonance_clock.debug.enabled"
        : "command.resonance_clock.debug.disabled"), true);
    return 1;
  }

  private static int debugStatus(CommandSourceStack source, ServerPlayer target) {
    ResonanceDebug.Snapshot snapshot = ResonanceDebug.snapshot(target);
    source.sendSuccess(() -> Component.translatable("command.resonance_clock.debug.status",
        target.getDisplayName(), snapshot.enabled(), snapshot.equipped(), snapshot.globalTick(),
        snapshot.entityTick(), snapshot.throttleLast(), snapshot.anchor(), snapshot.remainingTicks(),
        snapshot.executing(), snapshot.cooldownTicks(), snapshot.curseActive(),
        snapshot.curseRemainingTicks(), snapshot.temporalVersion(), snapshot.curseVersion()), false);
    return snapshot.anchor() || snapshot.curseActive() ? 1 : 0;
  }

  private ResonanceCommands() {}
}
