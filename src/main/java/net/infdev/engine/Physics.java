package net.infdev.engine;

import org.joml.Vector3f;

import net.infdev.engine.scene.Camera;

public class Physics {
	private float gravity = 0;//-0.005F;
    private float velocityY = 0f;
    private float cameraSpeed = 0.1f;

    public void applyPhysics(float delta, Camera camera) {
            System.out.println("delta=" + delta);

        // apply gravity
        velocityY += gravity * delta;

        // how much to move vertically this frame
        float dy = velocityY * delta;

        // debug log
        //System.out.println("velocityY=" + velocityY + " dy=" + dy + " pos=" + camera.getPosition());

        // move camera
        if (dy > 0) {
            camera.moveUp(dy);
        } else if (dy < 0) {
            camera.moveDown(-dy);
        }

        // ground collision check
        // if (camera.getPosition().y <= 0f) {
        //     camera.setPosition(camera.getPosition().x, 0f, camera.getPosition().z);
        //     resetVelocity();
        // }
    }


    public void resetVelocity() {
        velocityY = 0f;
    }

    public void changeVelocity(float amt) {
        velocityY += amt;
    }
}