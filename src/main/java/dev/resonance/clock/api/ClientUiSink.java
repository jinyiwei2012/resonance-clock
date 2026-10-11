package dev.resonance.clock.api;

import java.util.Objects;

/** Client package registers UI work during client setup; common code has no client-only reference. */
public final class ClientUiSink {
  private static volatile Runnable openRelicPanel = () -> {};

  public static void registerOpenRelicPanel(Runnable action) {
    openRelicPanel = Objects.requireNonNull(action);
  }

  public static void openRelicPanel() {
    openRelicPanel.run();
  }

  private ClientUiSink() {}
}
