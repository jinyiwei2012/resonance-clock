package dev.resonance.clock.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player, per-stage diagnostic sampler with rollback-safe server-clock handling. */
public final class DiagnosticSampling {
  private final long intervalTicks;
  private final Map<Key, Long> lastDetails = new HashMap<>();
  private final Map<Key, Integer> skippedDetails = new HashMap<>();

  public DiagnosticSampling(long intervalTicks) {
    if (intervalTicks < 1) {
      throw new IllegalArgumentException("intervalTicks must be positive");
    }
    this.intervalTicks = intervalTicks;
  }

  public synchronized Decision allow(UUID playerId, String stage, long currentTick) {
    Key key = new Key(playerId, stage);
    Long previousTick = lastDetails.get(key);
    if (previousTick != null && currentTick >= previousTick
        && currentTick - previousTick < intervalTicks) {
      skippedDetails.merge(key, 1, Integer::sum);
      return Decision.suppressed();
    }
    lastDetails.put(key, currentTick);
    Integer skipped = skippedDetails.remove(key);
    return Decision.allowed(skipped == null ? 0 : skipped);
  }

  public synchronized void clear(UUID playerId) {
    lastDetails.keySet().removeIf(key -> key.playerId().equals(playerId));
    skippedDetails.keySet().removeIf(key -> key.playerId().equals(playerId));
  }

  public synchronized void clear() {
    lastDetails.clear();
    skippedDetails.clear();
  }

  public record Decision(boolean allowed, int skippedCount) {
    private static Decision allowed(int skippedCount) {
      return new Decision(true, skippedCount);
    }

    private static Decision suppressed() {
      return new Decision(false, 0);
    }
  }

  private record Key(UUID playerId, String stage) {}
}
