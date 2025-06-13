package brightspark.asynclocator.logic;

import brightspark.asynclocator.ALConstants;
import brightspark.asynclocator.AsyncLocator;
import brightspark.asynclocator.mixins.LocateCommandAccess;
import com.google.common.base.Stopwatch;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.LocateCommand;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;

public class LocateCommandLogic {
	private LocateCommandLogic() {}

	public static void locateStructureAsync(
		CommandSourceStack sourceStack,
		ResourceOrTagKeyArgument.Result<Structure> structureResult,
		HolderSet<Structure> holderset
	) {
		BlockPos originPos = BlockPos.containing(sourceStack.getPosition());
		Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
		AsyncLocator.locateStructure(sourceStack.getLevel(), holderset, originPos, 100, false)
			.thenOnServerThread(pair -> {
				stopwatch.stop();
				if (pair != null) {
					ALConstants.logInfo("Location found - sending success back to command source");
					LocateCommand.showLocateResult(
						sourceStack,
						structureResult,
						originPos,
						pair,
						"commands.locate.structure.success",
						false,
						stopwatch.elapsed()
					);
				} else {
					ALConstants.logInfo("No location found - sending failure back to command source");
					sourceStack.sendFailure(Component.literal(
						LocateCommandAccess.getErrorFailed().create(structureResult.asPrintable()).getMessage()
					));
				}
			});
	}

	public static void locateBiomeAsync(
			CommandSourceStack sourceStack,
			ResourceOrTagArgument.Result<Biome> biomeResult
	) {
		BlockPos originPos = BlockPos.containing(sourceStack.getPosition());
		Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);

		AsyncLocator.locateBiome(sourceStack.getLevel(), biomeResult, originPos, 400, false)
				.thenOnServerThread(pair -> {
					stopwatch.stop();
					if (pair != null) {
						ALConstants.logInfo("Location found - sending success back to command source");
						LocateCommand.showLocateResult(
								sourceStack,
								biomeResult,
								originPos,
								pair,
								"commands.locate.biome.success",
								false,
								stopwatch.elapsed()
						);
					} else {
						ALConstants.logInfo("No location found - sending failure back to command source");
						sourceStack.sendFailure(Component.literal(
								LocateCommandAccess.getErrorFailed().create(biomeResult.asPrintable()).getMessage()
						));
					}
				});
	}
}
