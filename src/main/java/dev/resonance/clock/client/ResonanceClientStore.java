package dev.resonance.clock.client;

import dev.resonance.clock.api.ClientStateSink;
import dev.resonance.clock.api.FormMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Client-only cache. Private owner state and public tracked visuals are kept separate. */
public final class ResonanceClientStore {
  private static final Map<Integer, ClientStateSink.VisualState> VISUALS = new ConcurrentHashMap<>();
  private static volatile ClientStateSink.State ownerState;
  private static volatile int ownerEntityId = Integer.MIN_VALUE;
  private static volatile ClientStateSink.VisualState ownerVisual;
  private static volatile Object clientLevel;

  public static void acceptOwner(ClientStateSink.State state) {
    ownerState = state;
    ownerEntityId = state.entityId();
    ownerVisual = new ClientStateSink.VisualState(state.entityId(), state.equipped(), state.formMode());
  }

  public static void acceptVisual(ClientStateSink.VisualState state) {
    VISUALS.put(state.entityId(), state);
  }

  public static ClientStateSink.State ownerState() {
    return ownerState;
  }

  public static ClientStateSink.VisualState visual(int entityId) {
    if (entityId == ownerEntityId) {
      return ownerVisual;
    }
    return VISUALS.get(entityId);
  }

  public static void removeVisual(int entityId) {
    if (entityId != ownerEntityId) {
      VISUALS.remove(entityId);
    }
  }

  public static void observeClientLevel(Object level) {
    if (level != clientLevel) {
      clientLevel = level;
      clearStateOnly();
    }
  }

  private static void clearStateOnly() {
    ownerState = null;
    ownerEntityId = Integer.MIN_VALUE;
    ownerVisual = null;
    VISUALS.clear();
  }

  public static int ownerEntityId() {
    return ownerEntityId;
  }

  public static void clear() {
    clientLevel = null;
    clearStateOnly();
  }

  private ResonanceClientStore() {}
}
