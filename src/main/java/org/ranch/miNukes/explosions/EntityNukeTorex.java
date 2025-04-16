package org.ranch.miNukes.explosions;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.ranch.miNukes.MiNukes;
import org.ranch.miNukes.Util;
import org.ranch.miNukes.compat.Vec3;

import java.awt.*;
import java.util.ArrayList;

/*
 * Toroidial Convection Simulation Explosion Effect
 * Tor                             Ex
 */
public class EntityNukeTorex extends Entity {

	public double coreHeight = 3;
	public double convectionHeight = 3;
	public double torusWidth = 3;
	public double rollerSize = 1;
	public double heat = 1;
	public double lastSpawnY = - 1;
	public ArrayList<Cloudlet> cloudlets = new ArrayList();
	//public static int cloudletLife = 200;

	public boolean didPlaySound = false;
	public boolean didShake = false;

	public EntityNukeTorex(World world) {
		super(MiNukes.TOREX, world);
		//this.ignoreFrustumCheck = true;
		//this.setSize(1F, 50F);
	}

	public EntityNukeTorex(EntityType<?> type, World world) {
		super(type, world);
	}

	public static final TrackedData<Float> SCALE = DataTracker.registerData(EntityNukeTorex.class, TrackedDataHandlerRegistry.FLOAT);
	public static final TrackedData<Integer> TYPE = DataTracker.registerData(EntityNukeTorex.class, TrackedDataHandlerRegistry.INTEGER);

	@Override
	protected void initDataTracker() {
		this.dataTracker.startTracking(SCALE, 1.0F);
		this.dataTracker.startTracking(TYPE, 0);
	}

