package net.infdev.util;

import net.infdev.block.Blocks;
import net.infdev.engine.graph.Model;
import net.infdev.engine.scene.Entity;
import net.infdev.engine.scene.Scene;
import org.joml.SimplexNoise;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 64;

    private final int chunkX;
    private final int chunkZ;
    private final byte[][][] blocks = new byte[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
    private final List<Entity> entities = new ArrayList<>();

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void buildData() {
        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                int worldX = chunkX * CHUNK_SIZE + x;
                int worldZ = chunkZ * CHUNK_SIZE + z;

                int height = (int) (SimplexNoise.noise(worldX * 0.03f, worldZ * 0.03f) * 10 + 20);
                height = Math.max(1, Math.min(CHUNK_HEIGHT - 1, height));

                for (int y = 0; y <= height; y++) {
                    if (y == height) {
                        blocks[x][y][z] = Blocks.GRASS.getId();
                    } else if (y > height - 3) {
                        blocks[x][y][z] = Blocks.DIRT.getId();
                    } else {
                        //blocks[x][y][z] = Blocks.STONE.getId();
                    }
                }
            }
        }

        generateVisibleBlocks();
    }

    private void generateVisibleBlocks() {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    byte blockId = blocks[x][y][z];
                    if (blockId == Blocks.AIR.getId()) continue;

                    if (isAir(x + 1, y, z) ||
                        isAir(x - 1, y, z) ||
                        isAir(x, y + 1, z) ||
                        isAir(x, y - 1, z) ||
                        isAir(x, y, z + 1) ||
                        isAir(x, y, z - 1)) {

                        addBlockEntity(x, y, z, blockId);
                    }
                }
            }
        }
    }

    private boolean isAir(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE)
            return true;
        return blocks[x][y][z] == Blocks.AIR.getId();
    }

    private void addBlockEntity(int x, int y, int z, byte blockId) {
        Model model = getModelForBlock(blockId);
        if (model == null) return;

        int worldX = chunkX * CHUNK_SIZE + x;
        int worldZ = chunkZ * CHUNK_SIZE + z;

        Entity e = new Entity("block_" + chunkX + "_" + chunkZ + "_" + x + "_" + y + "_" + z, model.getId());
        e.setPosition(worldX, y, worldZ);
        e.setScale(1.0f);
        e.getModelMatrix().identity().translate(e.getPosition()).scale(1.0f);

        entities.add(e);
    }

    private Model getModelForBlock(byte blockId) {
        if (blockId == Blocks.GRASS.getId()) return Blocks.GRASS.getModel();
        if (blockId == Blocks.DIRT.getId()) return Blocks.DIRT.getModel();
        if (blockId == Blocks.STONE.getId()) return Blocks.STONE.getModel();
        return null;
    }

    public void uploadToScene(Scene scene) {
        for (Entity e : entities)
            scene.addEntity(e);
    }

    public void removeFromScene(Scene scene) {
        for (Entity e : entities)
            scene.removeEntity(e);
    }
}
