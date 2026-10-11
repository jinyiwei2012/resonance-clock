package dev.resonance.clock.api;

import java.util.Optional;

public enum FormMode {
  BASE,
  RESONANT;

  public static Optional<FormMode> decode(int value) {
    return value >= 0 && value < values().length ? Optional.of(values()[value]) : Optional.empty();
  }
}
