package dev.resonance.clock.state;

/** Describes a rejected persisted snapshot without retaining raw player data. */
public final class SnapshotValidationException extends IllegalArgumentException {
  public enum Reason {
    MISSING_FIELD,
    WRONG_TYPE,
    INVALID_VALUE,
    INVALID_EFFECT
  }

  private final Reason reason;
  private final String field;
  private final String expected;
  private final String actual;

  public SnapshotValidationException(Reason reason, String field, String expected, String actual) {
    super("Invalid snapshot " + field + ": " + reason);
    this.reason = reason;
    this.field = field;
    this.expected = expected;
    this.actual = actual;
  }

  public Reason reason() {
    return reason;
  }

  public String field() {
    return field;
  }

  public String expected() {
    return expected;
  }

  public String actual() {
    return actual;
  }
}
