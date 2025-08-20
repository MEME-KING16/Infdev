package net.infdev;

import org.lwjgl.*;
import org.lwjgl.opengl.*;

import net.infdev.block.Block;
import net.infdev.engine.Engine;
import net.infdev.engine.IAppLogic;
import net.infdev.engine.Renderer;
import net.infdev.engine.Window;
import net.infdev.engine.graph.Render;
import net.infdev.engine.scene.Scene;
import net.infdev.util.WorldGen;

import org.joml.*;

import java.nio.*;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.lang.Math;

// public class Main {
//     private Renderer renderer;
//     private void run() {
//         renderer = new Renderer();
//         renderer.init();
//         renderer.loop();

//         // Cleanup
//         glDeleteVertexArrays(renderer.vao);
//         glDeleteProgram(renderer.shaderProgram);
//         glfwDestroyWindow(renderer.window);
//         glfwTerminate();
//     }

    

//     public static void main(String[] args) {
//         new Main().run();
//         // Register some blocks
//        Registries.BLOCKS.register("stone", new Block("Stone"));
//        Registries.BLOCKS.register("grass", new Block("Grass"));
//
//        // Access later
//        Block stone = Registries.BLOCKS.get("stone");
//     }
// }

public class Main implements IAppLogic {

    public static void main(String[] args) {
        Main main = new Main();
        Engine gameEng = new Engine("Invdev 0.1.0-alpha.1", new Window.WindowOptions(), main);
        gameEng.start();
    }

    @Override
    public void cleanup() {
        // Nothing to be done yet
    }

    @Override
    public void init(Window window, Scene scene, Render render) {
        // Nothing to be done yet
    }

    @Override
    public void input(Window window, Scene scene, long diffTimeMillis) {
        // Nothing to be done yet
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMillis) {
        // Nothing to be done yet
    }
}

