package net.infdev.world;

import net.infdev.util.Chunk;
import net.infdev.api.Registries;
import net.infdev.api.world.item.Item;
import net.infdev.api.world.item.ItemStack;
import net.infdev.item.Items;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles saving and loading world data (chunks, metadata) to disk.
 */
public class WorldSaveManager {
    private final Path worldsRoot;
    private static final int CHUNK_HEADER_BYTES = Integer.BYTES * 2;
    private static final int PLAYER_DATA_VERSION = 1;
    private static final int DROPS_DATA_VERSION = 1;

    public WorldSaveManager(Path worldsRoot) {
        this.worldsRoot = worldsRoot;
    }

    public void ensureBaseDir() {
        try {
            Files.createDirectories(worldsRoot);
        } catch (IOException e) {
            System.err.println("Failed to create worlds directory: " + e.getMessage());
        }
    }

    public List<String> listWorlds() {
        ensureBaseDir();
        try (java.util.stream.Stream<Path> stream = Files.list(worldsRoot)) {
            return stream
                .filter(Files::isDirectory)
                .map(path -> path.getFileName().toString())
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void ensureWorldExists(String worldName) {
        Path chunkDir = getChunkDirectory(worldName);
        try {
            Files.createDirectories(chunkDir);
        } catch (IOException e) {
            System.err.println("Failed to create world folder for " + worldName + ": " + e.getMessage());
        }
    }

    public boolean loadChunk(String worldName, Chunk chunk) {
        Path chunkPath = getChunkPath(worldName, chunk.getChunkX(), chunk.getChunkZ());
        if (!Files.exists(chunkPath)) {
            return false;
        }

        try {
            byte[] raw = Files.readAllBytes(chunkPath);
            if (raw.length < CHUNK_HEADER_BYTES) {
                return false;
            }
            ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.BIG_ENDIAN);
            int storedX = buffer.getInt();
            int storedZ = buffer.getInt();
            if (storedX != chunk.getChunkX() || storedZ != chunk.getChunkZ()) {
                return false;
            }

            int expectedBlockBytes = Chunk.CHUNK_SIZE * Chunk.CHUNK_HEIGHT * Chunk.CHUNK_SIZE;
            if (buffer.remaining() < expectedBlockBytes) {
                return false;
            }

            byte[] blockData = new byte[expectedBlockBytes];
            buffer.get(blockData);
            chunk.loadBlocks(blockData);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to load chunk " + chunk.getChunkX() + "," + chunk.getChunkZ() + ": " + e.getMessage());
            return false;
        }
    }

    public void saveChunk(String worldName, Chunk chunk) {
        ensureWorldExists(worldName);
        Path chunkPath = getChunkPath(worldName, chunk.getChunkX(), chunk.getChunkZ());
        byte[] blockData = chunk.serializeBlocks();

        ByteBuffer buffer = ByteBuffer
            .allocate(CHUNK_HEADER_BYTES + blockData.length)
            .order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(chunk.getChunkX());
        buffer.putInt(chunk.getChunkZ());
        buffer.put(blockData);

        try {
            Files.write(
                chunkPath,
                buffer.array(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
        } catch (IOException e) {
            System.err.println("Failed to save chunk " + chunk.getChunkX() + "," + chunk.getChunkZ() + ": " + e.getMessage());
        }
    }

    public String sanitizeWorldName(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        while (cleaned.contains("__")) {
            cleaned = cleaned.replace("__", "_");
        }
        if (cleaned.length() > 32) {
            cleaned = cleaned.substring(0, 32);
        }
        return cleaned;
    }

    public static class PlayerState {
        public float x;
        public float y;
        public float z;
        public ItemStack[] hotbar;
        public ItemStack[] inventory;
    }

    public static class DroppedItemData {
        public Item item;
        public int count;
        public Vector3f position;
    }

    public PlayerState loadPlayerState(String worldName) {
        Path playerPath = getPlayerPath(worldName);
        if (!Files.exists(playerPath)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(playerPath))) {
            int version = in.readInt();
            if (version != PLAYER_DATA_VERSION) {
                return null;
            }
            PlayerState state = new PlayerState();
            state.x = in.readFloat();
            state.y = in.readFloat();
            state.z = in.readFloat();

            int hotbarSize = in.readInt();
            state.hotbar = readItemStacks(in, hotbarSize);

            int inventorySize = in.readInt();
            state.inventory = readItemStacks(in, inventorySize);
            return state;
        } catch (IOException e) {
            System.err.println("Failed to load player state: " + e.getMessage());
            return null;
        }
    }

    public void savePlayerState(String worldName, Vector3f position, ItemStack[] hotbar, ItemStack[] inventory) {
        if (worldName == null || position == null || hotbar == null || inventory == null) {
            return;
        }
        ensureWorldExists(worldName);
        Path playerPath = getPlayerPath(worldName);
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(playerPath,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
            out.writeInt(PLAYER_DATA_VERSION);
            out.writeFloat(position.x);
            out.writeFloat(position.y);
            out.writeFloat(position.z);

            writeItemStacks(out, hotbar);
            writeItemStacks(out, inventory);
        } catch (IOException e) {
            System.err.println("Failed to save player state: " + e.getMessage());
        }
    }

    private ItemStack[] readItemStacks(DataInputStream in, int size) throws IOException {
        ItemStack[] stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            String id = in.readUTF();
            int count = in.readInt();
            Item item = getItemById(id);
            stacks[i] = new ItemStack(item, count);
        }
        return stacks;
    }

    private void writeItemStacks(DataOutputStream out, ItemStack[] stacks) throws IOException {
        out.writeInt(stacks.length);
        for (ItemStack stack : stacks) {
            String id = getIdForItem(stack.getItem());
            out.writeUTF(id == null ? "" : id);
            out.writeInt(stack.getCount());
        }
    }

    private Item getItemById(String id) {
        if (id == null || id.isEmpty()) {
            return Items.AIR;
        }
        Item item = Registries.ITEM.getAll().get(id);
        return item != null ? item : Items.AIR;
    }

    private String getIdForItem(Item item) {
        if (item == null) {
            return "";
        }
        for (var entry : Registries.ITEM.getAll().entrySet()) {
            if (entry.getValue().equals(item)) {
                return entry.getKey();
            }
        }
        return "";
    }

    private Path getChunkDirectory(String worldName) {
        return worldsRoot.resolve(worldName).resolve("chunks");
    }

    private Path getChunkPath(String worldName, int chunkX, int chunkZ) {
        return getChunkDirectory(worldName).resolve(chunkX + "_" + chunkZ + ".bin");
    }

    private Path getPlayerPath(String worldName) {
        return worldsRoot.resolve(worldName).resolve("player.dat");
    }

    public List<DroppedItemData> loadDroppedItems(String worldName) {
        Path dropsPath = getDropsPath(worldName);
        if (!Files.exists(dropsPath)) {
            return new ArrayList<>();
        }
        List<DroppedItemData> result = new ArrayList<>();
        try (DataInputStream in = new DataInputStream(Files.newInputStream(dropsPath))) {
            int version = in.readInt();
            if (version != DROPS_DATA_VERSION) {
                return result;
            }
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                DroppedItemData data = new DroppedItemData();
                String id = in.readUTF();
                data.item = getItemById(id);
                data.count = in.readInt();
                float x = in.readFloat();
                float y = in.readFloat();
                float z = in.readFloat();
                data.position = new Vector3f(x, y, z);
                result.add(data);
            }
        } catch (IOException e) {
            System.err.println("Failed to load dropped items: " + e.getMessage());
        }
        return result;
    }

    public void saveDroppedItems(String worldName, List<net.infdev.api.world.entity.DroppedItem> items) {
        if (worldName == null || items == null) {
            return;
        }
        ensureWorldExists(worldName);
        Path dropsPath = getDropsPath(worldName);
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(dropsPath,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
            out.writeInt(DROPS_DATA_VERSION);
            out.writeInt(items.size());
            for (net.infdev.api.world.entity.DroppedItem item : items) {
                String id = getIdForItem(item.getItem());
                out.writeUTF(id == null ? "" : id);
                out.writeInt(item.getCount());
                Vector3f pos = item.getPosition();
                out.writeFloat(pos.x);
                out.writeFloat(pos.y);
                out.writeFloat(pos.z);
            }
        } catch (IOException e) {
            System.err.println("Failed to save dropped items: " + e.getMessage());
        }
    }

    private Path getDropsPath(String worldName) {
        return worldsRoot.resolve(worldName).resolve("drops.dat");
    }
}
