package net.infdev.util;

import net.infdev.api.Block;
import net.infdev.block.Blocks;
import net.infdev.engine.graph.*;
import net.infdev.engine.scene.Entity;
import net.infdev.engine.scene.Scene;
import org.joml.SimplexNoise;

import java.util.*;

public class Chunk {
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 128;

    private final int chunkX;
    private final int chunkZ;
    private final byte[][][] blocks = new byte[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
    private final List<Entity> entities = new ArrayList<>();
    private final List<Model> models = new ArrayList<>();
    private boolean inScene = false;

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public boolean isInScene() { return inScene; }

    public void buildData() {
        for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                int worldX = chunkX * CHUNK_SIZE + x;
                int worldZ = chunkZ * CHUNK_SIZE + z;

                double base = octaveNoise(worldX, worldZ, 4, 0.5, 0.01);
                double detail = octaveNoise(worldX + 1000, worldZ + 1000, 2, 0.6, 0.02);
                double mountains = octaveNoise(worldX, worldZ, 5, 0.45, 0.0025);
                mountains = 1.0 - Math.abs(mountains);
                mountains = Math.pow(mountains, 2.0);
                mountains *= 2;
                double hills = octaveNoise(worldX + 5000, worldZ + 5000, 4, 0.5, 0.0075);
                double continent = octaveNoise(worldX, worldZ, 2, 0.5, 0.001);
                continent = (continent + 1.0) / 2.0;
                double mask = Math.pow(continent, 2.5);
                double heightVal = (base * 15) + (hills * 20 + mountains * 50) * mask + detail * 3 + 32;
                int height = (int) Math.max(1, Math.min(CHUNK_HEIGHT - 1, heightVal));

                int seaLevel = 32;
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    if (y < height - 4) {
                        blocks[x][y][z] = Blocks.STONE.getId();
                    } else if (y < height - 1) {
                        blocks[x][y][z] = Blocks.DIRT.getId();
                    } else if (y == height) {
                        if (height < seaLevel) blocks[x][y][z] = Blocks.SAND.getId();
                        else blocks[x][y][z] = Blocks.GRASS.getId();
                    } else if (y <= seaLevel && y > height) {
                        blocks[x][y][z] = Blocks.WATER.getId();
                    } else {
                        blocks[x][y][z] = Blocks.AIR.getId();
                    }
                }
            }
        }
    }

    public void rebuildMesh() {
        for (Model model : models) {
            model.cleanup();
        }
        models.clear();
        entities.clear();
        buildMesh();
    }

    public void buildMesh() {
        Map<String, MeshData> meshDataMap = new HashMap<>();

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    byte blockId = blocks[x][y][z];
                    if (blockId == Blocks.AIR.getId()) continue;

                    int worldX = chunkX * CHUNK_SIZE + x;
                    int worldZ = chunkZ * CHUNK_SIZE + z;

                    if (isAir(x - 1, y, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_side", k -> new MeshData());
                        addLeftFace(meshData, worldX, y, worldZ);
                    }
                    if (isAir(x + 1, y, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_side", k -> new MeshData());
                        addRightFace(meshData, worldX, y, worldZ);
                    }
                    if (isAir(x, y - 1, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_bottom", k -> new MeshData());
                        addBottomFace(meshData, worldX, y, worldZ);
                    }
                    if (isAir(x, y + 1, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_top", k -> new MeshData());
                        addTopFace(meshData, worldX, y, worldZ);
                    }
                    if (isAir(x, y, z - 1)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_side", k -> new MeshData());
                        addBackFace(meshData, worldX, y, worldZ);
                    }
                    if (isAir(x, y, z + 1)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_side", k -> new MeshData());
                        addFrontFace(meshData, worldX, y, worldZ);
                    }
                }
            }
        }

        for (Map.Entry<String, MeshData> entry : meshDataMap.entrySet()) {
            String key = entry.getKey();
            MeshData meshData = entry.getValue();

            if (meshData.indices.isEmpty()) continue;

            String[] parts = key.split("_");
            byte blockId = Byte.parseByte(parts[0]);

            Block block = getBlockById(blockId);
            if (block == null) continue;

            Mesh mesh = createMesh(meshData);
            
            Material material = new Material();
            material.setTexturePath(getTextureForBlock(block));
            material.getMeshList().add(mesh);

            List<Material> materials = new ArrayList<>();
            materials.add(material);

            String modelId = "chunk_" + chunkX + "_" + chunkZ + "_" + key;
            Model model = new Model(modelId, materials);
            models.add(model);

            Entity entity = new Entity("entity_" + modelId, modelId);
            entity.setPosition(0, 0, 0);
            entity.setScale(1.0f);
            entity.updateModelMatrix();
            entities.add(entity);
        }
    }

    private Block getBlockById(byte id) {
        if (id == Blocks.GRASS.getId()) return Blocks.GRASS;
        if (id == Blocks.DIRT.getId()) return Blocks.DIRT;
        if (id == Blocks.STONE.getId()) return Blocks.STONE;
        if (id == Blocks.WATER.getId()) return Blocks.WATER;
        if (id == Blocks.SAND.getId()) return Blocks.SAND;
        return null;
    }

    private String getTextureForBlock(Block block) {
        if (block.getModel() != null && !block.getModel().getMaterialList().isEmpty()) {
            return block.getModel().getMaterialList().get(0).getTexturePath();
        }
        return "models/block/stone.png";
    }

    private Mesh createMesh(MeshData meshData) {
        float[] positions = toFloatArray(meshData.positions);
        float[] texCoords = toFloatArray(meshData.texCoords);
        float[] normals = toFloatArray(meshData.normals);
        int[] indices = toIntArray(meshData.indices);

        return new Mesh(positions, normals, texCoords, indices);
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private static class MeshData {
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexCount = 0;
    }

    private void addLeftFace(MeshData m, float x, float y, float z) {
        m.positions.add(x); m.positions.add(y); m.positions.add(z);
        m.positions.add(x); m.positions.add(y + 1); m.positions.add(z);
        m.positions.add(x); m.positions.add(y + 1); m.positions.add(z + 1);
        m.positions.add(x); m.positions.add(y); m.positions.add(z + 1);

        m.texCoords.add(0.0f); m.texCoords.add(0.0f);
        m.texCoords.add(0.0f); m.texCoords.add(0.5f);
        m.texCoords.add(0.5f); m.texCoords.add(0.5f);
        m.texCoords.add(0.5f); m.texCoords.add(0.0f);
        for (int i = 0; i < 4; i++) { m.normals.add(-1f); m.normals.add(0f); m.normals.add(0f); }
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 1); m.indices.add(m.vertexCount + 2);
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 2); m.indices.add(m.vertexCount + 3);
        m.vertexCount += 4;
    }

    private void addRightFace(MeshData m, float x, float y, float z) {
        m.positions.add(x + 1); m.positions.add(y); m.positions.add(z + 1);
        m.positions.add(x + 1); m.positions.add(y + 1); m.positions.add(z + 1);
        m.positions.add(x + 1); m.positions.add(y + 1); m.positions.add(z);
        m.positions.add(x + 1); m.positions.add(y); m.positions.add(z);

        m.texCoords.add(0.5f); m.texCoords.add(0.0f);
        m.texCoords.add(0.5f); m.texCoords.add(0.5f);
        m.texCoords.add(1.0f); m.texCoords.add(0.5f);
        m.texCoords.add(1.0f); m.texCoords.add(0.0f);
        for (int i = 0; i < 4; i++) { m.normals.add(1f); m.normals.add(0f); m.normals.add(0f); }
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 1); m.indices.add(m.vertexCount + 2);
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 2); m.indices.add(m.vertexCount + 3);
        m.vertexCount += 4;
    }

    private void addBottomFace(MeshData m, float x, float y, float z) {
        m.positions.add(x); m.positions.add(y); m.positions.add(z);
        m.positions.add(x + 1); m.positions.add(y); m.positions.add(z);
        m.positions.add(x + 1); m.positions.add(y); m.positions.add(z + 1);
        m.positions.add(x); m.positions.add(y); m.positions.add(z + 1);

        m.texCoords.add(0.5f); m.texCoords.add(1.0f);
        m.texCoords.add(0.5f); m.texCoords.add(0.5f);
        m.texCoords.add(1.0f); m.texCoords.add(0.5f);
        m.texCoords.add(1.0f); m.texCoords.add(1.0f);
        for (int i = 0; i < 4; i++) { m.normals.add(0f); m.normals.add(-1f); m.normals.add(0f); }
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 1); m.indices.add(m.vertexCount + 2);
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 2); m.indices.add(m.vertexCount + 3);
        m.vertexCount += 4;
    }

    private void addTopFace(MeshData m, float x, float y, float z) {
        m.positions.add(x); m.positions.add(y + 1); m.positions.add(z + 1);
        m.positions.add(x + 1); m.positions.add(y + 1); m.positions.add(z + 1);
        m.positions.add(x + 1); m.positions.add(y + 1); m.positions.add(z);
        m.positions.add(x); m.positions.add(y + 1); m.positions.add(z);

        m.texCoords.add(0.0f); m.texCoords.add(1.0f);
        m.texCoords.add(0.0f); m.texCoords.add(0.5f);
        m.texCoords.add(0.5f); m.texCoords.add(0.5f);
        m.texCoords.add(0.5f); m.texCoords.add(1.0f);
        for (int i = 0; i < 4; i++) { m.normals.add(0f); m.normals.add(1f); m.normals.add(0f); }
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 1); m.indices.add(m.vertexCount + 2);
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 2); m.indices.add(m.vertexCount + 3);
        m.vertexCount += 4;
    }

    private void addBackFace(MeshData m, float x, float y, float z) {
        m.positions.add(x + 1); m.positions.add(y); m.positions.add(z);
        m.positions.add(x + 1); m.positions.add(y + 1); m.positions.add(z);
        m.positions.add(x); m.positions.add(y + 1); m.positions.add(z);
        m.positions.add(x); m.positions.add(y); m.positions.add(z);

        m.texCoords.add(0.0f); m.texCoords.add(0.0f);
        m.texCoords.add(0.0f); m.texCoords.add(0.5f);
        m.texCoords.add(0.5f); m.texCoords.add(0.5f);
        m.texCoords.add(0.5f); m.texCoords.add(0.0f);
        for (int i = 0; i < 4; i++) { m.normals.add(0f); m.normals.add(0f); m.normals.add(-1f); }
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 1); m.indices.add(m.vertexCount + 2);
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 2); m.indices.add(m.vertexCount + 3);
        m.vertexCount += 4;
    }

    private void addFrontFace(MeshData m, float x, float y, float z) {
        m.positions.add(x); m.positions.add(y); m.positions.add(z + 1);
        m.positions.add(x); m.positions.add(y + 1); m.positions.add(z + 1);
        m.positions.add(x + 1); m.positions.add(y + 1); m.positions.add(z + 1);
        m.positions.add(x + 1); m.positions.add(y); m.positions.add(z + 1);

        m.texCoords.add(0.5f); m.texCoords.add(0.0f);
        m.texCoords.add(0.5f); m.texCoords.add(0.5f);
        m.texCoords.add(1.0f); m.texCoords.add(0.5f);
        m.texCoords.add(1.0f); m.texCoords.add(0.0f);
        for (int i = 0; i < 4; i++) { m.normals.add(0f); m.normals.add(0f); m.normals.add(1f); }
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 1); m.indices.add(m.vertexCount + 2);
        m.indices.add(m.vertexCount); m.indices.add(m.vertexCount + 2); m.indices.add(m.vertexCount + 3);
        m.vertexCount += 4;
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

    private boolean isAir(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE)
            return true;
        return blocks[x][y][z] == Blocks.AIR.getId();
    }

    public boolean hasBlockAt(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE)
            return false;
        return blocks[x][y][z] != Blocks.AIR.getId();
    }

    public void setBlock(int x, int y, int z, byte blockId) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE)
            return;
        blocks[x][y][z] = blockId;
    }

    public byte getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE)
            return Blocks.AIR.getId();
        return blocks[x][y][z];
    }

    public void uploadToScene(Scene scene) {
        if (!inScene) {
            for (Model model : models) {
                scene.addModel(model);
            }
            for (Entity entity : entities) {
                scene.addEntity(entity);
            }
            inScene = true;
        }
    }

    public void removeFromScene(Scene scene) {
        if (inScene) {
            for (Entity entity : entities) {
                scene.removeEntity(entity);
            }
            inScene = false;
        }
    }
}