package com.rubberducks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rubberducks.entity.RubberDuckBaseEntity;
import com.rubberducks.entity.RubberDuckEntity;
import com.rubberducks.entity.RubberDuckCowEntity;

public class RubberDucks implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("rubberducks");

	public static final Identifier SQUEAK1_ID = new Identifier("rubberducks:squeak1");
	public static SoundEvent SQUEAK1_EVENT = SoundEvent.of(SQUEAK1_ID);

	public static final EntityType<RubberDuckBaseEntity> RUBBER_DUCK = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier("rubberducks", "rubber_duck"),
			FabricEntityTypeBuilder.create(SpawnGroup.MISC, RubberDuckEntity::new).dimensions(EntityDimensions.fixed(1f, 1f))
					.build());

	public static final EntityType<RubberDuckBaseEntity> RUBBER_DUCK_COW = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier("rubberducks", "rubber_duck_cow"),
			FabricEntityTypeBuilder.create(SpawnGroup.MISC, RubberDuckCowEntity::new)
					.dimensions(EntityDimensions.fixed(1f, 1f))
					.build());

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		Registry.register(Registries.SOUND_EVENT, RubberDucks.SQUEAK1_ID, SQUEAK1_EVENT);
	}
}