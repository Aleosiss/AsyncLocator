package brightspark.asynclocator.platform;

import brightspark.asynclocator.AsyncLocatorConfigForge;
import brightspark.asynclocator.platform.services.ConfigHelper;

public class ForgeConfigHelper implements ConfigHelper {
	@Override
	public int locatorThreads() {
		return AsyncLocatorConfigForge.LOCATOR_THREADS.get();
	}

	@Override
	public boolean removeOffer() {
		return AsyncLocatorConfigForge.REMOVE_OFFER.get();
	}

	@Override
	public boolean dolphinTreasureEnabled() {
		return AsyncLocatorConfigForge.DOLPHIN_TREASURE_ENABLED.get();
	}

	@Override
	public boolean eyeOfEnderEnabled() {
		return AsyncLocatorConfigForge.EYE_OF_ENDER_ENABLED.get();
	}

	@Override
	public boolean explorationMapEnabled() {
		return AsyncLocatorConfigForge.EXPLORATION_MAP_ENABLED.get();
	}

	@Override
	public boolean locateCommandEnabled() {
		return AsyncLocatorConfigForge.LOCATE_COMMAND_ENABLED.get();
	}

	@Override
	public boolean villagerTradeEnabled() {
		return AsyncLocatorConfigForge.VILLAGER_TRADE_ENABLED.get();
	}

	@Override
	public int mapNameCacheExpiryMinutes() {
		return AsyncLocatorConfigForge.MAP_NAME_CACHE_EXPIRY_MINUTES.get();
	}

	@Override
	public int biomeSearchRadius() {
		return AsyncLocatorConfigForge.BIOME_SEARCH_RADIUS.get();
	}

	@Override
	public int structureSearchRadius() {
		return AsyncLocatorConfigForge.STRUCTURE_SEARCH_RADIUS.get();
	}
}
