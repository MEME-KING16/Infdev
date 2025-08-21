package net.infdev.engine.scene;

import net.infdev.engine.Renderer;

public class Scene {
	public Renderer renderer;

	public Scene() {
		renderer = new Renderer();
		
		renderer.init();
	}

	public void cleanup() { }
}