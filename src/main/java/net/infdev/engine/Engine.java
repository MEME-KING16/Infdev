package net.infdev.engine;

import net.infdev.Main;
import net.infdev.engine.graph.Render;
import net.infdev.engine.scene.Scene;

public class Engine {

    public static final int TARGET_UPS = 30;
    public final Long window;
    private Render render;
    private boolean running;
    public Scene scene;
    public Physics physics;
    public int targetFps;
    public int targetUps;

    public Engine(String windowTitle, Window.WindowOptions opts) {
        // window = new Window(windowTitle, opts, () -> {
        //     resize();
        //     return null;
        // });
        targetFps = opts.fps;
        targetUps = opts.ups;
        //render = new Render();
        physics = new Physics();
        scene = new Scene();
        window = scene.renderer.window;
        running = true;
    }

    private void cleanup() {
        render.cleanup();
        scene.cleanup();
    }

    private void resize() { }

    public float deltaUpdate;

	private void run() {
        
    }

	public void start() {
        running = true;
		scene.renderer.loop();
        run();
    }

    public void stop() {
        running = false;
    }
}