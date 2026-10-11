package dev.resonance.clock.config;

import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class ServerConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.BooleanValue FLIGHT_ENABLED;
  public static final ForgeConfigSpec.BooleanValue TWIST_ENABLED;
  public static final ForgeConfigSpec.DoubleValue STARVATION_MULTIPLIER;
  public static final ForgeConfigSpec.IntValue TWIST_DURATION_TICKS;
  public static final ForgeConfigSpec.IntValue TWIST_COOLDOWN_TICKS;
  public static final ForgeConfigSpec.DoubleValue RESONANT_COOLDOWN_MULTIPLIER;
  public static final ForgeConfigSpec.BooleanValue CURSE_ENABLED;
  public static final ForgeConfigSpec.IntValue CURSE_INTERVAL_TICKS;
  public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXCLUDED_EFFECTS;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.push("abilities");
    FLIGHT_ENABLED = builder.comment("Grant exclusive survival flight while the relic is equipped. Vanilla mayfly has no source attribution; enable only when no other mod or command grants mayfly.")
        .define("flightEnabled", false);
    TWIST_ENABLED = builder.define("temporalTwistEnabled", true);
    STARVATION_MULTIPLIER = builder.comment("Only multiplies vanilla starvation damage; it does not accelerate hunger drain.")
        .defineInRange("starvationDamageMultiplier", 8.0D, 1.0D, 50.0D);
    TWIST_DURATION_TICKS = builder.comment("Temporary MVP default: 10 seconds.")
        .defineInRange("temporalTwistDurationTicks", 200, 20, 72000);
    TWIST_COOLDOWN_TICKS = builder.comment("Temporary MVP default: 30 seconds.")
        .defineInRange("temporalTwistCooldownTicks", 600, 0, 72000);
    RESONANT_COOLDOWN_MULTIPLIER = builder.comment("Cooldown multiplier frozen when a twist starts in RESONANT form.")
        .defineInRange("resonantCooldownMultiplier", 0.5D, 0.1D, 1.0D);
    CURSE_ENABLED = builder.comment("Enable online-tick progression for Temporal Disorder. Disabling freezes existing curses without deleting them.")
        .define("temporalDisorderEnabled", true);
    CURSE_INTERVAL_TICKS = builder.comment("Online server ticks between Temporal Disorder rewinds. The default is one Minecraft day; 20 is permitted for tests.")
        .defineInRange("temporalDisorderIntervalTicks", 24000, 20, 1728000);
    EXCLUDED_EFFECTS = builder.comment("Effect IDs omitted from snapshots and therefore not restored.")
        .defineListAllowEmpty("excludedEffects", List.of(), value -> value instanceof String);
    builder.pop();
    SPEC = builder.build();
  }

  private ServerConfig() {}
}
