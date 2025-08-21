package net.infdev.util;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Random;

import net.infdev.block.Block;
import net.infdev.block.Blocks;

public class WorldGen {
    private static List<Block> world;
    /**
     * Setup world Gen
     */
    public static void init() {
        world = new ArrayList<>();
        int size = 8;

        float[] stone = {0.5f,0.5f,0.5f};

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    Block block = new Block();

                    if (y < 2) {
                        block.setCubeColor(stone);//Blocks.STONE.getColor());
                    } else if (y == size-1) {
                        block.setCubeColor(Blocks.GRASS.getColor());
                    } else {
                        block.setCubeColor(Blocks.DIRT.getColor());
                        
                    }

                    block.registerCube(x, y, z, x + 1, y + 1, z + 1);
                    
                    world.add(block);
                }
            }
        }
    }

    /**
     * Render The world
     * @param projection
     * @param view
     * @param mvpLoc
     * @param indicesCount
     * @param shaderProgram
     * @param vao
     */
    public static void loop(Matrix4f projection,Matrix4f view, int mvpLoc, int indicesCount, int shaderProgram, int vao) {
       int size = 8;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    int index = (x * size * size) + (y * size) + z;
                    world.get(index).render(projection, view, mvpLoc, indicesCount, shaderProgram, vao);
                }
            }
        }
    }
}