package org.ranch.miNukes.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.math.MathHelper;
import org.ranch.miNukes.MiNukes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

import static org.ranch.miNukes.MiNukes.FLASH_TIME;

@Mixin(InGameHud.class)
public class InGameHudMixin {

	@Inject(method = "render", at = @At("TAIL"))
	public void render(DrawContext context, float tickDelta, CallbackInfo ci) {
		float t = MathHelper.clamp((float)(System.currentTimeMillis() - MiNukes.flashTimestamp) / FLASH_TIME, 0.0f, 1.0f);
		float alpha = 1.0f - (t * t);

		if (alpha > 0) {
			Color color = new Color(1, 1, 1, alpha);
			context.fill(0,0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color.hashCode());
		}
	}
}
