package dev.resonance.clock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.resonance.clock.ResonanceClock;
import dev.resonance.clock.state.RelicState;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Lightweight, original geometry for the first visual pass. It intentionally
 * has no temporal-state prediction: only Curios equipment visibility is drawn.
 */
public final class ResonanceRenderLayer<T extends LivingEntity, M extends PlayerModel<T>>
    extends RenderLayer<T, M> {
  private static final ResourceLocation WHITE =
      new ResourceLocation(ResonanceClock.MOD_ID, "textures/misc/resonance_white.png");
  private static final RenderType GEOMETRY = RenderType.entityTranslucent(WHITE);
  private static final int BLUE = 0xBFEFFF;
  private static final int BRIGHT = 0xEFFFFF;

  public ResonanceRenderLayer(RenderLayerParent<T, M> parent) {
    super(parent);
  }

  @Override
  public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight,
                     T entity, float limbSwing, float limbSwingAmount, float partialTick,
                     float ageInTicks, float netHeadYaw, float headPitch) {
    if (!RelicState.isEquipped(entity)) {
      return;
    }

    float pulse = 0.92f + 0.08f * (float) Math.sin((entity.tickCount + partialTick) * 0.08f);
    poseStack.pushPose();
    // Player model units are 1/16 block. The positive Z side is the back.
    poseStack.translate(0.0d, 0.92d, 0.19d);
    VertexConsumer consumer = buffers.getBuffer(GEOMETRY);
    Matrix4f matrix = poseStack.last().pose();
    Matrix3f normal = poseStack.last().normal();

    drawRing(consumer, matrix, normal, 0.78f, 64, 0.035f, BLUE, 0.72f * pulse);
    drawRing(consumer, matrix, normal, 0.62f, 48, 0.012f, BRIGHT, 0.54f * pulse);
    drawTicks(consumer, matrix, normal, 0.69f, 0.77f, 0.018f, 12, BRIGHT, 0.72f * pulse);
    drawHands(consumer, matrix, normal, ageInTicks, 0.70f * pulse);
    drawWing(consumer, matrix, normal, -1.0f, pulse);
    drawWing(consumer, matrix, normal, 1.0f, pulse);
    poseStack.popPose();
  }

  private static void drawRing(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                               float radius, int segments, float width, int rgb, float alpha) {
    for (int i = 0; i < segments; i++) {
      float a0 = (float) (Math.PI * 2.0 * i / segments);
      float a1 = (float) (Math.PI * 2.0 * (i + 1) / segments);
      quad(out, matrix, normal,
          (float) Math.cos(a0) * radius, (float) Math.sin(a0) * radius,
          (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius,
          width, rgb, alpha);
    }
  }

  private static void drawTicks(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                                float inner, float outer, float width, int count,
                                int rgb, float alpha) {
    for (int i = 0; i < count; i++) {
      float a = (float) (Math.PI * 2.0 * i / count - Math.PI / 2.0);
      float x0 = (float) Math.cos(a) * inner;
      float y0 = (float) Math.sin(a) * inner;
      float x1 = (float) Math.cos(a) * outer;
      float y1 = (float) Math.sin(a) * outer;
      line(out, matrix, normal, x0, y0, x1, y1, width, rgb, alpha);
    }
  }

  private static void drawHands(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                                float age, float alpha) {
    float minute = age * 0.018f;
    float hour = -age * 0.004f;
    line(out, matrix, normal, 0, 0, (float) Math.cos(minute) * 0.52f,
        (float) Math.sin(minute) * 0.52f, 0.024f, BRIGHT, alpha);
    line(out, matrix, normal, 0, 0, (float) Math.cos(hour) * 0.34f,
        (float) Math.sin(hour) * 0.34f, 0.035f, BLUE, alpha);
  }

  private static void drawWing(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                               float side, float pulse) {
    float sway = (float) Math.sin(pulse * 2.0f) * 0.025f;
    float x = side * 0.12f;
    triangle(out, matrix, normal, x, 0.08f, side * 0.56f, 0.45f + sway,
        side * 0.42f, 0.08f, BLUE, 0.36f);
    triangle(out, matrix, normal, x, -0.03f, side * 0.53f, -0.42f - sway,
        side * 0.38f, -0.10f, BRIGHT, 0.25f);
    line(out, matrix, normal, x, 0.02f, side * 0.55f, 0.37f + sway,
        0.014f, BRIGHT, 0.65f);
  }

  private static void line(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                           float x0, float y0, float x1, float y1, float width,
                           int rgb, float alpha) {
    float dx = x1 - x0;
    float dy = y1 - y0;
    float length = (float) Math.sqrt(dx * dx + dy * dy);
    float px = -dy / length * width;
    float py = dx / length * width;
    quad(out, matrix, normal, x0, y0, x1, y1, px, py, rgb, alpha);
  }

  private static void quad(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                           float x0, float y0, float x1, float y1, float width,
                           int rgb, float alpha) {
    float dx = x1 - x0;
    float dy = y1 - y0;
    float length = (float) Math.sqrt(dx * dx + dy * dy);
    float px = -dy / length * width;
    float py = dx / length * width;
    quad(out, matrix, normal, x0, y0, x1, y1, px, py, rgb, alpha);
  }

  private static void quad(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                           float x0, float y0, float x1, float y1, float px, float py,
                           int rgb, float alpha) {
    int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
    int r = (rgb >> 16) & 255;
    int g = (rgb >> 8) & 255;
    int b = rgb & 255;
    vertex(out, matrix, normal, x0 + px, y0 + py, r, g, b, a, 0, 0);
    vertex(out, matrix, normal, x0 - px, y0 - py, r, g, b, a, 0, 1);
    vertex(out, matrix, normal, x1 - px, y1 - py, r, g, b, a, 1, 1);
    vertex(out, matrix, normal, x1 + px, y1 + py, r, g, b, a, 1, 0);
  }

  private static void triangle(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                               float x0, float y0, float x1, float y1, float x2, float y2,
                               int rgb, float alpha) {
    int a = Math.max(0, Math.min(255, (int) (alpha * 255.0f)));
    int r = (rgb >> 16) & 255;
    int g = (rgb >> 8) & 255;
    int b = rgb & 255;
    vertex(out, matrix, normal, x0, y0, r, g, b, a, 0, 0);
    vertex(out, matrix, normal, x1, y1, r, g, b, a, 1, 0);
    vertex(out, matrix, normal, x2, y2, r, g, b, a, 0.5f, 1);
  }

  private static void vertex(VertexConsumer out, Matrix4f matrix, Matrix3f normal,
                             float x, float y, int r, int g, int b, int a, float u, float v) {
    out.vertex(matrix, x, y, 0.0f).color(r, g, b, a).uv(u, v)
        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880)
        .normal(normal, 0.0f, 0.0f, 1.0f).endVertex();
  }
}
