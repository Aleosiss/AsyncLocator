package brightspark.asynclocator;

import brightspark.asynclocator.platform.Services;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

public class AsyncLocator {


	private static ExecutorService LOCATING_EXECUTOR_SERVICE = null;

	private AsyncLocator() {}

	public static void setupExecutorService() {
		shutdownExecutorService();

		int threads = Services.CONFIG.locatorThreads();
		ALConstants.logInfo("Starting locating executor service with thread pool size of {}", threads);
		LOCATING_EXECUTOR_SERVICE = Executors.newFixedThreadPool(
			threads,
			new ThreadFactory() {
				private static final AtomicInteger poolNum = new AtomicInteger(1);
				private final AtomicInteger threadNum = new AtomicInteger(1);
				private final String namePrefix = ALConstants.MOD_ID + "-" + poolNum.getAndIncrement() + "-thread-";

				@Override
				public Thread newThread(@NotNull Runnable r) {
					return new Thread(r, namePrefix + threadNum.getAndIncrement());
				}
			}
		);
	}

	public static void shutdownExecutorService() {
		if (LOCATING_EXECUTOR_SERVICE != null) {
			ALConstants.logInfo("Shutting down locating executor service");
			LOCATING_EXECUTOR_SERVICE.shutdown();
		}
	}

