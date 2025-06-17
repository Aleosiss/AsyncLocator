package brightspark.asynclocator.logic;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.AsyncLocator;
import brightspark.asynclocator.mixins.MerchantOfferAccess;
import brightspark.asynclocator.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.util.Optional;

public class MerchantLogic {
	private MerchantLogic() {}

	/**
	 * @deprecated Use {@link CommonLogic#createEmptyMap()} instead
	 */
	@Deprecated(since = "1.1.0", forRemoval = true)
	public static ItemStack createEmptyMap() {
		return CommonLogic.createEmptyMap();
	}

	public static void invalidateMap(AbstractVillager merchant, ItemStack mapStack) {
		mapStack.set(DataComponents.ITEM_NAME, Component.translatable("item.minecraft.map"));
		merchant.getOffers()
			.stream()
			.filter(offer -> offer.getResult() == mapStack)
			.findFirst()
			.ifPresentOrElse(
				offer -> removeOffer(merchant, offer),
				() -> ALConstants.logWarn("Failed to find merchant offer for map")
			);
	}

	public static void removeOffer(AbstractVillager merchant, MerchantOffer offer) {
		if (Services.CONFIG.removeOffer()) {
			if (merchant.getOffers().remove(offer)) ALConstants.logInfo("Removed merchant map offer");
			else ALConstants.logWarn("Failed to remove merchant map offer");
		} else {
			((MerchantOfferAccess) offer).setMaxUses(0);
			offer.setToOutOfStock();
			ALConstants.logInfo("Marked merchant map offer as out of stock");
		}
	}

	public static void tickMerchantOffers(ServerLevel level, AbstractVillager merchant) {
		merchant.getOffers()
				.stream()
				.map(MerchantOffer::getResult)
				.filter(result -> result.is(Items.FILLED_MAP))
				.forEach(offer -> {
					offer.inventoryTick(level, merchant, -1, false);
				});
	}

	public static void handleLocationFound(
		ServerLevel level,
		AbstractVillager merchant,
		ItemStack mapStack,
		String displayName,
		Holder<MapDecorationType> destinationType,
		BlockPos pos
	) {
		if (pos == null) {
			ALConstants.logInfo("No location found - invalidating merchant offer");

			invalidateMap(merchant, mapStack);
		} else {
			ALConstants.logInfo("Location found - updating treasure map in merchant offer");
			CommonLogic.updateMap(mapStack, level, pos, 2, destinationType, displayName);
		}

		if (merchant.getTradingPlayer() instanceof ServerPlayer tradingPlayer) {
			ALConstants.logInfo("Player {} currently trading - updating merchant offers", tradingPlayer);

			tradingPlayer.sendMerchantOffers(
				tradingPlayer.containerMenu.containerId,
				merchant.getOffers(),
				merchant instanceof Villager villager ? villager.getVillagerData().getLevel() : 1,
				merchant.getVillagerXp(),
				merchant.showProgressBar(),
				merchant.canRestock()
			);
		}
	}

	public static MerchantOffer updateMapAsync(
		Entity pTrader,
		int emeraldCost,
		String displayName,
		Holder<MapDecorationType> destinationType,
		int maxUses,
		int villagerXp,
		TagKey<Structure> destination
	) {


		return updateMapAsyncInternal(
			pTrader,
			emeraldCost,
			maxUses,
			villagerXp,
			(level, merchant, mapStack) -> AsyncLocator.locateStructure(level, destination, merchant.blockPosition(), 100, true)
				.thenOnServerThread(pos -> handleLocationFound(
					level,
					merchant,
					mapStack,
					displayName,
					destinationType,
					pos
				))
		);
	}

	public static MerchantOffer updateMapAsync(
		Entity pTrader,
		int emeraldCost,
		String displayName,
		Holder<MapDecorationType> destinationType,
		int maxUses,
		int villagerXp,
		HolderSet<Structure> structureSet
	) {
		return updateMapAsyncInternal(
			pTrader,
			emeraldCost,
			maxUses,
			villagerXp,
			(level, merchant, mapStack) -> AsyncLocator.locateStructure(level, structureSet, merchant.blockPosition(), 100, true)
				.thenOnServerThread(pair -> handleLocationFound(
					level,
					merchant,
					mapStack,
					displayName,
					destinationType,
					pair.getFirst()
				))
		);
	}

	private static MerchantOffer updateMapAsyncInternal(
		Entity trader, int emeraldCost, int maxUses, int villagerXp, MapUpdateTask task
	) {
		if (trader instanceof AbstractVillager merchant) {
			ItemStack mapStack = CommonLogic.createEmptyMap();
			task.apply((ServerLevel) trader.level(), merchant, mapStack);

			return new MerchantOffer(
				new ItemCost(Items.EMERALD, emeraldCost),
				Optional.of(new ItemCost(Items.COMPASS)),
				mapStack,
				maxUses,
				villagerXp,
				0.2F
			);
		} else {
			ALConstants.logInfo(
				"Merchant is not of type {} - not running async logic",
				AbstractVillager.class.getSimpleName()
			);
			return null;
		}
	}

	public interface MapUpdateTask {
		void apply(ServerLevel level, AbstractVillager merchant, ItemStack mapStack);
	}
}
