package com.rubberducks.renderer;

import org.joml.Quaternionf;

import com.rubberducks.RubberDucksClient;
import com.rubberducks.entity.RubberDuckEntity;
import com.rubberducks.model.RubberDuckEntityModel;

import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.ModelWithWaterPatch;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class RubberDuckBaseEntityRenderer extends EntityRenderer<RubberDuckEntity> {
  private final RubberDuckEntityModel entityModel;

  public RubberDuckBaseEntityRenderer(EntityRendererFactory.Context context) {
    super(context);
    this.shadowRadius = 0.8f;

    ModelPart model = context.getPart(RubberDucksClient.MODEL_RUBBER_DUCK_LAYER);
    entityModel = new RubberDuckEntityModel(model);
  }

  @Override
  public void render(RubberDuckEntity entity, float yaw, float tickDelta, MatrixStack matrices,
      VertexConsumerProvider vertexConsumers, int light) {
    Identifier texture = this.getTexture(entity);
    float k;
    matrices.push();

    matrices.translate(0.0f, 0.375f, 0.0f);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));

    float h = (float) entity.getDamageWobbleTicks() - yaw;
    float j = entity.getDamageWobbleStrength() - yaw;

    if (j < 0.0f)
      j = 0.0f;

    if (h > 0.0f) {
      matrices.multiply(RotationAxis.POSITIVE_X
          .rotationDegrees(MathHelper.sin((float) h) * h * j / 10.0f * (float) entity.getDamageWobbleSide()));
    }

    if (!MathHelper.approximatelyEquals((float) (k = entity.interpolateBubbleWobble(tickDelta)), (float) 0.0f)) {
      matrices.multiply(new Quaternionf()
          .setAngleAxis(entity.interpolateBubbleWobble(tickDelta) * ((float) Math.PI / 180), 1.0f, 0.0f, 1.0f));
    }

    matrices.scale(-1.0f, -1.0f, 1.0f);
    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0f));

    entityModel.setAngles(entity, tickDelta, 0.0f, -0.1f, 0.0f, 0.0f);

    VertexConsumer vertexConsumer = vertexConsumers.getBuffer(entityModel.getLayer(texture));
    entityModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1.0f, 1.0f, 1.0f, 1.0f);

    if (!entity.isSubmergedInWater()) {
      VertexConsumer vertexConsumer2 = vertexConsumers.getBuffer(RenderLayer.getWaterMask());
      if (entityModel instanceof ModelWithWaterPatch) {
        ModelWithWaterPatch modelWithWaterPatch = (ModelWithWaterPatch) ((Object) entityModel);
        modelWithWaterPatch.getWaterPatch().render(matrices, vertexConsumer2, light, OverlayTexture.DEFAULT_UV);
      }
    }

    matrices.pop();

    super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
  }

  @Override
  public Identifier getTexture(RubberDuckEntity entity) {
    return new Identifier("rubberducks", "textures/entity/rubber_duck/rubber_duck.png");
  }
}