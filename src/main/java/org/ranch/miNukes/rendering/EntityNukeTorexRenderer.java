package org.ranch.miNukes.rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.*;
import org.ranch.miNukes.MiNukes;
import org.ranch.miNukes.explosions.EntityNukeTorex;
import org.ranch.miNukes.explosions.EntityNukeTorex.Cloudlet;

import java.lang.Math;
import java.util.Random;

public class EntityNukeTorexRenderer extends EntityRenderer<EntityNukeTorex> {

	public static final Identifier CLOUDLET_TEXTURE = new Identifier(MiNukes.MODID, "textures/particle_base.png");
	public static final Identifier FLASH_TEXTURE = new Identifier(MiNukes.MODID, "textures/flare.png");

	public EntityNukeTorexRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(EntityNukeTorex entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {

		for (Cloudlet cloudlet : entity.cloudlets) {
			renderCloudlet(entity, cloudlet, matrices, vertexConsumers, tickDelta, LightmapTextureManager.MAX_LIGHT_COORDINATE);
		}

		if(entity.age < 101) renderFlashes(entity, 0.5f, matrices, vertexConsumers);
		if(entity.age < 10 && System.currentTimeMillis() - MiNukes.flashTimestamp > 1_000) MiNukes.flashTimestamp = System.currentTimeMillis();
		if(entity.didPlaySound && !entity.didShake && System.currentTimeMillis() - MiNukes.shakeTimestamp > 1_000) {
			MiNukes.shakeTimestamp = System.currentTimeMillis();
			entity.didShake = true;
			PlayerEntity player = MinecraftClient.getInstance().player;
			player.hurtTime = 15;
			player.maxHurtTime = 15;
			//player.attackedAtYaw = 0F;
		}

		//super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	private void renderFlashes(EntityNukeTorex cloud, float interp, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {

		double age = Math.min(cloud.age + interp, 100);
		float alpha = (float) ((100D - age) / 100F);

		java.util.Random rand = new Random(cloud.getId());

		for(int i = 0; i < 3; i++) {
			float x = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
			float y = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
			float z = (float) (rand.nextGaussian() * 0.5F * cloud.rollerSize);
			renderFlash(new Vec3d(x, y + cloud.coreHeight, z),matrices,vertexConsumers, (float) (25 * cloud.rollerSize * 2), alpha);
		}
	}

	@Override
	public boolean shouldRender(EntityNukeTorex entity, Frustum frustum, double x, double y, double z) {
		return true;
	}

	private void renderCloudlet(EntityNukeTorex entity, Cloudlet cloud, MatrixStack matrices,
								VertexConsumerProvider vertexConsumers, float tickDelta, int light) {

		float brightness = cloud.type == EntityNukeTorex.TorexType.CONDENSATION ? 0.9F : 0.75F * cloud.colorMod;

		Vec3d col = cloud.getInterpColor(0.5f).toVec3d();
		col = new Vec3d(MathHelper.clamp(col.x * brightness,0,1), MathHelper.clamp(col.y * brightness,0,1), MathHelper.clamp(col.z * brightness,0,1));

		float scale = cloud.getScale() * 1.25f;

		double x = MathHelper.lerp(tickDelta, cloud.prevPosX, cloud.posX) - entity.getX();
		double y = MathHelper.lerp(tickDelta, cloud.prevPosY, cloud.posY) - entity.getY();
		double z = MathHelper.lerp(tickDelta, cloud.prevPosZ, cloud.posZ) - entity.getZ();

		matrices.push();
		matrices.translate(x, y, z);

		// billboard to camera
		Quaternionf cameraRotation = this.dispatcher.getRotation();
		matrices.multiply(cameraRotation);
		matrices.scale(scale, scale, scale);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(CLOUDLET_TEXTURE));

		float halfSize = 0.5f;

		Vector3f normal = new Vector3f(0, 1, 0); // help
		normal.mul(normalMatrix.invert(new Matrix3f()));

		vertexConsumer.vertex(matrix, -halfSize, -halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, cloud.getAlpha())
				.texture(0f, 1f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		vertexConsumer.vertex(matrix, -halfSize, halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, cloud.getAlpha())
				.texture(0f, 0f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		vertexConsumer.vertex(matrix, halfSize, halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, cloud.getAlpha())
				.texture(1f, 0f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		vertexConsumer.vertex(matrix, halfSize, -halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, cloud.getAlpha())
				.texture(1f, 1f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		matrices.pop();
	}

	private void renderFlash(Vec3d pos, MatrixStack matrices,
								VertexConsumerProvider vertexConsumers, float scale, float alpha) {

		Vec3d col = new Vec3d(1, 1, 1);

		double x = pos.x;
		double y = pos.y;
		double z = pos.z;

		matrices.push();
		matrices.translate(x, y, z);

		Quaternionf cameraRotation = this.dispatcher.getRotation();
		matrices.multiply(cameraRotation);
		matrices.scale(scale, scale, scale);

		Matrix4f matrix = matrices.peek().getPositionMatrix();
		Matrix3f normalMatrix = matrices.peek().getNormalMatrix();
		VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(FLASH_TEXTURE));

		float halfSize = 0.5f;

		Vector3f normal = new Vector3f(0, 1, 0);
		normal.mul(normalMatrix.invert(new Matrix3f()));

		vertexConsumer.vertex(matrix, -halfSize, -halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, alpha)
				.texture(0f, 1f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		vertexConsumer.vertex(matrix, -halfSize, halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, alpha)
				.texture(0f, 0f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		vertexConsumer.vertex(matrix, halfSize, halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, alpha)
				.texture(1f, 0f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		vertexConsumer.vertex(matrix, halfSize, -halfSize, 0)
				.color((float) col.x, (float) col.y, (float) col.z, alpha)
				.texture(1f, 1f)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
				.normal(normal.x(), normal.y(), normal.z())
				.next();

		matrices.pop();
	}


	@Override
	public Identifier getTexture(EntityNukeTorex entity) {
		return null;
	}
}
