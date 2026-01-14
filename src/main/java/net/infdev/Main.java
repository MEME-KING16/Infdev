package net.infdev;

import imgui.*;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import net.infdev.api.world.item.Item;
import net.infdev.api.world.item.ItemStack;
import net.infdev.block.Blocks;
import net.infdev.engine.Engine;
import net.infdev.engine.IAppLogic;
import net.infdev.engine.MouseInput;
import net.infdev.engine.Physics;
import net.infdev.engine.Window;
import net.infdev.engine.graph.Render;
import net.infdev.engine.scene.Scene;
import net.infdev.engine.scene.lights.SceneLights;
import net.infdev.inventory.Inventory;
import net.infdev.item.Items;
import net.infdev.engine.scene.Camera;
import net.infdev.engine.IGuiInstance;
import net.infdev.util.BlockRaycast;
import net.infdev.util.Chunk;

import org.joml.*;

import java.util.*;
import java.util.concurrent.*;

import static org.lwjgl.glfw.GLFW.*;

import java.io.File;
import java.lang.Math;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Main implements IAppLogic, IGuiInstance {
    enum GameState {
        MENU,
        PLAYING,
        PAUSED,
        MODSLIST
    }
    
    private GameState currentState = GameState.MENU;
    
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;
    private static final int VIEW_RADIUS = 6;

    private Engine gameEng;
    private final Map<String, Chunk> loadedChunks = new ConcurrentHashMap<>();
    private final Set<String> loadingChunks = ConcurrentHashMap.newKeySet();
    private final ExecutorService chunkExecutor = Executors.newFixedThreadPool(4);
    private final ConcurrentLinkedQueue<Chunk> readyChunks = new ConcurrentLinkedQueue<>();
    public static Main main;
    public static String windowName = "Infdev 0.1.0-alpha.1";
    private Window window;
    private Inventory inventory = new Inventory();
    private boolean inventoryOpen = false;

    public static void main(String[] args) {
        main = new Main();
        String preload_mod_directory = Os.getHomeDirectory()+"/INFMODSPRE";
        if (!Files.isDirectory(Paths.get(preload_mod_directory))) {
            new File(preload_mod_directory).mkdirs();
        }
        ModLoader.runMods(preload_mod_directory);
        main.gameEng = new Engine(windowName, new Window.WindowOptions(), main);
        String mod_directory = Os.getHomeDirectory()+"/INFMODS";
        if (!Files.isDirectory(Paths.get(mod_directory))) {
            new File(mod_directory).mkdirs();
        }
        ModLoader.runMods(mod_directory);
        main.gameEng.start();
    }

    @Override
    public void cleanup() {
        chunkExecutor.shutdownNow();
    }

    @Override
    public void init(Window window, Scene scene, Render render) {

        this.window = window;

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(1f);
        scene.setSceneLights(sceneLights);
        scene.setGuiInstance(this);

        Blocks.registerBlocks(scene);
        Items.registerItems(scene);
        inventory.addItem(Items.GRASS_BLOCK, 64);
        inventory.addItem(Items.DIRT, 64);
        inventory.addItem(Items.STONE, 64);
        
        // Don't capture cursor in menu
        if (currentState == GameState.PLAYING && !inventoryOpen) {
            glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }

        scene.getCamera().moveUp(90f);
    }

    @Override
    public void drawGui() {
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        
        if (currentState == GameState.MENU) {
            renderMenu();
        } else if (currentState == GameState.MODSLIST) {
            renderModsList();
        } else if (currentState == GameState.PLAYING) {
            if (inventoryOpen) {
                renderInventory();
            } else {
                renderHotbar();
            }
        }
        
        ImGui.endFrame();
        ImGui.render();
    }

    
    private void renderHotbar() {
        ImGuiIO io = ImGui.getIO();
        float windowWidth = io.getDisplaySizeX();
        float windowHeight = io.getDisplaySizeY();
        
        float slotSize = 50;
        float spacing = 2;
        float totalWidth = (slotSize + spacing) * 9 - spacing;
        float startX = (windowWidth - totalWidth) / 2;
        float startY = windowHeight - slotSize - 20;
        
        ImDrawList drawList = ImGui.getBackgroundDrawList();
        
        for (int i = 0; i < 9; i++) {
            float x = startX + i * (slotSize + spacing);
            
            int bgColor = (i == inventory.getSelectedSlot()) ? 
                ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.5f) : 
                ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.5f);
            drawList.addRectFilled(x, startY, x + slotSize, startY + slotSize, bgColor);
            
            int borderColor = (i == inventory.getSelectedSlot()) ? 
                ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) : 
                ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
            drawList.addRect(x, startY, x + slotSize, startY + slotSize, borderColor, 0, 0, 2.0f);
            
            ItemStack item = inventory.getHotbarSlot(i);
            if (!item.isEmpty()) {
                String itemName = item.getItem().getName();
                
                ImGui.setCursorScreenPos(x + 5, startY + 5);
                ImGui.setWindowFontScale(0.6f);
                
                ImGui.setCursorScreenPos(x + 6, startY + 6);
                ImGui.pushStyleColor(ImGuiCol.Text, 0.0f, 0.0f, 0.0f, 1.0f);
                ImGui.text(itemName);
                ImGui.popStyleColor();
                
                ImGui.setCursorScreenPos(x + 5, startY + 5);
                ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
                ImGui.text(itemName);
                ImGui.popStyleColor();
                
                ImGui.setWindowFontScale(1.0f);
                
                String text = String.valueOf(item.getCount());
                float textX = x + slotSize - ImGui.calcTextSize(text).x - 5;
                float textY = startY + slotSize - ImGui.getFont().getFontSize() - 5;
                
                drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
                drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
            }
        }
    }


    private void renderInventory() {
        ImGuiIO io = ImGui.getIO();
        float windowWidth = io.getDisplaySizeX();
        float windowHeight = io.getDisplaySizeY();
        
        float slotSize = 50;
        float spacing = 2;
        float invWidth = (slotSize + spacing) * 9 - spacing;
        float invHeight = (slotSize + spacing) * 4 - spacing + 20;
        
        float startX = (windowWidth - invWidth) / 2;
        float startY = (windowHeight - invHeight) / 2;
        
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.0f, 0.0f, 0.0f, 0.7f);
        
        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                    ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar;
        
        ImGui.begin("InventoryBG", flags);
        ImDrawList drawList = ImGui.getWindowDrawList();
        
        String title = "Inventory";
        float titleX = startX;
        float titleY = startY - 30;
        drawList.addText(titleX, titleY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), title);
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                float x = startX + col * (slotSize + spacing);
                float y = startY + row * (slotSize + spacing);
                
                drawList.addRectFilled(x, y, x + slotSize, y + slotSize, 
                                    ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f));
                drawList.addRect(x, y, x + slotSize, y + slotSize, 
                            ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f), 0, 0, 1.0f);
                
                ItemStack item = inventory.getInventorySlot(index);
                if (!item.isEmpty()) {
                    String text = String.valueOf(item.getCount());
                    float textX = x + slotSize - ImGui.calcTextSize(text).x - 5;
                    float textY = y + slotSize - ImGui.getFont().getFontSize() - 5;
                    drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
                    drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
                }
            }
        }
        
        float hotbarY = startY + 3 * (slotSize + spacing) + 10;
        
        for (int i = 0; i < 9; i++) {
            float x = startX + i * (slotSize + spacing);
            
            int bgColor = (i == inventory.getSelectedSlot()) ? 
                ImGui.getColorU32(0.4f, 0.4f, 0.4f, 0.9f) : 
                ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f);
            drawList.addRectFilled(x, hotbarY, x + slotSize, hotbarY + slotSize, bgColor);
            
            int borderColor = (i == inventory.getSelectedSlot()) ? 
                ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) : 
                ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
            drawList.addRect(x, hotbarY, x + slotSize, hotbarY + slotSize, borderColor, 0, 0, 2.0f);
            
            ItemStack item = inventory.getHotbarSlot(i);
            if (!item.isEmpty()) {
                String text = String.valueOf(item.getCount());
                float textX = x + slotSize - ImGui.calcTextSize(text).x - 5;
                float textY = hotbarY + slotSize - ImGui.getFont().getFontSize() - 5;
                drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
                drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
            }
        }
        
        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }
    
    private void renderMenu() {
        ImGuiIO io = ImGui.getIO();
        float windowWidth = io.getDisplaySizeX();
        float windowHeight = io.getDisplaySizeY();
        
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);
        
        int windowFlags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize | 
                        ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar | 
                        ImGuiWindowFlags.NoSavedSettings;
        
        ImGui.begin("MainMenu", windowFlags);
        
        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(0, 0, windowWidth, windowHeight, 
                            ImGui.getColorU32(0.15f, 0.15f, 0.15f, 1.0f));
        
        float buttonWidth = 400;
        float buttonHeight = 40;
        float spacing = 20;
        float titleHeight = 100;
        
        float totalHeight = titleHeight + (buttonHeight * 3) + (spacing * 2);
        float startY = (windowHeight - totalHeight) / 2;
        float centerX = (windowWidth - buttonWidth) / 2;
        
        ImGui.setCursorPos(0, startY);
        
        String title = "INFDEV";
        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        float titleWidth = ImGui.calcTextSize(title).x * 2;
        ImGui.setCursorPos((windowWidth - titleWidth) / 2, startY);
        
        float titleScale = 4.0f;
        float titleTextWidth = ImGui.calcTextSize(title).x * titleScale;
        float titleX = (windowWidth - titleTextWidth) / 2;
        
        ImGui.setWindowFontScale(titleScale);
        
        ImGui.setCursorPos(titleX + 4, startY + 4);
        ImGui.pushStyleColor(ImGuiCol.Text, 0.0f, 0.0f, 0.0f, 0.25f);
        ImGui.text(title);
        ImGui.popStyleColor();
        
        ImGui.setCursorPos(titleX, startY);
        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        ImGui.text(title);
        ImGui.popStyleColor();
        
        ImGui.setWindowFontScale(1.0f);
        
        ImGui.popStyleColor();
        
        float buttonY = startY + titleHeight;
        
        ImGui.setCursorPos(centerX, buttonY);
        
        ImGui.pushStyleColor(ImGuiCol.Button, 0.0f, 0.0f, 0.0f, 0.5f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.3f, 0.8f, 0.8f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.2f, 0.6f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 2.0f);
        ImGui.pushStyleColor(ImGuiCol.Border, 0.6f, 0.6f, 0.6f, 1.0f);
        
        if (ImGui.button("Singleplayer", buttonWidth, buttonHeight)) {
            startNewGame();
        }
        
        ImGui.setCursorPos(centerX, buttonY + buttonHeight + spacing);
        if (ImGui.button("Multiplayer", buttonWidth, buttonHeight)) {
            // TODO: multiplayer
            startNewGame();
        }
        
        if (ModLoader.getModsLoaded() != 0) {
            ImGui.setCursorPos(centerX, buttonY + (buttonHeight + spacing) * 2);
            if (ImGui.button("Mods", buttonWidth, buttonHeight)) {
                currentState = GameState.MODSLIST;
            }
        }

        ImGui.setCursorPos(centerX, buttonY + (buttonHeight + spacing) * (ModLoader.getModsLoaded() == 0 ? 2 : 3));
        if (ImGui.button("Quit Game", buttonWidth, buttonHeight)) {
            System.exit(0);
        }
        
        ImGui.popStyleColor(5);
        ImGui.popStyleVar(2);
        
        String version = "Infdev 0.1.0-alpha.1";
        float versionWidth = ImGui.calcTextSize(version).x;
        ImGui.setCursorPos(windowWidth - versionWidth - 10, windowHeight - 30);
        ImGui.pushStyleColor(ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
        ImGui.text(version);
        ImGui.popStyleColor();
        
        ImGui.end();
        ImGui.popStyleVar(3);
    }

    private void renderModsList() {
        ImGui.begin("Mods List");
        
        ImGui.text("Mods Loaded: " + ModLoader.getModsLoaded());
        ImGui.text("Mods: " + ModLoader.getMods());
        
        ImGui.end();
    }
    
    private void startNewGame() {
        currentState = GameState.PLAYING;
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
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
        if (currentState != GameState.PLAYING) {
            return;
        }
        
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
            ItemStack selected = inventory.getSelectedItem();
            if (!selected.isEmpty()) {
                Vector3f camPos = camera.getPosition();
                Vector3f camDir = camera.getViewMatrix().positiveZ(new Vector3f()).negate();
                
                BlockRaycast.BlockHitResult result = BlockRaycast.raycast(camPos, camDir, loadedChunks, 5.0f);
                
                if (result.hit) {
                    int chunkX = (int) Math.floor((double) result.previousBlockPos.x / Chunk.CHUNK_SIZE);
                    int chunkZ = (int) Math.floor((double) result.previousBlockPos.z / Chunk.CHUNK_SIZE);
                    String key = chunkX + "_" + chunkZ;
                    
                    Chunk c = loadedChunks.get(key);
                    if (c != null) {
                        int localX = result.previousBlockPos.x - (chunkX * Chunk.CHUNK_SIZE);
                        int localZ = result.previousBlockPos.z - (chunkZ * Chunk.CHUNK_SIZE);
                        
                        byte blockId = getBlockIdFromItem(selected.getItem());
                        
                        c.setBlock(localX, result.previousBlockPos.y, localZ, blockId);
                        inventory.removeSelectedItem();
                        c.removeFromScene(scene);
                        c.rebuildMesh();
                        c.uploadToScene(scene);
                    }
                }
            }
        }

        if (mouseInput.isLeftButtonPressed() && !inputConsumed) {
            Vector3f camPos = camera.getPosition();
            Vector3f camDir = camera.getViewMatrix().positiveZ(new Vector3f()).negate();
            
            BlockRaycast.BlockHitResult result = BlockRaycast.raycast(camPos, camDir, loadedChunks, 5.0f);
            
            if (result.hit) {
                int chunkX = (int) Math.floor((double) result.blockPos.x / Chunk.CHUNK_SIZE);
                int chunkZ = (int) Math.floor((double) result.blockPos.z / Chunk.CHUNK_SIZE);
                String key = chunkX + "_" + chunkZ;
                
                Chunk c = loadedChunks.get(key);
                if (c != null) {
                    int localX = result.blockPos.x - (chunkX * Chunk.CHUNK_SIZE);
                    int localZ = result.blockPos.z - (chunkZ * Chunk.CHUNK_SIZE);
                    
                    byte brokenBlock = c.getBlock(localX, result.blockPos.y, localZ);
                    if (brokenBlock != Blocks.AIR.getId()) {
                        Item item = getItemFromBlock(brokenBlock);
                        inventory.addItem(item, 1);
                    }
                    
                    c.setBlock(localX, result.blockPos.y, localZ, Blocks.AIR.getId());
                    c.removeFromScene(scene);
                    c.rebuildMesh();
                    c.uploadToScene(scene);
                }
            }
        }

        if (window.isKeyPressed(GLFW_KEY_1)) inventory.setSelectedSlot(0);
        if (window.isKeyPressed(GLFW_KEY_2)) inventory.setSelectedSlot(1);
        if (window.isKeyPressed(GLFW_KEY_3)) inventory.setSelectedSlot(2);
        if (window.isKeyPressed(GLFW_KEY_4)) inventory.setSelectedSlot(3);
        if (window.isKeyPressed(GLFW_KEY_5)) inventory.setSelectedSlot(4);
        if (window.isKeyPressed(GLFW_KEY_6)) inventory.setSelectedSlot(5);
        if (window.isKeyPressed(GLFW_KEY_7)) inventory.setSelectedSlot(6);
        if (window.isKeyPressed(GLFW_KEY_8)) inventory.setSelectedSlot(7);
        if (window.isKeyPressed(GLFW_KEY_9)) inventory.setSelectedSlot(8);
        if (window.isKeyPressed(GLFW_KEY_E)) {
            inventoryOpen = !inventoryOpen;
            if (inventoryOpen) {
                glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            }
        }
        
        Vector2f displVec = mouseInput.getDisplVec();
        camera.addRotation((float) Math.toRadians(-displVec.x * MOUSE_SENSITIVITY), (float) Math.toRadians(-displVec.y * MOUSE_SENSITIVITY));
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMillis) {
        if (currentState == GameState.PLAYING) {
            updateChunks(scene);
            Chunk c;
            while ((c = readyChunks.poll()) != null) {
                String key = c.getChunkX() + "_" + c.getChunkZ();
                loadedChunks.put(key, c);
                c.buildMesh();
                c.uploadToScene(scene);
            }
            updatePhysics(scene);
        }
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

    public static void setWindowName(String windowName) {
        Main.windowName = windowName;
    }

    private byte getBlockIdFromItem(Item item) {
        if (item.equals(Items.GRASS_BLOCK)) return Blocks.GRASS.getId();
        if (item.equals(Items.DIRT)) return Blocks.DIRT.getId();
        if (item.equals(Items.STONE)) return Blocks.STONE.getId();
        if (item.equals(Items.SAND)) return Blocks.SAND.getId();
        return Blocks.AIR.getId();
    }

    private Item getItemFromBlock(byte blockId) {
        if (blockId == Blocks.GRASS.getId()) return Items.GRASS_BLOCK;
        if (blockId == Blocks.DIRT.getId()) return Items.DIRT;
        if (blockId == Blocks.STONE.getId()) return Items.STONE;
        if (blockId == Blocks.SAND.getId()) return Items.SAND;
        return Items.AIR;
    }
}