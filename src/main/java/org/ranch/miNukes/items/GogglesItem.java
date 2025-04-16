package org.ranch.miNukes.items;

import net.minecraft.item.ArmorItem;

public class GogglesItem extends ArmorItem {
	public GogglesItem(Settings settings) {
		super(new GogglesArmorMaterial(), Type.HELMET, settings.maxDamage(-1));
	}

	@Override
	public boolean isDamageable() {
		return false;
	}
}
