package brightspark.asynclocator;

import brightspark.asynclocator.SparkConfig.Category;
import brightspark.asynclocator.SparkConfig.Config;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AsyncLocatorConfigFabric {
	@Config(
		value = "asyncLocatorThreads",
		comment = """
			The maximum number of threads in the async locator thread pool.
			There's no upper bound to this, however this should only be increased if you're experiencing
			simultaneous location lookups causing issues AND you have the hardware capable of handling
			the extra possible threads.
			The default of 1 should be suitable for most users.
			""",
		min = 1,
		max = Integer.MAX_VALUE
	)
	public static int LOCATOR_THREADS = 1;
	@Config(
		value = "removeMerchantInvalidMapOffer",
		comment = """
			When a merchant's treasure map offer ends up not finding a feature location,
			whether the offer should be removed or marked as out of stock.
			"""
	)
	public static boolean REMOVE_OFFER = false;
	@Config(
		value = "mapNameCacheExpiryMinutes",
		comment = """
			The number of minutes before the map name cache expires.
			This is used to look up the name of a map once it's been located. 
			Increase this value if your locates are taking longer than this value.
			""",
		min = 1,
		max = Integer.MAX_VALUE
	)
	public static int MAP_NAME_CACHE_EXPIRY_MINUTES = 5;
	@Config(
		value = "biomeSearchRadius",
		comment = """
			The radius in chunks in which to search for biomes when locating them.
			This is used for locating biomes with the locate command.
			""",
		min = 1,
		max = Integer.MAX_VALUE
	)
	public static int BIOME_SEARCH_RADIUS = 400;
	@Config(
		value = "structureSearchRadius",
		comment = """
			The radius in chunks in which to search for structures when locating them. This is used
			for locating structures with the locate command, as well as for other features such as
			dolphin treasures, Eyes of Ender, exploration maps, and villager trades.
			""",
		min = 1,
		max = Integer.MAX_VALUE
	)
	public static int STRUCTURE_SEARCH_RADIUS = 400;

	@Category("Feature Toggles")
	public static class FeatureToggles {
		@Config(
			value = "dolphinTreasureEnabled",
			comment = "If true, enables asynchronous locating of structures for dolphin treasures."
		)
		public static boolean DOLPHIN_TREASURE_ENABLED = true;
		@Config(
			value = "eyeOfEnderEnabled",
			comment = "If true, enables asynchronous locating of structures when Eyes Of Ender are thrown."
		)
		public static boolean EYE_OF_ENDER_ENABLED = true;
		@Config(
			value = "explorationMspEnabled",
			comment = "If true, enables asynchronous locating of structures for exploration maps found in chests."
		)
		public static boolean EXPLORATION_MAP_ENABLED = true;
		@Config(
			value = "locateCommandEnabled",
			comment = "If true, enables asynchronous locating of structures for the locate command."
		)
		public static boolean LOCATE_COMMAND_ENABLED = true;
		@Config(
			value = "villagerTradeEnabled",
			comment = "If true, enables asynchronous locating of structures for villager trades."
		)
		public static boolean VILLAGER_TRADE_ENABLED = true;
	}

	private AsyncLocatorConfigFabric() {}

	public static void init() {
		Path configFile = FabricLoader.getInstance().getConfigDir().resolve(ALConstants.MOD_ID + ".properties");

		if (Files.exists(configFile)) {
			ALConstants.logInfo("Config file found");
			try {
				SparkConfig.read(configFile, AsyncLocatorConfigFabric.class);
			} catch (IOException | IllegalAccessException e) {
				ALConstants.logError(e, "Failed to read config file {}", configFile);
			}
		} else {
			ALConstants.logInfo("No config file found - creating it");
			try {
				SparkConfig.write(configFile, AsyncLocatorConfigFabric.class);
			} catch (IOException | IllegalAccessException e) {
				ALConstants.logError(e, "Failed to write config file {}", configFile);
			}
		}
	}
}
