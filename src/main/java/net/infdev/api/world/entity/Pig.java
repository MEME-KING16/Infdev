package net.infdev.api.world.entity;

import net.infdev.engine.scene.Camera;
import net.infdev.item.Items;
import net.infdev.util.Chunk;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Pig extends Mob {
    private static final float WANDER_RADIUS = 10.0f;

    public Pig(Vector3f position) {
        super("Pig", position, 10.0f, 2.0f);
        this.modelId = "pig_model";
    }

    @Override
    public void updateAI(Camera camera, Map<String, Chunk> loadedChunks, long deltaTime) {
        float dt = deltaTime / 1000.0f;
        long currentTime = System.currentTimeMillis();

        switch (state) {
            case IDLE:
                if (currentTime - lastStateChange > 3000) {
                    if (random.nextFloat() < 0.5f) {
                        state = MobState.WANDERING;
                        pickRandomTarget();
                    }
                    lastStateChange = currentTime;
                }
                velocity.x = 0;
                velocity.z = 0;
                break;

            case WANDERING:
                if (targetPosition != null) {
                    Vector3f direction = new Vector3f(targetPosition).sub(position);
                    direction.y = 0;
                    float distance = direction.length();

                    if (distance < 0.5f || currentTime - lastStateChange > 5000) {
                        state = MobState.IDLE;
                        lastStateChange = currentTime;
                        velocity.x = 0;
                        velocity.z = 0;
                    } else {
                        direction.normalize().mul(moveSpeed);
                        velocity.x = direction.x;
                        velocity.z = direction.z;
                    }
                }
                break;
        }
    }

    @Override
    public void update(float deltaTime, Vector3f playerPosition, Map<String, ?> chunks) {
        // This version is not used anymore, but kept for compatibility
    }

    private void pickRandomTarget() {
        float angle = random.nextFloat() * (float)(Math.PI * 2);
        float distance = random.nextFloat() * WANDER_RADIUS;
        targetPosition = new Vector3f(
            position.x + (float)Math.cos(angle) * distance,
            position.y,
            position.z + (float)Math.sin(angle) * distance
        );
    }

    @Override
    public List<LootDrop> getLootDrops() {
        List<LootDrop> drops = new ArrayList<>();
        drops.add(new LootDrop(Items.COOKED_PORKCHOP, 1, 3));
        return drops;
    }
}
