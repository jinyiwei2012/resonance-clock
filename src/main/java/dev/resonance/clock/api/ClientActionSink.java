package dev.resonance.clock.api;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/** Side-safe acknowledgement seam; client code registers the consumer during client setup. */
public final class ClientActionSink {
  public enum Action {
    FORM,
    TWIST,
    OPEN_PANEL;

    public static Optional<Action> decode(int value) {
      return value >= 0 && value < values().length ? Optional.of(values()[value]) : Optional.empty();
    }
  }

  public enum Result {
    ACCEPTED,
    THROTTLED,
    NOT_EQUIPPED,
    DISABLED,
    ACTIVE,
    COOLDOWN,
    INVALID_FORM,
    UNAVAILABLE;

    public static Optional<Result> decode(int value) {
      return value >= 0 && value < values().length ? Optional.of(values()[value]) : Optional.empty();
    }
  }

  public record ActionResult(Action action, int requestSeq, Result result) {}

  private static volatile Consumer<ActionResult> sink = result -> {};

  public static void register(Consumer<ActionResult> newSink) {
    sink = Objects.requireNonNull(newSink);
  }

  public static void accept(ActionResult result) {
    sink.accept(result);
  }

  private ClientActionSink() {}
}
