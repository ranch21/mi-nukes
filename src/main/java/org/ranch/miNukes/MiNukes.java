package org.ranch.miNukes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.ranch.miNukes.explosions.*;
import org.ranch.miNukes.rendering.EntityNukeTorexRenderer;

public class MiNukes implements ModInitializer {

	public static long flashTimestamp = 0;
	public static long shakeTimestamp = 0;
	public static final int FLASH_TIME = 2000;

	public static String MODID = "mi_nukes";

	public static final EntityType<EntityNukeExplosion> NUKE = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of(MiNukes.MODID, "nuke"),
			EntityType.Builder.<EntityNukeExplosion>create(EntityNukeExplosion::new, SpawnGroup.MISC).setDimensions(1,1).build("nuke")
	);

	public static final EntityType<EntityNukeTorex> TOREX = Registry.register(
			Registries.ENTITY_TYPE,
			new Identifier(MiNukes.MODID, "torex"),
			FabricEntityTypeBuilder.<EntityNukeTorex>create(SpawnGroup.MISC, EntityNukeTorex::new)
					.dimensions(EntityDimensions.fixed(1.0f, 1.0f))
					.trackRangeBlocks(256)
					.trackedUpdateRate(1)
					.build()
	);

	public static final ItemGroup NUKE_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(MiNukes.DETONATOR_ITEM))
			.displayName(Text.translatable("itemGroup.mi_nukes.item_group"))
			.entries((context, entries) -> {
				entries.add(MiNukes.DETONATOR_ITEM);
			})
			.build();

	public static final Identifier NUKE_SOUND_ID = Identifier.of(MiNukes.MODID,"nuclear_explosion");
	public static SoundEvent NUKE_SOUND_EVENT = SoundEvent.of(NUKE_SOUND_ID);

	public static final Item DETONATOR_ITEM = new DetonatorItem(new Item.Settings().maxCount(1));

	@Override
	public void onInitialize() {
		EntityRendererRegistry.register(NUKE, EmptyEntityRenderer::new);
		EntityRendererRegistry.register(TOREX, EntityNukeTorexRenderer::new);
		Registry.register(Registries.SOUND_EVENT, NUKE_SOUND_ID, NUKE_SOUND_EVENT);
		Registry.register(Registries.ITEM, new Identifier(MODID, "detonator"), DETONATOR_ITEM);
		Registry.register(Registries.ITEM_GROUP, new Identifier(MODID, "nuke_group"), NUKE_GROUP);
	}

	public static void nuke(int strength, Vec3d pos, World world) {
		EntityNukeExplosion explosion = EntityNukeExplosion.statFac(world, strength, pos.x, pos.y, pos.z);
		world.spawnEntity(explosion);

		EntityNukeTorex torex = new EntityNukeTorex(world);
		torex.setPos(pos.x, pos.y + 0.5, pos.z);
		torex.getDataTracker().set(EntityNukeTorex.SCALE, 1.2F);
		world.spawnEntity(torex);
	}
}
