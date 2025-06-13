package brightspark.asynclocator.platform;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.MapManager;
import brightspark.asynclocator.logic.CommonLogic;
import brightspark.asynclocator.platform.services.ExplorationMapFunctionLogicHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.UUID;
import java.util.function.BiConsumer;

public class ForgeExplorationMapFunctionLogicHelper implements ExplorationMapFunctionLogicHelper {
	@Override
	public void invalidateMap(ItemStack mapStack, ServerLevel level, BlockPos pos, UUID asyncId) {
		handleUpdateMapInChest(mapStack, level, pos, asyncId, (handler, slot) -> {
			if (handler instanceof IItemHandlerModifiable modifiableHandler) {
				modifiableHandler.setStackInSlot(slot, new ItemStack(Items.MAP));
			} else {
				handler.extractItem(slot, Item.DEFAULT_MAX_STACK_SIZE, false);
				handler.insertItem(slot, new ItemStack(Items.MAP), false);
			}
		});
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
		handleUpdateMapInChest(mapStack, level, invPos, asyncId, (handler, slot) -> {});
	}

	private static void handleUpdateMapInChest(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos invPos,
		UUID asyncId,
		BiConsumer<IItemHandler, Integer> handleSlotFound
	) {
		BlockEntity be = level.getBlockEntity(invPos);
		if (be != null) {
			be.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().ifPresentOrElse(
				itemHandler -> {
					for (int i = 0; i < itemHandler.getSlots(); i++) {
						ItemStack slotStack = itemHandler.getStackInSlot(i);
						if (slotStack == mapStack) {
							handleSlotFound.accept(itemHandler, i);
							CommonLogic.broadcastChestChanges(level, be);
							// since we found the map in the chest, no longer need the data in map manager
							MapManager.getInstance().removeLocateOperation(asyncId);
							return;
						}
					}
				},
				() -> ALConstants.logWarn(
					"Couldn't find item handler capability on chest {} at {}",
					be.getClass().getSimpleName(), invPos
				)
			);
		} else {
			ALConstants.logWarn(
				"Couldn't find block entity on chest {} at {}",
				level.getBlockState(invPos), invPos
			);
		}
	}
}
