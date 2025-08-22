package net.infdev.engine;

import net.infdev.engine.graph.Render;
import net.infdev.engine.scene.Scene;


// All of the App's logic
public interface IAppLogic {
	void cleanup();

	void init(Window window, Scene scene, Render render);

	void input(Window window, Scene scene, long diffTimeMillis, boolean inputConsumed);

	void update(Window window, Scene scene, long diffTimeMillis);
}