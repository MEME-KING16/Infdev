package net.infdev;

import imgui.*;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import net.infdev.api.world.item.Item;
import net.infdev.api.world.item.ItemStack;
import net.infdev.api.world.block.Block;
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
import net.infdev.util.MobRaycast;
import net.infdev.api.world.entity.MobManager;
import net.infdev.api.world.entity.DroppedItemManager;
import net.infdev.api.world.entity.DroppedItem;
import net.infdev.crafting.RecipeManager;

import org.joml.*;

import java.util.*;
import java.util.concurrent.*;
import org.joml.Vector3i;

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
        MODSLIST,
        CRAFTING_TABLE,
        INFO,
        DEATH
    }
    
    private GameState currentState = GameState.MENU;
    
    private static final float MOUSE_SENSITIVITY = 0.1f;
    private static final float MOVEMENT_SPEED = 0.005f;
    private static final int VIEW_RADIUS = 6;
    private static final int UNLOAD_RADIUS = VIEW_RADIUS + 2;
    private static final long UNLOAD_COOLDOWN_MS = 3000;

    private Engine gameEng;
    private final Map<String, Chunk> loadedChunks = new ConcurrentHashMap<>();
    private final Set<String> loadingChunks = ConcurrentHashMap.newKeySet();
    private final ExecutorService chunkExecutor = Executors.newFixedThreadPool(4);
    private final ConcurrentLinkedQueue<Chunk> readyChunks = new ConcurrentLinkedQueue<>();
    private final Map<String, Long> chunkLastSeen = new ConcurrentHashMap<>();
    private final Set<String> dirtyChunks = new HashSet<>();
    private final Map<String, Chunk> pendingRender = new HashMap<>();
    private final Map<String, Long> pendingSince = new HashMap<>();
    private static final long PENDING_RENDER_TIMEOUT_MS = 2500;
    private int lastPlayerChunkX = 0;
    private int lastPlayerChunkZ = 0;
    private Vector3f lastCameraPos = new Vector3f();
    private long lastMoveTime = 0;
    private static final long UNLOAD_MOVE_COOLDOWN_MS = 1000;
    public static Main main;
    public static String windowName = "Infdev 0.1.0-alpha.1";
    private Window window;
    private Inventory inventory = new Inventory();
    private boolean inventoryOpen = false;
    private long lastInvStatusChangeTime = 0;
    private long lastBlockBreakTime = 0;
    private long lastBlockPlaceTime = 0;
    private ItemStack heldItem = null;
    private int heldFromSlot = -1;
    private boolean heldFromHotbar = false;
    private int hoveredSlot = -1;
    private Scene scene;
    private boolean hoveredIsHotbar = false;
    private MobManager mobManager;
    private DroppedItemManager droppedItemManager;
    private long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN = 600; // 600ms attack cooldown (1.67 attacks per second)

    // Player stats
    private float playerHealth = 20.0f;
    private float maxPlayerHealth = 20.0f;
    private float playerHunger = 20.0f;
    private float maxPlayerHunger = 20.0f;
    private long lastHungerDepletion = 0;
    private long lastHealthRegeneration = 0;
    private long lastHungerDamage = 0;
    private Vector3f respawnPosition = new Vector3f(0, 90, 0);
    private long lastJumpTime = 0;
    private static final long JUMP_COOLDOWN_MS = 350;

    // Block breaking progress tracking
    private Vector3i targetBlock = null;
    private float breakingProgress = 0.0f;
    private long lastBreakingUpdateTime = 0;

    // Crafting variables
    private final ItemStack[][] craftingGrid2x2 = new ItemStack[2][2];
    private final ItemStack[][] craftingGrid3x3 = new ItemStack[3][3];
    private ItemStack craftingOutput = new ItemStack();
    private int hoveredCraftingSlot = -1;
    private int hoveredCraftingRow = -1;
    private boolean hoveredIsCraftingOutput = false;
    private Vector3i craftingTablePos = null;

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
        if (mobManager != null) {
            mobManager.cleanup();
        }
        if (droppedItemManager != null) {
            droppedItemManager.cleanup();
        }
    }

    @Override
    public void init(Window window, Scene scene, Render render) {

        this.window = window;
        this.scene = scene;

        SceneLights sceneLights = new SceneLights();
        sceneLights.getAmbientLight().setIntensity(1f);
        scene.setSceneLights(sceneLights);
        scene.setGuiInstance(this);

        Blocks.registerBlocks(scene);
        Items.registerItems(scene);
        RecipeManager.initializeRecipes();

        // Initialize mob system
        mobManager = new MobManager();
        mobManager.setScene(scene);

        // Initialize dropped item system
        droppedItemManager = new DroppedItemManager();
        droppedItemManager.setScene(scene);
        mobManager.setDroppedItemManager(droppedItemManager);

        // Initialize crafting grids
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                craftingGrid2x2[i][j] = new ItemStack();
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                craftingGrid3x3[i][j] = new ItemStack();
            }
        }

        inventory.addItem(Items.GRASS_BLOCK, 64);
        inventory.addItem(Items.DIRT, 64);
        inventory.addItem(Items.STONE, 64);
        inventory.addItem(Items.OAK_LOG, 64);
        inventory.addItem(Items.COOKED_PORKCHOP, 16);
        inventory.addItem(Items.COOKED_BEEF, 16);

        // Don't capture cursor in menu
        if (currentState == GameState.PLAYING && !inventoryOpen) {
            captureMouse();
        }

        setInitialSpawn(scene.getCamera());
    }

    @Override
    public void drawGui() {
        ImGui.newFrame();
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        
        if (currentState == GameState.MENU) {
            renderMenu();
        } else if (currentState == GameState.MODSLIST) {
            renderModsList();
        } else if (currentState == GameState.INFO) {
            renderInfo();
        } else if (currentState == GameState.DEATH) {
            renderDeathScreen();
        } else if (currentState == GameState.PLAYING) {
            if (inventoryOpen) {
                renderInventory();
            } else {
                renderHotbar();
                renderCrosshair();
            }
        } else if (currentState == GameState.CRAFTING_TABLE) {
            renderCraftingTable();
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

        // Render health bar (red hearts) above hotbar
        float heartSize = 16;
        float heartSpacing = 2;
        float healthBarY = startY - heartSize - 10;
        float healthBarX = startX;

        int maxHearts = 10; // Display 10 hearts for 20 HP
        for (int i = 0; i < maxHearts; i++) {
            float x = healthBarX + i * (heartSize + heartSpacing);
            float heartFill = Math.max(0, Math.min(1, (playerHealth - i * 2) / 2.0f));

            // Draw heart background (empty heart)
            drawList.addRectFilled(x, healthBarY, x + heartSize, healthBarY + heartSize,
                ImGui.getColorU32(0.2f, 0.0f, 0.0f, 0.8f));

            // Draw filled portion (red)
            if (heartFill > 0) {
                float fillWidth = heartSize * heartFill;
                drawList.addRectFilled(x, healthBarY, x + fillWidth, healthBarY + heartSize,
                    ImGui.getColorU32(1.0f, 0.0f, 0.0f, 1.0f));
            }

            // Draw heart border
            drawList.addRect(x, healthBarY, x + heartSize, healthBarY + heartSize,
                ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f), 0, 0, 1.0f);
        }

        // Render hunger bar (food icons) above health bar
        float hungerBarY = healthBarY - heartSize - 10;
        float hungerBarX = startX + totalWidth - (maxHearts * (heartSize + heartSpacing));

        for (int i = 0; i < maxHearts; i++) {
            float x = hungerBarX + i * (heartSize + heartSpacing);
            float hungerFill = Math.max(0, Math.min(1, (playerHunger - i * 2) / 2.0f));

            // Draw hunger background (empty food icon)
            drawList.addRectFilled(x, hungerBarY, x + heartSize, hungerBarY + heartSize,
                ImGui.getColorU32(0.2f, 0.15f, 0.0f, 0.8f));

            // Draw filled portion (orange/brown)
            if (hungerFill > 0) {
                float fillWidth = heartSize * hungerFill;
                drawList.addRectFilled(x, hungerBarY, x + fillWidth, hungerBarY + heartSize,
                    ImGui.getColorU32(0.8f, 0.5f, 0.2f, 1.0f));
            }

            // Draw border
            drawList.addRect(x, hungerBarY, x + heartSize, hungerBarY + heartSize,
                ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f), 0, 0, 1.0f);
        }

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
                // Always use 2D texture (3D rendering disabled)
                int textureId = -1;
                String texturePath = item.getItem().getTexturePath();
                if (texturePath != null && scene != null) {
                    try {
                        textureId = scene.getTextureCache().getTexture(texturePath).getTextureId();
                    } catch (Exception e) {
                        // If texture loading fails, skip icon
                    }
                }

                if (textureId != -1) {
                    float iconPadding = 4;
                    float iconSize = slotSize - iconPadding * 2;
                    float iconX = x + iconPadding;
                    float iconY = startY + iconPadding;

                    drawList.addImage(textureId, iconX, iconY, iconX + iconSize, iconY + iconSize);
                }

                // Render item count
                String text = String.valueOf(item.getCount());
                float textX = x + slotSize - ImGui.calcTextSize(text).x - 5;
                float textY = startY + slotSize - ImGui.getFont().getFontSize() - 5;

                drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
                drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
            }
        }

        // Render tooltip for hovered item
        float mouseX = io.getMousePosX();
        float mouseY = io.getMousePosY();
        for (int i = 0; i < 9; i++) {
            float x = startX + i * (slotSize + spacing);
            if (mouseX >= x && mouseX <= x + slotSize && mouseY >= startY && mouseY <= startY + slotSize) {
                ItemStack item = inventory.getHotbarSlot(i);
                if (!item.isEmpty()) {
                    renderTooltip(drawList, item.getItem().getName(), mouseX, mouseY);
                }
                break;
            }
        }
    }

    private void renderCrosshair() {
        ImGuiIO io = ImGui.getIO();
        float centerX = io.getDisplaySizeX() * 0.5f;
        float centerY = io.getDisplaySizeY() * 0.5f;

        float size = 6.0f;
        float thickness = 2.0f;

        ImDrawList drawList = ImGui.getForegroundDrawList();
        int color = ImGui.getColorU32(1.0f, 1.0f, 1.0f, 0.9f);
        int outline = ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.7f);

        drawList.addLine(centerX - size, centerY, centerX + size, centerY, outline, thickness + 1.0f);
        drawList.addLine(centerX, centerY - size, centerX, centerY + size, outline, thickness + 1.0f);
        drawList.addLine(centerX - size, centerY, centerX + size, centerY, color, thickness);
        drawList.addLine(centerX, centerY - size, centerX, centerY + size, color, thickness);
    }


    private void renderInventory() {
        ImGuiIO io = ImGui.getIO();
        float windowWidth = io.getDisplaySizeX();
        float windowHeight = io.getDisplaySizeY();
        float mouseX = io.getMousePosX();
        float mouseY = io.getMousePosY();

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

        hoveredSlot = -1;
        hoveredIsHotbar = false;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                float x = startX + col * (slotSize + spacing);
                float y = startY + row * (slotSize + spacing);

                boolean isHovered = mouseX >= x && mouseX <= x + slotSize &&
                                   mouseY >= y && mouseY <= y + slotSize;

                if (isHovered) {
                    hoveredSlot = index;
                    hoveredIsHotbar = false;
                }

                int bgColor = isHovered ?
                    ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.9f) :
                    ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f);
                drawList.addRectFilled(x, y, x + slotSize, y + slotSize, bgColor);

                int borderColor = isHovered ?
                    ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) :
                    ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
                drawList.addRect(x, y, x + slotSize, y + slotSize, borderColor, 0, 0, 1.0f);

                ItemStack item = inventory.getInventorySlot(index);
                if (!item.isEmpty() && !(heldItem != null && heldFromSlot == index && !heldFromHotbar)) {
                    // Always use 2D texture (3D rendering disabled)
                    int textureId = -1;
                    String texturePath = item.getItem().getTexturePath();
                    if (texturePath != null && scene != null) {
                        try {
                            textureId = scene.getTextureCache().getTexture(texturePath).getTextureId();
                        } catch (Exception e) {
                            // If texture loading fails, skip icon
                        }
                    }

                    if (textureId != -1) {
                        float iconPadding = 4;
                        float iconSize = slotSize - iconPadding * 2;
                        float iconX = x + iconPadding;
                        float iconY = y + iconPadding;

                        drawList.addImage(textureId, iconX, iconY, iconX + iconSize, iconY + iconSize);
                    }

                    // Render item count
                    String text = String.valueOf(item.getCount());
                    float textX = x + slotSize - ImGui.calcTextSize(text).x - 5;
                    float textY = y + slotSize - ImGui.getFont().getFontSize() - 5;
                    drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
                    drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
                }
            }
        }

        // Render 2x2 crafting grid (to the right of inventory)
        float craftingX = startX + invWidth + 30;
        float craftingY = startY;

        hoveredCraftingSlot = -1;
        hoveredCraftingRow = -1;
        hoveredIsCraftingOutput = false;

        String craftTitle = "Crafting";
        drawList.addText(craftingX, craftingY - 25, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), craftTitle);

        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                float x = craftingX + col * (slotSize + spacing);
                float y = craftingY + row * (slotSize + spacing);

                boolean isHovered = mouseX >= x && mouseX <= x + slotSize &&
                                   mouseY >= y && mouseY <= y + slotSize;

                if (isHovered) {
                    hoveredCraftingRow = row;
                    hoveredCraftingSlot = col;
                }

                int bgColor = isHovered ?
                    ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.9f) :
                    ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f);
                drawList.addRectFilled(x, y, x + slotSize, y + slotSize, bgColor);

                int borderColor = isHovered ?
                    ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) :
                    ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
                drawList.addRect(x, y, x + slotSize, y + slotSize, borderColor, 0, 0, 1.0f);

                ItemStack item = craftingGrid2x2[row][col];
                if (!item.isEmpty()) {
                    renderItemInSlot(drawList, item, x, y, slotSize);
                }
            }
        }

        // Render crafting output slot (below the 2x2 grid)
        float craftOutputX = craftingX + (slotSize + spacing) / 2;
        float craftOutputY = craftingY + 2 * (slotSize + spacing) + 10;

        boolean craftOutputHovered = mouseX >= craftOutputX && mouseX <= craftOutputX + slotSize &&
                                     mouseY >= craftOutputY && mouseY <= craftOutputY + slotSize;

        if (craftOutputHovered) {
            hoveredIsCraftingOutput = true;
        }

        int craftOutputBgColor = craftOutputHovered ?
            ImGui.getColorU32(0.5f, 0.5f, 0.3f, 0.9f) :
            ImGui.getColorU32(0.3f, 0.3f, 0.2f, 0.9f);
        drawList.addRectFilled(craftOutputX, craftOutputY, craftOutputX + slotSize, craftOutputY + slotSize, craftOutputBgColor);

        int craftOutputBorderColor = ImGui.getColorU32(1.0f, 1.0f, 0.0f, 1.0f);
        drawList.addRect(craftOutputX, craftOutputY, craftOutputX + slotSize, craftOutputY + slotSize, craftOutputBorderColor, 0, 0, 2.0f);

        if (!craftingOutput.isEmpty()) {
            renderItemInSlot(drawList, craftingOutput, craftOutputX, craftOutputY, slotSize);
        }

        float hotbarY = startY + 3 * (slotSize + spacing) + 10;

        for (int i = 0; i < 9; i++) {
            float x = startX + i * (slotSize + spacing);

            boolean isHovered = mouseX >= x && mouseX <= x + slotSize &&
                               mouseY >= hotbarY && mouseY <= hotbarY + slotSize;

            if (isHovered) {
                hoveredSlot = i;
                hoveredIsHotbar = true;
            }

            int bgColor;
            if (isHovered) {
                bgColor = ImGui.getColorU32(0.5f, 0.5f, 0.5f, 0.9f);
            } else if (i == inventory.getSelectedSlot()) {
                bgColor = ImGui.getColorU32(0.4f, 0.4f, 0.4f, 0.9f);
            } else {
                bgColor = ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f);
            }
            drawList.addRectFilled(x, hotbarY, x + slotSize, hotbarY + slotSize, bgColor);

            int borderColor = (i == inventory.getSelectedSlot() || isHovered) ?
                ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) :
                ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
            drawList.addRect(x, hotbarY, x + slotSize, hotbarY + slotSize, borderColor, 0, 0, 2.0f);

            ItemStack item = inventory.getHotbarSlot(i);
            if (!item.isEmpty() && !(heldItem != null && heldFromSlot == i && heldFromHotbar)) {
                // Always use 2D texture (3D rendering disabled)
                int textureId = -1;
                String texturePath = item.getItem().getTexturePath();
                if (texturePath != null && scene != null) {
                    try {
                        textureId = scene.getTextureCache().getTexture(texturePath).getTextureId();
                    } catch (Exception e) {
                        // If texture loading fails, skip icon
                    }
                }

                if (textureId != -1) {
                    float iconPadding = 4;
                    float iconSize = slotSize - iconPadding * 2;
                    float iconX = x + iconPadding;
                    float iconY = hotbarY + iconPadding;

                    drawList.addImage(textureId, iconX, iconY, iconX + iconSize, iconY + iconSize);
                }

                // Render item count
                String text = String.valueOf(item.getCount());
                float textX = x + slotSize - ImGui.calcTextSize(text).x - 5;
                float textY = hotbarY + slotSize - ImGui.getFont().getFontSize() - 5;
                drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
                drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
            }
        }

        if (heldItem != null && !heldItem.isEmpty()) {
            float heldX = mouseX - slotSize / 2;
            float heldY = mouseY - slotSize / 2;

            drawList.addRectFilled(heldX, heldY, heldX + slotSize, heldY + slotSize,
                                ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.95f));
            drawList.addRect(heldX, heldY, heldX + slotSize, heldY + slotSize,
                          ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 0, 0, 2.0f);

            // Always use 2D texture (3D rendering disabled)
            int textureId = -1;
            String texturePath = heldItem.getItem().getTexturePath();
            if (texturePath != null && scene != null) {
                try {
                    textureId = scene.getTextureCache().getTexture(texturePath).getTextureId();
                } catch (Exception e) {
                    // If texture loading fails, skip icon
                }
            }

            if (textureId != -1) {
                float iconPadding = 4;
                float iconSize = slotSize - iconPadding * 2;
                float iconX = heldX + iconPadding;
                float iconY = heldY + iconPadding;

                drawList.addImage(textureId, iconX, iconY, iconX + iconSize, iconY + iconSize);
            }

            // Render item count
            String text = String.valueOf(heldItem.getCount());
            float textX = heldX + slotSize - ImGui.calcTextSize(text).x - 5;
            float textY = heldY + slotSize - ImGui.getFont().getFontSize() - 5;
            drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
            drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
        }

        // Render tooltip for hovered item
        if (heldItem == null) {
            if (hoveredSlot != -1) {
                ItemStack item = hoveredIsHotbar ?
                    inventory.getHotbarSlot(hoveredSlot) :
                    inventory.getInventorySlot(hoveredSlot);
                if (!item.isEmpty()) {
                    renderTooltip(drawList, item.getItem().getName(), mouseX, mouseY);
                }
            } else if (hoveredCraftingRow != -1 && hoveredCraftingSlot != -1) {
                ItemStack item = craftingGrid2x2[hoveredCraftingRow][hoveredCraftingSlot];
                if (!item.isEmpty()) {
                    renderTooltip(drawList, item.getItem().getName(), mouseX, mouseY);
                }
            } else if (hoveredIsCraftingOutput && !craftingOutput.isEmpty()) {
                renderTooltip(drawList, craftingOutput.getItem().getName(), mouseX, mouseY);
            }
        }

        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }

    private void renderCraftingTable() {
        ImGuiIO io = ImGui.getIO();
        float windowWidth = io.getDisplaySizeX();
        float windowHeight = io.getDisplaySizeY();
        float mouseX = io.getMousePosX();
        float mouseY = io.getMousePosY();

        float slotSize = 50;
        float spacing = 2;
        float gridSize = (slotSize + spacing) * 3 - spacing;
        float invWidth = (slotSize + spacing) * 9 - spacing;

        float startX = (windowWidth - gridSize) / 2;
        float startY = (windowHeight - gridSize) / 2 - 60;

        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(windowWidth, windowHeight);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0, 0);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.0f, 0.0f, 0.0f, 0.7f);

        int flags = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoResize |
                    ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoScrollbar;

        ImGui.begin("CraftingTableUI", flags);
        ImDrawList drawList = ImGui.getWindowDrawList();

        String title = "Crafting Table";
        float titleX = startX;
        float titleY = startY - 30;
        drawList.addText(titleX, titleY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), title);

        hoveredCraftingSlot = -1;
        hoveredCraftingRow = -1;
        hoveredIsCraftingOutput = false;

        // Render 3x3 crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float x = startX + col * (slotSize + spacing);
                float y = startY + row * (slotSize + spacing);

                boolean isHovered = mouseX >= x && mouseX <= x + slotSize &&
                                   mouseY >= y && mouseY <= y + slotSize;

                if (isHovered) {
                    hoveredCraftingRow = row;
                    hoveredCraftingSlot = col;
                }

                int bgColor = isHovered ?
                    ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.9f) :
                    ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f);
                drawList.addRectFilled(x, y, x + slotSize, y + slotSize, bgColor);

                int borderColor = isHovered ?
                    ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) :
                    ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
                drawList.addRect(x, y, x + slotSize, y + slotSize, borderColor, 0, 0, 1.0f);

                ItemStack item = craftingGrid3x3[row][col];
                if (!item.isEmpty()) {
                    renderItemInSlot(drawList, item, x, y, slotSize);
                }
            }
        }

        // Render output slot (to the right of the grid)
        float outputX = startX + gridSize + 30;
        float outputY = startY + (slotSize + spacing);

        boolean outputHovered = mouseX >= outputX && mouseX <= outputX + slotSize &&
                               mouseY >= outputY && mouseY <= outputY + slotSize;

        if (outputHovered) {
            hoveredIsCraftingOutput = true;
        }

        int outputBgColor = outputHovered ?
            ImGui.getColorU32(0.5f, 0.5f, 0.3f, 0.9f) :
            ImGui.getColorU32(0.3f, 0.3f, 0.2f, 0.9f);
        drawList.addRectFilled(outputX, outputY, outputX + slotSize, outputY + slotSize, outputBgColor);

        int outputBorderColor = ImGui.getColorU32(1.0f, 1.0f, 0.0f, 1.0f);
        drawList.addRect(outputX, outputY, outputX + slotSize, outputY + slotSize, outputBorderColor, 0, 0, 2.0f);

        if (!craftingOutput.isEmpty()) {
            renderItemInSlot(drawList, craftingOutput, outputX, outputY, slotSize);
        }

        // Render player inventory below
        float invStartX = (windowWidth - invWidth) / 2;
        float invStartY = startY + gridSize + 40;

        hoveredSlot = -1;
        hoveredIsHotbar = false;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                float x = invStartX + col * (slotSize + spacing);
                float y = invStartY + row * (slotSize + spacing);

                boolean isHovered = mouseX >= x && mouseX <= x + slotSize &&
                                   mouseY >= y && mouseY <= y + slotSize;

                if (isHovered) {
                    hoveredSlot = index;
                    hoveredIsHotbar = false;
                }

                int bgColor = isHovered ?
                    ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.9f) :
                    ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f);
                drawList.addRectFilled(x, y, x + slotSize, y + slotSize, bgColor);

                int borderColor = isHovered ?
                    ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) :
                    ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
                drawList.addRect(x, y, x + slotSize, y + slotSize, borderColor, 0, 0, 1.0f);

                ItemStack item = inventory.getInventorySlot(index);
                if (!item.isEmpty()) {
                    renderItemInSlot(drawList, item, x, y, slotSize);
                }
            }
        }

        // Render hotbar
        float hotbarY = invStartY + 3 * (slotSize + spacing) + 10;

        for (int i = 0; i < 9; i++) {
            float x = invStartX + i * (slotSize + spacing);

            boolean isHovered = mouseX >= x && mouseX <= x + slotSize &&
                               mouseY >= hotbarY && mouseY <= hotbarY + slotSize;

            if (isHovered) {
                hoveredSlot = i;
                hoveredIsHotbar = true;
            }

            int bgColor = isHovered ?
                ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.9f) :
                ImGui.getColorU32(0.2f, 0.2f, 0.2f, 0.9f);
            drawList.addRectFilled(x, hotbarY, x + slotSize, hotbarY + slotSize, bgColor);

            int borderColor = isHovered ?
                ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f) :
                ImGui.getColorU32(0.5f, 0.5f, 0.5f, 1.0f);
            drawList.addRect(x, hotbarY, x + slotSize, hotbarY + slotSize, borderColor, 0, 0, 1.0f);

            ItemStack item = inventory.getHotbarSlot(i);
            if (!item.isEmpty()) {
                renderItemInSlot(drawList, item, x, hotbarY, slotSize);
            }
        }

        // Render held item
        if (heldItem != null && !heldItem.isEmpty()) {
            float heldX = mouseX - slotSize / 2;
            float heldY = mouseY - slotSize / 2;

            drawList.addRectFilled(heldX, heldY, heldX + slotSize, heldY + slotSize,
                                ImGui.getColorU32(0.3f, 0.3f, 0.3f, 0.95f));
            drawList.addRect(heldX, heldY, heldX + slotSize, heldY + slotSize,
                          ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), 0, 0, 2.0f);

            renderItemInSlot(drawList, heldItem, heldX, heldY, slotSize);
        }

        // Render tooltip for hovered item
        if (heldItem == null) {
            if (hoveredSlot != -1) {
                ItemStack item = hoveredIsHotbar ?
                    inventory.getHotbarSlot(hoveredSlot) :
                    inventory.getInventorySlot(hoveredSlot);
                if (!item.isEmpty()) {
                    renderTooltip(drawList, item.getItem().getName(), mouseX, mouseY);
                }
            } else if (hoveredCraftingRow != -1 && hoveredCraftingSlot != -1) {
                ItemStack item = craftingGrid3x3[hoveredCraftingRow][hoveredCraftingSlot];
                if (!item.isEmpty()) {
                    renderTooltip(drawList, item.getItem().getName(), mouseX, mouseY);
                }
            } else if (hoveredIsCraftingOutput && !craftingOutput.isEmpty()) {
                renderTooltip(drawList, craftingOutput.getItem().getName(), mouseX, mouseY);
            }
        }

        ImGui.end();
        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }

    private void renderItemInSlot(ImDrawList drawList, ItemStack item, float x, float y, float slotSize) {
        int textureId = -1;

        // Always use 2D texture (3D rendering disabled)
        String texturePath = item.getItem().getTexturePath();
        if (texturePath != null && scene != null) {
            try {
                textureId = scene.getTextureCache().getTexture(texturePath).getTextureId();
            } catch (Exception e) {
                // If texture loading fails, skip icon
            }
        }

        if (textureId != -1) {
            float iconPadding = 4;
            float iconSize = slotSize - iconPadding * 2;
            float iconX = x + iconPadding;
            float iconY = y + iconPadding;

            drawList.addImage(textureId, iconX, iconY, iconX + iconSize, iconY + iconSize);
        }

        String text = String.valueOf(item.getCount());
        float textX = x + slotSize - ImGui.calcTextSize(text).x - 5;
        float textY = y + slotSize - ImGui.getFont().getFontSize() - 5;
        drawList.addText(textX + 1, textY + 1, ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f), text);
        drawList.addText(textX, textY, ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), text);
    }

    private void renderTooltip(ImDrawList drawList, String itemName, float mouseX, float mouseY) {
        if (itemName == null || itemName.isEmpty()) return;

        float padding = 8;
        float fontSize = ImGui.getFont().getFontSize();
        imgui.ImVec2 textSize = ImGui.calcTextSize(itemName);

        float tooltipWidth = textSize.x + padding * 2;
        float tooltipHeight = fontSize + padding * 2;

        float tooltipX = mouseX + 12;
        float tooltipY = mouseY + 12;

        // Background
        drawList.addRectFilled(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight,
                             ImGui.getColorU32(0.0f, 0.0f, 0.0f, 0.9f));

        // Border
        drawList.addRect(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight,
                       ImGui.getColorU32(0.3f, 0.3f, 0.5f, 1.0f), 0, 0, 1.0f);

        // Text
        drawList.addText(tooltipX + padding, tooltipY + padding,
                       ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f), itemName);
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

        int buttonIndex = ModLoader.getModsLoaded() == 0 ? 2 : 3;
        ImGui.setCursorPos(centerX, buttonY + (buttonHeight + spacing) * buttonIndex);
        if (ImGui.button("Info", buttonWidth, buttonHeight)) {
            currentState = GameState.INFO;
        }

        ImGui.setCursorPos(centerX, buttonY + (buttonHeight + spacing) * (buttonIndex + 1));
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

    private void renderInfo() {
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

        ImGui.begin("InfoMenu", windowFlags);

        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(0, 0, windowWidth, windowHeight,
                            ImGui.getColorU32(0.15f, 0.15f, 0.15f, 1.0f));

        float contentWidth = 600;
        float contentHeight = 400;
        float centerX = (windowWidth - contentWidth) / 2;
        float centerY = (windowHeight - contentHeight) / 2;

        ImGui.setCursorPos(centerX, centerY);

        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        ImGui.setWindowFontScale(2.0f);
        ImGui.text("Game Info");
        ImGui.setWindowFontScale(1.0f);

        ImGui.setCursorPos(centerX, centerY + 60);
        ImGui.text("INFDEV - Minecraft Clone");

        ImGui.setCursorPos(centerX, centerY + 90);
        ImGui.text("Version: 0.1.0-alpha.1");

        ImGui.setCursorPos(centerX, centerY + 120);
        ImGui.text("Controls:");
        ImGui.setCursorPos(centerX + 20, centerY + 145);
        ImGui.text("WASD - Move");
        ImGui.setCursorPos(centerX + 20, centerY + 165);
        ImGui.text("Space - Jump");
        ImGui.setCursorPos(centerX + 20, centerY + 185);
        ImGui.text("E - Inventory");
        ImGui.setCursorPos(centerX + 20, centerY + 205);
        ImGui.text("Left Click - Break Block");
        ImGui.setCursorPos(centerX + 20, centerY + 225);
        ImGui.text("Right Click - Place Block/Use block/item");
        ImGui.setCursorPos(centerX + 20, centerY + 245);
        ImGui.text("1-9 - Select Hotbar Slot");

        ImGui.popStyleColor();

        float buttonWidth = 200;
        float buttonHeight = 40;
        float buttonX = (windowWidth - buttonWidth) / 2;
        float buttonY = centerY + contentHeight - 60;

        ImGui.setCursorPos(buttonX, buttonY);

        ImGui.pushStyleColor(ImGuiCol.Button, 0.0f, 0.0f, 0.0f, 0.5f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.3f, 0.3f, 0.8f, 0.8f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.2f, 0.2f, 0.6f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 0.0f);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 2.0f);
        ImGui.pushStyleColor(ImGuiCol.Border, 0.6f, 0.6f, 0.6f, 1.0f);

        if (ImGui.button("Back", buttonWidth, buttonHeight)) {
            currentState = GameState.MENU;
        }

        ImGui.popStyleColor(5);
        ImGui.popStyleVar(2);

        ImGui.end();
        ImGui.popStyleVar(3);
    }

    private void renderDeathScreen() {
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

        ImGui.begin("DeathScreen", windowFlags);

        ImDrawList drawList = ImGui.getWindowDrawList();
        drawList.addRectFilled(0, 0, windowWidth, windowHeight,
                            ImGui.getColorU32(0.05f, 0.0f, 0.0f, 0.8f));

        String title = "You Died";
        float titleWidth = ImGui.calcTextSize(title).x * 2.5f;
        float titleX = (windowWidth - titleWidth) / 2;
        float titleY = windowHeight * 0.35f;

        ImGui.setWindowFontScale(3.0f);
        ImGui.setCursorPos(titleX + 3, titleY + 3);
        ImGui.pushStyleColor(ImGuiCol.Text, 0.0f, 0.0f, 0.0f, 0.4f);
        ImGui.text(title);
        ImGui.popStyleColor();

        ImGui.setCursorPos(titleX, titleY);
        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 0.2f, 0.2f, 1.0f);
        ImGui.text(title);
        ImGui.popStyleColor();
        ImGui.setWindowFontScale(1.0f);

        float buttonWidth = 200;
        float buttonHeight = 45;
        float buttonX = (windowWidth - buttonWidth) / 2;
        float buttonY = windowHeight * 0.55f;

        ImGui.setCursorPos(buttonX, buttonY);
        ImGui.pushStyleColor(ImGuiCol.Button, 0.3f, 0.0f, 0.0f, 0.8f);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0.5f, 0.1f, 0.1f, 0.9f);
        ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0.4f, 0.05f, 0.05f, 1.0f);
        ImGui.pushStyleColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        if (ImGui.button("Respawn", buttonWidth, buttonHeight)) {
            respawn(scene);
        }
        ImGui.popStyleColor(4);

        ImGui.end();
        ImGui.popStyleVar(3);
    }

    private void startNewGame() {
        currentState = GameState.PLAYING;
        captureMouse();
        setInitialSpawn(scene.getCamera());
    }

    private void captureMouse() {
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        MouseInput mouseInput = window.getMouseInput();
        mouseInput.setInWindow(true);
        mouseInput.reset();
    }

    private void releaseMouse() {
        glfwSetInputMode(window.getWindowHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        window.getMouseInput().reset();
    }

    @Override
    public boolean handleGuiInput(Scene scene, Window window) {
        ImGuiIO imGuiIO = ImGui.getIO();
        MouseInput mouseInput = window.getMouseInput();
        Vector2f mousePos = mouseInput.getCurrentPos();
        imGuiIO.addMousePosEvent(mousePos.x, mousePos.y);
        imGuiIO.addMouseButtonEvent(0, mouseInput.isLeftButtonPressed());
        imGuiIO.addMouseButtonEvent(1, mouseInput.isRightButtonPressed());

        if (window.isKeyPressed(GLFW_KEY_E) && (System.currentTimeMillis() - lastInvStatusChangeTime) > 300) {
            lastInvStatusChangeTime = System.currentTimeMillis();

            if (currentState == GameState.DEATH) {
                return imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
            }

            if (currentState == GameState.CRAFTING_TABLE) {
                // Close crafting table
                closeCraftingTable();
            } else {
                // Toggle inventory
                inventoryOpen = !inventoryOpen;
                if (inventoryOpen) {
                    releaseMouse();
                } else {
                    captureMouse();
                    if (heldItem != null) {
                        if (heldFromHotbar) {
                            inventory.setHotbarSlot(heldFromSlot, heldItem);
                        } else {
                            inventory.setInventorySlot(heldFromSlot, heldItem);
                        }
                        heldItem = null;
                        heldFromSlot = -1;
                    }
                    clearCraftingGrid();
                }
            }
        }

        if (inventoryOpen && currentState == GameState.PLAYING) {
            handleInventoryClick(mouseInput);
        }

        if (currentState == GameState.CRAFTING_TABLE) {
            handleCraftingTableClick(mouseInput);
        }

        return imGuiIO.getWantCaptureMouse() || imGuiIO.getWantCaptureKeyboard();
    }

    private void handleInventoryClick(MouseInput mouseInput) {
        if (ImGui.isMouseClicked(0)) {
            if (hoveredIsCraftingOutput && !craftingOutput.isEmpty()) {
                // Take crafting output from 2x2 grid
                if (heldItem == null) {
                    heldItem = new ItemStack(craftingOutput.getItem(), craftingOutput.getCount());
                    consumeCraftingIngredients();
                    updateCraftingOutput();
                } else if (heldItem.getItem().equals(craftingOutput.getItem())) {
                    int space = 64 - heldItem.getCount();
                    if (space >= craftingOutput.getCount()) {
                        heldItem.setCount(heldItem.getCount() + craftingOutput.getCount());
                        consumeCraftingIngredients();
                        updateCraftingOutput();
                    }
                }
            } else if (hoveredCraftingRow != -1 && hoveredCraftingSlot != -1) {
                // Handle 2x2 crafting grid slot click
                ItemStack gridSlot = craftingGrid2x2[hoveredCraftingRow][hoveredCraftingSlot];

                if (heldItem == null) {
                    if (!gridSlot.isEmpty()) {
                        heldItem = new ItemStack(gridSlot.getItem(), gridSlot.getCount());
                        craftingGrid2x2[hoveredCraftingRow][hoveredCraftingSlot] = new ItemStack();
                        updateCraftingOutput();
                    }
                } else {
                    if (gridSlot.isEmpty()) {
                        craftingGrid2x2[hoveredCraftingRow][hoveredCraftingSlot] = heldItem;
                        heldItem = null;
                        updateCraftingOutput();
                    } else if (gridSlot.getItem().equals(heldItem.getItem())) {
                        int space = 64 - gridSlot.getCount();
                        int toTransfer = Math.min(space, heldItem.getCount());
                        gridSlot.setCount(gridSlot.getCount() + toTransfer);
                        heldItem.setCount(heldItem.getCount() - toTransfer);
                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                        }
                        updateCraftingOutput();
                    } else {
                        ItemStack temp = heldItem;
                        heldItem = new ItemStack(gridSlot.getItem(), gridSlot.getCount());
                        craftingGrid2x2[hoveredCraftingRow][hoveredCraftingSlot] = temp;
                        updateCraftingOutput();
                    }
                }
            } else if (hoveredSlot != -1) {
                if (heldItem == null) {
                    ItemStack clickedItem = hoveredIsHotbar ?
                        inventory.getHotbarSlot(hoveredSlot) :
                        inventory.getInventorySlot(hoveredSlot);

                    if (clickedItem != null && !clickedItem.isEmpty()) {
                        heldItem = new ItemStack(clickedItem.getItem(), clickedItem.getCount());
                        heldFromSlot = hoveredSlot;
                        heldFromHotbar = hoveredIsHotbar;

                        if (hoveredIsHotbar) {
                            inventory.setHotbarSlot(hoveredSlot, new ItemStack());
                        } else {
                            inventory.setInventorySlot(hoveredSlot, new ItemStack());
                        }
                    }
                } else {
                    ItemStack targetItem = hoveredIsHotbar ?
                        inventory.getHotbarSlot(hoveredSlot) :
                        inventory.getInventorySlot(hoveredSlot);

                    if (targetItem != null && !targetItem.isEmpty() &&
                        targetItem.getItem().equals(heldItem.getItem()) &&
                        targetItem.getCount() < 64) {

                        int space = 64 - targetItem.getCount();
                        int toTransfer = Math.min(space, heldItem.getCount());
                        targetItem.setCount(targetItem.getCount() + toTransfer);
                        heldItem.setCount(heldItem.getCount() - toTransfer);

                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                            heldFromSlot = -1;
                        }
                    } else {
                        if (hoveredIsHotbar) {
                            inventory.setHotbarSlot(hoveredSlot, heldItem);
                        } else {
                            inventory.setInventorySlot(hoveredSlot, heldItem);
                        }

                        if (targetItem != null && !targetItem.isEmpty()) {
                            heldItem = new ItemStack(targetItem.getItem(), targetItem.getCount());
                            heldFromSlot = hoveredSlot;
                            heldFromHotbar = hoveredIsHotbar;
                        } else {
                            heldItem = null;
                            heldFromSlot = -1;
                        }
                    }
                }
            } else {
                if (heldItem != null) {
                    if (heldFromHotbar) {
                        inventory.setHotbarSlot(heldFromSlot, heldItem);
                    } else {
                        inventory.setInventorySlot(heldFromSlot, heldItem);
                    }
                    heldItem = null;
                    heldFromSlot = -1;
                }
            }
        } else if (ImGui.isMouseClicked(1)) {
            if (hoveredSlot != -1) {
                if (heldItem == null) {
                    ItemStack clickedItem = hoveredIsHotbar ?
                        inventory.getHotbarSlot(hoveredSlot) :
                        inventory.getInventorySlot(hoveredSlot);

                    if (clickedItem != null && !clickedItem.isEmpty() && clickedItem.getCount() > 1) {
                        int halfCount = clickedItem.getCount() / 2;
                        int remainingCount = clickedItem.getCount() - halfCount;

                        clickedItem.setCount(remainingCount);
                        heldItem = new ItemStack(clickedItem.getItem(), halfCount);
                        heldFromSlot = hoveredSlot;
                        heldFromHotbar = hoveredIsHotbar;
                    }
                } else {
                    ItemStack targetItem = hoveredIsHotbar ?
                        inventory.getHotbarSlot(hoveredSlot) :
                        inventory.getInventorySlot(hoveredSlot);

                    if (targetItem != null && targetItem.isEmpty()) {
                        ItemStack singleItem = new ItemStack(heldItem.getItem(), 1);
                        if (hoveredIsHotbar) {
                            inventory.setHotbarSlot(hoveredSlot, singleItem);
                        } else {
                            inventory.setInventorySlot(hoveredSlot, singleItem);
                        }

                        heldItem.setCount(heldItem.getCount() - 1);
                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                            heldFromSlot = -1;
                        }
                    } else if (!targetItem.isEmpty() &&
                               targetItem.getItem().equals(heldItem.getItem()) &&
                               targetItem.getCount() < 64) {
                        targetItem.setCount(targetItem.getCount() + 1);
                        heldItem.setCount(heldItem.getCount() - 1);
                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                            heldFromSlot = -1;
                        }
                    }
                }
            }
        }
    }

    private void handleCraftingTableClick(MouseInput mouseInput) {
        if (ImGui.isMouseClicked(0)) {
            if (hoveredIsCraftingOutput && !craftingOutput.isEmpty()) {
                // Take crafting output
                if (heldItem == null) {
                    heldItem = new ItemStack(craftingOutput.getItem(), craftingOutput.getCount());
                    consumeCraftingIngredients();
                    updateCraftingOutput();
                } else if (heldItem.getItem().equals(craftingOutput.getItem())) {
                    int space = 64 - heldItem.getCount();
                    if (space >= craftingOutput.getCount()) {
                        heldItem.setCount(heldItem.getCount() + craftingOutput.getCount());
                        consumeCraftingIngredients();
                        updateCraftingOutput();
                    }
                }
            } else if (hoveredCraftingRow != -1 && hoveredCraftingSlot != -1) {
                // Handle crafting grid slot click
                ItemStack gridSlot = craftingGrid3x3[hoveredCraftingRow][hoveredCraftingSlot];

                if (heldItem == null) {
                    if (!gridSlot.isEmpty()) {
                        heldItem = new ItemStack(gridSlot.getItem(), gridSlot.getCount());
                        craftingGrid3x3[hoveredCraftingRow][hoveredCraftingSlot] = new ItemStack();
                        updateCraftingOutput();
                    }
                } else {
                    if (gridSlot.isEmpty()) {
                        craftingGrid3x3[hoveredCraftingRow][hoveredCraftingSlot] = heldItem;
                        heldItem = null;
                        updateCraftingOutput();
                    } else if (gridSlot.getItem().equals(heldItem.getItem())) {
                        int space = 64 - gridSlot.getCount();
                        int toTransfer = Math.min(space, heldItem.getCount());
                        gridSlot.setCount(gridSlot.getCount() + toTransfer);
                        heldItem.setCount(heldItem.getCount() - toTransfer);
                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                        }
                        updateCraftingOutput();
                    } else {
                        ItemStack temp = heldItem;
                        heldItem = new ItemStack(gridSlot.getItem(), gridSlot.getCount());
                        craftingGrid3x3[hoveredCraftingRow][hoveredCraftingSlot] = temp;
                        updateCraftingOutput();
                    }
                }
            } else if (hoveredSlot != -1) {
                // Handle regular inventory click (same as inventory screen)
                if (heldItem == null) {
                    ItemStack clickedItem = hoveredIsHotbar ?
                        inventory.getHotbarSlot(hoveredSlot) :
                        inventory.getInventorySlot(hoveredSlot);

                    if (clickedItem != null && !clickedItem.isEmpty()) {
                        heldItem = new ItemStack(clickedItem.getItem(), clickedItem.getCount());
                        heldFromSlot = hoveredSlot;
                        heldFromHotbar = hoveredIsHotbar;

                        if (hoveredIsHotbar) {
                            inventory.setHotbarSlot(hoveredSlot, new ItemStack());
                        } else {
                            inventory.setInventorySlot(hoveredSlot, new ItemStack());
                        }
                    }
                } else {
                    ItemStack targetItem = hoveredIsHotbar ?
                        inventory.getHotbarSlot(hoveredSlot) :
                        inventory.getInventorySlot(hoveredSlot);

                    if (targetItem != null && !targetItem.isEmpty() &&
                        targetItem.getItem().equals(heldItem.getItem()) &&
                        targetItem.getCount() < 64) {

                        int space = 64 - targetItem.getCount();
                        int toTransfer = Math.min(space, heldItem.getCount());
                        targetItem.setCount(targetItem.getCount() + toTransfer);
                        heldItem.setCount(heldItem.getCount() - toTransfer);

                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                            heldFromSlot = -1;
                        }
                    } else {
                        if (hoveredIsHotbar) {
                            inventory.setHotbarSlot(hoveredSlot, heldItem);
                        } else {
                            inventory.setInventorySlot(hoveredSlot, heldItem);
                        }

                        if (targetItem != null && !targetItem.isEmpty()) {
                            heldItem = new ItemStack(targetItem.getItem(), targetItem.getCount());
                            heldFromSlot = hoveredSlot;
                            heldFromHotbar = hoveredIsHotbar;
                        } else {
                            heldItem = null;
                            heldFromSlot = -1;
                        }
                    }
                }
            }
        } else if (ImGui.isMouseClicked(1)) {
            if (hoveredCraftingRow != -1 && hoveredCraftingSlot != -1) {
                ItemStack gridSlot = craftingGrid3x3[hoveredCraftingRow][hoveredCraftingSlot];

                if (heldItem == null) {
                    if (!gridSlot.isEmpty() && gridSlot.getCount() > 1) {
                        int halfCount = gridSlot.getCount() / 2;
                        int remainingCount = gridSlot.getCount() - halfCount;
                        gridSlot.setCount(remainingCount);
                        heldItem = new ItemStack(gridSlot.getItem(), halfCount);
                        updateCraftingOutput();
                    }
                } else {
                    if (gridSlot.isEmpty()) {
                        craftingGrid3x3[hoveredCraftingRow][hoveredCraftingSlot] = new ItemStack(heldItem.getItem(), 1);
                        heldItem.setCount(heldItem.getCount() - 1);
                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                        }
                        updateCraftingOutput();
                    } else if (gridSlot.getItem().equals(heldItem.getItem()) && gridSlot.getCount() < 64) {
                        gridSlot.setCount(gridSlot.getCount() + 1);
                        heldItem.setCount(heldItem.getCount() - 1);
                        if (heldItem.getCount() <= 0) {
                            heldItem = null;
                        }
                        updateCraftingOutput();
                    }
                }
            }
        }
    }

    private void updateCraftingOutput() {
        if (currentState == GameState.CRAFTING_TABLE) {
            craftingOutput = RecipeManager.getCraftingResult(craftingGrid3x3);
        } else if (inventoryOpen) {
            craftingOutput = RecipeManager.getCraftingResult(craftingGrid2x2);
        }
    }

    private void consumeCraftingIngredients() {
        if (currentState == GameState.CRAFTING_TABLE) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    ItemStack stack = craftingGrid3x3[row][col];
                    if (!stack.isEmpty()) {
                        stack.setCount(stack.getCount() - 1);
                        if (stack.getCount() <= 0) {
                            craftingGrid3x3[row][col] = new ItemStack();
                        }
                    }
                }
            }
        } else if (inventoryOpen) {
            for (int row = 0; row < 2; row++) {
                for (int col = 0; col < 2; col++) {
                    ItemStack stack = craftingGrid2x2[row][col];
                    if (!stack.isEmpty()) {
                        stack.setCount(stack.getCount() - 1);
                        if (stack.getCount() <= 0) {
                            craftingGrid2x2[row][col] = new ItemStack();
                        }
                    }
                }
            }
        }
    }

    private void clearCraftingGrid() {
        // Return items from 2x2 grid to inventory
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                ItemStack stack = craftingGrid2x2[row][col];
                if (!stack.isEmpty()) {
                    inventory.addItem(stack.getItem(), stack.getCount());
                    craftingGrid2x2[row][col] = new ItemStack();
                }
            }
        }
        craftingOutput = new ItemStack();
    }

    private void closeCraftingTable() {
        // Return items from 3x3 grid to inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                ItemStack stack = craftingGrid3x3[row][col];
                if (!stack.isEmpty()) {
                    inventory.addItem(stack.getItem(), stack.getCount());
                    craftingGrid3x3[row][col] = new ItemStack();
                }
            }
        }

        if (heldItem != null) {
            inventory.addItem(heldItem.getItem(), heldItem.getCount());
            heldItem = null;
            heldFromSlot = -1;
        }

        craftingOutput = new ItemStack();
        currentState = GameState.PLAYING;
        captureMouse();
    }


    @Override
    public void input(Window window, Scene scene, long diffTimeMillis, boolean inputConsumed) {
        if (currentState != GameState.PLAYING) {
            return;
        }
        
        float move = diffTimeMillis * MOVEMENT_SPEED;
        Camera camera = scene.getCamera();
        Vector3f pos = camera.getPosition();

        float dx = 0, dz = 0;

        if (window.isKeyPressed(GLFW_KEY_W)) {
            Vector3f forward = camera.getViewMatrix().positiveZ(new Vector3f()).negate();
            forward.y = 0;
            forward.normalize();
            dx += forward.x * move;
            dz += forward.z * move;
        } else if (window.isKeyPressed(GLFW_KEY_S)) {
            Vector3f backward = camera.getViewMatrix().positiveZ(new Vector3f());
            backward.y = 0;
            backward.normalize();
            dx += backward.x * move;
            dz += backward.z * move;
        }

        if (window.isKeyPressed(GLFW_KEY_A)) {
            Vector3f left = camera.getViewMatrix().positiveX(new Vector3f()).negate();
            left.y = 0;
            left.normalize();
            dx += left.x * move;
            dz += left.z * move;
        } else if (window.isKeyPressed(GLFW_KEY_D)) {
            Vector3f right = camera.getViewMatrix().positiveX(new Vector3f());
            right.y = 0;
            right.normalize();
            dx += right.x * move;
            dz += right.z * move;
        }

        // Apply X movement
        if (dx != 0) {
            float oldX = pos.x;
            pos.x += dx;
            if (Physics.checkCollision(pos)) {
                pos.x = oldX;
            }
        }

        // Apply Z movement
        if (dz != 0) {
            float oldZ = pos.z;
            pos.z += dz;
            if (Physics.checkCollision(pos)) {
                pos.z = oldZ;
            }
        }

        if (window.isKeyPressed(GLFW_KEY_SPACE)
                && (System.currentTimeMillis() - lastJumpTime) >= JUMP_COOLDOWN_MS
                && scene.getPhysics().isGrounded()) {
            scene.getPhysics().resetVelocity();
            scene.getPhysics().changeVelocity(0.25f);
            lastJumpTime = System.currentTimeMillis();
        }

        MouseInput mouseInput = window.getMouseInput();

        if (mouseInput.isRightButtonPressed() && !inputConsumed && (System.currentTimeMillis() - lastBlockPlaceTime) > 200) {
            lastBlockPlaceTime = System.currentTimeMillis();
            Vector3f camPos = camera.getPosition();
            Vector3f camDir = camera.getViewMatrix().positiveZ(new Vector3f()).negate();

            BlockRaycast.BlockHitResult result = BlockRaycast.raycast(camPos, camDir, loadedChunks, 5.0f);

            // Check if holding food item
            ItemStack selected = inventory.getSelectedItem();
            if (!selected.isEmpty() && selected.getItem().isFood()) {
                // Consume food
                if (playerHunger < maxPlayerHunger) {
                    playerHunger = Math.min(maxPlayerHunger, playerHunger + selected.getItem().getFoodValue());
                    inventory.removeSelectedItem();
                }
                return;
            }

            // Check if clicking on a crafting table
            if (result.hit) {
                int chunkX = (int) Math.floor((double) result.blockPos.x / Chunk.CHUNK_SIZE);
                int chunkZ = (int) Math.floor((double) result.blockPos.z / Chunk.CHUNK_SIZE);
                String key = chunkX + "_" + chunkZ;

                Chunk c = loadedChunks.get(key);
                if (c != null) {
                    int localX = result.blockPos.x - (chunkX * Chunk.CHUNK_SIZE);
                    int localZ = result.blockPos.z - (chunkZ * Chunk.CHUNK_SIZE);

                    byte clickedBlock = c.getBlock(localX, result.blockPos.y, localZ);
                    if (clickedBlock == Blocks.CRAFTING_TABLE.getId()) {
                        // Open crafting table UI
                        craftingTablePos = new Vector3i(result.blockPos);
                        currentState = GameState.CRAFTING_TABLE;
                        releaseMouse();
                        return;
                    }
                }
            }

            // Place block if not clicking on crafting table
            if (!selected.isEmpty() && result.hit && !result.previousBlockPos.equals(new Vector3i((int) camPos.x,(int) camPos.y,(int) camPos.z))) {
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
                    c.rebuildMesh(scene, snapshotLoadedChunks());
                    rebuildNeighborChunksIfEdge(scene, chunkX, chunkZ, localX, localZ);
                }
            }
        }

        // Combat and block breaking with progressive mining
        if (mouseInput.isLeftButtonPressed() && !inputConsumed) {
            Vector3f camPos = camera.getPosition();
            Vector3f camDir = camera.getViewMatrix().positiveZ(new Vector3f()).negate();

            // First check for mob attacks with cooldown
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime >= ATTACK_COOLDOWN) {
                MobRaycast.MobHitResult mobHit = MobRaycast.raycast(camPos, camDir, mobManager.getMobs(), 5.0f);

                if (mobHit.hit && mobHit.distance < 5.0f) {
                    // Attack the mob
                    lastAttackTime = currentTime;
                    float damage = 4.0f; // Base damage with fist

                    // Check if player is holding a weapon
                    ItemStack selected = inventory.getSelectedItem();
                    if (!selected.isEmpty()) {
                        if (selected.getItem().equals(Items.WOODEN_SWORD)) {
                            damage = 8.0f;
                        } else if (selected.getItem().equals(Items.WOODEN_PICKAXE)) {
                            damage = 6.0f;
                        } else if (selected.getItem().equals(Items.WOODEN_AXE)) {
                            damage = 7.0f;
                        }
                    }

                    mobHit.mob.damage(damage);

                    // Reset block breaking progress when attacking
                    targetBlock = null;
                    breakingProgress = 0.0f;
                    return; // Don't break blocks when attacking mobs
                }
            }

            // If no mob was hit or attack on cooldown, try to break blocks
            BlockRaycast.BlockHitResult result = BlockRaycast.raycast(camPos, camDir, loadedChunks, 5.0f);

            if (result.hit) {
                // Check if we're targeting a new block
                if (targetBlock == null || !targetBlock.equals(result.blockPos)) {
                    targetBlock = new Vector3i(result.blockPos);
                    breakingProgress = 0.0f;
                    lastBreakingUpdateTime = System.currentTimeMillis();
                }

                // Calculate mining speed
                long breakingTime = System.currentTimeMillis();
                long deltaTime = breakingTime - lastBreakingUpdateTime;
                lastBreakingUpdateTime = breakingTime;

                int chunkX = (int) Math.floor((double) result.blockPos.x / Chunk.CHUNK_SIZE);
                int chunkZ = (int) Math.floor((double) result.blockPos.z / Chunk.CHUNK_SIZE);
                String key = chunkX + "_" + chunkZ;

                Chunk c = loadedChunks.get(key);
                if (c != null) {
                    int localX = result.blockPos.x - (chunkX * Chunk.CHUNK_SIZE);
                    int localZ = result.blockPos.z - (chunkZ * Chunk.CHUNK_SIZE);

                    byte blockId = c.getBlock(localX, result.blockPos.y, localZ);
                    if (blockId != Blocks.AIR.getId()) {
                        Block block = getBlockFromId(blockId);
                        if (block != null) {
                            float blockHardness = block.getHardness();
                            boolean requiresTool = block.requiresTool();

                            // Get held item and calculate mining speed
                            ItemStack heldItemStack = inventory.getSelectedItem();
                            Item heldTool = heldItemStack.isEmpty() ? null : heldItemStack.getItem();
                            float miningSpeed = 1.0f;
                            boolean hasProperTool = !requiresTool;

                            if (heldTool != null) {
                                String toolType = heldTool.getToolType();
                                if (toolType != null) {
                                    if (toolType.equals("pickaxe") && requiresTool) {
                                        hasProperTool = true;
                                        miningSpeed = heldTool.getMiningSpeedMultiplier();
                                    } else if (toolType.equals("axe") && blockId == Blocks.OAK_LOG.getId()) {
                                        miningSpeed = heldTool.getMiningSpeedMultiplier();
                                    } else if (toolType.equals("shovel") &&
                                             (blockId == Blocks.DIRT.getId() || blockId == Blocks.GRASS.getId() || blockId == Blocks.SAND.getId())) {
                                        miningSpeed = heldTool.getMiningSpeedMultiplier();
                                    }
                                }
                            }

                            // Accumulate breaking progress
                            float progressPerSecond = 1.0f / blockHardness * miningSpeed;
                            breakingProgress += (deltaTime / 1000.0f) * progressPerSecond;

                            // Break block when progress reaches 100%
                            if (breakingProgress >= 1.0f) {
                                if (hasProperTool) {
                                    Item item = getItemFromBlock(blockId);
                                    inventory.addItem(item, 1);
                                }
                                // else: block requires tool but player doesn't have one, no drops

                                c.setBlock(localX, result.blockPos.y, localZ, Blocks.AIR.getId());
                                c.rebuildMesh(scene, snapshotLoadedChunks());
                                rebuildNeighborChunksIfEdge(scene, chunkX, chunkZ, localX, localZ);

                                // Reset breaking progress
                                targetBlock = null;
                                breakingProgress = 0.0f;
                            }
                        }
                    }
                }
            } else {
                // Not looking at any block, reset progress
                targetBlock = null;
                breakingProgress = 0.0f;
            }
        } else {
            // Not holding left click, reset progress
            targetBlock = null;
            breakingProgress = 0.0f;
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

        if (!inventoryOpen) {
            mouseInput.setInWindow(true);
            Vector2f displVec = mouseInput.getDisplVec();
            camera.addRotation((float) Math.toRadians(displVec.x * MOUSE_SENSITIVITY), (float) Math.toRadians(displVec.y * MOUSE_SENSITIVITY));
        }
    }

    @Override
    public void update(Window window, Scene scene, long diffTimeMillis) {
        if (currentState == GameState.PLAYING) {
            trackCameraMovement(scene);
            updateChunks(scene);
            List<Chunk> newChunks = new ArrayList<>();
            Chunk c;
            while ((c = readyChunks.poll()) != null) {
                String key = c.getChunkX() + "_" + c.getChunkZ();
                loadedChunks.put(key, c);
                chunkLastSeen.put(key, System.currentTimeMillis());
                newChunks.add(c);
                pendingRender.put(key, c);
                pendingSince.put(key, System.currentTimeMillis());
            }

            processPendingChunks(scene);

            unloadFar(scene, lastPlayerChunkX, lastPlayerChunkZ);
            rebuildDirtyChunks(scene);
            updatePhysics(scene);

            // Update player health and hunger
            updatePlayerStats();

            // Update mobs
            if (mobManager != null) {
                mobManager.update(scene.getCamera(), loadedChunks, (long) gameEng.getDeltaTime());
            }

            // Update dropped items
            if (droppedItemManager != null) {
                droppedItemManager.update(scene.getCamera().getPosition(), (float) gameEng.getDeltaTime());

                // Check for item pickup
                List<DroppedItem> pickedUp = droppedItemManager.checkPickup(scene.getCamera().getPosition());
                for (DroppedItem item : pickedUp) {
                    inventory.addItem(item.getItem(), item.getCount());
                }
            }

            // Check for death
            if (playerHealth <= 0) {
                handlePlayerDeath(scene);
            }
        }
    }

    private void updatePlayerStats() {
        long currentTime = System.currentTimeMillis();

        // Hunger depletion (1 hunger point every 4 seconds)
        if (currentTime - lastHungerDepletion >= 4000) {
            playerHunger = Math.max(0, playerHunger - 1);
            lastHungerDepletion = currentTime;
        }

        // Health regeneration when hunger is full (1 HP every 0.5 seconds)
        if (playerHunger >= maxPlayerHunger && playerHealth < maxPlayerHealth) {
            if (currentTime - lastHealthRegeneration >= 500) {
                playerHealth = Math.min(maxPlayerHealth, playerHealth + 1);
                lastHealthRegeneration = currentTime;
            }
        }

        // Damage from low hunger (1 HP every 4 seconds when hunger < 6)
        if (playerHunger < 6) {
            if (currentTime - lastHungerDamage >= 4000) {
                playerHealth = Math.max(0, playerHealth - 1);
                lastHungerDamage = currentTime;
            }
        }
    }

    private void handlePlayerDeath(Scene scene) {
        currentState = GameState.DEATH;
        releaseMouse();
        scene.getPhysics().resetVelocity();
    }

    public void damagePlayer(float damage) {
        playerHealth = Math.max(0, playerHealth - damage);
    }

    private void respawn(Scene scene) {
        playerHealth = maxPlayerHealth;
        playerHunger = maxPlayerHunger;
        Camera camera = scene.getCamera();
        placePlayerAtGround(camera, (int) respawnPosition.x, (int) respawnPosition.z);
        scene.getPhysics().resetVelocity();
        currentState = GameState.PLAYING;
        captureMouse();
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
        lastPlayerChunkX = playerChunkX;
        lastPlayerChunkZ = playerChunkZ;
        long now = System.currentTimeMillis();
        for (int dz = -VIEW_RADIUS; dz <= VIEW_RADIUS; dz++) {
            for (int dx = -VIEW_RADIUS; dx <= VIEW_RADIUS; dx++) {
                int cx = playerChunkX + dx;
                int cz = playerChunkZ + dz;
                String key = cx + "_" + cz;
                if (loadedChunks.containsKey(key)) {
                    chunkLastSeen.put(key, now);
                }
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
    }

    public Map<String, Chunk> getLoadedChunks() {
        return loadedChunks;
    }

    private void unloadFar(Scene scene, int px, int pz) {
        long now = System.currentTimeMillis();
        if (!loadingChunks.isEmpty() || (now - lastMoveTime) < UNLOAD_MOVE_COOLDOWN_MS) {
            return;
        }
        List<int[]> toRemove = new ArrayList<>();
        for (Map.Entry<String, Chunk> e : loadedChunks.entrySet()) {
            String[] s = e.getKey().split("_");
            int cx = Integer.parseInt(s[0]);
            int cz = Integer.parseInt(s[1]);
            int dx = Math.abs(cx - px);
            int dz = Math.abs(cz - pz);
            if (dx > UNLOAD_RADIUS || dz > UNLOAD_RADIUS) {
                Long lastSeen = chunkLastSeen.get(e.getKey());
                if (lastSeen == null || now - lastSeen > UNLOAD_COOLDOWN_MS) {
                    toRemove.add(new int[] { cx, cz });
                }
            }
        }

        for (int[] coords : toRemove) {
            String key = coords[0] + "_" + coords[1];
            Chunk removed = loadedChunks.remove(key);
            if (removed != null) {
                chunkLastSeen.remove(key);
                pendingRender.remove(key);
                pendingSince.remove(key);
                removed.removeFromScene(scene);
                markDirtyNeighbors(coords[0], coords[1]);
            }
        }
    }

    private void trackCameraMovement(Scene scene) {
        Vector3f pos = scene.getCamera().getPosition();
        if (pos.distanceSquared(lastCameraPos) > 0.0001f) {
            lastCameraPos.set(pos);
            lastMoveTime = System.currentTimeMillis();
        }
    }

    public static void setWindowName(String windowName) {
        Main.windowName = windowName;
    }


    private byte getBlockIdFromItem(Item item) {
        if (item.equals(Items.GRASS_BLOCK)) return Blocks.GRASS.getId();
        if (item.equals(Items.DIRT)) return Blocks.DIRT.getId();
        if (item.equals(Items.STONE)) return Blocks.STONE.getId();
        if (item.equals(Items.SAND)) return Blocks.SAND.getId();
        if (item.equals(Items.OAK_LOG)) return Blocks.OAK_LOG.getId();
        if (item.equals(Items.OAK_LEAVES)) return Blocks.OAK_LEAVES.getId();
        if (item.equals(Items.CRAFTING_TABLE)) return Blocks.CRAFTING_TABLE.getId();
        return Blocks.AIR.getId();
    }

    private Block getBlockFromId(byte blockId) {
        if (blockId == Blocks.GRASS.getId()) return Blocks.GRASS;
        if (blockId == Blocks.DIRT.getId()) return Blocks.DIRT;
        if (blockId == Blocks.STONE.getId()) return Blocks.STONE;
        if (blockId == Blocks.SAND.getId()) return Blocks.SAND;
        if (blockId == Blocks.OAK_LOG.getId()) return Blocks.OAK_LOG;
        if (blockId == Blocks.OAK_LEAVES.getId()) return Blocks.OAK_LEAVES;
        if (blockId == Blocks.CRAFTING_TABLE.getId()) return Blocks.CRAFTING_TABLE;
        if (blockId == Blocks.WATER.getId()) return Blocks.WATER;
        return Blocks.AIR;
    }

    private Item getItemFromBlock(byte blockId) {
        if (blockId == Blocks.GRASS.getId()) return Items.GRASS_BLOCK;
        if (blockId == Blocks.DIRT.getId()) return Items.DIRT;
        if (blockId == Blocks.STONE.getId()) return Items.STONE;
        if (blockId == Blocks.SAND.getId()) return Items.SAND;
        if (blockId == Blocks.OAK_LOG.getId()) return Items.OAK_LOG;
        if (blockId == Blocks.OAK_LEAVES.getId()) return Items.OAK_LEAVES;
        if (blockId == Blocks.CRAFTING_TABLE.getId()) return Items.CRAFTING_TABLE;
        return Items.AIR;
    }

    private void rebuildNeighborChunks(Scene scene, int chunkX, int chunkZ) {
        rebuildChunkIfLoaded(scene, chunkX - 1, chunkZ);
        rebuildChunkIfLoaded(scene, chunkX + 1, chunkZ);
        rebuildChunkIfLoaded(scene, chunkX, chunkZ - 1);
        rebuildChunkIfLoaded(scene, chunkX, chunkZ + 1);
    }

    private void rebuildNeighborChunksIfEdge(Scene scene, int chunkX, int chunkZ, int localX, int localZ) {
        if (localX == 0) {
            rebuildChunkIfLoaded(scene, chunkX - 1, chunkZ);
        } else if (localX == Chunk.CHUNK_SIZE - 1) {
            rebuildChunkIfLoaded(scene, chunkX + 1, chunkZ);
        }

        if (localZ == 0) {
            rebuildChunkIfLoaded(scene, chunkX, chunkZ - 1);
        } else if (localZ == Chunk.CHUNK_SIZE - 1) {
            rebuildChunkIfLoaded(scene, chunkX, chunkZ + 1);
        }
    }

    private void rebuildChunkIfLoaded(Scene scene, int chunkX, int chunkZ) {
        Chunk neighbor = loadedChunks.get(chunkX + "_" + chunkZ);
        if (neighbor != null) {
            neighbor.rebuildMesh(scene, snapshotLoadedChunks());
        }
    }

    private Map<String, Chunk> snapshotLoadedChunks() {
        return new HashMap<>(loadedChunks);
    }

    private void markDirtyChunk(String key) {
        dirtyChunks.add(key);
    }

    private void markDirtyNeighbors(int chunkX, int chunkZ) {
        dirtyChunks.add((chunkX - 1) + "_" + chunkZ);
        dirtyChunks.add((chunkX + 1) + "_" + chunkZ);
        dirtyChunks.add(chunkX + "_" + (chunkZ - 1));
        dirtyChunks.add(chunkX + "_" + (chunkZ + 1));
    }

    private void rebuildDirtyChunks(Scene scene) {
        if (dirtyChunks.isEmpty()) {
            return;
        }
        Map<String, Chunk> snapshot = snapshotLoadedChunks();
        for (String key : dirtyChunks) {
            Chunk chunk = loadedChunks.get(key);
            if (chunk != null) {
                chunk.rebuildMesh(scene, snapshot);
            }
        }
        dirtyChunks.clear();
    }

    private void processPendingChunks(Scene scene) {
        if (pendingRender.isEmpty()) {
            return;
        }
        Map<String, Chunk> snapshot = snapshotLoadedChunks();
        long now = System.currentTimeMillis();
        List<String> readyKeys = new ArrayList<>();

        for (Map.Entry<String, Chunk> entry : pendingRender.entrySet()) {
            String key = entry.getKey();
            Chunk chunk = entry.getValue();
            int cx = chunk.getChunkX();
            int cz = chunk.getChunkZ();
            boolean neighborsReady = hasNeighbor(snapshot, cx - 1, cz)
                && hasNeighbor(snapshot, cx + 1, cz)
                && hasNeighbor(snapshot, cx, cz - 1)
                && hasNeighbor(snapshot, cx, cz + 1)
                && hasNeighbor(snapshot, cx - 1, cz - 1)
                && hasNeighbor(snapshot, cx - 1, cz + 1)
                && hasNeighbor(snapshot, cx + 1, cz - 1)
                && hasNeighbor(snapshot, cx + 1, cz + 1);
            Long since = pendingSince.get(key);
            boolean timedOut = since == null || (now - since) > PENDING_RENDER_TIMEOUT_MS;
            if (neighborsReady || timedOut) {
                chunk.buildMesh(snapshot);
                chunk.uploadToScene(scene);
                markDirtyNeighbors(cx, cz);
                readyKeys.add(key);
            }
        }

        for (String key : readyKeys) {
            pendingRender.remove(key);
            pendingSince.remove(key);
        }
    }

    private boolean hasNeighbor(Map<String, Chunk> snapshot, int cx, int cz) {
        return snapshot.containsKey(cx + "_" + cz);
    }

    private void setInitialSpawn(Camera camera) {
        int spawnX = 0;
        int spawnZ = 0;
        placePlayerAtGround(camera, spawnX, spawnZ);
        respawnPosition.set(camera.getPosition());
    }

    private void placePlayerAtGround(Camera camera, int worldX, int worldZ) {
        int surfaceY = getSurfaceHeight(worldX, worldZ);
        float y = surfaceY + 1.5f;
        camera.setPosition(worldX + 0.5f, y, worldZ + 0.5f);
    }

    private int getSurfaceHeight(int worldX, int worldZ) {
        int chunkX = (int) Math.floor((double) worldX / Chunk.CHUNK_SIZE);
        int chunkZ = (int) Math.floor((double) worldZ / Chunk.CHUNK_SIZE);
        String key = chunkX + "_" + chunkZ;
        Chunk chunk = loadedChunks.get(key);
        if (chunk == null) {
            Chunk temp = new Chunk(chunkX, chunkZ);
            temp.buildData();
            return findSurfaceHeightInChunk(temp, worldX, worldZ);
        }
        return findSurfaceHeightInChunk(chunk, worldX, worldZ);
    }

    private int findSurfaceHeightInChunk(Chunk chunk, int worldX, int worldZ) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        int localX = worldX - (chunkX * Chunk.CHUNK_SIZE);
        int localZ = worldZ - (chunkZ * Chunk.CHUNK_SIZE);
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
            byte blockId = chunk.getBlock(localX, y, localZ);
            if (blockId != Blocks.AIR.getId() && blockId != Blocks.WATER.getId()) {
                return y;
            }
        }
        return 0;
    }
}
