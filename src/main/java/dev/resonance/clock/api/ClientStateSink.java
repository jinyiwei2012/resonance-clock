package dev.resonance.clock.api;

/** Client package registers a sink during client setup; common code never references client-only classes. */
public final class ClientStateSink {
  /** Complete private state for the owning player's UI. */
  public record State(int entityId, boolean equipped, FormMode formMode, boolean twistActive,
                      int remainingTicks, int cooldownTicks, boolean curseActive,
                      int curseRemainingTicks, int resolvedDurationTicks,
                      int resolvedCooldownTicks, boolean twistEnabled,
                      int configuredDurationTicks, int configuredCooldownTicks,
                      double resonantCooldownMultiplier, boolean curseEnabled,
                      int curseIntervalTicks, boolean flightEnabled) {
    public State(int entityId, boolean equipped, FormMode formMode, boolean twistActive,
        int remainingTicks, int cooldownTicks, boolean curseActive, int curseRemainingTicks,
        int resolvedDurationTicks, int resolvedCooldownTicks) {
      this(entityId, equipped, formMode, twistActive, remainingTicks, cooldownTicks, curseActive,
          curseRemainingTicks, resolvedDurationTicks, resolvedCooldownTicks, false, 0, 0, 1.0D,
          false, 0, false);
    }

    public State(int entityId, boolean equipped, boolean twistActive, int remainingTicks,
        int cooldownTicks) {
      this(entityId, equipped, FormMode.BASE, twistActive, remainingTicks, cooldownTicks, false, 0,
          0, 0, false, 0, 0, 1.0D, false, 0, false);
    }
  }

  /** Public render state for another player currently tracked by the client. */
  public record VisualState(int entityId, boolean equipped, FormMode formMode) {}

  private static volatile java.util.function.Consumer<State> sink = state -> {};
  private static volatile java.util.function.Consumer<VisualState> visualSink = state -> {};

  public static void register(java.util.function.Consumer<State> newSink) {
    sink = java.util.Objects.requireNonNull(newSink);
  }

  public static void registerVisual(java.util.function.Consumer<VisualState> newSink) {
    visualSink = java.util.Objects.requireNonNull(newSink);
  }

  public static void accept(State state) {
    sink.accept(state);
  }

  public static void acceptVisual(VisualState state) {
    visualSink.accept(state);
  }

  private ClientStateSink() {}
}
