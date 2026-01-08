package net.infdev.engine.graph;

import org.lwjgl.opengl.GL;

import net.infdev.engine.Window;
import net.infdev.engine.scene.Scene;

import static org.lwjgl.opengl.GL11.*;

public class Render {
    private SceneRender sceneRender;
    private SkyBoxRender skyBoxRender;
    private GuiRender guiRender;

    public Render(Window window) {
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        //glEnable(GL_CULL_FACE);
        //glCullFace(GL_BACK);

        //glFrontFace(GL_CCW);
        glEnable(GL_BLEND);
        guiRender = new GuiRender(window);
        sceneRender = new SceneRender();
        skyBoxRender = new SkyBoxRender();
    }

    public void cleanup() {
        sceneRender.cleanup();
        guiRender.cleanup();
    }

    public void render(Window window, Scene scene) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, window.getWidth(), window.getHeight());

        skyBoxRender.render(scene);
        sceneRender.render(scene);
        guiRender.render(scene);
    }

    public void resize(int width, int height) {
        guiRender.resize(width, height);
    }
}