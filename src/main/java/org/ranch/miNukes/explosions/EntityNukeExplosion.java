package org.ranch.miNukes.explosions;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.ranch.miNukes.MiNukes;
import org.ranch.miNukes.explosions.EntityExplosionChunkloading;

public class EntityNukeExplosion extends EntityExplosionChunkloading {

	public int strength;
	public int speed;
	public int length;

	public boolean fallout = true;
	private int falloutAdd = 0;

	ExplosionNuke explosion;

	public EntityNukeExplosion(World world) {
		super(MiNukes.NUKE, world);
	}

	public EntityNukeExplosion(EntityType<?> type, World world) {
		super(type, world);
	}

	public EntityNukeExplosion(World world, int strength, int speed, int length) {
		this(MiNukes.NUKE, world); // Assuming registered
		this.strength = strength;
		this.speed = speed;
		this.length = length;
	}

	@Override
	public void tick() {
		super.tick();

		if (strength == 0) {
			this.clearChunkLoader();
			this.discard();
			return;
		}

		if (!getWorld().isClient) {
			int cx = (int) Math.floor(getX() / 16D);
			int cz = (int) Math.floor(getZ() / 16D);
			loadChunk(cx, cz);
		}

		if (!getWorld().isClient) {
			for (PlayerEntity player : getWorld().getPlayers()) {
				// Grant advancement here if using custom Advancements system
				// player.incrementStat(Stats.CUSTOM.get(MainRegistry.MANHATTAN_ACHIEVEMENT));
			}
		}

		if (!getWorld().isClient && fallout && explosion != null && this.age < 10 && strength >= 75) {
			//radiate(2_500_000F / (this.age * 5 + 1), this.length * 2);
		}

		dealDamage(this.getWorld(), this.getX(), this.getY(), this.getZ(), this.length * 2, 50, this);

		if (explosion == null) {
			explosion = new ExplosionNuke(getWorld(), (int) getX(), (int) getY(), (int) getZ(), strength, speed, length);
		}

		if (!explosion.isAusf3Complete) {
			explosion.collectTip(speed * 10);
		} else if (!explosion.perChunk.isEmpty()) {
			long start = System.currentTimeMillis();

			while (!explosion.perChunk.isEmpty() && System.currentTimeMillis() < start + 30) {
				explosion.processChunk();
			}
		} else if (fallout) {
			// nevah

			this.clearChunkLoader();
			this.discard();
		} else {
			this.clearChunkLoader();
			this.discard();
		}
	}

	public static void dealDamage(World world, double x, double y, double z, double radius, float maxDamage, Entity ent) {
		Box aabb = new Box(x, y, z, x, y, z).expand(radius);
		List<Entity> entities = world.getOtherEntities(null, aabb);

		for (Entity entity : entities) {
			double dist = entity.getPos().distanceTo(new Vec3d(x, y, z));

			if (dist <= radius) {
				if (!isObstructed(world, x, y, z, entity.getX(), entity.getY() + entity.getStandingEyeHeight(), entity.getZ(), ent)) {

					double damage = maxDamage * (radius - dist) / radius;
					DamageSource explosionDamage = world.getDamageSources().create(DamageTypes.EXPLOSION);
					entity.damage(explosionDamage, (float) damage);
					entity.setOnFireFor(5);

					Vec3d knockVec = entity.getPos().add(0, entity.getStandingEyeHeight(), 0).subtract(x, y, z).normalize().multiply(0.2);
					entity.setVelocity(entity.getVelocity().add(knockVec));
				}
			}
		}
	}

	public static boolean isObstructed(World world, double x, double y, double z, double a, double b, double c, Entity ent) {
		Vec3d start = new Vec3d(x, y, z);
		Vec3d end = new Vec3d(a, b, c);

		HitResult result = world.raycast(new RaycastContext(
				start,
				end,
				RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE,
				ent
		));

		// If ray hits a block before reaching the target point, it's obstructed
		return result.getType() != HitResult.Type.MISS;
	}

	private void radiate(float rads, double range) {
		Box box = new Box(getX(), getY(), getZ(), getX(), getY(), getZ()).expand(range);
		List<LivingEntity> entities = getWorld().getEntitiesByClass(LivingEntity.class, box, e -> true);

		for (LivingEntity entity : entities) {
			Vec3d direction = new Vec3d(
					entity.getX() - getX(),
					entity.getY() + entity.getStandingEyeHeight() - getY(),
					entity.getZ() - getZ()
			);

			double length = direction.length();
			Vec3d normalized = direction.normalize();

			float resistance = 0;

			for (int i = 1; i < length; i++) {
				int x = (int) Math.floor(getX() + normalized.x * i);
				int y = (int) Math.floor(getY() + normalized.y * i);
				int z = (int) Math.floor(getZ() + normalized.z * i);

				resistance += 1.0F; // Fake resistance for now
			}

			if (resistance < 1)
				resistance = 1;

			float finalRads = rads / resistance / (float)(length * length);

			//ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.RAD_BYPASS, finalRads);
		}
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		this.age = nbt.getInt("ticksExisted");
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putInt("ticksExisted", this.age);
	}

	public static EntityNukeExplosion statFac(World world, int r, double x, double y, double z) {
		//if (GeneralConfig.enableExtendedLogging && !world.isClient) {
		//	MainRegistry.LOGGER.info("[NUKE] Initialized explosion at {}, {}, {} with strength {}!", x, y, z, r);
		//}

		if (r == 0)
			r = 25;

		r *= 2;

		EntityNukeExplosion mk5 = new EntityNukeExplosion(world);
		mk5.strength = r;
		mk5.speed = (int) Math.ceil(100000.0 / r);
		mk5.setPos(x, y, z);
		mk5.length = r / 2;

		return mk5;
	}

	public static EntityNukeExplosion statFacNoRad(World world, int r, double x, double y, double z) {
		EntityNukeExplosion mk5 = statFac(world, r, x, y, z);
		mk5.fallout = false;
		return mk5;
	}

	public EntityNukeExplosion moreFallout(int fallout) {
		this.falloutAdd = fallout;
		return this;
	}
}
