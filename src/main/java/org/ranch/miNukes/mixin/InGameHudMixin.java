package org.ranch.miNukes.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.ranch.miNukes.MiItems;
import org.ranch.miNukes.MiNukes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

import static org.ranch.miNukes.MiNukes.FLASH_TIME;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

	@Unique
	private static final Identifier GOGGLES = new Identifier(MiNukes.MODID,"textures/overlay_goggles.png");

	@Shadow protected abstract void renderOverlay(DrawContext context, Identifier texture, float opacity);

	@Shadow @Final private MinecraftClient client;
	@Shadow private int scaledHeight;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getLastFrameDuration()F"))
	public void render(DrawContext context, float tickDelta, CallbackInfo ci) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (client.options.getPerspective().isFirstPerson()) {
			ItemStack itemStack = client.player.getInventory().getArmorStack(3);
			if (!client.player.isUsingSpyglass() && itemStack.isOf(MiItems.GOGGLES.asItem())) {
				renderOverlay(context, GOGGLES, 1.0f);
			} else {
				renderFlash(context);
			}
		}
	}

	@Unique
	private static void renderFlash(DrawContext context) {
		float t = MathHelper.clamp((float)(System.currentTimeMillis() - MiNukes.flashTimestamp) / FLASH_TIME, 0.0f, 1.0f);
		float alpha = 1.0f - (t * t);

		if (alpha > 0) {
			Color color = new Color(1, 1, 1, alpha);
			RenderSystem.disableDepthTest();
			RenderSystem.depthMask(false);
			context.fill(0,0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color.hashCode());
			RenderSystem.depthMask(true);
			RenderSystem.enableDepthTest();
		}
	}
}
