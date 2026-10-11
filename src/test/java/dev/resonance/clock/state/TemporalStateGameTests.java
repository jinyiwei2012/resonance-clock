package dev.resonance.clock.state;

import java.util.List;
import java.util.Map;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.ISlotType;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.data.CuriosEntityManager;
import dev.resonance.clock.item.ModItems;
import dev.resonance.clock.api.FormMode;
import dev.resonance.clock.api.ServerActions;
import dev.resonance.clock.network.RequestSetFormPacket;
import dev.resonance.clock.network.SyncFormVisualPacket;
import dev.resonance.clock.network.SyncResonanceStatePacket;
import dev.resonance.clock.ServerEvents;
import dev.resonance.clock.api.ClientActionSink;
import dev.resonance.clock.network.ActionResultPacket;
import dev.resonance.clock.network.RequestOpenRelicPanelPacket;
import dev.resonance.clock.network.RequestTemporalTwistPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

@GameTestHolder("resonance_clock")
public final class TemporalStateGameTests {
  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void validSnapshotRoundTrip(GameTestHelper helper) {
    TemporalSnapshot snapshot = validSnapshot();
    require(helper, snapshot.equals(TemporalSnapshot.load(snapshot.save())), "Valid snapshot changed");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void fireMinusOneSnapshotRoundTrip(GameTestHelper helper) {
    TemporalSnapshot snapshot = validSnapshotWith(-1, -1, 21, 21.0F, 91.0F);
    require(helper, snapshot.equals(TemporalSnapshot.load(snapshot.save())),
        "Vanilla-compatible fire or air values changed");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void serverPlayerCaptureRoundTrip(GameTestHelper helper) {
    ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
        new GameProfile(UUID.randomUUID(), "snapshot-capture-test"));
    player.setPos(2.25D, -12.0D, 7.5D);
    player.setYRot(45.0F);
    player.setXRot(91.0F);
    player.setAirSupply(-1);
    player.setRemainingFireTicks(-1);
    player.getFoodData().setFoodLevel(21);
    player.getFoodData().setSaturation(21.0F);
    TemporalSnapshot captured = TemporalSnapshot.capture(player);
    require(helper, captured.fireTicks() == -1, "Capture did not retain vanilla not-on-fire value");
    require(helper, captured.air() == player.getAirSupply()
            && captured.food() == player.getFoodData().getFoodLevel()
            && captured.saturation() == player.getFoodData().getSaturationLevel()
            && captured.pitch() == player.getXRot() && captured.x() == player.getX()
            && captured.y() == player.getY() && captured.z() == player.getZ(),
        "Capture fields differ from server player values");
    require(helper, captured.equals(TemporalSnapshot.load(captured.save())),
        "Captured server player snapshot did not round trip");
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
    CompoundTag fireExtreme = validSnapshot().save();
    fireExtreme.putInt("fireTicks", Integer.MIN_VALUE);
    expectInvalid(helper, fireExtreme, "Extreme fire ticks accepted");
    CompoundTag airExtreme = validSnapshot().save();
    airExtreme.putInt("air", Integer.MIN_VALUE);
    expectInvalid(helper, airExtreme, "Extreme air accepted");
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
    TemporalState.ValidationDetail invalidFood = TemporalState.validateDetailed(activeTwistRootWithWrongFood());
    require(helper, invalidFood.validation() == TemporalState.Validation.INVALID
            && "food".equals(invalidFood.field()) && "wrong_type".equals(invalidFood.reason()),
        "Twist validation lost detailed rejection reason");
    TemporalCurseState.ValidationDetail invalidCurse = TemporalCurseState.validateDetailed(
        activeCurseRootWithWrongFood());
    require(helper, invalidCurse.validation() == TemporalCurseState.Validation.INVALID
            && "food".equals(invalidCurse.field()) && "wrong_type".equals(invalidCurse.reason()),
        "Curse validation lost detailed rejection reason");
    CompoundTag compatibleTwist = activeTwistRoot(200);
    CompoundTag twistBefore = compatibleTwist.copy();
    require(helper, TemporalState.validate(compatibleTwist) == TemporalState.Validation.ACTIVE
            && TemporalState.validateDetailed(compatibleTwist).validation()
                == TemporalState.Validation.ACTIVE && compatibleTwist.equals(twistBefore),
        "Compatible twist root changed or became inactive");
    CompoundTag compatibleCurse = activeCompatibleCurseRoot(200);
    CompoundTag curseBefore = compatibleCurse.copy();
    require(helper, TemporalCurseState.validate(compatibleCurse) == TemporalCurseState.Validation.ACTIVE
            && TemporalCurseState.validateDetailed(compatibleCurse).validation()
                == TemporalCurseState.Validation.ACTIVE && compatibleCurse.equals(curseBefore),
        "Compatible curse root changed or became inactive");
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

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void independentCuriosSlotEquipsRelic(GameTestHelper helper) {
    Map<String, ISlotType> configuredSlots = CuriosEntityManager.SERVER.getEntitySlots(EntityType.PLAYER);
    ISlotType resonanceSlot = configuredSlots.get("resonance_clock");
    require(helper, resonanceSlot != null && resonanceSlot.getSize() == 1,
        "Independent resonance slot was not registered with size one");
    require(helper, !"charm".equals(resonanceSlot.getIdentifier()),
        "Independent slot unexpectedly replaced the shared charm identifier");

    ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
        new GameProfile(UUID.randomUUID(), "curios-slot-test"));
    ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
    require(helper, inventory != null, "Curios capability was not attached to server player");
    ICurioStacksHandler slotHandler = inventory.getStacksHandler("resonance_clock").orElse(null);
    require(helper, slotHandler != null && slotHandler.getSlots() == 1,
        "Independent resonance slot handler was not created");
    ItemStack relic = new ItemStack(ModItems.RESONANCE_RELIC.get());
    require(helper, slotHandler.getStacks().isItemValid(0, relic), "Relic failed resonance slot validator");
    require(helper, !slotHandler.getStacks().isItemValid(0, new ItemStack(ModItems.TEMPORAL_PURIFIER.get())),
        "Unrelated item passed resonance slot validator");
    slotHandler.getStacks().insertItem(0, relic, false);
    require(helper, RelicState.isEquipped(player), "Inserted relic was not recognized as equipped");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void formDefaultsToBase(GameTestHelper helper) {
    require(helper, FormState.read(new CompoundTag()) == FormMode.BASE, "Absent form did not default BASE");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void invalidFormVersionAndEnumDefaultBase(GameTestHelper helper) {
    CompoundTag persistent = new CompoundTag();
    FormState.write(persistent, FormMode.RESONANT);
    persistent.getCompound(FormState.ROOT).putInt("version", 2);
    require(helper, FormState.read(persistent) == FormMode.BASE, "Unknown form version accepted");
    persistent.getCompound(FormState.ROOT).putInt("version", FormState.DATA_VERSION);
    persistent.getCompound(FormState.ROOT).putInt("mode", 99);
    require(helper, FormState.read(persistent) == FormMode.BASE, "Illegal form enum accepted");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void formCopiesAcrossDeath(GameTestHelper helper) {
    CompoundTag original = new CompoundTag();
    CompoundTag replacement = new CompoundTag();
    FormState.write(original, FormMode.RESONANT);
    FormState.copyPersistentData(original, replacement);
    original.getCompound(FormState.ROOT).putInt("mode", FormMode.BASE.ordinal());
    require(helper, FormState.read(replacement) == FormMode.RESONANT, "Death copy lost or aliased form");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void formChangeRejectsUnequippedPlayer(GameTestHelper helper) {
    ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
        new GameProfile(UUID.randomUUID(), "form-equip-reject"));
    require(helper, ServerActions.requestSetForm(player, FormMode.RESONANT)
        == ServerActions.SetFormResult.NOT_EQUIPPED && FormState.get(player) == FormMode.BASE,
        "Unequipped player changed form");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void resonantCooldownIsFrozen(GameTestHelper helper) {
    int resolved = TemporalTwistService.resolveCooldownTicks(FormMode.RESONANT, 600, 0.5D);
    CompoundTag state = new CompoundTag();
    state.putInt("resolvedCooldownTicks", resolved);
    require(helper, resolved == 300 && TemporalState.resolvedCooldownTicks(state) == 300,
        "Resonant cooldown was not resolved and frozen");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void oldAnchorUsesConfigFallback(GameTestHelper helper) {
    CompoundTag state = new CompoundTag();
    require(helper, TemporalState.resolvedDurationTicks(state)
            == dev.resonance.clock.config.ServerConfig.TWIST_DURATION_TICKS.get()
        && TemporalState.resolvedCooldownTicks(state)
            == dev.resonance.clock.config.ServerConfig.TWIST_COOLDOWN_TICKS.get(),
        "0.2.2 anchor fallback did not use current config");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void formPacketCodecsAreLegal(GameTestHelper helper) {
    FriendlyByteBuf requestBuffer = new FriendlyByteBuf(Unpooled.buffer());
    RequestSetFormPacket.encode(new RequestSetFormPacket(FormMode.RESONANT, 41), requestBuffer);
    RequestSetFormPacket decodedForm = RequestSetFormPacket.decode(requestBuffer);
    require(helper, decodedForm.mode() == FormMode.RESONANT && decodedForm.requestSeq() == 41,
        "Set-form packet changed legal enum");
    FriendlyByteBuf invalidBuffer = new FriendlyByteBuf(Unpooled.buffer());
    invalidBuffer.writeVarInt(99);
    invalidBuffer.writeVarInt(42);
    RequestSetFormPacket invalidForm = RequestSetFormPacket.decode(invalidBuffer);
    require(helper, invalidForm.mode() == null && invalidForm.requestSeq() == 42,
        "Set-form packet accepted illegal enum");
    FriendlyByteBuf visualBuffer = new FriendlyByteBuf(Unpooled.buffer());
    SyncFormVisualPacket original = new SyncFormVisualPacket(7, true, FormMode.RESONANT);
    SyncFormVisualPacket.encode(original, visualBuffer);
    require(helper, original.equals(SyncFormVisualPacket.decode(visualBuffer)),
        "Visual state packet failed codec round trip");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void actionIntentAndResultCodecsPreserveSequence(GameTestHelper helper) {
    FriendlyByteBuf twistBuffer = new FriendlyByteBuf(Unpooled.buffer());
    RequestTemporalTwistPacket.encode(new RequestTemporalTwistPacket(101), twistBuffer);
    require(helper, RequestTemporalTwistPacket.decode(twistBuffer).requestSeq() == 101,
        "Twist intent lost request sequence");
    FriendlyByteBuf openBuffer = new FriendlyByteBuf(Unpooled.buffer());
    RequestOpenRelicPanelPacket.encode(new RequestOpenRelicPanelPacket(102), openBuffer);
    require(helper, RequestOpenRelicPanelPacket.decode(openBuffer).requestSeq() == 102,
        "Open intent lost request sequence");
    ActionResultPacket result = new ActionResultPacket(ClientActionSink.Action.FORM, 103,
        ClientActionSink.Result.NOT_EQUIPPED);
    FriendlyByteBuf resultBuffer = new FriendlyByteBuf(Unpooled.buffer());
    ActionResultPacket.encode(result, resultBuffer);
    require(helper, result.equals(ActionResultPacket.decode(resultBuffer)),
        "Action result lost action, status or request sequence");
    FriendlyByteBuf malformed = new FriendlyByteBuf(Unpooled.buffer());
    malformed.writeVarInt(99);
    malformed.writeVarInt(104);
    malformed.writeVarInt(99);
    ActionResultPacket rejected = ActionResultPacket.decode(malformed);
    require(helper, rejected.action() == null && rejected.requestSeq() == 104
            && rejected.result() == null,
        "Malformed acknowledgement enum was treated as valid");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void cooldownSyncContinuesUnequippedAndSendsFinalZero(GameTestHelper helper) {
    require(helper, ServerEvents.shouldSyncOwnerPeriodic(false, false, false, 19, 20, 20),
        "Unequipped cooldown stopped periodic owner sync");
    require(helper, ServerEvents.shouldSyncOwnerPeriodic(false, false, false, 0, 1, 21),
        "Cooldown transition to zero was not synchronized");
    require(helper, !ServerEvents.shouldSyncOwnerPeriodic(false, false, false, 0, 0, 40),
        "Idle player continued periodic owner sync");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void ownerPacketFullCodecKeepsPrivateRules(GameTestHelper helper) {
    SyncResonanceStatePacket original = new SyncResonanceStatePacket(9, true, FormMode.RESONANT,
        true, 81, 241, true, 12000, 200, 300, true, 200, 600, 0.5D, true, 24000,
        false);
    FriendlyByteBuf ownerBuffer = new FriendlyByteBuf(Unpooled.buffer());
    SyncResonanceStatePacket.encode(original, ownerBuffer);
    require(helper, original.equals(SyncResonanceStatePacket.decode(ownerBuffer)),
        "Owner state packet lost private server rules");

    SyncFormVisualPacket visual = new SyncFormVisualPacket(9, true, FormMode.RESONANT);
    FriendlyByteBuf visualBuffer = new FriendlyByteBuf(Unpooled.buffer());
    SyncFormVisualPacket.encode(visual, visualBuffer);
    require(helper, visualBuffer.readableBytes() < ownerBuffer.writerIndex(),
        "Tracking visual packet unexpectedly matched private owner payload size");
    helper.succeed();
  }

  @GameTest(template = "empty", templateNamespace = "resonance_clock", batch = "resonance_clock")
  public static void openPanelRequiresGenuineHeldOrEquippedRelic(GameTestHelper helper) {
    ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
        new GameProfile(UUID.randomUUID(), "open-panel-auth"));
    require(helper, !dev.resonance.clock.network.ModNetwork.mayOpenRelicPanel(player),
        "Empty hands were authorized to open relic panel");
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
        new ItemStack(ModItems.TEMPORAL_PURIFIER.get()));
    require(helper, !dev.resonance.clock.network.ModNetwork.mayOpenRelicPanel(player),
        "Unrelated held item was authorized to open relic panel");
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
        new ItemStack(ModItems.RESONANCE_RELIC.get()));
    require(helper, dev.resonance.clock.network.ModNetwork.mayOpenRelicPanel(player),
        "Genuine held relic was rejected for panel opening");
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

  private static CompoundTag activeTwistRootWithWrongFood() {
    CompoundTag persistent = new CompoundTag();
    CompoundTag state = new CompoundTag();
    state.putInt("version", 1);
    CompoundTag snapshot = validSnapshot().save();
    snapshot.putString("food", "invalid");
    state.put("snapshot", snapshot);
    state.putInt("remainingTicks", 20);
    state.putBoolean("executing", false);
    persistent.put(TemporalState.ROOT, state);
    return persistent;
  }

  private static CompoundTag activeTwistRoot(int remainingTicks) {
    CompoundTag persistent = new CompoundTag();
    CompoundTag state = new CompoundTag();
    state.putInt("version", 1);
    state.put("snapshot", compatibleSnapshot().save());
    state.putInt("remainingTicks", remainingTicks);
    state.putBoolean("executing", false);
    persistent.put(TemporalState.ROOT, state);
    return persistent;
  }

  private static CompoundTag activeCurseRootWithWrongFood() {
    CompoundTag persistent = activeCurseRoot(20);
    persistent.getCompound(TemporalCurseState.ROOT).getCompound("snapshot")
        .putString("food", "invalid");
    return persistent;
  }

  private static CompoundTag activeCompatibleCurseRoot(int remainingTicks) {
    CompoundTag persistent = new CompoundTag();
    CompoundTag state = TemporalCurseState.getOrCreate(persistent);
    state.put("snapshot", compatibleSnapshot().save());
    state.putInt("remainingTicks", remainingTicks);
    state.putBoolean("executing", false);
    return persistent;
  }

  private static TemporalSnapshot validSnapshot() {
    return validSnapshotWith(12, 111, 9, 2.5F, -15.0F);
  }

  private static TemporalSnapshot compatibleSnapshot() {
    return validSnapshotWith(-1, -1, 21, 21.0F, 91.0F);
  }

  private static TemporalSnapshot validSnapshotWith(int fireTicks, int air, int food, float saturation,
      float pitch) {
    ResourceKey<Level> dimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
        new ResourceLocation("resonance_clock", "test_dimension"));
    return new TemporalSnapshot(dimension, 1.25D, 64.0D, -8.5D, 90.0F, pitch,
        7.5F, food, saturation, air, fireTicks, 3.0F, List.of());
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