	@Override
	public void tick() {
		super.tick();

		double s = 1.5;
		double cs = 1.5;
		int maxAge = this.getMaxAge();

		if (this.getWorld().isClient()) {

			if (this.age == 1) {
				this.setScale((float) s);
			}

			if (lastSpawnY == -1) {
				lastSpawnY = this.getY() - 3;
			}

			int spawnTarget = Math.max(this.getWorld().getChunk((int)this.getX() >> 4, (int)this.getZ() >> 4)
					.getHeightmap(Heightmap.Type.WORLD_SURFACE)
					.get((int)this.getX() & 15, (int)this.getZ() & 15) - 3, 1);

			double moveSpeed = 0.5;

			if (Math.abs(spawnTarget - lastSpawnY) < moveSpeed) {
				lastSpawnY = spawnTarget;
			} else {
				lastSpawnY += moveSpeed * Math.signum(spawnTarget - lastSpawnY);
			}

			// Spawn mush clouds
			double range = (torusWidth - rollerSize) * 0.25;
			double simSpeed = getSimulationSpeed();
			int toSpawn = (int) Math.ceil(10 * simSpeed * simSpeed);
			int lifetime = Math.min((this.age * this.age) + 200, maxAge - this.age + 200);

			for (int i = 0; i < toSpawn; i++) {
				double x = this.getX() + random.nextGaussian() * range;
				double z = this.getZ() + random.nextGaussian() * range;
				Cloudlet cloud = new Cloudlet(x, lastSpawnY, z, (float)(random.nextDouble() * 2D * Math.PI), 0, lifetime);
				cloud.setScale(1F + this.age * 0.005F * (float) cs, 5F * (float) cs);
				cloudlets.add(cloud);
			}

			// Spawn shock clouds
			if (this.age < 150) {
				int cloudCount = this.age * 5;
				int shockLife = Math.max(300 - this.age * 20, 50);

				for (int i = 0; i < cloudCount; i++) {
					Vec3d vec = new Vec3d((this.age * 1.5 + random.nextDouble()) * 1.5, 0, 0);
					float rot = (float)(Math.PI * 2 * random.nextDouble());
					vec = vec.rotateY(rot);

					BlockPos pos = new BlockPos((int) (this.getX() + vec.x), (int) this.getY(), (int) (this.getZ() + vec.z));
					int height = this.getWorld().getChunk(pos).getHeightmap(Heightmap.Type.WORLD_SURFACE).get(pos.getX() & 15, pos.getZ() & 15);

					Cloudlet cloud = new Cloudlet(vec.x + this.getX(), height, vec.z + this.getZ(), rot, 0, shockLife, TorexType.SHOCK);
					cloud.setScale(7F, 2F).setMotion(this.age > 15 ? 0.75 : 0);
					cloudlets.add(cloud);
				}

				if (!didPlaySound) {
					PlayerEntity player = MinecraftClient.getInstance().player;
					if (player != null && player.distanceTo(this) < (this.age * 1.5 + 1) * 1.5) {
						this.getWorld().playSound(
								this.getX(), this.getY(), this.getZ(),
								MiNukes.NUKE_SOUND_EVENT, SoundCategory.BLOCKS, 1.0F, 1.0F, false);
						didPlaySound = true;
					}
				}
			}

			// Spawn ring clouds
			if (this.age < 130 * s) {
				lifetime *= s;
				for (int i = 0; i < 2; i++) {
					Cloudlet cloud = new Cloudlet(this.getX(), this.getY() + coreHeight, this.getZ(),
							(float)(random.nextDouble() * 2D * Math.PI), 0, lifetime, TorexType.RING);
					cloud.setScale(1F + this.age * 0.0025F * (float)(cs * cs), 3F * (float)(cs * cs));
					cloudlets.add(cloud);
				}
			}

			// Spawn condensation clouds
			if (this.age > 130 * s && this.age < 600 * s) {
				for (int i = 0; i < 20; i++) {
					for (int j = 0; j < 4; j++) {
						float angle = (float)(Math.PI * 2 * random.nextDouble());
						Vec3d vec = new Vec3d(torusWidth + rollerSize * (5 + random.nextDouble()), 0, 0)
								.rotateZ((float)(Math.PI / 45 * j))
								.rotateY(angle);
						Cloudlet cloud = new Cloudlet(this.getX() + vec.x, this.getY() + coreHeight - 5 + j * s, this.getZ() + vec.z, angle, 0,
								(int)((20 + this.age / 10) * (1 + random.nextDouble() * 0.1)), TorexType.CONDENSATION);
						cloud.setScale(0.125F * (float)(cs), 3F * (float)(cs));
						cloudlets.add(cloud);
					}
				}
			}

			if (this.age > 200 * s && this.age < 600 * s) {
				for (int i = 0; i < 20; i++) {
					for (int j = 0; j < 4; j++) {
						float angle = (float)(Math.PI * 2 * random.nextDouble());
						Vec3d vec = new Vec3d(torusWidth + rollerSize * (3 + random.nextDouble() * 0.5), 0, 0)
								.rotateZ((float)(Math.PI / 45 * j))
								.rotateY(angle);
						Cloudlet cloud = new Cloudlet(this.getX() + vec.x, this.getY() + coreHeight + 25 + j * cs, this.getZ() + vec.z, angle, 0,
								(int)((20 + this.age / 10) * (1 + random.nextDouble() * 0.1)), TorexType.CONDENSATION);
						cloud.setScale(0.125F * (float)(cs), 3F * (float)(cs));
						cloudlets.add(cloud);
					}
				}
			}

			// Update all cloudlets
			cloudlets.removeIf(cloud -> {
				cloud.update();
				return cloud.isDead;
			});

			coreHeight += 0.15 / s;
			torusWidth += 0.05 / s;
			rollerSize = torusWidth * 0.35;
			convectionHeight = coreHeight + rollerSize;

			int maxHeat = (int)(50 * cs);
			heat = maxHeat - Math.pow((maxHeat * this.age) / maxAge, 1);
		}

		if (!this.getWorld().isClient() && this.age > maxAge) {
			this.discard(); // Equivalent to setDead() in modern versions
		}
	}

	public EntityNukeTorex setScale(float scale) {
		if(!getWorld().isClient) dataTracker.set(SCALE, scale);
		this.coreHeight = this.coreHeight / 1.5D * scale;
		this.convectionHeight = this.convectionHeight / 1.5D * scale;
		this.torusWidth = this.torusWidth / 1.5D * scale;
		this.rollerSize = this.rollerSize / 1.5D * scale;
		return this;
	}

	public EntityNukeTorex setType(int type) {
		dataTracker.set(TYPE, type);
		return this;
	}

	public double getSimulationSpeed() {

		int lifetime = getMaxAge();
		int simSlow = lifetime / 4;
		int simStop = lifetime / 2;
		int life = EntityNukeTorex.this.age;

		if(life > simStop) {
			return 0D;
		}

		if(life > simSlow) {
			return 1D - ((double)(life - simSlow) / (double)(simStop - simSlow));
		}

		return 1.0D;
	}

	public double getScale() {
		return dataTracker.get(SCALE);
	}

	public double getSaturation() {
		double d = (double) this.age / (double) this.getMaxAge();
		return 1D - (d * d * d * d);
	}

	public double getGreying() {

		int lifetime = getMaxAge();
		int greying = lifetime * 3 / 4;

		if(age > greying) {
			return 1 + ((double)(age - greying) / (double)(lifetime - greying));
		}

		return 1D;
	}

