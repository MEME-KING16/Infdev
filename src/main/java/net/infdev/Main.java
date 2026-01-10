package net.infdev;

import imgui.*;
import imgui.flag.ImGuiCond;
import net.infdev.block.Blocks;
import net.infdev.engine.Engine;
import net.infdev.engine.IAppLogic;
import net.infdev.engine.MouseInput;
import net.infdev.engine.Physics;
import net.infdev.engine.Window;
import net.infdev.engine.graph.Render;
import net.infdev.engine.scene.Scene;
import net.infdev.engine.scene.lights.SceneLights;
import net.infdev.engine.scene.Camera;
import net.infdev.engine.IGuiInstance;
import net.infdev.util.BlockRaycast;
import net.infdev.util.Chunk;

import org.joml.*;

import java.util.*;
import java.util.concurrent.*;

import static org.lwjgl.glfw.GLFW.*;

import java.lang.Math;


public class Main implements IAppLogic, IGuiInstance {
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;
    private static final int VIEW_RADIUS = 10;

    private Engine gameEng;
    private final Map<String, Chunk> loadedChunks = new ConcurrentHashMap<>();
    private final Set<String> loadingChunks = ConcurrentHashMap.newKeySet();
    private final ExecutorService chunkExecutor = Executors.newFixedThreadPool(4);
    private final ConcurrentLinkedQueue<Chunk> readyChunks = new ConcurrentLinkedQueue<>();
    public static Main main;

    public static void main(String[] args) {
        main = new Main();
        main.gameEng = new Engine("Infdev 0.1.0-alpha.1", new Window.WindowOptions(), main);
        main.gameEng.start();
    }

    @Override
    public void cleanup() {
        chunkExecutor.shutdownNow();
    }

    @Override
    public void init(Window window, Scene scene, Render render) {

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(1f);
        scene.setSceneLights(sceneLights);

        Blocks.registerBlocks(scene);
        
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        // SkyBox skyBox = new SkyBox("models/skybox/skybox.obj", scene.getTextureCache());
        // skyBox.getSkyBoxEntity().setScale(50);
        // scene.setSkyBox(skyBox);

        scene.getCamera().moveUp(90f);
    }

    @Override
    public void drawGui() {
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        //ImGui.showDemoWindow();
        ImGui.endFrame();
        ImGui.render();
    }

    @Override
    public boolean handleGuiInput(Scene scene, Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        MouseInput mouseInput = window.getMouseInput();
        Vector2f mousePos = mouseInput.getCurrentPos();
        imGuiIO.addMousePosEvent(mousePos.x, mousePos.y);
        imGuiIO.addMouseButtonEvent(0, mouseInput.isLeftButtonPressed());
        imGuiIO.addMouseButtonEvent(1, mouseInput.isRightButtonPressed());

        return imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
    }


