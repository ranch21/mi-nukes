package org.ranch.miNukes.explosions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;

public abstract class EntityExplosionChunkloading extends Entity {

	private static final ChunkTicketType<EntityExplosionChunkloading> TICKET_TYPE =
			ChunkTicketType.create("entity_explosion_loader", (a, b) -> 1);

	private ChunkPos loadedChunk;

	public EntityExplosionChunkloading(EntityType<?> type, World world) {
		super(type, world);
	}

	@Override
	protected void initDataTracker() {

	}

	public void init() {
		if (!this.getWorld().isClient && loadedChunk == null) {
			ChunkPos chunkPos = this.getChunkPos();
			this.loadedChunk = chunkPos;
			forceChunk(chunkPos);
		}
	}

	public void loadChunk(int x, int z) {
		if (!this.getWorld().isClient) {
			ChunkPos chunkPos = new ChunkPos(x, z);
			if (this.loadedChunk == null || !this.loadedChunk.equals(chunkPos)) {
				this.loadedChunk = chunkPos;
				forceChunk(chunkPos);
			}
		}
	}

	public void clearChunkLoader() {
		if (!this.getWorld().isClient && loadedChunk != null) {
			ServerWorld world = (ServerWorld) this.getWorld();
			world.getChunkManager().removeTicket(TICKET_TYPE, loadedChunk, 1, this);
			loadedChunk = null;
		}
	}

	private void forceChunk(ChunkPos chunkPos) {
		ServerWorld world = (ServerWorld) this.getWorld();
		world.getChunkManager().addTicket(TICKET_TYPE, chunkPos, 1, this);
		world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);
	}

	@Override
	public void remove(RemovalReason reason) {
		clearChunkLoader();
		super.remove(reason);
	}
}
