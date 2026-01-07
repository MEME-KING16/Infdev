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
    public static final int CHUNK_HEIGHT = 128;

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

                // layered noise
                double base = octaveNoise(worldX, worldZ, 4, 0.5, 0.01);  // large terrain
                double detail = octaveNoise(worldX + 1000, worldZ + 1000, 2, 0.6, 0.02); // small variation

                // mountain and mask noise
                double mountains = octaveNoise(worldX, worldZ, 5, 0.45, 0.0025);
                mountains = 1.0 - Math.abs(mountains);   // ridged peaks
                mountains = Math.pow(mountains, 2.0);    //flaten 
                mountains *= 2; // amp

                double hills = octaveNoise(worldX + 5000, worldZ + 5000, 4, 0.5, 0.0075);

                double continent = octaveNoise(worldX, worldZ, 2, 0.5, 0.001);
                continent = (continent + 1.0) / 2.0;     // normalize to 0–1
                double mask = Math.pow(continent, 2.5);  // where mountains appear

                // height
                double heightVal = (base * 15) + (hills * 20 + mountains * 50) * mask + detail * 3 + 32;  // base amplitude + offset
                int height = (int) Math.max(1, Math.min(CHUNK_HEIGHT - 1, heightVal));

                // sea
                int seaLevel = 32;
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    if (y < height - 4) {
                        blocks[x][y][z] = Blocks.STONE.getId();
                    } else if (y < height - 1) {
                        blocks[x][y][z] = Blocks.DIRT.getId();
                    } else if (y == height) {
                        if (height < seaLevel) blocks[x][y][z] = Blocks.SAND.getId();
                        else blocks[x][y][z] = Blocks.GRASS.getId(); //grass
                    } else if (y <= seaLevel && y > height) {
                        blocks[x][y][z] = Blocks.WATER.getId();
                    } else {
                        blocks[x][y][z] = Blocks.AIR.getId();
                    }
                }
            }
        }

        generateVisibleBlocks();
    }

    private double octaveNoise(double x, double z, int octaves, double persistence, double scale) {
        double total = 0;
        double amplitude = 1;
        double frequency = scale;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += SimplexNoise.noise((float) (x * frequency), (float) (z * frequency)) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }

        return total / maxValue;
    }

    //mesh soon
    public void rebuildMesh() {
        entities.clear();
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

    public boolean hasBlockAt(int x, int y, int z) {
        return blocks[x][y][z] != Blocks.AIR.getId();
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

    public void setBlock(int x, int y, int z, byte blockId) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE)
            return;
        blocks[x][y][z] = blockId;
    }

    private Model getModelForBlock(byte blockId) {
        if (blockId == Blocks.GRASS.getId()) return Blocks.GRASS.getModel();
        if (blockId == Blocks.DIRT.getId()) return Blocks.DIRT.getModel();
        if (blockId == Blocks.STONE.getId()) return Blocks.STONE.getModel();
        if (blockId == Blocks.WATER.getId()) return Blocks.WATER.getModel();
        if (blockId == Blocks.SAND.getId()) return Blocks.SAND.getModel();
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

    public String getChunkX() {
        return Integer.toString(chunkX);
    }

    public String getChunkZ() {
        return Integer.toString(chunkZ);
    }
    public List<Entity> getEntities() {
        return entities;
    }
}
