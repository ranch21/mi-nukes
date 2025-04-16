package org.ranch.miNukes.items;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.ranch.miNukes.MiNukes;

public class DetonatorItem extends Item {
	public DetonatorItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		ItemStack stack = context.getStack();
		Block block = world.getBlockState(pos).getBlock();

		if (!world.isClient) {
			NbtCompound nbt = stack.getOrCreateNbt();

			Identifier id = Registries.BLOCK.getId(block);
			if (id.getNamespace().equals("modern_industrialization") && id.getPath().equals("nuke")) {
				nbt.putInt("x", pos.getX());
				nbt.putInt("y", pos.getY());
				nbt.putInt("z", pos.getZ());
				MinecraftClient.getInstance().player.sendMessage(Text.of("Linked Nuke"));
			}

		}

		return ActionResult.SUCCESS;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		if (!world.isClient) {
			ItemStack stack = player.getStackInHand(hand);

			NbtCompound nbt = stack.getOrCreateNbt();
			BlockPos savedPos = readPos(nbt);
			if (savedPos != null) {

				Block block = world.getBlockState(savedPos).getBlock();

				Identifier id = Registries.BLOCK.getId(block);
				if (id.getNamespace().equals("modern_industrialization") && id.getPath().equals("nuke")) {
					MiNukes.nuke(140, savedPos.toCenterPos(), world);

					// Clear the position
					nbt.remove("x");
					nbt.remove("y");
					nbt.remove("z");
				}
			}
		}

		return TypedActionResult.success(player.getStackInHand(hand));
	}

	private BlockPos readPos(NbtCompound nbt) {
		if (nbt.contains("x")) {
			int x = nbt.getInt("x");
			int y = nbt.getInt("y");
			int z = nbt.getInt("z");

			return new BlockPos(x, y, z);
		}
		return null;
	}
}
