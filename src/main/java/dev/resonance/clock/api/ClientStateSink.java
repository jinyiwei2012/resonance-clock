package dev.resonance.clock.api;

/** Client package registers a sink during client setup; common code never references client-only classes. */
public final class ClientStateSink {
  public record State(int entityId, boolean equipped, boolean twistActive, int remainingTicks,
                      int cooldownTicks) {}

  private static volatile java.util.function.Consumer<State> sink = state -> {};

  public static void register(java.util.function.Consumer<State> newSink) {
    sink = java.util.Objects.requireNonNull(newSink);
  }

  public static void accept(State state) {
    sink.accept(state);
  }

  private ClientStateSink() {}
}