	public float getAlpha() {

		int lifetime = getMaxAge();
		int fadeOut = lifetime * 3 / 4;
		int life = EntityNukeTorex.this.age;

		if(life > fadeOut) {
			float fac = (float)(life - fadeOut) / (float)(lifetime - fadeOut);
			return 1F - fac;
		}

		return 1.0F;
	}

	public int getMaxAge() {
		double s = this.getScale();
		return (int) (45 * 20 * s);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		didPlaySound = nbt.getBoolean("didPlaySound");
		didShake = nbt.getBoolean("didShake");
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putBoolean("didPlaySound", didPlaySound);
		nbt.putBoolean("didShake", didShake);
	}

	public class Cloudlet {
		public double posX;
		public double posY;
		public double posZ;
		public double prevPosX;
		public double prevPosY;
		public double prevPosZ;
		public double motionX;
		public double motionY;
		public double motionZ;
		public int age;
		public int cloudletLife;
		public float angle;
		public boolean isDead = false;
		float rangeMod = 1.0F;
		public float colorMod = 1.0F;
		public Vec3 color;
		public Vec3 prevColor;
		public TorexType type;

		public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
			this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
		}

		public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge, TorexType type) {
			this.posX = posX;
			this.posY = posY;
			this.posZ = posZ;
			this.age = age;
			this.cloudletLife = maxAge;
			this.angle = angle;
			this.rangeMod = 0.3F + random.nextFloat() * 0.7F;
			this.colorMod = 0.8F + random.nextFloat() * 0.2F;
			this.type = type;

			this.updateColor();
		}

		private void update() {

			age++;

			if (age > cloudletLife) {
				this.isDead = true;
			}

			this.prevPosX = this.posX;
			this.prevPosY = this.posY;
			this.prevPosZ = this.posZ;

			Vec3 simPos = Vec3.createVectorHelper(EntityNukeTorex.this.getX() - this.posX, 0, EntityNukeTorex.this.getZ() - this.posZ);
			double simPosX = EntityNukeTorex.this.getX() + simPos.lengthVector();
			double simPosZ = EntityNukeTorex.this.getZ() + 0D;

			if (this.type == TorexType.STANDARD) {
				Vec3 convection = getConvectionMotion(simPosX, simPosZ);
				Vec3 lift = getLiftMotion(simPosX, simPosZ);

				double factor = MathHelper.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0, 1);
				this.motionX = convection.xCoord * factor + lift.xCoord * (1D - factor);
				this.motionY = convection.yCoord * factor + lift.yCoord * (1D - factor);
				this.motionZ = convection.zCoord * factor + lift.zCoord * (1D - factor);
			} else if (this.type == TorexType.SHOCK) {

				double factor = MathHelper.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0, 1);
				Vec3 motion = Vec3.createVectorHelper(1, 0, 0);
				motion.rotateAroundY(this.angle);
				this.motionX = motion.xCoord * factor;
				this.motionY = motion.yCoord * factor;
				this.motionZ = motion.zCoord * factor;
			} else if (this.type == TorexType.RING) {
				Vec3 motion = getRingMotion(simPosX, simPosZ);
				this.motionX = motion.xCoord;
				this.motionY = motion.yCoord;
				this.motionZ = motion.zCoord;
			} else if (this.type == TorexType.CONDENSATION) {
				Vec3 motion = getCondensationMotion();
				this.motionX = motion.xCoord;
				this.motionY = motion.yCoord;
				this.motionZ = motion.zCoord;
			}

			double mult = this.motionMult * getSimulationSpeed();

			this.posX += this.motionX * mult;
			this.posY += this.motionY * mult;
			this.posZ += this.motionZ * mult;

			this.updateColor();
		}

		private Vec3 getCondensationMotion() {
			Vec3 delta = Vec3.createVectorHelper(posX - EntityNukeTorex.this.getX(), 0, posZ - EntityNukeTorex.this.getZ());
			double speed = 0.00002 * EntityNukeTorex.this.age;
			delta.xCoord *= speed;
			delta.zCoord *= speed;
			return delta;
		}

		private Vec3 getRingMotion(double simPosX, double simPosZ) {

			if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2)
				return Vec3.createVectorHelper(0, 0, 0);

			/* the position of the torus' outer ring center */
			Vec3 torusPos = Vec3.createVectorHelper(
					(EntityNukeTorex.this.getX() + torusWidth),
					(EntityNukeTorex.this.getY() + coreHeight * 0.5),
					EntityNukeTorex.this.getZ());

			/* the difference between the cloudlet and the torus' ring center */
			Vec3 delta = Vec3.createVectorHelper(torusPos.xCoord - simPosX, torusPos.yCoord - this.posY, torusPos.zCoord - simPosZ);

			/* the distance this cloudlet wants to achieve to the torus' ring center */
			double roller = EntityNukeTorex.this.rollerSize * this.rangeMod * 0.25;
			/* the distance between this cloudlet and the torus' outer ring perimeter */
			double dist = delta.lengthVector() / roller - 1D;

			/* euler function based on how far the cloudlet is away from the perimeter */
			double func = 1D - Math.pow(Math.E, -dist); // [0;1]
			/* just an approximation, but it's good enough */
			float angle = (float) (func * Math.PI * 0.5D); // [0;90°]

			/* vector going from the ring center in the direction of the cloudlet, stopping at the perimeter */
			Vec3 rot = Vec3.createVectorHelper(-delta.xCoord / dist, -delta.yCoord / dist, -delta.zCoord / dist);
			/* rotate by the approximate angle */
			rot.rotateAroundZ(angle);

			/* the direction from the cloudlet to the target position on the perimeter */
			Vec3 motion = Vec3.createVectorHelper(
					torusPos.xCoord + rot.xCoord - simPosX,
					torusPos.yCoord + rot.yCoord - this.posY,
					torusPos.zCoord + rot.zCoord - simPosZ);

			double speed = 0.001D;
			motion.xCoord *= speed;
			motion.yCoord *= speed;
			motion.zCoord *= speed;

			motion = motion.normalize();
			motion.rotateAroundY(this.angle);

			return motion;
		}

		/* simulated on a 2D-plane along the X/Y axis */
		private Vec3 getConvectionMotion(double simPosX, double simPosZ) {

			/* the position of the torus' outer ring center */
			Vec3 torusPos = Vec3.createVectorHelper(
					(EntityNukeTorex.this.getX() + torusWidth),
					(EntityNukeTorex.this.getY() + coreHeight),
					EntityNukeTorex.this.getZ());

			/* the difference between the cloudlet and the torus' ring center */
			Vec3 delta = Vec3.createVectorHelper(torusPos.xCoord - simPosX, torusPos.yCoord - this.posY, torusPos.zCoord - simPosZ);

			/* the distance this cloudlet wants to achieve to the torus' ring center */
			double roller = EntityNukeTorex.this.rollerSize * this.rangeMod;
			/* the distance between this cloudlet and the torus' outer ring perimeter */
			double dist = delta.lengthVector() / roller - 1D;

			/* euler function based on how far the cloudlet is away from the perimeter */
			double func = 1D - Math.pow(Math.E, -dist); // [0;1]
			/* just an approximation, but it's good enough */
			float angle = (float) (func * Math.PI * 0.5D); // [0;90°]

			/* vector going from the ring center in the direction of the cloudlet, stopping at the perimeter */
			Vec3 rot = Vec3.createVectorHelper(-delta.xCoord / dist, -delta.yCoord / dist, -delta.zCoord / dist);
			/* rotate by the approximate angle */
			rot.rotateAroundZ(angle);

			/* the direction from the cloudlet to the target position on the perimeter */
			Vec3 motion = Vec3.createVectorHelper(
					torusPos.xCoord + rot.xCoord - simPosX,
					torusPos.yCoord + rot.yCoord - this.posY,
					torusPos.zCoord + rot.zCoord - simPosZ);

			motion = motion.normalize();
			motion.rotateAroundY(this.angle);

			return motion;
		}

		private Vec3 getLiftMotion(double simPosX, double simPosZ) {
			double scale = MathHelper.clamp(1D - (simPosX - (EntityNukeTorex.this.getX() + torusWidth)), 0, 1);

			Vec3 motion = Vec3.createVectorHelper(EntityNukeTorex.this.getX() - this.posX, (EntityNukeTorex.this.getY() + convectionHeight) - this.posY, EntityNukeTorex.this.getZ() - this.posZ);

			motion = motion.normalize();
			motion.xCoord *= scale;
			motion.yCoord *= scale;
			motion.zCoord *= scale;

			return motion;
		}

		private void updateColor() {
			this.prevColor = this.color;

			double exX = EntityNukeTorex.this.getX();
			double exY = EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight;
			double exZ = EntityNukeTorex.this.getZ();

			double distX = exX - posX;
			double distY = exY - posY;
			double distZ = exZ - posZ;

			double distSq = distX * distX + distY * distY + distZ * distZ;
			distSq /= EntityNukeTorex.this.heat;
			double dist = Math.sqrt(distSq);

			dist = Math.max(dist, 1);
			double col = 2D / dist;

			int type = EntityNukeTorex.this.dataTracker.get(TYPE);

			if (type == 1) {
				this.color = Vec3.createVectorHelper(
						Math.max(col * 1, 0.25),
						Math.max(col * 2, 0.25),
						Math.max(col * 0.5, 0.25)
				);
			} else if (type == 2) {
				Color color = Color.getHSBColor(this.angle / 2F / (float) Math.PI, 1F, 1F);
				if (this.type == TorexType.RING) {
					this.color = Vec3.createVectorHelper(
							Math.max(col * 1, 0.25),
							Math.max(col * 1, 0.25),
							Math.max(col * 1, 0.25)
					);
				} else {
					this.color = Vec3.createVectorHelper(color.getRed() / 255D, color.getGreen() / 255D, color.getBlue() / 255D);
				}
			} else {
				this.color = Vec3.createVectorHelper(
						Math.max(col * 2, 0.25),
						Math.max(col * 1.5, 0.25),
						Math.max(col * 0.5, 0.25)
				);
			}
		}

		public Vec3 getInterpPos(float interp) {
			float scale = (float) EntityNukeTorex.this.getScale();
			Vec3 base = Vec3.createVectorHelper(
					prevPosX + (posX - prevPosX) * interp,
					prevPosY + (posY - prevPosY) * interp,
					prevPosZ + (posZ - prevPosZ) * interp);

			if (this.type != TorexType.SHOCK) { //no rescale for the shockwave as this messes with the positions
				base.xCoord = ((base.xCoord) - EntityNukeTorex.this.getX()) * scale + EntityNukeTorex.this.getX();
				base.yCoord = ((base.yCoord) - EntityNukeTorex.this.getY()) * scale + EntityNukeTorex.this.getY();
				base.zCoord = ((base.zCoord) - EntityNukeTorex.this.getZ()) * scale + EntityNukeTorex.this.getZ();
			}

			return base;
		}

		public Vec3 getInterpColor(float interp) {

			if (this.type == TorexType.CONDENSATION) {
				return Vec3.createVectorHelper(1F, 1F, 1F);
			}

			double greying = EntityNukeTorex.this.getGreying();

			if (this.type == TorexType.RING) {
				greying += 1;
			}

			return Vec3.createVectorHelper(
					(prevColor.xCoord + (color.xCoord - prevColor.xCoord) * interp) * greying,
					(prevColor.yCoord + (color.yCoord - prevColor.yCoord) * interp) * greying,
					(prevColor.zCoord + (color.zCoord - prevColor.zCoord) * interp) * greying);
		}

		public float getAlpha() {
			float alpha = (1F - ((float) age / (float) cloudletLife)) * EntityNukeTorex.this.getAlpha();
			if (this.type == TorexType.CONDENSATION) alpha *= 0.25;
			return alpha;
		}

		private float startingScale = 1;
		private float growingScale = 5F;

		public float getScale() {
			float base = startingScale + ((float) age / (float) cloudletLife) * growingScale;
			if (this.type != TorexType.SHOCK) base *= (float) EntityNukeTorex.this.getScale();
			return base;
		}

		public Cloudlet setScale(float start, float grow) {
			this.startingScale = start;
			this.growingScale = grow;
			return this;
		}

		private double motionMult = 1F;

		public Cloudlet setMotion(double mult) {
			this.motionMult = mult;
			return this;
		}
	}

	public static enum TorexType {
		STANDARD,
		SHOCK,
		RING,
		CONDENSATION
	}

	public static void statFac(World world, double x, double y, double z, float scale) {
		EntityNukeTorex torex = new EntityNukeTorex(world).setScale(MathHelper.clamp((float) Util.squirt(scale * 0.01) * 1.5F, 0.5F, 5F));
		torex.setPosition(x, y, z);
		//torex.forceSpawn = true;
		world.spawnEntity(torex);
		//TrackerUtil.setTrackingRange(world, torex, 1000);
	}

	public static void statFacBale(World world, double x, double y, double z, float scale) {
		EntityNukeTorex torex = new EntityNukeTorex(world).setScale(MathHelper.clamp((float) Util.squirt(scale * 0.01) * 1.5F, 0.5F, 5F)).setType(1);
		torex.setPosition(x, y, z);
		//torex.forceSpawn = true;
		world.spawnEntity(torex);
		//TrackerUtil.setTrackingRange(world, torex, 1000);
	}
}
