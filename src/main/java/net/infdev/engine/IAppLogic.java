package net.infdev.engine;

import net.infdev.engine.graph.Render;
import net.infdev.engine.scene.Scene;

public interface IAppLogic {
	void cleanup();
	
	void init(Window window, Scene scene, Render render);

	void input(Window window, Scene scene, long diffTimeMillis);

	void update(Window window, Scene scene, long diffTimeMillis);
}