    @Override
    public void input(Window window, Scene scene, long diffTimeMillis, boolean inputConsumed) {
        float move = diffTimeMillis * MOVEMENT_SPEED;
        Camera camera = scene.getCamera();
        Vector3f oldPos = new Vector3f(camera.getPosition());

        if (window.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForwardFlat(move);
            if (Physics.checkCollision(camera.getPosition())) {
                camera.getPosition().set(oldPos);
            }
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackwardsFlat(move);
            if (Physics.checkCollision(camera.getPosition())) {
                camera.getPosition().set(oldPos);
            }
        }
        if (window.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeftFlat(move);
            if (Physics.checkCollision(camera.getPosition())) {
                camera.getPosition().set(oldPos);
            }
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRightFlat(move);
            if (Physics.checkCollision(camera.getPosition())) {
                camera.getPosition().set(oldPos);
            }
        }
        // if (window.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
        //     camera.moveDownFlat(move);
        //     if (Physics.checkCollision(camera.getPosition())) {
        //         camera.getPosition().set(oldPos);
        //     }
        // }
        if (window.isKeyPressed(GLFW_KEY_SPACE)) {
            scene.getPhysics().resetVelocity();
            scene.getPhysics().changeVelocity(0.15f);
            if (Physics.checkCollision(camera.getPosition())) {
                camera.getPosition().set(oldPos);
            }
        }

        oldPos.set(camera.getPosition());

        MouseInput mouseInput = window.getMouseInput();
        if (mouseInput.isRightButtonPressed() && !inputConsumed) {
            Vector3f camPos = camera.getPosition();
            Vector3f camDir = camera.getViewMatrix().positiveZ(new Vector3f()).negate();
            
            BlockRaycast.BlockHitResult result = BlockRaycast.raycast(
                camPos, camDir, loadedChunks, 5.0f
            );
            
            if (result.hit) {
                
                int chunkX = (int) Math.floor((double) result.previousBlockPos.x / Chunk.CHUNK_SIZE);
                int chunkZ = (int) Math.floor((double) result.previousBlockPos.z / Chunk.CHUNK_SIZE);
                String key = chunkX + "_" + chunkZ;
                
                Chunk c = loadedChunks.get(key);
                if (c != null) {
                    int localX = result.previousBlockPos.x - (chunkX * Chunk.CHUNK_SIZE);
                    int localZ = result.previousBlockPos.z - (chunkZ * Chunk.CHUNK_SIZE);
                    
                    c.setBlock(localX, result.previousBlockPos.y, localZ, Blocks.STONE.getId());
                    c.removeFromScene(scene);
                    c.rebuildMesh();
                    c.uploadToScene(scene);
                    
                } else {
                    System.out.println("CHUNK NOT FOUND: " + key);
                }
            }
        }

        if (mouseInput.isLeftButtonPressed() && !inputConsumed) {
            Vector3f camPos = camera.getPosition();
            Vector3f camDir = camera.getViewMatrix().positiveZ(new Vector3f()).negate();
            
            BlockRaycast.BlockHitResult result = BlockRaycast.raycast(
                camPos, camDir, loadedChunks, 5.0f
            );
            
            if (result.hit) {
                
                int chunkX = (int) Math.floor((double) result.blockPos.x / Chunk.CHUNK_SIZE);
                int chunkZ = (int) Math.floor((double) result.blockPos.z / Chunk.CHUNK_SIZE);
                String key = chunkX + "_" + chunkZ;
                
                Chunk c = loadedChunks.get(key);
                if (c != null) {
                    int localX = result.blockPos.x - (chunkX * Chunk.CHUNK_SIZE);
                    int localZ = result.blockPos.z - (chunkZ * Chunk.CHUNK_SIZE);
                    
                    c.setBlock(localX, result.blockPos.y, localZ, Blocks.AIR.getId());
                    c.removeFromScene(scene);
                    c.rebuildMesh();
                    c.uploadToScene(scene);
                    
                } else {
                    System.out.println("CHUNK NOT FOUND: " + key);
                }
            }
        }
        
        Vector2f displVec = mouseInput.getDisplVec();
        camera.addRotation((float) Math.toRadians(-displVec.x * MOUSE_SENSITIVITY), (float) Math.toRadians(-displVec.y * MOUSE_SENSITIVITY));
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMillis) {
        updateChunks(scene);
        Chunk c;
        while ((c = readyChunks.poll()) != null) {
            String key = c.getChunkX() + "_" + c.getChunkZ();
            loadedChunks.put(key, c);
            c.buildMesh();
            c.uploadToScene(scene);
        }
        updatePhysics(scene);
        //logInfo(scene);
    }

    private void logInfo(Scene scene) {
        float fps = 1000f / gameEng.getDeltaTime();
        System.out.printf(
            "\r[FPS: %.1f] [Chunks: %d] [Camera: (%.2f, %.2f, %.2f)]",
            fps,
            loadedChunks.size(),
            scene.getCamera().getPosition().x,
            scene.getCamera().getPosition().y,
            scene.getCamera().getPosition().z
        );
        System.out.flush();

    }


    public void updatePhysics(Scene scene) {
        scene.getPhysics().applyPhysics(gameEng.getDeltaTime(), scene.getCamera());
    }

    private void updateChunks(Scene scene) {
        Vector3f pos = scene.getCamera().getPosition();
        int playerChunkX = (int)Math.floor(pos.x / Chunk.CHUNK_SIZE);
        int playerChunkZ = (int)Math.floor(pos.z / Chunk.CHUNK_SIZE);
        for (int dz = -VIEW_RADIUS; dz <= VIEW_RADIUS; dz++) {
            for (int dx = -VIEW_RADIUS; dx <= VIEW_RADIUS; dx++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                String key = cx + "_" + cz;
                if (!loadedChunks.containsKey(key) && !loadingChunks.contains(key)) {
                    loadingChunks.add(key);
                    chunkExecutor.submit(() -> {
                        Chunk c = new Chunk(cx, cz);
                        c.buildData();
                        readyChunks.add(c);
                        loadingChunks.remove(key);
                    });
                }
            }
        }
        unloadFar(scene, playerChunkX, playerChunkZ);
    }

    public Map<String, Chunk> getLoadedChunks() {
        return loadedChunks;
    }

    private void unloadFar(Scene scene, int px, int pz) {
        loadedChunks.entrySet().removeIf(e -> {
            String[] s = e.getKey().split("_");
            int cx = Integer.parseInt(s[0]);
            int cz = Integer.parseInt(s[1]);
            int dx = Math.abs(cx - px);
            int dz = Math.abs(cz - pz);
            if (dx > VIEW_RADIUS + 1 || dz > VIEW_RADIUS + 1) {
                e.getValue().removeFromScene(scene);
                return true;
            }
            return false;
        });
    }
}