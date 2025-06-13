package brightspark.asynclocator.platform;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.MapManager;
import brightspark.asynclocator.logic.CommonLogic;
import brightspark.asynclocator.platform.services.ExplorationMapFunctionLogicHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.util.UUID;
import java.util.function.BiConsumer;

public class FabricExplorationMapFunctionLogicHelper implements ExplorationMapFunctionLogicHelper {
	@Override
	public void invalidateMap(ItemStack mapStack, ServerLevel level, BlockPos pos, UUID uuid) {
		handleUpdateMapInChest(mapStack, level, pos, uuid, (chest, slot) -> chest.setItem(slot, new ItemStack(Items.MAP)));
	}

	@Override
	public void updateMap(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos pos,
		int scale,
		Holder<MapDecorationType> destinationType,
		BlockPos invPos,
		Component displayName,
		UUID asyncId
	) {

		CommonLogic.updateMap(mapStack, level, pos, scale, destinationType, displayName);
		// Shouldn't need to set the stack in its slot again, as we're modifying the same instance
		handleUpdateMapInChest(mapStack, level, invPos, asyncId, (chest, slot) -> {});
	}

	private static void handleUpdateMapInChest(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos invPos,
		UUID asyncId,
		BiConsumer<ChestBlockEntity, Integer> handleSlotFound
	) {
		BlockEntity be = level.getBlockEntity(invPos);
		if (be instanceof ChestBlockEntity chest) {
			for (int i = 0; i < chest.getContainerSize(); i++) {
				ItemStack slotStack = chest.getItem(i);
				if (slotStack == mapStack) {
					handleSlotFound.accept(chest, i);
					CommonLogic.broadcastChestChanges(level, be);
					// since we found the map in the chest, no longer need the data in map manager
					MapManager.getInstance().removeLocateOperation(asyncId);
					return;
				}
			}
		} else {
			ALConstants.logWarn(
				"Couldn't find chest block entity on block {} at {}",
				level.getBlockState(invPos), invPos
			);
		}
	}
}