	/**
	 * Queues a task to locate a feature using {@link ServerLevel#findNearestMapStructure(TagKey, BlockPos, int, boolean)}
	 * and returns a {@link LocateTask} with the futures for it.
	 */
	public static LocateTask<BlockPos> locateStructure(
		ServerLevel level,
		TagKey<Structure> structureTag,
		BlockPos pos,
		int searchRadius,
		boolean skipKnownStructures
	) {
		ALConstants.logDebug(
			"Creating locate task for {} in {} around {} within {} chunks",
			structureTag, level, pos, searchRadius
		);
		CompletableFuture<BlockPos> completableFuture = new CompletableFuture<>();
		Future<?> future = LOCATING_EXECUTOR_SERVICE.submit(
			() -> {
                try {
                    doLocateStructureLevel(completableFuture, level, structureTag, pos, searchRadius, skipKnownStructures);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
		);
		return new LocateTask<>(level.getServer(), completableFuture, future);
	}

	/**
	 * Queues a task to locate a feature using
	 * {@link ChunkGenerator#findNearestMapStructure(ServerLevel, HolderSet, BlockPos, int, boolean)} and returns a
	 * {@link LocateTask} with the futures for it.
	 */
	public static LocateTask<Pair<BlockPos, Holder<Structure>>> locateStructure(
		ServerLevel level,
		HolderSet<Structure> structureSet,
		BlockPos pos,
		int searchRadius,
		boolean skipKnownStructures
	) {
		ALConstants.logDebug(
			"Creating locate task for {} in {} around {} within {} chunks",
			structureSet, level, pos, searchRadius
		);
		CompletableFuture<Pair<BlockPos, Holder<Structure>>> completableFuture = new CompletableFuture<>();
		Future<?> future = LOCATING_EXECUTOR_SERVICE.submit(
			() -> doLocateStructureChunkGenerator(completableFuture, level, structureSet, pos, searchRadius, skipKnownStructures)
		);
		return new LocateTask<>(level.getServer(), completableFuture, future);
	}

	private static void doLocateStructureLevel(
		CompletableFuture<BlockPos> completableFuture,
		ServerLevel level,
		TagKey<Structure> structureTag,
		BlockPos pos,
		int searchRadius,
		boolean skipExistingChunks
	) throws InterruptedException {
		ALConstants.logInfo(
			"Trying to locate {} in {} around {} within {} chunks",
			structureTag, level, pos, searchRadius
		);
		long start = System.nanoTime();
		BlockPos foundPos = level.findNearestMapStructure(structureTag, pos, searchRadius, skipExistingChunks);
		String time = NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		if (foundPos == null)
			ALConstants.logInfo("No {} found (took {}ms)", structureTag, time);
		else
			ALConstants.logInfo("Found {} at {} (took {}ms)", structureTag, foundPos, time);
		completableFuture.complete(foundPos);
	}

	private static void doLocateStructureChunkGenerator(
		CompletableFuture<Pair<BlockPos, Holder<Structure>>> completableFuture,
		ServerLevel level,
		HolderSet<Structure> structureSet,
		BlockPos pos,
		int searchRadius,
		boolean skipExistingChunks
	) {
		ALConstants.logInfo(
			"Trying to locate {} in {} around {} within {} chunks",
			structureSet, level, pos, searchRadius
		);
		long start = System.nanoTime();
		Pair<BlockPos, Holder<Structure>> foundPair = level.getChunkSource().getGenerator()
			.findNearestMapStructure(level, structureSet, pos, searchRadius, skipExistingChunks);
		String time = NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		if (foundPair == null)
			ALConstants.logInfo("No {} found (took {}ms)", structureSet, time);
		else
			ALConstants.logInfo("Found {} at {} (took {}ms)",
				foundPair.getSecond().value().getClass().getSimpleName(), foundPair.getFirst(), time
			);
		completableFuture.complete(foundPair);
	}

	/**
	 * Queues a task to locate a feature using {@link ServerLevel#findClosestBiome3d(Predicate, BlockPos, int, int, int)}
	 * and returns a {@link LocateTask} with the futures for it.
	 */
	public static LocateTask<Pair<BlockPos, Holder<Biome>>> locateBiome(
			ServerLevel level,
			ResourceOrTagArgument.Result biomeSet,
			BlockPos pos,
			int searchRadius,
			boolean skipKnownBiomes
	) {
		ALConstants.logDebug(
				"Creating locate task for {} in {} around {} within {} chunks",
				biomeSet, level, pos, searchRadius
		);
		CompletableFuture<Pair<BlockPos, Holder<Biome>>> completableFuture = new CompletableFuture<>();
		Future<?> future = LOCATING_EXECUTOR_SERVICE.submit(
				() -> doLocateBiome(completableFuture, level, biomeSet, pos, searchRadius, skipKnownBiomes)
		);
		return new LocateTask<>(level.getServer(), completableFuture, future);
	}

	private static void doLocateBiome(
			CompletableFuture<Pair<BlockPos, Holder<Biome>>> completableFuture,
			ServerLevel level,
			ResourceOrTagArgument.Result<Biome> biomeSet,
			BlockPos pos,
			int searchRadius,
			boolean skipExistingChunks
	) {
		ALConstants.logInfo(
				"Trying to locate {} in {} around {} within {} chunks",
				biomeSet, level, pos, searchRadius
		);
		long start = System.nanoTime();

		var radius = searchRadius * 16;
		var horizontalStep = 32;
		var verticalStep = 64;

		Pair<BlockPos, Holder<Biome>> locate = level.findClosestBiome3d(
                biomeSet,
				pos,
				radius,
				horizontalStep,
				verticalStep
		);

		var foundPos = locate == null ? null : locate.getFirst();
		String time = NumberFormat.getNumberInstance().format(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
		if (foundPos == null)
			ALConstants.logInfo("No {} found (took {}ms)", biomeSet, time);
		else
			ALConstants.logInfo("Found {} at {} (took {}ms)", biomeSet, foundPos, time);
		completableFuture.complete(locate);
	}

	/**
	 * Holder of the futures for an async locate task as well as providing some helper functions.
	 * The completableFuture will be completed once the call to
	 * {@link ServerLevel#findNearestMapStructure(TagKey, BlockPos, int, boolean)} has completed, and will hold the
	 * result of it.
	 * The taskFuture is the future for the {@link Runnable} itself in the executor service.
	 */
	public record LocateTask<T>(MinecraftServer server, CompletableFuture<T> completableFuture, Future<?> taskFuture) {
		/**
		 * Helper function that calls {@link CompletableFuture#thenAccept(Consumer)} with the given action.
		 * Bear in mind that the action will be executed from the task's thread. If you intend to change any game data,
		 * it's strongly advised you use {@link #thenOnServerThread(Consumer)} instead so that it's queued and executed
		 * on the main server thread instead.
		 */
		public LocateTask<T> then(Consumer<T> action) {
			completableFuture.thenAccept(action);
			return this;
		}

		/**
		 * Helper function that calls {@link CompletableFuture#thenAccept(Consumer)} with the given action on the server
		 * thread.
		 */
		public LocateTask<T> thenOnServerThread(Consumer<T> action) {
			completableFuture.thenAccept(pos -> server.submit(() -> action.accept(pos)));
			return this;
		}

		/**
		 * Helper function that cancels both completableFuture and taskFuture.
		 */
		public void cancel() {
			taskFuture.cancel(true);
			completableFuture.cancel(false);
		}
	}
}
