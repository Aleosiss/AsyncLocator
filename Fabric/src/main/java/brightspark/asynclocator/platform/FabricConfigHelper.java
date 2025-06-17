package brightspark.asynclocator.platform;

import brightspark.asynclocator.AsyncLocatorConfigFabric;
import brightspark.asynclocator.platform.services.ConfigHelper;

public class FabricConfigHelper implements ConfigHelper {
	@Override
	public int locatorThreads() {
		return AsyncLocatorConfigFabric.LOCATOR_THREADS;
	}

	@Override
	public boolean removeOffer() {
		return AsyncLocatorConfigFabric.REMOVE_OFFER;
	}

	@Override
	public boolean dolphinTreasureEnabled() {
		return AsyncLocatorConfigFabric.FeatureToggles.DOLPHIN_TREASURE_ENABLED;
	}

	@Override
	public boolean eyeOfEnderEnabled() {
		return AsyncLocatorConfigFabric.FeatureToggles.EYE_OF_ENDER_ENABLED;
	}

	@Override
	public boolean explorationMapEnabled() {
		return AsyncLocatorConfigFabric.FeatureToggles.EXPLORATION_MAP_ENABLED;
	}

	@Override
	public boolean locateCommandEnabled() {
		return AsyncLocatorConfigFabric.FeatureToggles.LOCATE_COMMAND_ENABLED;
	}

	@Override
	public boolean villagerTradeEnabled() {
		return AsyncLocatorConfigFabric.FeatureToggles.VILLAGER_TRADE_ENABLED;
	}

	@Override
	public int mapNameCacheExpiryMinutes() {
		return AsyncLocatorConfigFabric.MAP_NAME_CACHE_EXPIRY_MINUTES;
	}

	@Override
	public int biomeSearchRadius() {
		return AsyncLocatorConfigFabric.BIOME_SEARCH_RADIUS;
	}

	@Override
	public int structureSearchRadius() {
		return AsyncLocatorConfigFabric.STRUCTURE_SEARCH_RADIUS;
	}

}
