package brightspark.asynclocator;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import java.util.HashMap;
import java.util.UUID;

public class MapManager {
    private static final HashMap<UUID, LocateOperation> LOCATE_OPERATIONS = new HashMap<>();
    private static final MapManager INSTANCE = new MapManager();

    public static MapManager getInstance() {
        return INSTANCE;
    }

    /**
     * Represents a locate operation in the map manager.
     */
    public static class LocateOperation {
        public ItemStack mapItemStack;
        public ResourceKey<Level> levelKey;
        public BlockPos pos;
        public int scale;
        public Holder<MapDecorationType> destinationType;
        public boolean initialized = false;
        public String displayName = null;
        public boolean completed = false;
        public boolean invalidated = false;

        public LocateOperation(ItemStack mapItemStack, ResourceKey<Level> levelKey, int scale, Holder<MapDecorationType> destinationType) {
            this.mapItemStack = mapItemStack;
            this.levelKey = levelKey;
            this.scale = scale;
            this.destinationType = destinationType;
        }
    }

    public LocateOperation getLocateOperation(UUID uuid) {
        LocateOperation operation = LOCATE_OPERATIONS.get(uuid);
        if (operation == null) {
            throw new IllegalStateException("No locate operation found for UUID: " + uuid);
        }

        return operation;
    }

public void removeLocateOperation(UUID uuid) {
        LOCATE_OPERATIONS.remove(uuid);
    }

    public void initializeLocateOperation(UUID uuid, String displayName) {
        LocateOperation operation = LOCATE_OPERATIONS.get(uuid);
        if (operation == null) {
            throw new IllegalStateException("No locate operation found for UUID: " + uuid);
        }

        operation.initialized = true;
        operation.displayName = displayName;

        LOCATE_OPERATIONS.put(uuid, operation);
    }

    public void addLocateOperation(UUID uuid, LocateOperation operation) {
        if (LOCATE_OPERATIONS.containsKey(uuid)) {
            throw new IllegalStateException("Locate operation already exists for UUID: " + uuid);
        }
        LOCATE_OPERATIONS.put(uuid, operation);
    }

    public void completeLocateOperation(UUID asyncId, BlockPos pos) {
        LocateOperation operation = LOCATE_OPERATIONS.get(asyncId);
        if (operation == null) {
            throw new IllegalStateException("No locate operation found for UUID: " + asyncId);
        }

        operation.completed = true;
        operation.pos = pos;

        LOCATE_OPERATIONS.put(asyncId, operation);
        ALConstants.logInfo("Locate operation completed for UUID: {}, position: {}", asyncId, pos);
    }

    public void invalidateLocateOperation(UUID asyncId) {
        LocateOperation operation = LOCATE_OPERATIONS.get(asyncId);
        if (operation == null) {
            throw new IllegalStateException("No locate operation found for UUID: " + asyncId);
        }

        operation.invalidated = true;
        LOCATE_OPERATIONS.put(asyncId, operation);
        ALConstants.logInfo("Locate operation invalidated for UUID: {}", asyncId);
    }

    private static String getName(ItemStack is) {
        return is.getHoverName().getString();
    }

    private static ServerLevel getServerLevel(Level itemLevel, ResourceKey<Level> levelResourceKey) {
        return itemLevel.getServer().getLevel(levelResourceKey);
    }
}