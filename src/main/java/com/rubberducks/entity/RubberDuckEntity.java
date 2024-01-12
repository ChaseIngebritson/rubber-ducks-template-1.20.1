package com.rubberducks.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.world.World;

public class RubberDuckEntity extends RubberDuckBaseEntity {
  public RubberDuckEntity(EntityType<RubberDuckBaseEntity> type, World world) {
    super(type, world);
    this.intersectionChecked = true;
  }
}
