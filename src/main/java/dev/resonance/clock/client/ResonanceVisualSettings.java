package dev.resonance.clock.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only visual preferences; these never affect server gameplay. */
public final class ResonanceVisualSettings {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.BooleanValue SHOW_RING;
  public static final ForgeConfigSpec.BooleanValue SHOW_WINGS;
  public static final ForgeConfigSpec.BooleanValue SIMPLE_QUALITY;
  public static final ForgeConfigSpec.BooleanValue ANIMATION;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
    builder.comment("Resonance Clock client visuals only").push("visuals");
    SHOW_RING = builder.comment("Show the clock projection ring")
        .define("showRing", true);
    SHOW_WINGS = builder.comment("Show the projection wings")
        .define("showWings", true);
    SIMPLE_QUALITY = builder.comment("Use the simple, lower-cost visual pass")
        .define("simpleQuality", false);
    ANIMATION = builder.comment("Animate hands, pulse and petals")
        .define("animation", true);
    builder.pop();
    SPEC = builder.build();
  }

  private ResonanceVisualSettings() {}
}
