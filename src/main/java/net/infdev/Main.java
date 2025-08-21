package net.infdev;

import org.lwjgl.*;
import org.lwjgl.opengl.*;

import net.infdev.block.Block;
import net.infdev.block.Blocks;
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

public class Main implements IAppLogic {

    public static void main(String[] args) {
        Main main = new Main();
        Engine gameEng = new Engine("Infdev 0.1.0-alpha.1", new Window.WindowOptions(), main);
        Blocks.registerBlocks();
        
        gameEng.start();
        

    }

    @Override
    public void cleanup() {
        // Nothing to be done yet
    }

    @Override
    public void init(Window window, Scene scene, Render render) {
        
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

