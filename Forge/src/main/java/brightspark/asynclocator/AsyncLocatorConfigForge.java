package brightspark.asynclocator;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class AsyncLocatorConfigForge {
	public static ForgeConfigSpec SPEC;

	public static ConfigValue<Integer> LOCATOR_THREADS;
	public static ConfigValue<Boolean> REMOVE_OFFER;
	public static ConfigValue<Integer> MAP_NAME_CACHE_EXPIRY_MINUTES;
	public static ConfigValue<Integer> BIOME_SEARCH_RADIUS;
	public static ConfigValue<Integer> STRUCTURE_SEARCH_RADIUS;

	// Feature toggles
	public static ConfigValue<Boolean> DOLPHIN_TREASURE_ENABLED;
	public static ConfigValue<Boolean> EYE_OF_ENDER_ENABLED;
	public static ConfigValue<Boolean> EXPLORATION_MAP_ENABLED;
	public static ConfigValue<Boolean> LOCATE_COMMAND_ENABLED;
	public static ConfigValue<Boolean> VILLAGER_TRADE_ENABLED;

	static {
		SPEC = new Builder()
			.configure(builder -> {
				LOCATOR_THREADS = builder
					.worldRestart()
					.comment(
						"The maximum number of threads in the async locator thread pool.",
						"There's no upper bound to this, however this should only be increased if you're experiencing",
						"simultaneous location lookups causing issues AND you have the hardware capable of handling",
						"the extra possible threads.",
						"The default of 1 should be suitable for most users."
					)
					.defineInRange("asyncLocatorThreads", 1, 1, Integer.MAX_VALUE);
				REMOVE_OFFER = builder
					.comment(
						"When a merchant's treasure map offer ends up not finding a feature location,",
						"whether the offer should be removed or marked as out of stock."
					)
					.define("removeMerchantInvalidMapOffer", false);

				builder.push("Feature Toggles");
				DOLPHIN_TREASURE_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures for dolphin treasures.")
					.define("dolphinTreasureEnabled", true);
				EYE_OF_ENDER_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures when Eyes Of Ender are thrown.")
					.define("eyeOfEnderEnabled", true);
				EXPLORATION_MAP_ENABLED = builder
					.comment(
						"If true, enables asynchronous locating of structures for exploration maps found in chests.")
					.define("explorationMspEnabled", true);
				LOCATE_COMMAND_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures for the locate command.")
					.define("locateCommandEnabled", true);
				VILLAGER_TRADE_ENABLED = builder
					.comment("If true, enables asynchronous locating of structures for villager trades.")
					.define("villagerTradeEnabled", true);
				MAP_NAME_CACHE_EXPIRY_MINUTES = builder
						.comment(
							"The number of minutes before the map name cache expires.",
							"This is used to look up the name of a map once it's been located.",
							"Increase this value if your locates are taking longer than this value."
						)
						.define("mapNameCacheExpiryMinutes", 5);
				BIOME_SEARCH_RADIUS = builder
					.comment(
						"The radius in chunks in which to search for biomes when locating them.",
						"This is used for locating biomes with the locate command."
					)
					.defineInRange("biomeSearchRadius", 400, 1, Integer.MAX_VALUE);
				STRUCTURE_SEARCH_RADIUS = builder
					.comment(
						"The radius in chunks in which to search for structures when locating them. This is used",
						"for locating structures with the locate command, as well as for other features such as",
						"dolphin treasures, Eyes of Ender, exploration maps, and villager trades."
					)
					.defineInRange("structureSearchRadius", 400, 1, Integer.MAX_VALUE);
				builder.pop();
				return null;
			})
			.getValue();
	}

	private AsyncLocatorConfigForge() {}
}
