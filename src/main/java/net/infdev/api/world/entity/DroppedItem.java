package net.infdev.api.world.entity;

import net.infdev.api.world.item.Item;
import net.infdev.engine.scene.Entity;
import org.joml.Vector3f;

public class DroppedItem {
    private Vector3f position;
    private Item item;
    private int count;
    private Entity renderEntity;
    private float bobOffset;
    private long spawnTime;
    private static final float BOB_SPEED = 2.0f;
    private static final float BOB_HEIGHT = 0.1f;
    private static final float PICKUP_RADIUS = 1.5f;

    public DroppedItem(Vector3f position, Item item, int count) {
        this.position = new Vector3f(position);
        this.item = item;
        this.count = count;
        this.bobOffset = 0;
        this.spawnTime = System.currentTimeMillis();
    }

    public void update(float deltaTime) {
        // Bobbing animation
        long timeSinceSpawn = System.currentTimeMillis() - spawnTime;
        bobOffset = (float) Math.sin(timeSinceSpawn / 1000.0 * BOB_SPEED) * BOB_HEIGHT;

        // Update render entity position if it exists
        if (renderEntity != null) {
            renderEntity.setPosition(position.x, position.y + 0.25f + bobOffset, position.z);
            renderEntity.updateModelMatrix();
        }
    }

    public boolean canPickup(Vector3f playerPos) {
        float dx = playerPos.x - position.x;
        float dy = playerPos.y - position.y;
        float dz = playerPos.z - position.z;
        float distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= PICKUP_RADIUS * PICKUP_RADIUS;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Item getItem() {
        return item;
    }

    public int getCount() {
        return count;
    }

    public Entity getRenderEntity() {
        return renderEntity;
    }

    public void setRenderEntity(Entity renderEntity) {
        this.renderEntity = renderEntity;
    }

    public String getModelId() {
        return "dropped_item_model";
    }
}
