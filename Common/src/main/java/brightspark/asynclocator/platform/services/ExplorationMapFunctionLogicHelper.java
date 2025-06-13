package brightspark.asynclocator.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.util.UUID;

public interface ExplorationMapFunctionLogicHelper {
	void invalidateMap(ItemStack mapStack, ServerLevel level, BlockPos pos, UUID uuid);

	void updateMap(
		ItemStack mapStack,
		ServerLevel level,
		BlockPos pos,
		int scale,
		Holder<MapDecorationType> destinationType,
		BlockPos invPos,
		Component displayName,
		UUID asyncId
	);
}
