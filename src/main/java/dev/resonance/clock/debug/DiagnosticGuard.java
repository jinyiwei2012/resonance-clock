package dev.resonance.clock.debug;

/** Isolates optional diagnostics from gameplay paths without catching fatal VM errors. */
public final class DiagnosticGuard {
  public static void runBestEffort(Runnable diagnostic) {
    try {
      diagnostic.run();
    } catch (RuntimeException ignored) {
      // Diagnostics must not change gameplay failure or commit behavior.
    }
  }

  private DiagnosticGuard() {}
}
