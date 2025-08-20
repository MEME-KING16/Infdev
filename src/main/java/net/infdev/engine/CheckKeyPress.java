package net.infdev.engine;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

import org.joml.Vector3f;

public class CheckKeyPress {
    public static void checkKeyPress(long window, Vector3f cameraPos, Vector3f cameraFront,Vector3f cameraUp, float cameraSpeed) {
        			if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)
    			cameraPos.add(new Vector3f(cameraFront).mul(cameraSpeed));

			if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)
				cameraPos.sub(new Vector3f(cameraFront).mul(cameraSpeed));

			if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS)
				cameraPos.sub(new Vector3f(cameraFront).cross(cameraUp).normalize().mul(cameraSpeed));

			if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS)
				cameraPos.add(new Vector3f(cameraFront).cross(cameraUp).normalize().mul(cameraSpeed));
			if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS)
    			cameraPos.add(new Vector3f(cameraUp).mul(cameraSpeed));

			if (glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS)
				cameraPos.sub(new Vector3f(cameraUp).mul(cameraSpeed));
            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS)
                glfwSetWindowShouldClose(window, true);
    }
}
