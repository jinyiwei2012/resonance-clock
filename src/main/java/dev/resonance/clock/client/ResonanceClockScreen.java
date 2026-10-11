package dev.resonance.clock.client;

import dev.resonance.clock.api.ClientActionSink;
import dev.resonance.clock.api.ClientStateSink;
import dev.resonance.clock.api.FormMode;
import dev.resonance.clock.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Native 1.20.1 screen for the held relic panel. */
public final class ResonanceClockScreen extends Screen {
  private static final int PANEL_W = 430;
  private static final int PANEL_H = 270;
  private FormMode displayedMode;
  private boolean pendingForm;
  private boolean pendingTwist;
  private FormMode pendingMode;
  private Button baseButton;
  private Button resonantButton;
  private Button castButton;
  private Button refreshButton;
  private Button prefsButton;
  private Button closeButton;
  private Button ringButton;
  private Button wingsButton;
  private Button qualityButton;
  private Button animationButton;
  private boolean showPreferences;
  private ClientActionSink.Action pendingAction;
  private int pendingSeq;
  private long pendingSinceNanos;
  private boolean pendingFormAcked;
  private boolean pendingFormStateConfirmed;
  private Component feedback;

  public ResonanceClockScreen() {
    super(Component.translatable("screen.resonance_clock.relic.title"));
    ClientStateSink.State state = ResonanceClientStore.ownerState();
    displayedMode = state == null ? FormMode.BASE : state.formMode();
  }

