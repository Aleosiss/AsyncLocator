package brightspark.asynclocator.logic;

import brightspark.asynclocator.mixins.MapItemAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.UUID;

public class CommonLogic {
	public static final String MAP_HOVER_NAME_KEY = "menu.working";
	public static final String KEY_LOCATING = "asynclocator.locating";
	public static final String KEY_LOCATING_MANAGED = KEY_LOCATING + ".managed";

	private CommonLogic() {}

	/**
	 * Creates an empty "Filled Map", with a hover tooltip name stating that it's locating a feature.
	 *
	 * @return The ItemStack
	 */
	public static ItemStack createEmptyMap() {
		ItemStack stack = new ItemStack(Items.FILLED_MAP);
		stack.set(DataComponents.ITEM_NAME, Component.translatable(MAP_HOVER_NAME_KEY));

		CompoundTag customData = new CompoundTag();
		customData.putByte(KEY_LOCATING, (byte) 1);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

		return stack;
	}

	public static ItemStack createEmptyManagedMap() {
		ItemStack stack = new ItemStack(Items.FILLED_MAP);
		stack.set(DataComponents.ITEM_NAME, Component.translatable(MAP_HOVER_NAME_KEY));

		CompoundTag customData = new CompoundTag();
		customData.putUUID(KEY_LOCATING_MANAGED, UUID.randomUUID());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customData));

		return stack;
	}

	/**
	 * Returns true if the stack is an empty FILLED_MAP item with the hover tooltip name stating that it's locating a
	 * feature.
	 *
	 * @param stack The stack to check.
	 * @return True if the stack is an empty FILLED_MAP awaiting to be populated with location data.
	 */
	@SuppressWarnings("DataFlowIssue")
	public static boolean isEmptyPendingMap(ItemStack stack) {
		if (!stack.is(Items.FILLED_MAP)) {
			return false;
		}

		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData != null && customData.contains(KEY_LOCATING);
	}

	/**
	 * Updates the map stack with all the given data.
	 *
	 * @param mapStack        The map ItemStack to update
	 * @param level           The ServerLevel
	 * @param pos             The feature position
	 * @param scale           The map scale
	 * @param destinationType The map feature type
	 */
	public static void updateMap(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos pos,
		int scale,
		Holder<MapDecorationType> destinationType
	) {
		updateMap(mapStack, level, pos, scale, destinationType, (Component) null);
	}

	/**
	 * Updates the map stack with all the given data.
	 *
	 * @param mapStack        The map ItemStack to update
	 * @param level           The ServerLevel
	 * @param pos             The feature position
	 * @param scale           The map scale
	 * @param destinationType The map feature type
	 * @param displayName     The hover tooltip display name of the ItemStack
	 */
	public static void updateMap(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos pos,
		int scale,
		Holder<MapDecorationType> destinationType,
		String displayName
	) {
		updateMap(mapStack, level, pos, scale, destinationType, Component.translatable(displayName));
	}

	/**
	 * Updates the map stack with all the given data.
	 *
	 * @param mapStack        The map ItemStack to update
	 * @param level           The ServerLevel
	 * @param pos             The feature position
	 * @param scale           The map scale
	 * @param destinationType The map feature type
	 * @param displayName     The hover tooltip display name of the ItemStack
	 */
	public static void updateMap(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos pos,
		int scale,
		Holder<MapDecorationType> destinationType,
		Component displayName
	) {
		MapId mapId = MapItemAccess.callCreateNewSavedData(level, pos.getX(), pos.getZ(), scale, true, true, level.dimension());
		mapStack.set(DataComponents.MAP_ID, mapId);
		MapItem.renderBiomePreviewMap(level, mapStack);
		MapItemSavedData.addTargetDecoration(mapStack, pos, "+", destinationType);
		if (displayName != null)
			mapStack.set(DataComponents.ITEM_NAME, displayName);

		CustomData currentData = mapStack.get(DataComponents.CUSTOM_DATA);
		if (currentData != null) {
			CompoundTag newTag = currentData.copyTag();
			newTag.remove(KEY_LOCATING);

			if (newTag.isEmpty()) {
				mapStack.remove(DataComponents.CUSTOM_DATA);
			} else {
				mapStack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
			}
		}
	}

	/**
	 * Broadcasts slot changes to all players that have the chest container open.
	 * Won't do anything if the BlockEntity isn't an instance of {@link ChestBlockEntity}.
	 */
	public static void broadcastChestChanges(ServerLevel level, BlockEntity be) {
		if (!(be instanceof ChestBlockEntity))
			return;

		level.players().forEach(player -> {
			AbstractContainerMenu container = player.containerMenu;
			if (container instanceof ChestMenu chestMenu && chestMenu.getContainer() == be) {
				chestMenu.broadcastChanges();
			}
		});
	}
}
