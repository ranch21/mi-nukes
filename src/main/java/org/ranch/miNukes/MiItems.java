package org.ranch.miNukes;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.ranch.miNukes.items.DetonatorItem;
import org.ranch.miNukes.items.GogglesItem;


import static org.ranch.miNukes.MiNukes.MODID;

public class MiItems {
	public static final Item DETONATOR = new DetonatorItem(new Item.Settings().maxCount(1));
	public static final Item GOGGLES = new GogglesItem(new Item.Settings().maxCount(1));

	public static final ItemGroup NUKE_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(DETONATOR))
			.displayName(Text.translatable("itemGroup.mi_nukes.item_group"))
			.entries((context, entries) -> {
				entries.add(DETONATOR);
				entries.add(GOGGLES);
			})
			.build();

	public static void register(Item item, String id) {
		Registry.register(Registries.ITEM, new Identifier(MODID, id), item);
	}

	public static void initialize() {
		register(DETONATOR, "detonator");
		register(GOGGLES, "goggles");
		Registry.register(Registries.ITEM_GROUP, new Identifier("mi_nukes", "item_group"), NUKE_GROUP);
	}
}
