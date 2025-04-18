package org.ranch.miNukes.explosions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.ranch.miNukes.MiNukes;

import java.util.ArrayList;
import java.util.List;

import static org.ranch.miNukes.MiNukes.NUCLEAR_BLAST;

public class EntityNukeExplosion extends EntityExplosionChunkloading {
	public int strength;
	public int speed;
	public int length;
	private final ArrayList<Integer> processed = new ArrayList<>();
	public boolean fallout = true;
	ExplosionNuke explosion;
	private boolean nukeDone = false;
	private boolean damageDone = false;

	public EntityNukeExplosion(World world) {
		super(MiNukes.NUKE, world);
	}

	public EntityNukeExplosion(EntityType<?> type, World world) {
		super(type, world);
	}

	public EntityNukeExplosion(World world, int strength, int speed, int length) {
		this(MiNukes.NUKE, world);
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

		if (explosion == null) {
			explosion = new ExplosionNuke(getWorld(), (int) getX(), (int) getY(), (int) getZ(), strength, speed, length);
		}

		if (!nukeDone) {
			if (!explosion.isAusf3Complete) {
				explosion.collectTip(speed * 10);
			} else if (!explosion.perChunk.isEmpty()) {
				long start = System.currentTimeMillis();

				while (!explosion.perChunk.isEmpty() && System.currentTimeMillis() < start + 30) {
					explosion.processChunk();
				}
			} else {
				nukeDone = true;
			}
		}

		if (!damageDone) {
			processEnts(this.getWorld(), 200, 300);
			if ((this.age * 1.5 + 1) * 1.5 > 200) {
				damageDone = true;
			}
		}

		if (nukeDone && damageDone) {
			this.clearChunkLoader();
			this.discard();
		}
	}

	private void processEnts(World world, double radius, float maxDamage) {
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();

		Box aabb = new Box(x, y, z, x, y, z).expand(radius);
		List<Entity> entities = world.getOtherEntities(null, aabb);

		for (Entity entity : entities) {

			if (processed.contains(entity.getId())) {
				continue;
			}

			double dist = entity.getPos().distanceTo(new Vec3d(x, y, z));

			if (dist < radius) {

				entity.setOnFireFor(5);

				if (dist < (this.age * 1.5 + 1) * 1.5) {

					double damage = maxDamage * (radius - dist) / radius;
					entity.damage(new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(NUCLEAR_BLAST)), (float) damage);

					Vec3d pushDir = entity.getPos().subtract(this.getPos()).normalize();
					pushDir = pushDir.normalize().multiply(3);
					pushDir = new Vec3d(pushDir.x, 1, pushDir.z);
					entity.addVelocity(pushDir);
					processed.add(entity.getId());
				}
			}
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
}
