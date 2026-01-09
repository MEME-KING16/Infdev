package net.infdev.engine;


import org.joml.Vector3f;

import net.infdev.Main;
import net.infdev.block.Blocks;
import net.infdev.engine.scene.Camera;
import net.infdev.util.Chunk;

public class Physics {
	private float gravity = -0.000005F; // addde 2 0s
    private float velocityY = 0f;
    // private float cameraSpeed = 0.1f;


    private static final float PLAYER_WIDTH = 0.05f;
    private static final float PLAYER_HEIGHT = 1.8f;

    public static boolean checkCollision(Vector3f pos) {
        int minX = (int)Math.floor(pos.x - PLAYER_WIDTH);
        int maxX = (int)Math.ceil(pos.x + PLAYER_WIDTH);
        int minY = (int)Math.floor(pos.y - PLAYER_HEIGHT);
        int maxY = (int)Math.ceil(pos.y);
        int minZ = (int)Math.floor(pos.z - PLAYER_WIDTH);
        int maxZ = (int)Math.ceil(pos.z + PLAYER_WIDTH);
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isBlockSolid(x, y, z)) {
                        return true;
                    }
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
            //System.out.println("delta=" + delta);

        // apply gravity
        velocityY += gravity * delta;

        // how much to move vertically this frame
        float dy = velocityY * delta;

        // debug log
        System.out.println("velocityY=" + velocityY + " dy=" + dy + " pos=" + camera.getPosition());

        // move camera
        if (dy > 0) {
            camera.moveUp(dy);
        } else if (dy < 0) {
            camera.moveDown(-dy);
        }

        // ground collision check
        if (Physics.checkCollision(camera.getPosition())) {
            camera.moveUp(-dy); // undo movement
            resetVelocity();
        }
    }


    public void resetVelocity() {
        velocityY = 0f;
    }

    public void changeVelocity(float amt) {
        velocityY += amt;
    }
}