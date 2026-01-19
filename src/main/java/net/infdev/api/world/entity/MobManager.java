package net.infdev.api.world.entity;

import net.infdev.block.Blocks;
import net.infdev.engine.graph.*;
import net.infdev.engine.scene.Camera;
import net.infdev.engine.scene.Entity;
import net.infdev.engine.scene.Scene;
import net.infdev.util.Chunk;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MobManager {
    private static final int MAX_MOBS = 15;
    private static final int MIN_SPAWN_DISTANCE = 10;
    private static final int MAX_SPAWN_DISTANCE = 30;
    private static final long SPAWN_INTERVAL = 10000; // 10 seconds
    private static final int SEA_LEVEL = 32;

    private final List<Mob> mobs;
    private final Random random;
    private long lastSpawnTime;
    private Scene scene;
    private DroppedItemManager droppedItemManager;

    public MobManager() {
        this.mobs = new ArrayList<>();
        this.random = new Random();
        this.lastSpawnTime = 0;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        createMobModels();
    }

    public void setDroppedItemManager(DroppedItemManager droppedItemManager) {
        this.droppedItemManager = droppedItemManager;
    }

    private void createMobModels() {
        // Create simple cube models for mobs
        createMobModel("pig_model", 0.8f, 0.6f, 0.8f, 1.0f, 0.7f, 0.7f, "models/mob/pig.png");
        createMobModel("cow_model", 0.9f, 0.7f, 0.9f, 0.6f, 0.4f, 0.2f, "models/mob/cow.png");
        createMobModel("zombie_model", 0.6f, 0.9f, 0.6f, 0.3f, 0.7f, 0.3f, "models/mob/zombie.png");
    }

    private void createMobModel(String modelId, float w, float h, float d, float r, float g, float b, String texturePath) {
        // Create box mesh data
        float hw = w / 2, hh = h / 2, hd = d / 2;

        float[] positions = {
            // Front, Back, Top, Bottom, Right, Left faces (all 6)
            -hw,-hh,hd, hw,-hh,hd, hw,hh,hd, -hw,hh,hd,
            hw,-hh,-hd, -hw,-hh,-hd, -hw,hh,-hd, hw,hh,-hd,
            -hw,hh,hd, hw,hh,hd, hw,hh,-hd, -hw,hh,-hd,
            -hw,-hh,-hd, hw,-hh,-hd, hw,-hh,hd, -hw,-hh,hd,
            hw,-hh,hd, hw,-hh,-hd, hw,hh,-hd, hw,hh,hd,
            -hw,-hh,-hd, -hw,-hh,hd, -hw,hh,hd, -hw,hh,-hd
        };

        float[] normals = new float[72]; // 6 faces * 4 vertices * 3 components
        float[] texCoords = new float[48]; // 6 faces * 4 vertices * 2 components

        // Fill normals and texcoords
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                if (i == 0) { normals[idx*3] = 0; normals[idx*3+1] = 0; normals[idx*3+2] = 1; }
                else if (i == 1) { normals[idx*3] = 0; normals[idx*3+1] = 0; normals[idx*3+2] = -1; }
                else if (i == 2) { normals[idx*3] = 0; normals[idx*3+1] = 1; normals[idx*3+2] = 0; }
                else if (i == 3) { normals[idx*3] = 0; normals[idx*3+1] = -1; normals[idx*3+2] = 0; }
                else if (i == 4) { normals[idx*3] = 1; normals[idx*3+1] = 0; normals[idx*3+2] = 0; }
                else { normals[idx*3] = -1; normals[idx*3+1] = 0; normals[idx*3+2] = 0; }

                texCoords[idx*2] = (j == 1 || j == 2) ? 1.0f : 0.0f;
                texCoords[idx*2+1] = (j >= 2) ? 1.0f : 0.0f;
            }
        }

        int[] indices = new int[36];
        for (int i = 0; i < 6; i++) {
            int base = i * 4;
            indices[i*6] = base; indices[i*6+1] = base+1; indices[i*6+2] = base+2;
            indices[i*6+3] = base; indices[i*6+4] = base+2; indices[i*6+5] = base+3;
        }

        Mesh mesh = new Mesh(positions, normals, texCoords, indices);

        Material material = new Material();
        material.setTexturePath(texturePath);
        material.setDiffuseColor(new Vector4f(r, g, b, 1.0f));
        material.setAmbientColor(new Vector4f(r * 0.3f, g * 0.3f, b * 0.3f, 1.0f));
        material.getMeshList().add(mesh);

        List<Material> materials = new ArrayList<>();
        materials.add(material);

        Model model = new Model(modelId, materials);
        scene.addModel(model);
    }

    public void update(Camera camera, Map<String, Chunk> loadedChunks, long deltaTime) {
        long currentTime = System.currentTimeMillis();

        // Update all mobs
        for (int i = mobs.size() - 1; i >= 0; i--) {
            Mob mob = mobs.get(i);

            // Remove dead mobs and drop loot
            if (mob.isDead()) {
                dropLoot(mob);
                removeMob(mob);
                mobs.remove(i);
                continue;
            }

            // Update AI and movement
            mob.updateAI(camera, loadedChunks, deltaTime);
            mob.update(deltaTime, loadedChunks);
        }

        // Spawn new mobs
        if (currentTime - lastSpawnTime >= SPAWN_INTERVAL && mobs.size() < MAX_MOBS) {
            trySpawnMob(camera, loadedChunks);
            lastSpawnTime = currentTime;
        }
    }

    private void trySpawnMob(Camera camera, Map<String, Chunk> loadedChunks) {
        Vector3f playerPos = camera.getPosition();

        // Try up to 10 times to find a valid spawn location
        for (int attempt = 0; attempt < 10; attempt++) {
            // Random angle and distance from player
            float angle = random.nextFloat() * (float) Math.PI * 2;
            float distance = MIN_SPAWN_DISTANCE + random.nextFloat() * (MAX_SPAWN_DISTANCE - MIN_SPAWN_DISTANCE);

            int spawnX = (int) (playerPos.x + Math.cos(angle) * distance);
            int spawnZ = (int) (playerPos.z + Math.sin(angle) * distance);

            // Find surface height at this position
            int surfaceY = findSurfaceHeight(spawnX, spawnZ, loadedChunks);

            if (surfaceY > SEA_LEVEL && surfaceY < Chunk.CHUNK_HEIGHT - 5) {
                // Check if it's a grass block
                int chunkX = (int) Math.floor((double) spawnX / Chunk.CHUNK_SIZE);
                int chunkZ = (int) Math.floor((double) spawnZ / Chunk.CHUNK_SIZE);
                String key = chunkX + "_" + chunkZ;

                Chunk chunk = loadedChunks.get(key);
                if (chunk == null) continue;

                int localX = spawnX - (chunkX * Chunk.CHUNK_SIZE);
                int localZ = spawnZ - (chunkZ * Chunk.CHUNK_SIZE);

                byte blockId = chunk.getBlock(localX, surfaceY, localZ);

                if (blockId == Blocks.GRASS.getId()) {
                    // Valid spawn location - spawn a random mob
                    Vector3f spawnPos = new Vector3f(spawnX + 0.5f, surfaceY + 1, spawnZ + 0.5f);
                    spawnRandomMob(spawnPos);
                    return;
                }
            }
        }
    }

    private int findSurfaceHeight(int x, int z, Map<String, Chunk> loadedChunks) {
        int chunkX = (int) Math.floor((double) x / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor((double) z / Chunk.CHUNK_SIZE);
        String key = chunkX + "_" + chunkZ;

        Chunk chunk = loadedChunks.get(key);
        if (chunk == null) return -1;

        int localX = x - (chunkX * Chunk.CHUNK_SIZE);
        int localZ = z - (chunkZ * Chunk.CHUNK_SIZE);

        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
            if (chunk.hasBlockAt(localX, y, localZ)) {
                return y;
            }
        }
        return -1;
    }

    private void spawnRandomMob(Vector3f position) {
        if (scene == null) return;

        float rand = random.nextFloat();
        Mob mob;

        if (rand < 0.4f) {
            // 40% chance for pig
            mob = new Pig(position);
        } else if (rand < 0.7f) {
            // 30% chance for cow
            mob = new Cow(position);
        } else {
            // 30% chance for zombie
            mob = new Zombie(position);
        }

        float adjustedY = position.y;
        if (mob.getHeight() > 0) {
            adjustedY = position.y + (mob.getHeight() * 0.5f);
        }
        mob.setPosition(new Vector3f(position.x, adjustedY, position.z));

        // Create render entity
        Entity entity = new Entity("mob_" + System.currentTimeMillis(), mob.getModelId());
        entity.setPosition(mob.getPosition().x, mob.getPosition().y, mob.getPosition().z);
        entity.setScale(1.0f);
        entity.updateModelMatrix();

        mob.setRenderEntity(entity);
        scene.addEntity(entity);
        mobs.add(mob);
    }

    private void removeMob(Mob mob) {
        if (scene != null && mob.getRenderEntity() != null) {
            scene.removeEntity(mob.getRenderEntity());
        }
    }

    private void dropLoot(Mob mob) {
        if (droppedItemManager == null) return;

        List<Mob.LootDrop> lootDrops = mob.getLootDrops();
        for (Mob.LootDrop drop : lootDrops) {
            int count = drop.minCount + random.nextInt(drop.maxCount - drop.minCount + 1);
            if (count > 0) {
                droppedItemManager.spawnItem(mob.getPosition(), drop.item, count);
            }
        }
    }

    public void cleanup() {
        for (Mob mob : mobs) {
            removeMob(mob);
        }
        mobs.clear();
    }

    public List<Mob> getMobs() {
        return mobs;
    }
}
