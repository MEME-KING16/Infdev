package net.infdev.api.world.entity;

import net.infdev.api.world.item.Item;
import net.infdev.engine.graph.*;
import net.infdev.engine.scene.Entity;
import net.infdev.engine.scene.Scene;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class DroppedItemManager {
    private final List<DroppedItem> droppedItems;
    private Scene scene;

    public DroppedItemManager() {
        this.droppedItems = new ArrayList<>();
    }

    public void setScene(Scene scene) {
        this.scene = scene;
        createDroppedItemModel();
    }

    private void createDroppedItemModel() {
        // Create a small cube model for dropped items
        float size = 0.25f;
        float hs = size / 2;

        float[] positions = {
            // Front, Back, Top, Bottom, Right, Left faces
            -hs,-hs,hs, hs,-hs,hs, hs,hs,hs, -hs,hs,hs,
            hs,-hs,-hs, -hs,-hs,-hs, -hs,hs,-hs, hs,hs,-hs,
            -hs,hs,hs, hs,hs,hs, hs,hs,-hs, -hs,hs,-hs,
            -hs,-hs,-hs, hs,-hs,-hs, hs,-hs,hs, -hs,-hs,hs,
            hs,-hs,hs, hs,-hs,-hs, hs,hs,-hs, hs,hs,hs,
            -hs,-hs,-hs, -hs,-hs,hs, -hs,hs,hs, -hs,hs,-hs
        };

        float[] normals = new float[72];
        float[] texCoords = new float[48];

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 4; j++) {
                int idx = i * 4 + j;
                if (i == 0) { normals[idx*3] = 0; normals[idx*3+1] = 0; normals[idx*3+2] = 1; }
                else if (i == 1) { normals[idx*3] = 0; normals[idx*3+1] = 0; normals[idx*3+2] = -1; }
                else if (i == 2) { normals[idx*3] = 0; normals[idx*3+1] = 1; normals[idx*3+2] = 0; }
                else if (i == 3) { normals[idx*3] = 0; normals[idx*3+1] = -1; normals[idx*3+2] = 0; }
                else if (i == 4) { normals[idx*3] = 1; normals[idx*3+1] = 0; normals[idx*3+2] = 0; }
                else { normals[idx*3] = -1; normals[idx*3+1] = 0; normals[idx*3+2] = 0; }

                texCoords[idx*2] = (j == 1 || j == 2) ? 1.0f : 0.0f;
                texCoords[idx*2+1] = (j >= 2) ? 1.0f : 0.0f;
            }
        }

        int[] indices = new int[36];
        for (int i = 0; i < 6; i++) {
            int base = i * 4;
            indices[i*6] = base; indices[i*6+1] = base+1; indices[i*6+2] = base+2;
            indices[i*6+3] = base; indices[i*6+4] = base+2; indices[i*6+5] = base+3;
        }

        Mesh mesh = new Mesh(positions, normals, texCoords, indices);

        Material material = new Material();
        material.setDiffuseColor(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        material.setAmbientColor(new Vector4f(0.3f, 0.3f, 0.3f, 1.0f));
        material.getMeshList().add(mesh);

        List<Material> materials = new ArrayList<>();
        materials.add(material);

        Model model = new Model("dropped_item_model", materials);
        scene.addModel(model);
    }

    public void spawnItem(Vector3f position, Item item, int count) {
        if (scene == null || item == null || count <= 0) return;

        DroppedItem droppedItem = new DroppedItem(position, item, count);

        // Create render entity
        Entity entity = new Entity("dropped_item_" + System.currentTimeMillis(), droppedItem.getModelId());
        entity.setPosition(position.x, position.y + 0.25f, position.z);
        entity.setScale(1.0f);
        entity.updateModelMatrix();

        // Set texture based on item
        String texturePath = item.getTexturePath();
        if (texturePath != null && scene != null) {
            try {
                // Ensure texture is loaded
                scene.getTextureCache().createTexture(texturePath);

                // Apply texture to the model's material
                Model model = scene.getModelMap().get(droppedItem.getModelId());
                if (model != null && !model.getMaterialList().isEmpty()) {
                    Material material = model.getMaterialList().get(0);
                    material.setTexturePath(texturePath);
                }
            } catch (Exception e) {
                // Texture loading failed, use default color
            }
        }

        droppedItem.setRenderEntity(entity);
        scene.addEntity(entity);
        droppedItems.add(droppedItem);
    }

    public void update(Vector3f playerPosition, float deltaTime) {
        for (int i = droppedItems.size() - 1; i >= 0; i--) {
            DroppedItem item = droppedItems.get(i);
            item.update(deltaTime / 1000.0f);
        }
    }

    public List<DroppedItem> checkPickup(Vector3f playerPosition) {
        List<DroppedItem> pickedUp = new ArrayList<>();

        for (int i = droppedItems.size() - 1; i >= 0; i--) {
            DroppedItem item = droppedItems.get(i);
            if (item.canPickup(playerPosition)) {
                pickedUp.add(item);
                removeItem(item);
                droppedItems.remove(i);
            }
        }

        return pickedUp;
    }

    private void removeItem(DroppedItem item) {
        if (scene != null && item.getRenderEntity() != null) {
            scene.removeEntity(item.getRenderEntity());
        }
    }

    public void cleanup() {
        for (DroppedItem item : droppedItems) {
            removeItem(item);
        }
        droppedItems.clear();
    }

    public List<DroppedItem> getDroppedItems() {
        return droppedItems;
    }
}
