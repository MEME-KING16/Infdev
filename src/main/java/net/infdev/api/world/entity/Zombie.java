package net.infdev.api.world.entity;

import net.infdev.Main;
import net.infdev.engine.scene.Camera;
import net.infdev.item.Items;
import net.infdev.util.Chunk;
import net.infdev.util.Pathfinding;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Zombie extends Mob {
    private static final float DETECTION_RANGE = 15.0f;
    private static final float ATTACK_RANGE = 2.0f;
    private static final float ATTACK_DAMAGE = 3.0f;
    private static final long ATTACK_COOLDOWN = 1000; // 1 second between attacks
    private Pathfinding pathfinder;
    private long lastAttackTime = 0;

    public Zombie(Vector3f position) {
        super("Zombie", position, 20.0f, 6.5f, 0.6f, 0.9f);
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

                    // Deal damage to player
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAttackTime >= ATTACK_COOLDOWN) {
                        if (Main.main != null) {
                            Main.main.damagePlayer(ATTACK_DAMAGE);
                        }
                        lastAttackTime = currentTime;
                    }
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

    @Override
    public List<LootDrop> getLootDrops() {
        List<LootDrop> drops = new ArrayList<>();
        drops.add(new LootDrop(Items.ROTTEN_FLESH, 0, 2));
        return drops;
    }
}
