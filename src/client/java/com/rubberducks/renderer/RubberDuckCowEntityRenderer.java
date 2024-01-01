package com.rubberducks.renderer;

import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

import com.rubberducks.entity.RubberDuckEntity;

import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class RubberDuckCowEntityRenderer extends RubberDuckEntityRenderer {
  public RubberDuckCowEntityRenderer(EntityRendererFactory.Context context) {
    super(context);
  }

  @Override
  public Identifier getTexture(RubberDuckEntity entity) {
    return new Identifier("rubberducks", "textures/entity/rubber_duck/rubber_duck_cow.png");
  }
}
