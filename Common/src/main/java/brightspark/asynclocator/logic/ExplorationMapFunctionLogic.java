package brightspark.asynclocator.logic;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.AsyncLocator;
import brightspark.asynclocator.MapManager;
import brightspark.asynclocator.platform.Services;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.time.Duration;
import java.util.UUID;

import static brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING;
import static brightspark.asynclocator.logic.CommonLogic.KEY_LOCATING_MANAGED;

public class ExplorationMapFunctionLogic {
	// I'd like to think that structure locating shouldn't take *this* long
	private static final Cache<ItemStack, Component> MAP_NAME_CACHE =
			CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(Services.CONFIG.mapNameCacheExpiryMinutes())).build();

	public static void cacheName(ItemStack stack, Component name) {
		MAP_NAME_CACHE.put(stack, name);
	}

	public static Component getCachedName(ItemStack stack) {
		Component name = MAP_NAME_CACHE.getIfPresent(stack);
		MAP_NAME_CACHE.invalidate(stack);
		return name;
	}

	private ExplorationMapFunctionLogic() {}

	public static void handleLocationFound(
			UUID asyncId,
			BlockPos pos
	) {
		MapManager.getInstance().completeLocateOperation(asyncId, pos);
	}

	public static void handleLocationFound(
			ItemStack mapStack,
			ServerLevel level,
			BlockPos pos,
			int scale,
			Holder<MapDecorationType> destinationType,
			BlockPos invPos,
			UUID asyncId
	) {
		if (pos == null) {
			ALConstants.logInfo("No location found - invalidating map stack");
			Services.EXPLORATION_MAP_FUNCTION_LOGIC.invalidateMap(mapStack, level, invPos, asyncId);
		} else {
			ALConstants.logInfo("Location found - updating treasure map in chest");
			// complete the operation in MapManager to close the loop
			MapManager.getInstance().completeLocateOperation(asyncId, pos);
			Services.EXPLORATION_MAP_FUNCTION_LOGIC.updateMap(
					mapStack,
					level,
					pos,
					scale,
					destinationType,
					invPos,
					getCachedName(mapStack),
					asyncId
			);
		}
	}

	public static ItemStack updateMapAsync(
		ServerLevel level,
		BlockPos blockPos,
		int scale,
		int searchRadius,
		boolean skipKnownStructures,
		Holder<MapDecorationType> destinationType,
		TagKey<Structure> destination
	) {
		ItemStack mapStack = CommonLogic.createEmptyManagedMap();
		var asyncId = mapStack.get(DataComponents.CUSTOM_DATA)
				.copyTag()
				.getUUID(KEY_LOCATING_MANAGED);

		// If the map is from a chest, we need to handle it differently
		boolean isFromChest = (level.getBlockEntity(blockPos) instanceof ChestBlockEntity);

		// Even if the map was generated as part of a chest's loot, we still need to add the locate operation
		// in case the chest is broken and the map is picked up later.
		MapManager.getInstance().addLocateOperation(asyncId, new MapManager.LocateOperation(
				level.dimension(),
					scale,
					destinationType
		));

		AsyncLocator.locateStructure(level, destination, blockPos, searchRadius, skipKnownStructures)
			.thenOnServerThread(pos -> {
				if(isFromChest) {
					handleLocationFound(mapStack, level, pos, scale, destinationType, blockPos, asyncId);
				} else {
					handleLocationFound(asyncId, pos);
				}

			});
		return mapStack;
	}

}
