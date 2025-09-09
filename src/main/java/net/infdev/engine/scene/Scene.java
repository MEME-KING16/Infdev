package net.infdev.engine.scene;

import java.util.HashMap;
import java.util.Map;

import net.infdev.engine.graph.Mesh;
import net.infdev.engine.graph.Model;
import net.infdev.engine.graph.TextureCache;
import net.infdev.engine.scene.lights.SceneLights;
import net.infdev.engine.IGuiInstance;
import net.infdev.engine.Physics;

public class Scene {
    private Map<String, Model> modelMap;
    private Camera camera;
	private TextureCache textureCache;
    private Projection projection;
    private SceneLights sceneLights;
    private IGuiInstance guiInstance;
    private SkyBox skyBox;
    private Physics physics;

    public Scene(int width, int height) {
        modelMap = new HashMap<>();
        projection = new Projection(width, height);
		textureCache = new TextureCache();
        camera = new Camera();
        physics = new Physics();
    }

    public void cleanup() {
        modelMap.values().forEach(Model::cleanup);
    }

    public void resize(int width, int height) {
        projection.updateProjMatrix(width, height);
    }

    public void addEntity(Entity entity) {
        String modelId = entity.getModelId();
        Model model = modelMap.get(modelId);
        if (model == null) {
            throw new RuntimeException("Could not find model [" + modelId + "]");
        }
        model.getEntitiesList().add(entity);
    }

    public void addModel(Model model) {
        modelMap.put(model.getId(), model);
    }

    public Map<String, Model> getModelMap() {
        return modelMap;
    }

    public Projection getProjection() {
        return projection;
    }

	public TextureCache getTextureCache() {
        return textureCache;
    }

    public Camera getCamera() {
        return camera;
    }

    public IGuiInstance getGuiInstance() {
        return guiInstance;
    }

    public SceneLights getSceneLights() {
        return sceneLights;
    }

    public SkyBox getSkyBox() {
        return skyBox;
    }

    public Physics getPhysics() {
        return physics;
    }

    public void setGuiInstance(IGuiInstance guiInstance) {
        this.guiInstance = guiInstance;
    }

    public void setSceneLights(SceneLights sceneLights) {
        this.sceneLights = sceneLights;
    }

    public void setSkyBox(SkyBox skyBox) {
        this.skyBox = skyBox;
    }
}