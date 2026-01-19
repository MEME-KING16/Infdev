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

    public enum MobState {
        IDLE, WANDERING, FOLLOWING_PLAYER, FLEEING
    }

    public Mob(String name, Vector3f position, float maxHealth, float moveSpeed) {
        super(name);
        this.position = position;
        this.velocity = new Vector3f(0, 0, 0);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.state = MobState.IDLE;
        this.random = new Random();
        this.moveSpeed = moveSpeed;
        this.lastStateChange = System.currentTimeMillis();
    }

    public abstract void update(float deltaTime, Vector3f playerPosition, Map<String, ?> chunks);

    // Update method that takes just deltaTime
    public void update(long deltaTime) {
        float dt = deltaTime / 1000.0f; // Convert milliseconds to seconds
        applyGravity(dt);
        move(dt);

        // Update render entity position if it exists
        if (renderEntity != null) {
            renderEntity.setPosition(position.x, position.y, position.z);
            renderEntity.updateModelMatrix();
        }
    }

    // AI update method
    public abstract void updateAI(Camera camera, Map<String, Chunk> loadedChunks, long deltaTime);

    public void applyGravity(float deltaTime) {
        // Simple ground check - stop falling at y = 0 or below ground level
        if (position.y > 1.0f) {
            velocity.y -= 9.8f * deltaTime;
        } else {
            position.y = Math.max(1.0f, position.y);
            velocity.y = 0;
        }
    }

    public void move(float deltaTime) {
        position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
        // Clamp to ground level
        if (position.y < 1.0f) {
            position.y = 1.0f;
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

    // Method to be overridden by subclasses to define loot drops
    public abstract java.util.List<LootDrop> getLootDrops();

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
