package net.infdev.util;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Random;

import net.infdev.block.Block;

public class WorldGen {
    private static List<Block> world;
    /**
     * Setup world Gen
     */
    public static void init() {
        world = new ArrayList<>();
        Random rand = new Random();

        int size = 30;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    Block block = new Block();
                    block.registerCube(x, y, z, rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
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
        int size = 30;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    world.get(x+y+z).render(projection, view, mvpLoc, indicesCount, shaderProgram, vao);
                }
            }
        }
    }
}