package net.infdev.util;

import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Map;

public class BlockRaycast {
    
    public static BlockHitResult raycast(Vector3f camPos, Vector3f camDir, Map<String, Chunk> chunks, float maxDist) {
        Vector3f pos = new Vector3f(camPos);
        
        if (Math.abs(camDir.x) < 0.0001f) camDir.x = 0.0001f;
        if (Math.abs(camDir.y) < 0.0001f) camDir.y = 0.0001f;
        if (Math.abs(camDir.z) < 0.0001f) camDir.z = 0.0001f;
        
        int stepX = camDir.x > 0 ? 1 : -1;
        int stepY = camDir.y > 0 ? 1 : -1;
        int stepZ = camDir.z > 0 ? 1 : -1;
        
        float tDeltaX = Math.abs(1.0f / camDir.x);
        float tDeltaY = Math.abs(1.0f / camDir.y);
        float tDeltaZ = Math.abs(1.0f / camDir.z);
        
        int x = (int) Math.floor(pos.x);
        int y = (int) Math.floor(pos.y);
        int z = (int) Math.floor(pos.z);
        
        float tMaxX = tDeltaX * (stepX > 0 ? (x + 1 - pos.x) : (pos.x - x));
        float tMaxY = tDeltaY * (stepY > 0 ? (y + 1 - pos.y) : (pos.y - y));
        float tMaxZ = tDeltaZ * (stepZ > 0 ? (z + 1 - pos.z) : (pos.z - z));
        
        float dist = 0;
        int hitFace = 0;
        Vector3i previousBlockPos = new Vector3i(x, y, z);
        
        while (dist < maxDist) {
            if (isBlockSolid(x, y, z, chunks)) {
                Vector3i blockPos = new Vector3i(x, y, z);
                Vector3i faceNormal = getFaceNormal(hitFace, stepX, stepY, stepZ);
                return new BlockHitResult(blockPos, faceNormal, true, previousBlockPos);
            }

            previousBlockPos = new Vector3i(x, y, z);
            
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    dist = tMaxX;
                    tMaxX += tDeltaX;
                    hitFace = 0;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                    hitFace = 2;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    dist = tMaxY;
                    tMaxY += tDeltaY;
                    hitFace = 1;
                } else {
                    z += stepZ;
                    dist = tMaxZ;
                    tMaxZ += tDeltaZ;
                    hitFace = 2;
                }
            }
        }
        
        return new BlockHitResult(null, null, false, null);
    }
    
    private static boolean isBlockSolid(int x, int y, int z, Map<String, Chunk> chunks) {
        int chunkX = (int) Math.floor((double) x / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor((double) z / Chunk.CHUNK_SIZE);
        String key = chunkX + "_" + chunkZ;
        
        Chunk chunk = chunks.get(key);
        if (chunk == null) return false;
        
        int localX = x - (chunkX * Chunk.CHUNK_SIZE);
        int localZ = z - (chunkZ * Chunk.CHUNK_SIZE);
        
        if (y < 0 || y >= Chunk.CHUNK_HEIGHT || localX < 0 || localX >= Chunk.CHUNK_SIZE 
            || localZ < 0 || localZ >= Chunk.CHUNK_SIZE) {
            return false;
        }
        
        return chunk.hasBlockAt(localX, y, localZ);
    }
    
    private static Vector3i getFaceNormal(int face, int stepX, int stepY, int stepZ) {
        switch (face) {
            case 0: return new Vector3i(-stepX, 0, 0);
            case 1: return new Vector3i(0, -stepY, 0);
            case 2: return new Vector3i(0, 0, -stepZ);
            default: return new Vector3i(0, 0, 0);
        }
    }


    
    public static class BlockHitResult {
        public final Vector3i blockPos;
        public final Vector3i faceNormal;
        public final boolean hit;
        public final Vector3i previousBlockPos;
        
        public BlockHitResult(Vector3i blockPos, Vector3i faceNormal, boolean hit, Vector3i previousBlockPos) {
            this.blockPos = blockPos;
            this.faceNormal = faceNormal;
            this.hit = hit;
            this.previousBlockPos = previousBlockPos;

        }
    }
}