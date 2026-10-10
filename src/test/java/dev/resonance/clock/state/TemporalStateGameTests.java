package dev.resonance.clock.state;

import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.gametest.GameTestHolder;

@GameTestHolder("resonance_clock")
public final class TemporalStateGameTests {
  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void validSnapshotRoundTrip(GameTestHelper helper) {
    TemporalSnapshot snapshot = validSnapshot();
    require(helper, snapshot.equals(TemporalSnapshot.load(snapshot.save())), "Valid snapshot changed");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void missingAndWrongTypesRejected(GameTestHelper helper) {
    CompoundTag missing = validSnapshot().save();
    missing.remove("health");
    expectInvalid(helper, missing, "Missing health accepted");
    CompoundTag wrongType = validSnapshot().save();
    wrongType.putString("food", "20");
    expectInvalid(helper, wrongType, "Wrong food type accepted");
    CompoundTag wrongEffects = validSnapshot().save();
    ListTag list = new ListTag();
    list.add(StringTag.valueOf("bad"));
    wrongEffects.put("effects", list);
    expectInvalid(helper, wrongEffects, "Non-compound effects accepted");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void nonFiniteAndExtremeValuesRejected(GameTestHelper helper) {
    String[] doubles = {"x", "y", "z"};
    for (String key : doubles) {
      CompoundTag nan = validSnapshot().save();
      nan.putDouble(key, Double.NaN);
      expectInvalid(helper, nan, key + " NaN accepted");
      CompoundTag infinity = validSnapshot().save();
      infinity.putDouble(key, Double.POSITIVE_INFINITY);
      expectInvalid(helper, infinity, key + " infinity accepted");
    }
    CompoundTag xExtreme = validSnapshot().save();
    xExtreme.putDouble("x", 29_999_985.0D);
    expectInvalid(helper, xExtreme, "Extreme X accepted");
    CompoundTag zExtreme = validSnapshot().save();
    zExtreme.putDouble("z", -29_999_985.0D);
    expectInvalid(helper, zExtreme, "Extreme Z accepted");
    CompoundTag badHealth = validSnapshot().save();
    badHealth.putFloat("health", -1.0F);
    expectInvalid(helper, badHealth, "Negative health accepted");
    CompoundTag badAbsorption = validSnapshot().save();
    badAbsorption.putFloat("absorption", Float.NaN);
    expectInvalid(helper, badAbsorption, "NaN absorption accepted");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void rootsVersionsCloneAndClear(GameTestHelper helper) {
    CompoundTag original = activeCurseRoot(73);
    require(helper, TemporalCurseState.validate(original) == TemporalCurseState.Validation.ACTIVE,
        "Valid curse root inactive");
    CompoundTag replacement = new CompoundTag();
    TemporalCurseState.copyPersistentData(original, replacement);
    original.getCompound(TemporalCurseState.ROOT).putInt("remainingTicks", 1);
    require(helper, replacement.getCompound(TemporalCurseState.ROOT).getInt("remainingTicks") == 73,
        "Curse clone aliased original");
    require(helper, TemporalCurseState.clearPersistent(replacement), "Curse clear rejected active root");
    require(helper, TemporalCurseState.validate(replacement) == TemporalCurseState.Validation.INACTIVE,
        "Curse clear did not create clean current root");

    CompoundTag unknownCurse = activeCurseRoot(20);
    unknownCurse.getCompound(TemporalCurseState.ROOT).putInt("version", 2);
    require(helper, TemporalCurseState.validate(unknownCurse) == TemporalCurseState.Validation.INVALID,
        "Unknown curse version accepted");
    CompoundTag wrongRoot = new CompoundTag();
    wrongRoot.putString(TemporalCurseState.ROOT, "bad");
    require(helper, TemporalCurseState.validate(wrongRoot) == TemporalCurseState.Validation.INVALID,
        "Wrong curse root type accepted");

    CompoundTag unknownTwist = new CompoundTag();
    CompoundTag twist = new CompoundTag();
    twist.putInt("version", 2);
    twist.put("snapshot", validSnapshot().save());
    twist.putInt("remainingTicks", 20);
    twist.putBoolean("executing", false);
    unknownTwist.put(TemporalState.ROOT, twist);
    require(helper, TemporalState.validate(unknownTwist) == TemporalState.Validation.INVALID,
        "Unknown twist version accepted");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void cycleFreezeAndFailureReset(GameTestHelper helper) {
    CompoundTag state = new CompoundTag();
    state.putInt("remainingTicks", 2);
    state.putBoolean("executing", false);
    require(helper, TemporalCurseService.advanceCycle(state, false, true)
        == TemporalCurseService.CycleAction.FROZEN && state.getInt("remainingTicks") == 2,
        "Disabled cycle advanced");
    require(helper, TemporalCurseService.advanceCycle(state, true, false)
        == TemporalCurseService.CycleAction.FROZEN && state.getInt("remainingTicks") == 2,
        "Dead cycle advanced");
    require(helper, TemporalCurseService.advanceCycle(state, true, true)
        == TemporalCurseService.CycleAction.WAITING && state.getInt("remainingTicks") == 1,
        "Live cycle did not wait");
    require(helper, TemporalCurseService.advanceCycle(state, true, true)
        == TemporalCurseService.CycleAction.RESTORE,
        "Expired cycle did not request restore");
    state.putBoolean("executing", true);
    TemporalCurseService.resetCycle(state, 24000);
    require(helper, state.getInt("remainingTicks") == 24000 && !state.getBoolean("executing"),
        "Failure cycle did not reset cleanly");
    helper.succeed();
  }

  private static CompoundTag activeCurseRoot(int remainingTicks) {
    CompoundTag persistent = new CompoundTag();
    CompoundTag state = TemporalCurseState.getOrCreate(persistent);
    state.put("snapshot", validSnapshot().save());
    state.putInt("remainingTicks", remainingTicks);
    state.putBoolean("executing", false);
    return persistent;
  }

  private static TemporalSnapshot validSnapshot() {
    ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
        new ResourceLocation("resonance_clock", "test_dimension"));
    return new TemporalSnapshot(dimension, 1.25D, 64.0D, -8.5D, 90.0F, -15.0F,
        7.5F, 9, 2.5F, 111, 12, 3.0F, List.of());
  }

  private static void expectInvalid(GameTestHelper helper, CompoundTag tag, String message) {
    try {
      TemporalSnapshot.load(tag);
      helper.fail(message);
    } catch (IllegalArgumentException expected) {
      // Expected strict rejection.
    }
  }

  private static void require(GameTestHelper helper, boolean condition, String message) {
    if (!condition) {
      helper.fail(message);
    }
  }

  private TemporalStateGameTests() {}
}
