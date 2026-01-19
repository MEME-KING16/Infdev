package net.infdev.engine;

import org.joml.Vector3f;
import net.infdev.Main;
import net.infdev.block.Blocks;
import net.infdev.engine.scene.Camera;
import net.infdev.util.Chunk;

public class Physics {
    private float gravity = -0.025F;
    private float velocityY = 0f;
    private boolean isGrounded = false;
    private float fallStartY = 0f;
    private boolean wasFalling = false;

    private static final float PLAYER_WIDTH = 0.3f;
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float SAFE_FALL_DISTANCE = 3.0f; // No damage below this
    
    public static boolean checkCollision(Vector3f pos) {
        float minX = pos.x - PLAYER_WIDTH;
        float maxX = pos.x + PLAYER_WIDTH;
        float minY = pos.y - PLAYER_HEIGHT;
        float maxY = pos.y;
        float minZ = pos.z - PLAYER_WIDTH;
        float maxZ = pos.z + PLAYER_WIDTH;

        int blockMinX = (int)Math.floor(minX);
        int blockMaxX = (int)Math.floor(maxX);
        int blockMinY = (int)Math.floor(minY);
        int blockMaxY = (int)Math.floor(maxY);
        int blockMinZ = (int)Math.floor(minZ);
        int blockMaxZ = (int)Math.floor(maxZ);

        for (int x = blockMinX; x <= blockMaxX; x++) {
            for (int y = blockMinY; y <= blockMaxY; y++) {
                for (int z = blockMinZ; z <= blockMaxZ; z++) {
                    if (isBlockSolid(x, y, z)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    public static boolean checkGroundBelow(Vector3f pos) {
        int minX = (int)Math.floor(pos.x - PLAYER_WIDTH);
        int maxX = (int)Math.ceil(pos.x + PLAYER_WIDTH);
        int checkY = (int)Math.floor(pos.y - PLAYER_HEIGHT - 0.1f);
        int minZ = (int)Math.floor(pos.z - PLAYER_WIDTH);
        int maxZ = (int)Math.ceil(pos.z + PLAYER_WIDTH);
        
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (isBlockSolid(x, checkY, z)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static boolean isBlockSolid(int x, int y, int z) {
        int chunkX = (int)Math.floor((double)x / Chunk.CHUNK_SIZE);
        int chunkZ = (int)Math.floor((double)z / Chunk.CHUNK_SIZE);
        String key = chunkX + "_" + chunkZ;
        
        Chunk chunk = Main.main.getLoadedChunks().get(key);
        if (chunk == null) return true;
        
        int localX = x - (chunkX * Chunk.CHUNK_SIZE);
        int localZ = z - (chunkZ * Chunk.CHUNK_SIZE);
        
        if (localX < 0 || localX >= Chunk.CHUNK_SIZE || 
            localZ < 0 || localZ >= Chunk.CHUNK_SIZE ||
            y < 0 || y >= Chunk.CHUNK_HEIGHT) {
            return true;
        }
        
        int blockId = chunk.getBlock(localX, y, localZ);
        return blockId != Blocks.AIR.getId();
    }
    
    public void applyPhysics(float delta, Camera camera) {
        if (isGrounded && !checkGroundBelow(camera.getPosition())) {
            isGrounded = false;
            fallStartY = camera.getPosition().y;
            wasFalling = true;
        }

        if (!isGrounded) {
            velocityY += gravity;
        }

        float dy = velocityY;
        Vector3f pos = camera.getPosition();

        // Apply Y movement
        float oldY = pos.y;
        pos.y += dy;

        // Check collision after Y movement
        if (Physics.checkCollision(pos)) {
            pos.y = oldY;

            // Calculate fall damage if landing
            if (dy < 0 && wasFalling) {
                float fallDistance = fallStartY - pos.y;
                if (fallDistance > SAFE_FALL_DISTANCE) {
                    float damage = (fallDistance - SAFE_FALL_DISTANCE);
                    if (Main.main != null) {
                        Main.main.damagePlayer(damage);
                    }
                }
                wasFalling = false;
            }

            resetVelocity();
            if (dy < 0) {
                isGrounded = true;
            }
        } else {
            if (dy < 0) {
                isGrounded = false;
            }
        }
    }
    
    public void resetVelocity() {
        velocityY = 0f;
    }
    
    public void changeVelocity(float amt) {
        velocityY += amt;
        isGrounded = false;
    }
}