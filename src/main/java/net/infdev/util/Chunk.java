package net.infdev.util;

import net.infdev.api.world.block.Block;
import net.infdev.block.Blocks;
import net.infdev.engine.graph.*;
import net.infdev.engine.scene.Entity;
import net.infdev.engine.scene.Scene;
import org.joml.SimplexNoise;

import java.util.*;
import java.util.Random;

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
                    if (y <= height - 1) {
                        if (y < height - 4) {
                            blocks[x][y][z] = Blocks.STONE.getId();
                        } else if (y < height - 1) {
                            blocks[x][y][z] = Blocks.DIRT.getId();
                        } else {
                            if (height <= seaLevel) {
                                blocks[x][y][z] = Blocks.SAND.getId();
                            } else {
                                blocks[x][y][z] = Blocks.GRASS.getId();
                            }
                        }
                    } else if (y <= seaLevel) {
                        blocks[x][y][z] = Blocks.WATER.getId();
                    } else {
                        blocks[x][y][z] = Blocks.AIR.getId();
                    }
                }
            }
        }

        generateTrees();
    }

    private void generateTrees() {
        int seaLevel = 32;
        Random rand = new Random((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);

        for (int attempt = 0; attempt < 3; attempt++) {
            int x = rand.nextInt(CHUNK_SIZE);
            int z = rand.nextInt(CHUNK_SIZE);

            int worldX = chunkX * CHUNK_SIZE + x;
            int worldZ = chunkZ * CHUNK_SIZE + z;

            double treeNoise = octaveNoise(worldX, worldZ, 2, 0.5, 0.05);
            if (treeNoise < 0.3) {
                continue;
            }

            int y = findSurfaceHeight(x, z);

            if (y > seaLevel && y < CHUNK_HEIGHT - 10 && blocks[x][y][z] == Blocks.GRASS.getId()) {
                placeTree(x, y + 1, z, rand);
            }
        }
    }

    private int findSurfaceHeight(int x, int z) {
        for (int y = CHUNK_HEIGHT - 1; y >= 0; y--) {
            if (blocks[x][y][z] != Blocks.AIR.getId() && blocks[x][y][z] != Blocks.WATER.getId()) {
                return y;
            }
        }
        return 0;
    }

    private void placeTree(int x, int y, int z, Random rand) {
        int trunkHeight = 4 + rand.nextInt(3);

        for (int i = 0; i < trunkHeight; i++) {
            setBlockSafe(x, y + i, z, Blocks.OAK_LOG.getId());
        }

        int leavesY = y + trunkHeight - 2;

        for (int dy = 0; dy < 4; dy++) {
            int currentY = leavesY + dy;
            int radius = (dy == 0 || dy == 3) ? 1 : 2;

            if (dy == 3) {
                setBlockSafe(x, currentY, z, Blocks.OAK_LEAVES.getId());
            } else {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (dx == 0 && dz == 0 && dy < 2) {
                            continue;
                        }

                        int dist = Math.abs(dx) + Math.abs(dz);
                        if (dist <= radius + 1) {
                            if (rand.nextFloat() < 0.85f || dist <= radius) {
                                setBlockSafe(x + dx, currentY, z + dz, Blocks.OAK_LEAVES.getId());
                            }
                        }
                    }
                }
            }
        }
    }

    private void setBlockSafe(int x, int y, int z, byte blockId) {
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_HEIGHT && z >= 0 && z < CHUNK_SIZE) {
            if (blocks[x][y][z] == Blocks.AIR.getId() || blocks[x][y][z] == Blocks.OAK_LEAVES.getId()) {
                blocks[x][y][z] = blockId;
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

    public void rebuildMesh(Scene scene) {
        // Save old entities
        List<Entity> oldEntities = new ArrayList<>(entities);

        // Clean up old models and entities lists
        for (Model model : models) {
            model.cleanup();
        }
        models.clear();
        entities.clear();

        // Build new mesh
        buildMesh();

        // If in scene, atomically swap entities to prevent flickering
        if (inScene) {
            // Remove old entities
            for (Entity entity : oldEntities) {
                scene.removeEntity(entity);
            }
            // Add new entities
            for (Entity entity : entities) {
                scene.addEntity(entity);
            }
            // Add new models
            for (Model model : models) {
                scene.addModel(model);
            }
        }
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

                    if (isTransparent(x - 1, y, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_left", k -> new MeshData());
                        addLeftFace(meshData, worldX, y, worldZ);
                    }
                    if (isTransparent(x + 1, y, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_right", k -> new MeshData());
                        addRightFace(meshData, worldX, y, worldZ);
                    }
                    if (isTransparent(x, y - 1, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_bottom", k -> new MeshData());
                        addBottomFace(meshData, worldX, y, worldZ);
                    }
                    if (isTransparent(x, y + 1, z)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_top", k -> new MeshData());
                        addTopFace(meshData, worldX, y, worldZ);
                    }
                    if (isTransparent(x, y, z - 1)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_back", k -> new MeshData());
                        addBackFace(meshData, worldX, y, worldZ);
                    }
                    if (isTransparent(x, y, z + 1)) {
                        MeshData meshData = meshDataMap.computeIfAbsent(blockId + "_front", k -> new MeshData());
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
        if (id == Blocks.OAK_LOG.getId()) return Blocks.OAK_LOG;
        if (id == Blocks.OAK_LEAVES.getId()) return Blocks.OAK_LEAVES;
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

    private boolean isTransparent(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            return true;
        }
        byte blockId = blocks[x][y][z];
        return blockId == Blocks.AIR.getId();
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