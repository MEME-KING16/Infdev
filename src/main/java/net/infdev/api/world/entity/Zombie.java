package net.infdev.api.world.entity;

import net.infdev.engine.scene.Camera;
import net.infdev.util.Chunk;
import net.infdev.util.Pathfinding;
import org.joml.Vector3f;
import java.util.List;
import java.util.Map;

public class Zombie extends Mob {
    private static final float DETECTION_RANGE = 15.0f;
    private static final float ATTACK_RANGE = 2.0f;
    private Pathfinding pathfinder;

    public Zombie(Vector3f position) {
        super("Zombie", position, 20.0f, 3.5f);
        this.pathfinder = new Pathfinding();
        this.modelId = "zombie_model";
    }

    @Override
    public void updateAI(Camera camera, Map<String, Chunk> loadedChunks, long deltaTime) {
        Vector3f playerPosition = camera.getPosition();

        // Calculate distance to player
        Vector3f toPlayer = new Vector3f(playerPosition).sub(position);
        float distanceToPlayer = toPlayer.length();

        // Decide state based on player distance
        if (distanceToPlayer < DETECTION_RANGE) {
            state = MobState.FOLLOWING_PLAYER;
        } else if (state == MobState.FOLLOWING_PLAYER) {
            state = MobState.IDLE;
            currentPath = null;
        }

        switch (state) {
            case IDLE:
                velocity.x = 0;
                velocity.z = 0;
                break;

            case FOLLOWING_PLAYER:
                if (distanceToPlayer < ATTACK_RANGE) {
                    // In attack range, stop moving
                    velocity.x = 0;
                    velocity.z = 0;
                    // TODO: Deal damage to player
                } else {
                    // Move towards player
                    Vector3f direction = new Vector3f(toPlayer);
                    direction.y = 0;
                    direction.normalize().mul(moveSpeed);
                    velocity.x = direction.x;
                    velocity.z = direction.z;
                }
                break;
        }
    }

    @Override
    public void update(float deltaTime, Vector3f playerPosition, Map<String, ?> chunks) {
        // This version is not used anymore, but kept for compatibility
    }
}
