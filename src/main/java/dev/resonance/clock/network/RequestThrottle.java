package dev.resonance.clock.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Maintains per-player request limits against a monotonic server clock. */
public final class RequestThrottle {
  private final long intervalTicks;
  private final Map<UUID, Long> lastAcceptedTicks = new HashMap<>();

  public RequestThrottle(long intervalTicks) {
    if (intervalTicks < 1) {
      throw new IllegalArgumentException("intervalTicks must be positive");
    }
    this.intervalTicks = intervalTicks;
  }

  /** Returns whether this request is accepted and records accepted requests. */
  public boolean tryAccept(UUID playerId, long currentTick) {
    Long previousTick = lastAcceptedTicks.get(playerId);
    if (previousTick != null
        && currentTick >= previousTick
        && currentTick - previousTick < intervalTicks) {
      return false;
    }
    lastAcceptedTicks.put(playerId, currentTick);
    return true;
  }

  public void clear(UUID playerId) {
    lastAcceptedTicks.remove(playerId);
  }

  /** Returns the last accepted server tick for diagnostics without changing throttle state. */
  public Long getLastAcceptedTick(UUID playerId) {
    return lastAcceptedTicks.get(playerId);
  }
}
