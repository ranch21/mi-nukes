package org.ranch.miNukes.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.ranch.miNukes.MiNukes;
import org.ranch.miNukes.explosions.EntityNukeTorex;
import org.ranch.miNukes.rendering.EntityNukeTorexRenderer;

import static org.ranch.miNukes.MiNukes.NUKE;
import static org.ranch.miNukes.MiNukes.TOREX;

public class MiNukesClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(NUKE, EmptyEntityRenderer::new);
		EntityRendererRegistry.register(TOREX, EntityNukeTorexRenderer::new);
	}
}
