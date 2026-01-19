package net.infdev.engine;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_DEPTH_BITS;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_MAXIMIZED;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_COMPAT_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.concurrent.Callable;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;

public class Window {
	private Callable<Void> resizeFunc;
	private final long windowHandle;
	private int width;
	private int height;
	private MouseInput mouseInput;

	public static class WindowOptions {
		public boolean compatibleProfile;
		public int ups = Engine.TARGET_UPS;
		public int fps;
		public int height;
		public int width;
	}

	public Window(String title, WindowOptions opts, Callable<Void> resizeFunc) {
		this.resizeFunc = resizeFunc;
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
		glfwWindowHint(GLFW_DEPTH_BITS, 24);

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
		if (opts.compatibleProfile) {
			glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
		} else {
			glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
			glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		}

		if (opts.width > 0 && opts.height > 0) {
			this.width = opts.width;
			this.height = opts.height;
		} else {
			glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);
			GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
			width = vidMode.width();
			height = vidMode.height();
		}

		windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
		if (windowHandle == NULL) {
			throw new RuntimeException("Failed to create the GLFW window");
		}

		glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> resized(w, h));

		glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
			keyCallBack(key, action);
		});

		glfwMakeContextCurrent(windowHandle);

		if (opts.fps > 0) {
			glfwSwapInterval(0);
		} else {
			glfwSwapInterval(1);
		}

		glfwShowWindow(windowHandle);

		int[] arrWidth = new int[1];
		int[] arrHeight = new int[1];
		glfwGetFramebufferSize(windowHandle, arrWidth, arrHeight);
		width = arrWidth[0];
		height = arrHeight[0];
		mouseInput = new MouseInput(windowHandle);
		glfwSetWindowFocusCallback(windowHandle, (window, focused) -> {
			mouseInput.setInWindow(focused);
			if (focused) {
				mouseInput.reset();
			}
		});
	}

	public void keyCallBack(int key, int action) {
		if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
			glfwSetWindowShouldClose(windowHandle, true); // We will detect this in the rendering loop
		}
	}

	public MouseInput getMouseInput() {
        return mouseInput;
    }

	public void cleanup() {
		glfwDestroyWindow(windowHandle);
		glfwTerminate();
		GLFWErrorCallback callback = glfwSetErrorCallback(null);
		if (callback != null) {
			callback.free();
		}
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public long getWindowHandle() {
		return windowHandle;
	}
	
	public boolean isKeyPressed(int keyCode) {
		return glfwGetKey(windowHandle, keyCode) == GLFW_PRESS;
	}

	public void pollEvents() {
		glfwPollEvents();
	}

	protected void resized(int width, int height) {
		this.width = width;
		this.height = height;
		try {
			resizeFunc.call();
		} catch (Exception excp) { }
	}

	public void update() {
		glfwSwapBuffers(windowHandle);
	}

	public boolean windowShouldClose() {
		return glfwWindowShouldClose(windowHandle);
	}
}
