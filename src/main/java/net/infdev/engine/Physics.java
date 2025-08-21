package net.infdev.engine;

import org.joml.Vector3f;

public class Physics {
	private float gravity = 0;//-9.8F;
    private float velocityY = 0f;

	public void applyPhysics(float delta, Vector3f cameraPos, Vector3f cameraUp, float cameraSpeed) {
		velocityY += gravity * delta;
        System.out.println("velocityY=" + velocityY + " pos=" + cameraPos);
        cameraPos.add(new Vector3f(cameraUp).mul(velocityY * delta));
	}

    public void resetVelocity() {
        velocityY = 0f;
    }
}