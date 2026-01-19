package net.infdev.api.world.entity;

import net.infdev.engine.graph.Model;
import net.infdev.engine.scene.Entity;
import net.infdev.engine.scene.Camera;
import net.infdev.util.Chunk;
import org.joml.Vector3f;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class Mob extends net.infdev.api.world.entity.Entity {
    protected Vector3f position;
    protected Vector3f velocity;
    protected float health;
    protected float maxHealth;
    protected MobState state;
    protected Model model;
    protected Random random;
    protected float moveSpeed;
    protected long lastStateChange;
    protected Vector3f targetPosition;
    protected List<int[]> currentPath;
    protected int pathIndex;
    protected net.infdev.engine.scene.Entity renderEntity;
    protected String modelId;
    protected float halfWidth;
    protected float height;

    private static final float GRAVITY = -9.8f;

    public enum MobState {
        IDLE, WANDERING, FOLLOWING_PLAYER, FLEEING
    }

    public Mob(String name, Vector3f position, float maxHealth, float moveSpeed, float width, float height) {
        super(name);
        this.position = position;
        this.velocity = new Vector3f(0, 0, 0);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.state = MobState.IDLE;
        this.random = new Random();
        this.moveSpeed = moveSpeed;
        this.lastStateChange = System.currentTimeMillis();
        this.halfWidth = width * 0.5f;
        this.height = height;
    }

    public abstract void update(float deltaTime, Vector3f playerPosition, Map<String, ?> chunks);

    // Update method that takes just deltaTime
    public void update(long deltaTime) {
        update(deltaTime, null);
    }

    public void update(long deltaTime, Map<String, Chunk> loadedChunks) {
        float dt = deltaTime / 1000.0f; // Convert milliseconds to seconds
        applyGravity(dt);
        move(dt, loadedChunks);

        // Update render entity position if it exists
        if (renderEntity != null) {
            renderEntity.setPosition(position.x, position.y, position.z);
            renderEntity.updateModelMatrix();
        }
    }

    // AI update method
    public abstract void updateAI(Camera camera, Map<String, Chunk> loadedChunks, long deltaTime);

    public void applyGravity(float deltaTime) {
        velocity.y += GRAVITY * deltaTime;
    }

    public void move(float deltaTime, Map<String, Chunk> loadedChunks) {
        Vector3f pos = position;

        float oldX = pos.x;
        pos.x += velocity.x * deltaTime;
        if (checkCollision(pos, loadedChunks)) {
            pos.x = oldX;
        }

        float oldZ = pos.z;
        pos.z += velocity.z * deltaTime;
        if (checkCollision(pos, loadedChunks)) {
            pos.z = oldZ;
        }

        float oldY = pos.y;
        pos.y += velocity.y * deltaTime;
        if (checkCollision(pos, loadedChunks)) {
            float epsilon = 0.001f;
            if (velocity.y < 0) {
                float bottom = pos.y - (height * 0.5f);
                pos.y = (float) Math.floor(bottom) + 1.0f + (height * 0.5f) + epsilon;
            } else if (velocity.y > 0) {
                float top = pos.y + (height * 0.5f);
                pos.y = (float) Math.ceil(top) - (height * 0.5f) - epsilon;
            } else {
                pos.y = oldY;
            }
            velocity.y = 0;
        }
    }

    public void damage(float amount) {
        health -= amount;
        if (health <= 0) health = 0;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public Vector3f getPosition() { return position; }
    public void setPosition(Vector3f position) { this.position = position; }

    public Vector3f getVelocity() { return velocity; }
    public void setVelocity(Vector3f velocity) { this.velocity = velocity; }

    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = health; }

    public float getMaxHealth() { return maxHealth; }

    public MobState getState() { return state; }
    public void setState(MobState state) { this.state = state; }

    public Model getModel() { return model; }
    public void setModel(Model model) { this.model = model; }

    public float getMoveSpeed() { return moveSpeed; }
    public void setMoveSpeed(float moveSpeed) { this.moveSpeed = moveSpeed; }

    public net.infdev.engine.scene.Entity getRenderEntity() { return renderEntity; }
    public void setRenderEntity(net.infdev.engine.scene.Entity renderEntity) { this.renderEntity = renderEntity; }

    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public float getHeight() { return height; }
    public float getHalfWidth() { return halfWidth; }

    // Method to be overridden by subclasses to define loot drops
    public abstract java.util.List<LootDrop> getLootDrops();

    private boolean checkCollision(Vector3f pos, Map<String, Chunk> loadedChunks) {
        if (loadedChunks == null) {
            return false;
        }

        float minX = pos.x - halfWidth;
        float maxX = pos.x + halfWidth;
        float minY = pos.y - (height * 0.5f);
        float maxY = pos.y + (height * 0.5f);
        float minZ = pos.z - halfWidth;
        float maxZ = pos.z + halfWidth;

        int blockMinX = (int) Math.floor(minX);
        int blockMaxX = (int) Math.floor(maxX);
        int blockMinY = (int) Math.floor(minY);
        int blockMaxY = (int) Math.floor(maxY);
        int blockMinZ = (int) Math.floor(minZ);
        int blockMaxZ = (int) Math.floor(maxZ);

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    if (isBlockSolid(loadedChunks, x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isBlockSolid(Map<String, Chunk> loadedChunks, int x, int y, int z) {
        int chunkX = (int) Math.floor((double) x / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor((double) z / Chunk.CHUNK_SIZE);
        String key = chunkX + "_" + chunkZ;

        Chunk chunk = loadedChunks.get(key);
        if (chunk == null) return true;

        int localX = x - (chunkX * Chunk.CHUNK_SIZE);
        int localZ = z - (chunkZ * Chunk.CHUNK_SIZE);

        if (localX < 0 || localX >= Chunk.CHUNK_SIZE ||
            localZ < 0 || localZ >= Chunk.CHUNK_SIZE ||
            y < 0 || y >= Chunk.CHUNK_HEIGHT) {
            return true;
        }

        int blockId = chunk.getBlock(localX, y, localZ);
        return blockId != net.infdev.block.Blocks.AIR.getId();
    }

    // Helper class to define loot drops
    public static class LootDrop {
        public final net.infdev.api.world.item.Item item;
        public final int minCount;
        public final int maxCount;

        public LootDrop(net.infdev.api.world.item.Item item, int minCount, int maxCount) {
            this.item = item;
            this.minCount = minCount;
            this.maxCount = maxCount;
        }
    }
}
