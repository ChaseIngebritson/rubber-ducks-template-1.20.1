package com.rubberducks.renderer;

import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

import com.rubberducks.entity.RubberDuckEntity;

import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class RubberDuckEntityRenderer extends RubberDuckBaseEntityRenderer {
  public RubberDuckEntityRenderer(EntityRendererFactory.Context context) {
    super(context);
  }

  @Override
  public Identifier getTexture(RubberDuckEntity entity) {
    return new Identifier("rubberducks", "textures/entity/rubber_duck/rubber_duck.png");
  }
}
