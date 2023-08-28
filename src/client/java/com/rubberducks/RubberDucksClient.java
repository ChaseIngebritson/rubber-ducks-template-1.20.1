package com.rubberducks;

import com.rubberducks.model.RubberDuckEntityModel;
import com.rubberducks.renderer.RubberDuckCowEntityRenderer;
import com.rubberducks.renderer.RubberDuckEntityRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.fabricmc.api.EnvType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class RubberDucksClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("rubberducks");

	public static final EntityModelLayer MODEL_RUBBER_DUCK_LAYER = new EntityModelLayer(
			new Identifier("rubberducks", "rubber_duck"), "main");
	public static final EntityModelLayer MODEL_RUBBER_DUCK_COW_LAYER = new EntityModelLayer(
			new Identifier("rubberducks", "rubber_duck_cow"), "main");

	@Override
	public void onInitializeClient() {
		LOGGER.info("Hello Fabric world (client)!");

		EntityRendererRegistry.register(RubberDucks.RUBBER_DUCK, (context) -> {
			return new RubberDuckEntityRenderer(context);
		});

		EntityRendererRegistry.register(RubberDucks.RUBBER_DUCK_COW, (context) -> {
			return new RubberDuckCowEntityRenderer(context);
		});

		EntityModelLayerRegistry.registerModelLayer(MODEL_RUBBER_DUCK_LAYER, RubberDuckEntityModel::getTexturedModelData);
	}
}