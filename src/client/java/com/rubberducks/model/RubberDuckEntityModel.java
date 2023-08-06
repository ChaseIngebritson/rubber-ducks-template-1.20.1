// Made with Blockbench 4.8.1
// Exported for Minecraft version 1.17+ for Yarn

package com.rubberducks.model;

import com.rubberducks.entity.RubberDuckEntity;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;


public class RubberDuckEntityModel extends EntityModel<RubberDuckEntity> {
	private final ModelPart bb_main;

	public RubberDuckEntityModel(ModelPart root) {
		this.bb_main = root.getChild("bb_main");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();

		modelPartData.addChild("bb_main", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -6.0F, -6.0F, 8.0F, 6.0F, 12.0F, new Dilation(0.0F))
			.uv(0, 18).cuboid(-3.0F, -12.0F, -5.0F, 6.0F, 6.0F, 6.0F, new Dilation(0.0F))
			.uv(0, 0).cuboid(-2.0F, -9.0F, -7.0F, 4.0F, 2.0F, 2.0F, new Dilation(0.0F))
			.uv(28, 0).cuboid(-6.0F, -5.0F, -5.0F, 2.0F, 3.0F, 7.0F, new Dilation(0.0F))
			.uv(17, 23).cuboid(4.0F, -5.0F, -5.0F, 2.0F, 3.0F, 7.0F, new Dilation(0.0F))
			.uv(18, 18).cuboid(-3.0F, -7.0F, 6.0F, 6.0F, 3.0F, 2.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
		
    return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(RubberDuckEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
  
	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
		bb_main.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}
}