  public static void openOrRefresh() {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen instanceof ResonanceClockScreen screen) {
      screen.rebuildWidgets();
      return;
    }
    if (minecraft.player != null) {
      minecraft.setScreen(new ResonanceClockScreen());
    }
  }

  public static void acceptState(ClientStateSink.State state) {
    ResonanceClientStore.acceptOwner(state);
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen instanceof ResonanceClockScreen screen) {
      if (screen.pendingForm && screen.pendingMode == state.formMode()) {
        screen.pendingFormStateConfirmed = true;
        screen.finishFormIfAcknowledged();
      }
      screen.rebuildWidgets();
    }
  }

  public static void acceptAction(ClientActionSink.ActionResult result) {
    Minecraft minecraft = Minecraft.getInstance();
    if (!(minecraft.screen instanceof ResonanceClockScreen screen)
        || screen.pendingAction != result.action() || screen.pendingSeq != result.requestSeq()) {
      return;
    }
    if (result.action() == ClientActionSink.Action.FORM
        && result.result() == ClientActionSink.Result.ACCEPTED) {
      screen.pendingFormAcked = true;
      screen.feedback = Component.translatable("screen.resonance_clock.relic.status.waiting_form");
      screen.finishFormIfAcknowledged();
      return;
    }
    screen.clearPending();
    if (result.result() != ClientActionSink.Result.ACCEPTED) {
      screen.feedback = Component.translatable(resultKey(result.result()));
    }
    screen.rebuildWidgets();
  }

  public static void tickPending() {
    Minecraft minecraft = Minecraft.getInstance();
    if (!(minecraft.screen instanceof ResonanceClockScreen screen) || screen.pendingAction == null) {
      return;
    }
    if (System.nanoTime() - screen.pendingSinceNanos >= 5_000_000_000L) {
      screen.clearPending();
      screen.feedback = Component.translatable("screen.resonance_clock.relic.status.timeout");
      screen.rebuildWidgets();
    }
  }

  @Override
  protected void init() {
    rebuildWidgets();
  }

  @Override
  protected void rebuildWidgets() {
    if (minecraft == null || width <= 0) return;
    clearWidgets();
    ClientStateSink.State current = ResonanceClientStore.ownerState();
    if (!pendingForm && current != null) {
      displayedMode = current.formMode();
    }
    boolean compact = isCompact();
    int panelWidth = panelWidth();
    int panelHeight = panelHeight();
    int left = (width - panelWidth) / 2;
    int top = (height - panelHeight) / 2;
    int buttonWidth = compact ? 96 : 122;
    baseButton = addRenderableWidget(Button.builder(
        Component.translatable("screen.resonance_clock.relic.form.base"), b -> requestForm(FormMode.BASE))
        .bounds(left + 18, top + 42, buttonWidth, 20).build());
    resonantButton = addRenderableWidget(Button.builder(
        Component.translatable("screen.resonance_clock.relic.form.resonant"), b -> requestForm(FormMode.RESONANT))
        .bounds(left + 18, top + 66, buttonWidth, 20).build());
    castButton = addRenderableWidget(Button.builder(
        Component.translatable("screen.resonance_clock.relic.action.cast"), b -> castTwist())
        .bounds(left + 18, top + 122, buttonWidth, 20).build());
    refreshButton = addRenderableWidget(Button.builder(
        Component.translatable("screen.resonance_clock.relic.action.refresh"), b -> refresh())
        .bounds(left + 18, top + 146, buttonWidth, 20).build());
    prefsButton = addRenderableWidget(Button.builder(
        Component.translatable("screen.resonance_clock.relic.action.visuals"), b -> {
          showPreferences = !showPreferences;
          rebuildWidgets();
        }).bounds(left + 18, top + 170, buttonWidth, 20).build());
    if (showPreferences) {
      ringButton = addRenderableWidget(Button.builder(
          Component.translatable("screen.resonance_clock.relic.visuals.ring",
              ResonanceVisualSettings.SHOW_RING.get()), b -> {
            ResonanceVisualSettings.SHOW_RING.set(!ResonanceVisualSettings.SHOW_RING.get());
            rebuildWidgets();
          }).bounds(left + 122, top + 146, compact ? 82 : 122, 20).build());
      wingsButton = addRenderableWidget(Button.builder(
          Component.translatable("screen.resonance_clock.relic.visuals.wings",
              ResonanceVisualSettings.SHOW_WINGS.get()), b -> {
            ResonanceVisualSettings.SHOW_WINGS.set(!ResonanceVisualSettings.SHOW_WINGS.get());
            rebuildWidgets();
          }).bounds(left + (compact ? 208 : 252), top + 146, compact ? 82 : 122, 20).build());
      qualityButton = addRenderableWidget(Button.builder(
          Component.translatable("screen.resonance_clock.relic.visuals.quality",
              ResonanceVisualSettings.SIMPLE_QUALITY.get()), b -> {
            ResonanceVisualSettings.SIMPLE_QUALITY.set(!ResonanceVisualSettings.SIMPLE_QUALITY.get());
            rebuildWidgets();
          }).bounds(left + 122, top + 170, compact ? 82 : 122, 20).build());
      animationButton = addRenderableWidget(Button.builder(
          Component.translatable("screen.resonance_clock.relic.visuals.animation",
              ResonanceVisualSettings.ANIMATION.get()), b -> {
            ResonanceVisualSettings.ANIMATION.set(!ResonanceVisualSettings.ANIMATION.get());
            rebuildWidgets();
          }).bounds(left + (compact ? 208 : 252), top + 170, compact ? 82 : 122, 20).build());
    }
    closeButton = addRenderableWidget(Button.builder(
        Component.translatable("screen.resonance_clock.relic.action.close"), b -> onClose())
        .bounds(left + panelWidth - 112, top + panelHeight - 28, 94, 20).build());
    updateEnabledState();
  }

  private void requestForm(FormMode mode) {
    ClientStateSink.State state = ResonanceClientStore.ownerState();
    if (pendingAction != null || state == null || !state.equipped()
        || pendingForm || state.formMode() == mode) return;
    pendingForm = true;
    pendingMode = mode;
    beginPending(ClientActionSink.Action.FORM, ModNetwork.sendSetForm(mode));
    rebuildWidgets();
  }

  private void castTwist() {
    ClientStateSink.State state = ResonanceClientStore.ownerState();
    if (pendingAction != null || state == null || !state.equipped() || !state.twistEnabled()
        || state.twistActive() || state.cooldownTicks() > 0 || pendingTwist) return;
    pendingTwist = true;
    beginPending(ClientActionSink.Action.TWIST, ModNetwork.requestTemporalTwist());
    rebuildWidgets();
  }

  private void refresh() {
    if (pendingAction != null) return;
    beginPending(ClientActionSink.Action.OPEN_PANEL, ModNetwork.requestOpenPanel());
    rebuildWidgets();
  }

  private void beginPending(ClientActionSink.Action action, int seq) {
    pendingAction = action;
    pendingSeq = seq;
    pendingSinceNanos = System.nanoTime();
    pendingFormAcked = false;
    pendingFormStateConfirmed = false;
  }

  private void finishFormIfAcknowledged() {
    if (pendingAction == ClientActionSink.Action.FORM
        && pendingFormAcked && pendingFormStateConfirmed) {
      clearPending();
      feedback = Component.translatable("screen.resonance_clock.relic.status.accepted");
      rebuildWidgets();
    }
  }

  private void clearPending() {
    pendingAction = null;
    pendingSeq = 0;
    pendingSinceNanos = 0L;
    pendingFormAcked = false;
    pendingFormStateConfirmed = false;
    pendingForm = false;
    pendingMode = null;
    pendingTwist = false;
  }

  private static String resultKey(ClientActionSink.Result result) {
    return switch (result) {
      case THROTTLED -> "screen.resonance_clock.relic.status.throttled";
      case NOT_EQUIPPED -> "screen.resonance_clock.relic.status.not_equipped_action";
      case DISABLED -> "screen.resonance_clock.relic.status.disabled_action";
      case ACTIVE -> "screen.resonance_clock.relic.status.active_action";
      case COOLDOWN -> "screen.resonance_clock.relic.status.cooldown_action";
      case INVALID_FORM -> "screen.resonance_clock.relic.status.invalid_form";
      case UNAVAILABLE -> "screen.resonance_clock.relic.status.unavailable";
      case ACCEPTED -> "screen.resonance_clock.relic.status.accepted";
    };
  }

  private void updateEnabledState() {
    ClientStateSink.State state = ResonanceClientStore.ownerState();
    boolean equipped = state != null && state.equipped();
    boolean formReady = equipped && !pendingForm;
    boolean actionReady = pendingAction == null;
    baseButton.active = actionReady && formReady && displayedMode != FormMode.BASE;
    resonantButton.active = actionReady && formReady && displayedMode != FormMode.RESONANT;
    castButton.active = actionReady && equipped && !pendingTwist && state != null && state.twistEnabled()
        && !state.twistActive()
        && state.cooldownTicks() <= 0;
    refreshButton.active = actionReady;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    renderBackground(graphics);
    boolean compact = isCompact();
    int panelWidth = panelWidth();
    int panelHeight = panelHeight();
    int left = (width - panelWidth) / 2;
    int top = (height - panelHeight) / 2;
    graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFF182B43);
    graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF0101724);
    graphics.drawString(font, title, left + 18, top + 14, 0xEAF7FF, false);
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.form"),
        left + 18, top + 30, 0x8BC9EF, false);
    drawPreview(graphics, left + (compact ? 160 : 215), top + 92, compact ? 0.8f : 1.0f);
    drawStatus(graphics, left + (compact ? 210 : 285), top + 42);
    if (showPreferences && !compact) drawPreferences(graphics, left + 155, top + 202);
    super.render(graphics, mouseX, mouseY, partialTick);
  }

  private void drawStatus(GuiGraphics graphics, int x, int y) {
    ClientStateSink.State state = ResonanceClientStore.ownerState();
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.server"), x, y,
        0x8BC9EF, false);
    if (state == null) {
      graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.waiting"), x, y + 16,
          0xE6C56A, false);
      return;
    }
    int color = state.equipped() ? 0xA7F3D0 : 0xF6B5B5;
    graphics.drawString(font, Component.translatable(state.equipped()
        ? "screen.resonance_clock.relic.status.equipped"
        : "screen.resonance_clock.relic.not_equipped"), x, y + 16, color, false);
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.twist",
        formatTicks(state.remainingTicks()), formatTicks(state.cooldownTicks())), x, y + 34, 0xD7E8F5, false);
    if (!state.twistEnabled()) {
      graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.twist_unavailable"),
          x, y + 52, 0xF0B6A8, false);
    }
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.curse",
        state.curseActive() ? formatTicks(state.curseRemainingTicks()) :
            Component.translatable("screen.resonance_clock.relic.status.none")), x, y + 70, 0xD7E8F5, false);
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.rules",
        formatTicks(state.configuredDurationTicks()),
        formatTicks(effectiveConfiguredCooldownTicks(state))), x, y + 88,
        0x9DB7C9, false);
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.configured",
        formatTicks(state.configuredDurationTicks()), formatTicks(state.configuredCooldownTicks()),
        formatMultiplier(state.resonantCooldownMultiplier())), x, y + 106, 0x9DB7C9, false);
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.rules_extra",
        state.curseEnabled() ? formatTicks(state.curseIntervalTicks())
            : Component.translatable("screen.resonance_clock.relic.status.disabled"),
        state.flightEnabled() ? Component.translatable("screen.resonance_clock.relic.status.enabled")
            : Component.translatable("screen.resonance_clock.relic.status.disabled")), x, y + 124,
        0x9DB7C9, false);
    if (pendingForm || pendingTwist) {
      graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.status.pending"), x, y + 142,
          0xE6C56A, false);
    }
    if (feedback != null) {
      graphics.drawString(font, feedback, x, y + 160, 0xF0C979, false);
    }
  }

  private void drawPreview(GuiGraphics graphics, int cx, int cy, float scale) {
    int radius = (int) (52 * scale);
    int color = displayedMode == FormMode.BASE ? 0xBFEFFF : 0xC4E8FF;
    graphics.drawString(font, Component.translatable(displayedMode == FormMode.BASE
        ? "screen.resonance_clock.relic.form.base.preview"
        : "screen.resonance_clock.relic.form.resonant.preview"), cx - 52, cy - 78, color, false);
    for (int i = 0; i < 12; i++) {
      double angle = -Math.PI / 2.0 + i * Math.PI * 2.0 / 12.0;
      int x = cx + (int) (Math.cos(angle) * radius);
      int y = cy + (int) (Math.sin(angle) * radius);
      int phase = displayedMode == FormMode.BASE ? 0 : (i % 4);
      int size = displayedMode == FormMode.BASE ? 5 : 4 + phase;
      graphics.fill(x - size, y - size, x + size + 1, y + size + 1, 0xB83391C8);
      if (displayedMode == FormMode.BASE) {
        String numeral = romanNumeral(i + 1);
        graphics.drawString(font, numeral, x - font.width(numeral) / 2, y - 3, 0xEFFFFF, false);
      }
    }
    graphics.fill(cx - 60, cy - 1, cx + 60, cy + 1, 0x9CC9F3FF);
    graphics.fill(cx - 1, cy - 60, cx + 1, cy + 60, 0x6CC9F3FF);
    if (displayedMode == FormMode.RESONANT) {
      graphics.fill(cx - 72, cy - 66, cx - 58, cy - 54, 0xB8D9F3FF);
      graphics.fill(cx + 58, cy - 66, cx + 72, cy - 54, 0xB8D9F3FF);
    }
  }

  private boolean isCompact() {
    return width < PANEL_W + 16 || height < PANEL_H + 16;
  }

  private int panelWidth() {
    return Math.min(PANEL_W, Math.max(304, width - 16));
  }

  private int panelHeight() {
    return Math.min(PANEL_H, Math.max(224, height - 16));
  }

  private void drawPreferences(GuiGraphics graphics, int x, int y) {
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.visuals.title"), x, y,
        0x8BC9EF, false);
    graphics.drawString(font, Component.translatable("screen.resonance_clock.relic.visuals.summary",
        ResonanceVisualSettings.SHOW_RING.get(), ResonanceVisualSettings.SHOW_WINGS.get(),
        ResonanceVisualSettings.SIMPLE_QUALITY.get(), ResonanceVisualSettings.ANIMATION.get()), x, y + 14,
        0xBBD1E0, false);
  }

  private static String formatTicks(int ticks) {
    return ticks <= 0 ? "—" : Integer.toString((ticks + 19) / 20) + "s";
  }

  private static String formatMultiplier(double multiplier) {
    return String.format(java.util.Locale.ROOT, "%.2fx", multiplier);
  }

  private static String romanNumeral(int value) {
    return switch (value) {
      case 1 -> "I";
      case 2 -> "II";
      case 3 -> "III";
      case 4 -> "IV";
      case 5 -> "V";
      case 6 -> "VI";
      case 7 -> "VII";
      case 8 -> "VIII";
      case 9 -> "IX";
      case 10 -> "X";
      case 11 -> "XI";
      case 12 -> "XII";
      default -> "";
    };
  }

  /** Server-rule cooldown, deliberately separate from the live cooldownTicks field. */
  private static int effectiveConfiguredCooldownTicks(ClientStateSink.State state) {
    if (state.configuredCooldownTicks() <= 0) {
      return 0;
    }
    return Math.max(1, (int) Math.ceil(
        state.configuredCooldownTicks() * state.resonantCooldownMultiplier()));
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      onClose();
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void onClose() {
    clearPending();
    super.onClose();
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }
}
