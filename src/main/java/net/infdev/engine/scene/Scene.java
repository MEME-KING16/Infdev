package net.infdev.engine.scene;

import net.infdev.engine.Renderer;

public class Scene {
	private Renderer renderer;

    public Scene() {
		renderer = new Renderer();
        renderer.init();
        renderer.loop();
    }

    public void cleanup() {
    }
}