package org.ranch.miNukes;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class Util {
	public static double squirt(double x) {
		return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
	}

	public static boolean isObstructed(World world, Vec3d start, Vec3d end, Entity ent) {

		HitResult result = world.raycast(new RaycastContext(
				start,
				end,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				ent
		));

		return result.getType() == HitResult.Type.BLOCK;
	}
}
