package craft3dgl;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.io.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import craft3dgl.ui.UIStyle;
import craft3dgl.world.ChestStorage;
import craft3dgl.world.CloudRenderer;
import craft3dgl.world.DoorSystem;
import craft3dgl.combat.Hit;
import craft3dgl.audio.AudioCategories;
import craft3dgl.entities.Particle;
import craft3dgl.entities.ParticleSystem;
import craft3dgl.input.ChatLog;
import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;
import craft3dgl.items.ItemRegistry;
import craft3dgl.ui.FontRenderer;
import craft3dgl.ui.HotbarRenderer;
import craft3dgl.ui.MenuBackgroundRenderer;
import craft3dgl.ui.MenuButton;
import craft3dgl.ui.TextureAtlas;
import craft3dgl.ui.Tooltip;
import craft3dgl.world.BiomeGenerator;
import craft3dgl.world.FarmingSystem;
import craft3dgl.world.TreeGenerator;
import craft3dgl.world.WaterSimulation;
import craft3dgl.world.WorldConstants;

public class MinecraftGL {
    static final int WIN_W = 1280;
    static final int WIN_H = 720;
    static final int WORLD_X = 1024;
    static final int WORLD_Y = 64;
    static final int WORLD_Z = 1024;
    static final int CHUNK = 16;
    static final int CHUNKS_X = WORLD_X / CHUNK;
    static final int CHUNKS_Y = WORLD_Y / CHUNK;
    static final int CHUNKS_Z = WORLD_Z / CHUNK;

    public static final int AIR = 0;
    public static final int GRASS = 1;
    public static final int DIRT = 2;
    public static final int STONE = 3;
    public static final int WOOD = 4;
    public static final int LEAVES = 5;
    public static final int SAND = 6;
    public static final int PLANKS = 7;
    public static final int CRAFTING_TABLE = 8;
    public static final int WATER = 15;
    public static final int DOOR_BOTTOM = 16;
    public static final int DOOR_TOP = 17;
    public static final int CHEST = 18;
    // ====== FARMING ======
    public static final int FARMLAND = 50;        // ziemia po motyce
    public static final int TALL_GRASS = 51;      // dekoracyjna trawka, daje seeds
    public static final int WHEAT_0 = 52;         // stadium 1 (just planted)
    public static final int WHEAT_1 = 53;
    public static final int WHEAT_2 = 54;
    public static final int WHEAT_3 = 55;         // dojrzala - daje wheat + seeds
    static final int WATER_LEVEL = 24;
    // WATER_TICK + WATER_UPDATES_PER_TICK przeniesione do WaterSimulation
    static final double WATER_TICK = WaterSimulation.WATER_TICK;
    static final int WATER_UPDATES_PER_TICK = WaterSimulation.WATER_UPDATES_PER_TICK;

    static final int BIOME_PLAINS = 0;
    static final int BIOME_FOREST = 1;
    static final int BIOME_DESERT = 2;
    static final int BIOME_MOUNTAINS = 3;
    static final int BIOME_OCEAN = 4;
    static final int BIOME_BEACH = 5;
    static final int BIOME_RIVER = 6;

    public static final int ITEM_STICK = 9;
    public static final int ITEM_WOOD_PICKAXE = 10;
    public static final int ITEM_STONE_PICKAXE = 11;
    public static final int ITEM_PORK = 12;
    public static final int ITEM_BEEF = 13;
    public static final int ITEM_MUTTON = 14;
    public static final int ITEM_EMERALD = 25;
    public static final int ITEM_BREAD = 26;
    public static final int ITEM_SEEDS = 27;
    public static final int ITEM_WHEAT = 28;
    public static final int ITEM_WOOD_HOE = 29;
    public static final int ITEM_STONE_HOE = 30;
    public static final int ITEM_WOOD_AXE = 19;
    public static final int ITEM_STONE_AXE = 20;
    public static final int ITEM_WOOD_SHOVEL = 21;
    public static final int ITEM_STONE_SHOVEL = 22;
    public static final int ITEM_WOOD_SWORD = 23;
    public static final int ITEM_STONE_SWORD = 24;

    static final int HOTBAR_SIZE = 9;
    static final int INVENTORY_SIZE = 36;

    static final int TILE = TextureAtlas.TILE;
    static final int ATLAS_PAD = TextureAtlas.ATLAS_PAD;
    static final int ATLAS_TILE = TextureAtlas.ATLAS_TILE;
    static final int ATLAS_COLS = TextureAtlas.ATLAS_COLS;
    static final int ATLAS_ROWS = TextureAtlas.ATLAS_ROWS;

    static final double PLAYER_RADIUS = 0.28;
    static final double PLAYER_HEIGHT = 1.80;
    static final double EYE_HEIGHT = 1.62;
    // EntityPlayer.updateSize/getEyeHeight z MCP 9.40.
    static final double SNEAK_HEIGHT = 1.65;
    static final double SNEAK_EYE_HEIGHT = 1.54;
    static final double WALK_SPEED = 4.317;
    static final double SPRINT_SPEED = 5.612;
    static final double SNEAK_SPEED = 1.295;

    long window;
    int width = WIN_W;
    int height = WIN_H;

    final byte[][][] world = new byte[WORLD_X][WORLD_Y][WORLD_Z];
    /** Silnik oswietlenia: sky + block light. Init w initGL po utworzeniu chunkow. */
    craft3dgl.world.LightEngine lightEngine;
    /** Aktualny mnoznik dla sky light zsynchronizowany z dayFraction. */
    float currentDayMult = 1.0f;
    boolean buildingTerrainMesh = false;
    /** Night vision effect - do kiedy aktywny (System.currentTimeMillis()). 0 = nieaktywny. */
    long nightVisionExpireMs = 0L;

    /** True gdy jest aktywny efekt night vision. */
    boolean hasNightVision() {
        return nightVisionExpireMs > System.currentTimeMillis();
    }

    /** Ile sekund zostalo night vision (0 gdy brak). */
    int nightVisionSecondsLeft() {
        long ms = nightVisionExpireMs - System.currentTimeMillis();
        return ms > 0 ? (int)((ms + 999) / 1000) : 0;
    }
    final Chunk[][][] chunks = new Chunk[CHUNKS_X][CHUNKS_Y][CHUNKS_Z];
    final boolean[][] generatedColumns = new boolean[CHUNKS_X][CHUNKS_Z];
    /** Lighting is prepared lazily for generated chunk columns near the player. */
    final boolean[][] litColumns = new boolean[CHUNKS_X][CHUNKS_Z];
    boolean bulkWorldGeneration = false;
    boolean loadingScreenActive = false;
    int loadingTerrainDone = 0;
    int loadingTerrainTarget = 1;
    boolean terrainVboLogged = false;
    long worldSeed = 0;

    // Komorki gdzie juz raz wygenerowano wioske - zapobiega ponownemu spawnowi villagerow.
    final java.util.HashSet<Long> spawnedVillageCells = new java.util.HashSet<>();
    static long packCell(int gx, int gz) { return ((long)(gx & 0xFFFFFFFFL) << 32) | (gz & 0xFFFFFFFFL); }

    final int[] invId = new int[INVENTORY_SIZE];
    final int[] invCount = new int[INVENTORY_SIZE];
    final int[] craftId = new int[9];
    final int[] craftCount = new int[9];
    final int[] equipId = new int[5];
    final int[] equipCount = new int[5];

    final List<DroppedItemGL> drops = new ArrayList<>();
    final List<AnimalGL> animals = new ArrayList<>();
    final List<VillagerGL> villagers = new ArrayList<>();
    final SoundEngine sound = new SoundEngine();
    CraftingSystemGL.Recipe craftResult = CraftingSystemGL.EMPTY;

    int textureAtlas;
    int fontTexture;
    FontRenderer fontRenderer;
    static final int FONT_CELL = FontRenderer.FONT_CELL;

    double x = WORLD_X / 2.0;
    double y = 35;
    double z = WORLD_Z / 2.0;
    double velY = 0;
    boolean onGround = false;
    boolean sneaking = false;
    int eatingItemId = 0;
    double eatingProgress = 0;
    int eatingSoundStep = 0;
    int health = 20;
    int maxHealth = 20;
    int hunger = 20;
    // === XP SYSTEM ===
    int playerXp = 0;         // total XP zebrane
    int xpLevel = 0;          // aktualny level
    float xpProgress = 0f;    // 0..1 postep na aktualnym levelu
    final List<craft3dgl.entities.ExperienceOrb> xpOrbs = new ArrayList<>();
    /** MC formula: XP potrzebne do awansu na level+1 */
    int xpNeededForLevel(int level) {
        if (level < 16) return 2 * level + 7;
        if (level < 31) return 5 * level - 38;
        return 9 * level - 158;
    }
    /** Dodaje xp do gracza, robi level up gdy przekroczy prog */
    void addPlayerXp(int amount) {
        playerXp += amount;
        xpProgress += (float) amount / xpNeededForLevel(xpLevel);
        while (xpProgress >= 1.0f) {
            xpProgress -= 1.0f;
            xpLevel++;
            sound.playLevelUp();
        }
    }
    double hungerTimer = 0;
    double regenTimer = 0;
    double starveTimer = 0;
    double stepSoundTimer = 0;

    double yaw = Math.PI * 0.25;
    double pitch = -0.10;
    boolean mouseCaptured = true;
    boolean firstMouse = true;
    double lastMouseX, lastMouseY;
    double pendingScroll = 0;

    int selectedBlock = DIRT;
    int selectedSlot = 0;
    int cursorId = 0;
    int cursorCount = 0;

    boolean inventoryOpen = false;
    boolean usingCraftingTable = false;
    boolean leftWasDown = false;
    boolean rightWasDown = false;
    boolean eWasDown = false;
    boolean qWasDown = false;
    boolean mWasDown = false;
    boolean escWasDown = false;
    boolean f5WasDown = false;
    boolean f9WasDown = false;
    boolean f10WasDown = false;
    boolean f12WasDown = false;
    // TUNING MODES:
    //   F9 = TOOL tuning (miecze, kilofy, siekiery, motyki)
    //   F10 = BLOCK tuning (bloki w rece)
    //   F12 = FOOD tuning (chleb, mieso, emerald, wheat, seeds)
    static final int TUNE_OFF = 0, TUNE_TOOL = 1, TUNE_BLOCK = 2, TUNE_FOOD = 3;
    static int itemTuneMode = TUNE_OFF;
    // TOOL values (miecze/kilofy) - twoje idealne z F9
    static float itemTuneX = 1.040f;
    static float itemTuneY = 0.000f;
    static float itemTuneZ = -0.700f;
    static float itemTuneRotX = 14.0f;
    static float itemTuneRotY = 272.0f;
    static float itemTuneRotZ = 50.0f;
    static float itemTuneScale = 0.760f;
    // BLOCK values (osobne) - default = ta sama pozycja co tool
    // Twoje wartosci z F10 tuning mode
    static float blockTuneX = 0.960f;
    static float blockTuneY = -0.060f;
    static float blockTuneZ = -0.700f;
    static float blockTuneRotX = 210.0f;   // 930 mod 360
    static float blockTuneRotY = 34.0f;    // 754 mod 360
    static float blockTuneRotZ = 192.0f;   // 552 mod 360
    static float blockTuneScale = 0.240f;
    // FOOD values (chleb, mieso, wheat, seeds, emerald) - default MC-native (jak inne items)
    // Twoje wartosci z F12 tuning
    static float foodTuneX = 1.140f;
    static float foodTuneY = -0.020f;
    static float foodTuneZ = -0.700f;
    static float foodTuneRotX = 338.0f;
    static float foodTuneRotY = 86.0f;    // 1526 mod 360
    static float foodTuneRotZ = 56.0f;    // -304 mod 360
    static float foodTuneScale = 0.400f;
    long lastTuneLogTime = 0;
    /**
     * Minecraft 1.12 has no JSON core-shader terrain pipeline. Keep the later
     * experiment compiled for source compatibility, but never select it for
     * gameplay: the MCP fixed-function atlas/lightmap path is authoritative.
     */
    static final boolean USE_MODERN_RENDERER = false;
    /** GameRenderer - laduje core shadery raz na start. */
    final craft3dgl.blaze3d.renderer.GameRenderer gameRenderer = craft3dgl.blaze3d.renderer.GameRenderer.getInstance();
    boolean menuMouseWasDown = false;
    boolean deathScreen = false;
    boolean deathMouseWasDown = false;
    boolean deathDropsDone = false;
    boolean paused = false;
    boolean villagerTradeOpen = false;
    boolean villagerMouseWasDown = false;
    VillagerGL tradingVillager = null;
    int pauseScreen = 0;
    boolean pauseMouseWasDown = false;
    boolean draggingVolume = false;
    int cameraMode = 0;

    boolean inMainMenu = true;
    boolean worldLoaded = false;
    int menuScreen = 0;
    String[] worldNames = new String[0];
    int selectedWorldIndex = 0;
    String currentWorldName = "";
    String menuMessage = "";

    static final int GAMEMODE_SURVIVAL = 0;
    static final int GAMEMODE_CREATIVE = 1;
    int gameMode = GAMEMODE_SURVIVAL;
    boolean flying = false;
    double lastSpacePressTime = -10;
    boolean spaceWasDown = false;
    boolean middleMouseWasDown = false;

    boolean chatOpen = false;
    boolean chatIgnoreNextChar = false;
    StringBuilder chatInput = new StringBuilder();
    int chatCursor = 0;
    int chatSelection = 0;
    int chatScroll = 0;
    final java.util.ArrayList<String> sentChatHistory = new java.util.ArrayList<String>();
    int sentHistoryCursor = 0;
    String chatHistoryDraft = "";
    java.util.List<String> chatCompletions = java.util.Collections.emptyList();
    int chatCompletionIndex = -1;
    final java.util.ArrayList<ChatMessage> chatLog = new java.util.ArrayList<>();
    boolean tWasDown = false;
    boolean slashWasDown = false;

    static final int CHEST_SIZE = ChestStorage.CHEST_SIZE;
    final ChestStorage chestStorage = new ChestStorage();
    // Delegowane mapy (dla starego API i serializacji)
    final java.util.HashMap<Long, int[]> chestIds = chestStorage.ids;
    final java.util.HashMap<Long, int[]> chestCounts = chestStorage.counts;
    final java.util.HashMap<Long, Integer> chestFacings = chestStorage.facings;
    boolean chestOpen = false;
    int chestOpenX, chestOpenY, chestOpenZ;
    /** Combined ContainerChest view: 27 slots for single, 54 for double. */
    final int[] openChestIds = new int[CHEST_SIZE * 2];
    final int[] openChestCounts = new int[CHEST_SIZE * 2];
    int openChestSlots = CHEST_SIZE;
    long openChestPrimaryKey = Long.MIN_VALUE;
    long openChestSecondaryKey = Long.MIN_VALUE;
    /** TileEntityChest lidAngle, 0 closed .. 1 open. */
    float chestLidProgress = 0f;
    boolean chestMouseWasDown = false;

    static long packChestKey(int x, int y, int z) { return ChestStorage.packKey(x, y, z); }
    int[] chestIdsAt(int x, int y, int z) { return chestStorage.idsAt(x, y, z); }
    int[] chestCountsAt(int x, int y, int z) { return chestStorage.countsAt(x, y, z); }
    int chestFacingAt(int x, int y, int z) { return chestStorage.facingAt(x, y, z); }
    void setChestFacing(int x, int y, int z, int facing) { chestStorage.setFacing(x, y, z, facing); }
    void removeChest(int x, int y, int z) { chestStorage.remove(x, y, z); }

    /** Upgrade old saves where an unopened, empty chest had no tile-entity entry. */
    void registerMissingChestTileEntities() {
        for (int cx = 0; cx < CHUNKS_X; cx++) for (int cz = 0; cz < CHUNKS_Z; cz++) {
            if (!generatedColumns[cx][cz]) continue;
            int minX = cx * CHUNK, minZ = cz * CHUNK;
            for (int bx = minX; bx < minX + CHUNK; bx++) {
                for (int by = 0; by < WORLD_Y; by++) {
                    for (int bz = minZ; bz < minZ + CHUNK; bz++) {
                        if ((world[bx][by][bz] & 0xff) != CHEST) continue;
                        chestIdsAt(bx, by, bz);
                        chestCountsAt(bx, by, bz);
                    }
                }
            }
        }
    }

    // Adapter kolizji: pelne bloki oraz dokladny panel drzwi o grubosci 3/16.
    final craft3dgl.entities.EntityCollision.SolidCheck entitySolidCheck = new craft3dgl.entities.EntityCollision.SolidCheck() {
        @Override public boolean isSolid(int x, int y, int z) { return MinecraftGL.this.solid(x, y, z); }
        @Override public int getBlock(int x, int y, int z) { return world[x][y][z] & 0xff; }
        @Override public boolean inWorld(int x, int y, int z) { return MinecraftGL.this.inWorld(x, y, z); }
        @Override public double[] getCollisionBounds(int x, int y, int z) {
            int id = world[x][y][z] & 0xff;
            return MinecraftGL.this.isDoor(id)
                    ? DoorSystem.doorBox(getDoorMeta(x, y, z)) : null;
        }
    };

    final Translations translationSys = new Translations();
    String language = "pl";
    java.util.HashMap<String, String[]> translations = new java.util.HashMap<>();

    void initTranslations() { /* tlumaczenia w Translations class */ }

    String tr(String key) {
        translationSys.setLanguage(language);
        return translationSys.tr(key);
    }

    int settingsTab = 0;
    int draggingSlider = -1;

    long lastClickTime = 0;
    int lastClickItem = 0;
    boolean dragging = false;
    boolean draggingRight = false;
    final java.util.HashSet<Long> dragSlots = new java.util.HashSet<>();
    int dragStartCount = 0;
    int dragStartItem = 0;
    static long packSlotKey(int kind, int idx) { return ((long) kind << 32) | (idx & 0xffffffffL); }

    boolean creativeInvOpen = false;
    int creativeScroll = 0;
    int creativeTab = 0;
    StringBuilder creativeSearch = new StringBuilder();
    boolean backspaceWasDown = false;
    static final int[] CREATIVE_ITEMS = {GRASS, DIRT, STONE, SAND, WOOD, PLANKS, LEAVES, CRAFTING_TABLE, DOOR_BOTTOM, CHEST, WATER, FARMLAND, TALL_GRASS, ITEM_STICK, ITEM_WOOD_PICKAXE, ITEM_STONE_PICKAXE, ITEM_WOOD_AXE, ITEM_STONE_AXE, ITEM_WOOD_SHOVEL, ITEM_STONE_SHOVEL, ITEM_WOOD_SWORD, ITEM_STONE_SWORD, ITEM_WOOD_HOE, ITEM_STONE_HOE, ITEM_SEEDS, ITEM_WHEAT, ITEM_PORK, ITEM_BEEF, ITEM_MUTTON, ITEM_EMERALD, ITEM_BREAD};

    final DoorSystem doorSystem = new DoorSystem();
    final java.util.HashMap<Long, Integer> doorMeta = doorSystem.meta;
    static long packDoorKey(int x, int y, int z) { return DoorSystem.packKey(x, y, z); }
    int getDoorMeta(int x, int y, int z) { return doorSystem.getMeta(x, y, z); }
    void setDoorMeta(int x, int y, int z, int meta) { doorSystem.setMeta(x, y, z, meta); }
    void removeDoorMeta(int x, int y, int z) { doorSystem.removeMeta(x, y, z); }
    boolean isDoor(int id) { return id == DOOR_BOTTOM || id == DOOR_TOP; }

    final DoorSystem.PlacementWorld doorPlacementWorld = new DoorSystem.PlacementWorld() {
        @Override public boolean isNormalCube(int x, int y, int z) {
            return isNormalCubeForDoor(x, y, z);
        }
        @Override public boolean isDoor(int x, int y, int z) {
            return inWorld(x, y, z) && MinecraftGL.this.isDoor(world[x][y][z] & 0xff);
        }
    };

    int yawToFacing(double yaw) { return DoorSystem.yawToFacing(yaw); }
    double[] doorAabb(int meta) { return DoorSystem.doorAabb(meta); }

    Hit miningHit = null;
    double miningProgress = 0;
    double miningParticleTimer = 0;
    /** Sila screen shake (0-1) - obniza sie z czasem. */
    double screenShake = 0;
    /** Alpha czerwonego overlay (0-1) po dostaniu damage. */
    double damageFlash = 0;
    /** Czas w grze w sekundach (Minecraft 1.12: 24000 tickow = 1200 sekund). */
    double gameTime = 600.0; // start w poludnie (0.5 fraction)
    final craft3dgl.world.WeatherState weather = new craft3dgl.world.WeatherState(0L);
    double rainSoundTimer = 0.0;
    /** Poprzedni stan gracza w wodzie - do splash particles. */
    boolean wasInWater = false;
    /** Timer footstep dust particles. */
    double footstepDustTimer = 0;
    /** Timer smoke z crafting table (dekoracyjny, gdy jestes blisko craftingu). */
    double ambientSmokeTimer = 0;

    double swingTimer = 0;
    /** EntityPlayer.ticksSinceLastSwing, advanced at 20 ticks per second. */
    double ticksSinceLastSwing = 1000.0;
    int lastAttackItemId = Integer.MIN_VALUE;
    /** Sprint is cancelled briefly after a charged sprint-knockback attack. */
    double sprintDisableTimer = 0.0;
    double walkPhase = 0;
    double lastX = 0, lastZ = 0;
    /** Yaw ciala - dogania yaw kamery powoli (MC renderYawOffset). */
    double bodyYaw = 0;
    /** Amplituda swingu (0..1) - płynnie rośnie gdy sie ruszamy, spada gdy stoimy. */
    float limbSwingAmount = 0;
    /** Kumulator kroków - do animacji sinusowej. */
    float limbSwing = 0;
    boolean lastPosInit = false;

    int frames, fps;
    long fpsTimer = System.currentTimeMillis();

    final WaterSimulation waterSim = new WaterSimulation();
    final ArrayDeque<int[]> waterQueue = waterSim.queue;  // alias for old code
    // Tick wzrostu pszenicy delegowany do FarmingSystem
    final FarmingSystem farming = new FarmingSystem();
    static final double WHEAT_TICK = FarmingSystem.WHEAT_TICK;

    final Random random = new Random();

    public static void main(String[] args) {
        new MinecraftGL().run();
    }

    void run() {
        initWindow();
        initGL();
        sound.setSoundDir(findAssetDir("sounds"));
        refreshWorldList();
        mouseCaptured = false;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        loop();
        cleanup();
    }

    void initWindow() {
        if (!glfwInit()) throw new IllegalStateException("Nie mozna zainicjalizowac GLFW/LWJGL");
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        // Wylacz MSAA - psuje ostre pixel-art (rozmazuje krawedzie w polprzezroczyste)
        glfwWindowHint(GLFW_SAMPLES, 0);
        window = glfwCreateWindow(WIN_W, WIN_H, "Craft3D OpenGL / LWJGL", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Nie mozna stworzyc okna GLFW");
        GLFWVidMode vid = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vid != null) glfwSetWindowPos(window, (vid.width() - WIN_W) / 2, (vid.height() - WIN_H) / 2);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        glfwSetFramebufferSizeCallback(window, (w, newW, newH) -> {
            width = Math.max(1, newW);
            height = Math.max(1, newH);
            glViewport(0, 0, width, height);
        });
        glfwSetCursorPosCallback(window, (w, mx, my) -> {
            if (!mouseCaptured) return;
            if (firstMouse) {
                lastMouseX = mx;
                lastMouseY = my;
                firstMouse = false;
                return;
            }
            double dx = mx - lastMouseX;
            double dy = my - lastMouseY;
            lastMouseX = mx;
            lastMouseY = my;
            double sensitivity = 0.0026;
            yaw -= dx * sensitivity;
            pitch -= dy * sensitivity;
            pitch = clamp(pitch, -1.50, 1.50);
        });
        glfwSetScrollCallback(window, (w, xOffset, yOffset) -> {
            if (chatOpen) {
                int amount = (int)Math.round(yOffset * 7.0);
                chatScroll = Math.max(0, Math.min(Math.max(0, chatLog.size() - 1), chatScroll + amount));
            } else {
                pendingScroll += yOffset;
            }
        });
        glfwSetCharCallback(window, (w, codepoint) -> {
            if (creativeInvOpen) {
                if (codepoint >= 32 && codepoint < 127 && creativeSearch.length() < 32) creativeSearch.append((char) codepoint);
                return;
            }
            if (!chatOpen) return;
            if (chatIgnoreNextChar) { chatIgnoreNextChar = false; return; }
            if (codepoint >= 32 && codepoint < 0x10000) {
                deleteChatSelection();
                if (chatInput.length() < 256) {
                    chatInput.insert(chatCursor, (char) codepoint);
                    chatCursor++;
                    chatSelection = chatCursor;
                    resetChatCompletion();
                }
            }
        });
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action != GLFW_PRESS && action != GLFW_REPEAT) return;
            if (inMainMenu || paused || deathScreen) return;
            if (chatOpen) {
                boolean control = (mods & GLFW_MOD_CONTROL) != 0;
                boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
                if (key == GLFW_KEY_ENTER || key == GLFW_KEY_KP_ENTER) {
                    submitChat();
                } else if (key == GLFW_KEY_BACKSPACE) {
                    if (!deleteChatSelection() && chatCursor > 0) {
                        chatInput.deleteCharAt(--chatCursor);
                        chatSelection = chatCursor;
                        resetChatCompletion();
                    }
                } else if (key == GLFW_KEY_DELETE) {
                    if (!deleteChatSelection() && chatCursor < chatInput.length()) {
                        chatInput.deleteCharAt(chatCursor);
                        chatSelection = chatCursor;
                        resetChatCompletion();
                    }
                } else if (key == GLFW_KEY_LEFT) {
                    if (!shift && chatCursor != chatSelection) chatCursor = Math.min(chatCursor, chatSelection);
                    else if (chatCursor > 0) chatCursor--;
                    if (!shift) chatSelection = chatCursor;
                } else if (key == GLFW_KEY_RIGHT) {
                    if (!shift && chatCursor != chatSelection) chatCursor = Math.max(chatCursor, chatSelection);
                    else if (chatCursor < chatInput.length()) chatCursor++;
                    if (!shift) chatSelection = chatCursor;
                } else if (key == GLFW_KEY_HOME) {
                    chatCursor = 0;
                    if (!shift) chatSelection = chatCursor;
                } else if (key == GLFW_KEY_END) {
                    chatCursor = chatInput.length();
                    if (!shift) chatSelection = chatCursor;
                } else if (key == GLFW_KEY_UP) {
                    navigateChatHistory(-1);
                } else if (key == GLFW_KEY_DOWN) {
                    navigateChatHistory(1);
                } else if (key == GLFW_KEY_TAB) {
                    completeChatInput();
                } else if (key == GLFW_KEY_PAGE_UP) {
                    chatScroll = Math.min(Math.max(0, chatLog.size() - 1), chatScroll + 7);
                } else if (key == GLFW_KEY_PAGE_DOWN) {
                    chatScroll = Math.max(0, chatScroll - 7);
                } else if (control && key == GLFW_KEY_A) {
                    chatSelection = 0;
                    chatCursor = chatInput.length();
                } else if (control && key == GLFW_KEY_C) {
                    String selected = selectedChatText();
                    if (!selected.isEmpty()) glfwSetClipboardString(window, selected);
                } else if (control && key == GLFW_KEY_X) {
                    String selected = selectedChatText();
                    if (!selected.isEmpty()) {
                        glfwSetClipboardString(window, selected);
                        deleteChatSelection();
                    }
                } else if (control && key == GLFW_KEY_V) {
                    String paste = glfwGetClipboardString(window);
                    if (paste != null && !paste.isEmpty()) insertChatText(paste);
                } else if (key == GLFW_KEY_ESCAPE) {
                    closeChat(false);
                }
            } else if (action == GLFW_PRESS) {
                if (inventoryOpen || creativeInvOpen || chestOpen) return;
                if (key == GLFW_KEY_T) { openChat(false); chatIgnoreNextChar = true; }
                else if (key == GLFW_KEY_SLASH) { openChat(true); chatIgnoreNextChar = true; }
            }
        });
    }

    void initGL() {
        GL.createCapabilities();
        // Select core/ARB VBO entry points for this exact driver before any
        // terrain buffer is created.
        craft3dgl.blaze3d.platform.GLX.init();
        System.out.println("[OpenGL] vendor=" + glGetString(GL_VENDOR)
                + " renderer=" + glGetString(GL_RENDERER)
                + " version=" + glGetString(GL_VERSION)
                + " textureUnits=" + glGetInteger(org.lwjgl.opengl.GL13.GL_MAX_TEXTURE_UNITS));
        glViewport(0, 0, width, height);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.08f);
        glShadeModel(GL_FLAT);
        glDisable(GL_CULL_FACE);
        glClearColor(0.7529412f, 0.84705883f, 1.0f, 1.0f);
        glEnable(GL_FOG);
        // EntityRenderer.setupFog: linear fog from 80% to 100% of the
        // five-chunk render distance.
        glFogi(GL_FOG_MODE, GL_LINEAR);
        glFogf(GL_FOG_START, 64.0f);
        glFogf(GL_FOG_END, 80.0f);
        FloatBuffer fogColor = BufferUtils.createFloatBuffer(4).put(new float[]{0.7529412f, 0.84705883f, 1.0f, 1f});
        fogColor.flip();
        glFogfv(GL_FOG_COLOR, fogColor);
        // Fog hint - jakosc
        glHint(GL_FOG_HINT, GL_NICEST);

        initTranslations();
        textureAtlas = createTextureAtlas();
        // Postprocess init WYLACZONY - zawiesza render pipeline
        // (klasy PostProcessManager + shadery zostaja w kodzie ale nieuzywane)
        fontTexture = createFontTexture();
        renderLoadingScreen(0.20, "Wczytywanie tekstur...");
        // Ladowanie tekstur narzedzi (kilofy, siekiery, miecze, motyki)
        craft3dgl.ui.ToolTextures.load();
        renderLoadingScreen(0.42, "Budowanie modeli przedmiotow...");
        craft3dgl.ui.ToolMeshBuilder.load();
        renderLoadingScreen(0.64, "Wczytywanie interfejsu...");
        craft3dgl.ui.GuiTextures.load();
        renderLoadingScreen(0.78, "Uruchamianie renderera...");
        // Minecraft 1.12 dynamic 16x16 fixed-function lightmap.
        try {
            gameRenderer.init();
        } catch (Throwable t) {
            System.err.println("[GameRenderer] init failed: " + t.getMessage());
            t.printStackTrace();
        }
        lightEngine = new craft3dgl.world.LightEngine(world);
        for (int cx = 0; cx < CHUNKS_X; cx++) {
            for (int cy = 0; cy < CHUNKS_Y; cy++) {
                for (int cz = 0; cz < CHUNKS_Z; cz++) chunks[cx][cy][cz] = new Chunk();
            }
        }
        renderLoadingScreen(1.0, "Gotowe");
    }

    /** Responsive Minecraft-style startup/world loading screen. */
    void renderLoadingScreen(double progress, String status) {
        if (window == NULL || fontRenderer == null) return;
        progress = clamp(progress, 0.0, 1.0);
        glfwPollEvents();
        glViewport(0, 0, width, height);
        org.lwjgl.opengl.GL20.glUseProgram(0);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_FOG);
        glDisable(GL_CULL_FACE);
        glDisable(GL_ALPHA_TEST);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        drawPrettyMenuBackground();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
        glColor4f(0f, 0f, 0f, 0.56f);
        quad(0, 0, width, height);
        glDisable(GL_BLEND);

        String logo = "CRAFT3D";
        float logoScale = Math.max(1.5f, Math.min(3.0f, width / 430.0f));
        int logoW = FontRenderer.textWidth(logo, logoScale);
        drawTextDark(logo, width / 2 - logoW / 2 + 3, height / 2 - 108 + 3, logoScale);
        drawText(logo, width / 2 - logoW / 2, height / 2 - 108, logoScale);

        String message = status == null ? "" : status;
        int messageW = FontRenderer.textWidth(message, 0.86f);
        drawText(message, width / 2 - messageW / 2, height / 2 - 18, 0.86f);

        int barW = Math.min(520, Math.max(240, width - 180));
        int barH = 14;
        int barX = width / 2 - barW / 2;
        int barY = height / 2 + 18;
        glDisable(GL_TEXTURE_2D);
        glColor3f(0.08f, 0.08f, 0.08f); quad(barX - 2, barY - 2, barW + 4, barH + 4);
        glColor3f(0.34f, 0.34f, 0.34f); quad(barX, barY, barW, barH);
        glColor3f(0.45f, 0.78f, 0.22f); quad(barX, barY, (int)Math.round(barW * progress), barH);
        String percent = (int)Math.round(progress * 100.0) + "%";
        int percentW = FontRenderer.textWidth(percent, 0.68f);
        drawText(percent, width / 2 - percentW / 2, barY + 25, 0.68f);
        glfwSwapBuffers(window);

        // Leave the context in the baseline state expected by render().
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.08f);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        glEnable(GL_FOG);
        glColor4f(1f, 1f, 1f, 1f);
    }

    int createFontTexture() {
        // Delegate do FontRenderer (lazy init - wymaga textureAtlas juz ustawione, ale tu zwracamy tylko ID)
        if (fontRenderer == null) fontRenderer = new FontRenderer(textureAtlas);
        return fontRenderer.getFontTexture();
    }

    int createTextureAtlas() {
        return TextureAtlas.createTextureAtlas();
    }

    int jitter(int rgb, int amount) { return TextureAtlas.jitter(rgb, amount); }
    int shadeColor(int rgb, double mul) { return TextureAtlas.shadeColor(rgb, mul); }

    void generateWorld(long seed) {
        worldSeed = seed;
        for (int cx = 0; cx < CHUNKS_X; cx++) for (int cz = 0; cz < CHUNKS_Z; cz++) {
            generatedColumns[cx][cz] = false;
            litColumns[cx][cz] = false;
        }
        waterQueue.clear();
        animals.clear();
        villagers.clear();
        spawnedVillageCells.clear();

        loadingScreenActive = true;
        loadingTerrainDone = 0;
        loadingTerrainTarget = 121; // initial 11x11 area; extra land chunks remain at this stage
        bulkWorldGeneration = true;
        renderLoadingScreen(0.08, "Tworzenie terenu...");

        // Find nearby land by biome before generating blocks, so an ocean at
        // the world centre does not make us build a second, unused spawn area.
        int centerX = WORLD_X / 2;
        int centerZ = WORLD_Z / 2;
        int foundX = centerX, foundZ = centerZ;
        outer:
        for (int r = 0; r < 80; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) != r && Math.abs(dz) != r && r > 0) continue;
                int tx = centerX + dx * 8;
                int tz = centerZ + dz * 8;
                if (tx < 5 || tz < 5 || tx >= WORLD_X - 5 || tz >= WORLD_Z - 5) continue;
                int biome = craft3dgl.world.BiomeGenerator.biomeAt(tx, tz, worldSeed);
                if (biome != BIOME_OCEAN && biome != BIOME_RIVER && biome != BIOME_BEACH) {
                    foundX = tx;
                    foundZ = tz;
                    break outer;
                }
            }
        }

        int landCx = clampInt(foundX / CHUNK, 0, CHUNKS_X - 1);
        int landCz = clampInt(foundZ / CHUNK, 0, CHUNKS_Z - 1);
        generateChunksAround(landCx, landCz, 5, 9999);
        bulkWorldGeneration = false;

        int[] safe = findSafePlayerSpawn(foundX, foundZ, 48);
        x = safe[0] + 0.5;
        y = safe[1];
        z = safe[2] + 0.5;
        clearSpawnVegetation(safe[0], safe[1], safe[2]);
        System.out.println("[Spawn] Bezpieczny spawn na ladzie: " + safe[0] + "," + safe[2] + " y=" + safe[1]);

        renderLoadingScreen(0.57, "Obliczanie oswietlenia...");
        rebuildLightAround((int)Math.floor(x) / CHUNK, (int)Math.floor(z) / CHUNK, 6);
        markAllChunksDirty();
    }

    void generateChunksAroundPlayer(int budget) {
        if (!worldLoaded || inMainMenu) return;
        int pcx = clampInt((int)Math.floor(x) / CHUNK, 0, CHUNKS_X - 1);
        int pcz = clampInt((int)Math.floor(z) / CHUNK, 0, CHUNKS_Z - 1);
        generateChunksAround(pcx, pcz, 6, budget);
    }

    void generateChunksAround(int centerCx, int centerCz, int radius, int budget) {
        int made = 0;
        for (int r = 0; r <= radius && made < budget; r++) {
            for (int dx = -r; dx <= r && made < budget; dx++) {
                for (int dz = -r; dz <= r && made < budget; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int cx = centerCx + dx;
                    int cz = centerCz + dz;
                    if (cx < 0 || cz < 0 || cx >= CHUNKS_X || cz >= CHUNKS_Z) continue;
                    if (!generatedColumns[cx][cz]) {
                        generateChunkColumn(cx, cz);
                        made++;
                    }
                }
            }
        }
    }

    void generateChunkColumn(int cx, int cz) {
        if (generatedColumns[cx][cz]) return;
        generatedColumns[cx][cz] = true;
        int minX = cx * CHUNK;
        int minZ = cz * CHUNK;
        int maxX = Math.min(WORLD_X, minX + CHUNK);
        int maxZ = Math.min(WORLD_Z, minZ + CHUNK);

        int[][] localHeights = new int[CHUNK][CHUNK];
        int[][] localBiomes = new int[CHUNK][CHUNK];
        for (int wx = minX; wx < maxX; wx++) {
            for (int wz = minZ; wz < maxZ; wz++) {
                int biome = biomeAt(wx, wz, worldSeed);
                int h = terrainHeightAt(wx, wz, biome, worldSeed);
                localHeights[wx - minX][wz - minZ] = h;
                localBiomes[wx - minX][wz - minZ] = biome;
                craft3dgl.world.TerrainGenerator.fillColumn(world, wx, wz, h, biome);
                carveCaveNoiseColumn(wx, wz, h);
            }
        }
        repairChunkSurface(minX, minZ, maxX, maxZ, localHeights, localBiomes);
        // WODOSPADY: po wycieciu jaskin, jesli kolumna byla pod woda a jaskinia sie otworzyla, wypelnij WATER
        for (int wx = minX; wx < maxX; wx++) {
            for (int wz = minZ; wz < maxZ; wz++) {
                int h = localHeights[wx - minX][wz - minZ];
                craft3dgl.world.TerrainGenerator.addWaterfalls(world, wx, wz, h);
                craft3dgl.world.TerrainGenerator.placeBedrock(world, wx, wz, worldSeed);
            }
        }
        addVillageInChunk(minX, minZ, maxX, maxZ, localHeights, localBiomes);
        addTreesInChunk(minX, minZ, maxX, maxZ, localHeights, localBiomes);
        addTallGrassInChunk(minX, minZ, maxX, maxZ, localHeights, localBiomes);
        spawnAnimalsInChunk(minX, minZ, maxX, maxZ, localHeights, localBiomes);
        // Uwaga: spawnVillagersInChunk USUNIETY - villagerzy spawnuja sie tylko w wioskach (raz).
        // During initial generation one combined light rebuild is much cheaper
        // than rebuilding the same padded neighbourhood for every column.
        if (lightEngine != null && !bulkWorldGeneration) {
            lightEngine.rebuildRegion(minX, minZ, maxX - 1, maxZ - 1);
            litColumns[cx][cz] = true;
        }
        markChunkColumnDirty(cx, cz);
        if (loadingScreenActive && bulkWorldGeneration) {
            loadingTerrainDone++;
            double part = Math.min(1.0, loadingTerrainDone / (double)Math.max(1, loadingTerrainTarget));
            renderLoadingScreen(0.10 + part * 0.44, "Tworzenie terenu...");
        }
    }

    int terrainHeightAt(int wx, int wz, int biome, long seed) { return BiomeGenerator.terrainHeightAt(wx, wz, biome, seed); }

    void carveCaveNoiseColumn(int wx, int wz, int surface) {
        craft3dgl.world.TerrainGenerator.carveCaves(world, wx, wz, surface, worldSeed);
    }

    void repairChunkSurface(int minX, int minZ, int maxX, int maxZ, int[][] hmap, int[][] biomeMap) {
        craft3dgl.world.TerrainGenerator.repairSurface(world, minX, minZ, maxX, maxZ, hmap, biomeMap);
    }

    void addTreesInChunk(int minX, int minZ, int maxX, int maxZ, int[][] hmap, int[][] biomeMap) {
        Random rand = new Random(worldSeed ^ (minX * 73428767L) ^ (minZ * 9122719L));
        for (int wx = minX + 4; wx < maxX - 4; wx++) for (int wz = minZ + 4; wz < maxZ - 4; wz++) {
            int h = hmap[wx - minX][wz - minZ];
            int biome = biomeMap[wx - minX][wz - minZ];
            if (h <= WATER_LEVEL || world[wx][h][wz] != GRASS) continue;
            double chance = biome == BIOME_FOREST ? 0.045 : biome == BIOME_PLAINS ? 0.005 : biome == BIOME_MOUNTAINS ? 0.010 : 0.0;
            if (rand.nextDouble() < chance && canPlaceTree(wx, h + 1, wz, biome == BIOME_FOREST ? 5 : 4)) {
                if (biome == BIOME_MOUNTAINS) makePineTree(wx, h + 1, wz, rand);
                else if (biome == BIOME_FOREST && rand.nextDouble() < 0.06) makeBigOakTree(wx, h + 1, wz, rand);
                else makeOakTree(wx, h + 1, wz, rand);
            }
        }
    }

    void addTallGrassInChunk(int minX, int minZ, int maxX, int maxZ, int[][] hmap, int[][] biomeMap) {
        Random rand = new Random(worldSeed ^ (minX * 31337L) ^ (minZ * 7919L) ^ 0xF00DL);
        for (int wx = minX; wx < maxX; wx++) {
            for (int wz = minZ; wz < maxZ; wz++) {
                int h = hmap[wx - minX][wz - minZ];
                int biome = biomeMap[wx - minX][wz - minZ];
                if (biome == BIOME_DESERT) continue;
                if (h + 1 >= WORLD_Y) continue;
                if (world[wx][h][wz] != GRASS) continue;
                if (world[wx][h + 1][wz] != AIR) continue;
                // Plains: 12%, Forest: 18%, Mountains: 6%
                double chance = biome == BIOME_FOREST ? 0.18 : biome == BIOME_MOUNTAINS ? 0.06 : 0.12;
                if (rand.nextDouble() < chance) {
                    world[wx][h + 1][wz] = (byte) TALL_GRASS;
                }
            }
        }
    }

    void spawnAnimalsInChunk(int minX, int minZ, int maxX, int maxZ, int[][] hmap, int[][] biomeMap) {
        craft3dgl.entities.AnimalSpawner.spawnInChunk(minX, minZ, maxX, maxZ, hmap, biomeMap, animalSpawnCtx);
    }

    void markChunkColumnDirty(int cx, int cz) {
        for (int cy = 0; cy < CHUNKS_Y; cy++) markChunk(cx, cy, cz);
        if (cx > 0) for (int cy = 0; cy < CHUNKS_Y; cy++) markChunk(cx - 1, cy, cz);
        if (cz > 0) for (int cy = 0; cy < CHUNKS_Y; cy++) markChunk(cx, cy, cz - 1);
        if (cx + 1 < CHUNKS_X) for (int cy = 0; cy < CHUNKS_Y; cy++) markChunk(cx + 1, cy, cz);
        if (cz + 1 < CHUNKS_Z) for (int cy = 0; cy < CHUNKS_Y; cy++) markChunk(cx, cy, cz + 1);
    }

    int biomeAt(int x, int z, long seed) { return BiomeGenerator.biomeAt(x, z, seed); }

    double fbm(double x, double z, long seed, int octaves) { return BiomeGenerator.fbm(x, z, seed, octaves); }
    double smoothNoise(double x, double z, long seed) { return BiomeGenerator.smoothNoise(x, z, seed); }
    double randomValue(int x, int z, long seed) { return BiomeGenerator.randomValue(x, z, seed); }
    double riverNoise(int x, int z, long seed) { return BiomeGenerator.riverNoise(x, z, seed); }
    double lakeNoise(int x, int z, long seed) { return BiomeGenerator.lakeNoise(x, z, seed); }
    double lerp(double a, double b, double t) { return BiomeGenerator.lerp(a, b, t); }

    boolean canPlaceTree(int x, int y, int z, int radius) { return craft3dgl.world.TerrainGenerator.canPlaceTree(world, x, y, z, radius, WORLD_X, WORLD_Z); }

    void spawnAnimalsInLoadedWorld(Random rand) {
        craft3dgl.entities.AnimalSpawner.spawnInLoadedWorld(animalSpawnCtx, rand);
    }

    int findSurfaceSpawnY(int ax, int az) { return craft3dgl.entities.AnimalSpawner.findSurfaceSpawnY(world, ax, az); }

    boolean validAnimalSpawn(int ax, int ay, int az) { return craft3dgl.entities.AnimalSpawner.isValidSpawn(ax, ay, az, animalSpawnCtx); }

    // BlockSetter adapter dla TreeGenerator
    private final TreeGenerator.BlockSetter treeBlockSetter = new TreeGenerator.BlockSetter() {
        @Override public void setBlockRaw(int x, int y, int z, int id) { MinecraftGL.this.setBlockRaw(x, y, z, id); }
        @Override public int getBlockRaw(int x, int y, int z) { return world[x][y][z] & 0xff; }
        @Override public boolean inWorld(int x, int y, int z) { return MinecraftGL.this.inWorld(x, y, z); }
    };

    final craft3dgl.entities.AnimalSpawner.SpawnContext animalSpawnCtx = new craft3dgl.entities.AnimalSpawner.SpawnContext() {
        @Override public craft3dgl.entities.EntityCollision.SolidCheck solidCheck() { return entitySolidCheck; }
        @Override public byte[][][] world() { return world; }
        @Override public long worldSeed() { return worldSeed; }
        @Override public java.util.List<AnimalGL> animals() { return animals; }
        @Override public void unstickAnimal(AnimalGL a) { MinecraftGL.this.unstickAnimal(a); }
    };

    void makeOakTree(int x, int y, int z, Random rand) { TreeGenerator.makeOakTree(x, y, z, rand, treeBlockSetter); }
    void makeBigOakTree(int x, int y, int z, Random rand) { TreeGenerator.makeBigOakTree(x, y, z, rand, treeBlockSetter); }
    void makePineTree(int x, int y, int z, Random rand) { TreeGenerator.makePineTree(x, y, z, rand, treeBlockSetter); }

    void spawnPlayer() {
        int[] safe = findSafePlayerSpawn(WORLD_X / 2, WORLD_Z / 2, 64);
        x = safe[0] + 0.5;
        y = safe[1];
        z = safe[2] + 0.5;
        clearSpawnVegetation(safe[0], safe[1], safe[2]);
    }

    /** Finds solid ground with two replaceable blocks above it. */
    int[] findSafePlayerSpawn(int centerX, int centerZ, int radius) {
        for (int pass = 0; pass < 2; pass++) {
            for (int r = 0; r <= radius; r++) {
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    int sx = centerX + dx, sz = centerZ + dz;
                    if (sx < 2 || sz < 2 || sx >= WORLD_X - 2 || sz >= WORLD_Z - 2) continue;
                    int sy = playerSpawnYAt(sx, sz, pass == 0);
                    if (sy > 0) return new int[]{sx, sy, sz};
                }
            }
        }

        // Last-resort grass platform: never return y=-1 or spawn in water/void.
        int sx = clampInt(centerX, 3, WORLD_X - 4);
        int sz = clampInt(centerZ, 3, WORLD_Z - 4);
        int sy = Math.min(WORLD_Y - 3, WATER_LEVEL + 4);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            world[sx + dx][sy - 1][sz + dz] = (byte)GRASS;
            world[sx + dx][sy][sz + dz] = (byte)AIR;
            world[sx + dx][sy + 1][sz + dz] = (byte)AIR;
        }
        return new int[]{sx, sy, sz};
    }

    int playerSpawnYAt(int sx, int sz, boolean requireGrass) {
        for (int groundY = WORLD_Y - 3; groundY >= 1; groundY--) {
            int ground = world[sx][groundY][sz] & 0xff;
            if (requireGrass && ground != GRASS) continue;
            if (!requireGrass && craft3dgl.world.LightEngine.lightOpacity(ground) < 15) continue;
            if (ground == CHEST || ground == CRAFTING_TABLE) continue;
            int feet = world[sx][groundY + 1][sz] & 0xff;
            int head = world[sx][groundY + 2][sz] & 0xff;
            if (isSpawnReplaceable(feet) && isSpawnReplaceable(head)) return groundY + 1;
        }
        return -1;
    }

    boolean isSpawnReplaceable(int id) {
        return id == AIR || id == TALL_GRASS
                || id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3;
    }

    void clearSpawnVegetation(int sx, int sy, int sz) {
        if (inWorld(sx, sy, sz) && isSpawnReplaceable(world[sx][sy][sz] & 0xff)) world[sx][sy][sz] = (byte)AIR;
        if (inWorld(sx, sy + 1, sz) && isSpawnReplaceable(world[sx][sy + 1][sz] & 0xff)) world[sx][sy + 1][sz] = (byte)AIR;
    }

    void rebuildLightAround(int centerCx, int centerCz, int radius) {
        if (lightEngine == null) return;
        int minCx = Math.max(0, centerCx - radius);
        int maxCx = Math.min(CHUNKS_X - 1, centerCx + radius);
        int minCz = Math.max(0, centerCz - radius);
        int maxCz = Math.min(CHUNKS_Z - 1, centerCz + radius);
        lightEngine.rebuildRegion(minCx * CHUNK, minCz * CHUNK,
                Math.min(WORLD_X - 1, (maxCx + 1) * CHUNK - 1),
                Math.min(WORLD_Z - 1, (maxCz + 1) * CHUNK - 1));
        for (int cx = minCx; cx <= maxCx; cx++) for (int cz = minCz; cz <= maxCz; cz++) {
            if (generatedColumns[cx][cz]) litColumns[cx][cz] = true;
        }
    }

    /** Lazily prepares one old/save-game column as it enters render distance. */
    void ensureLightingAroundPlayer() {
        if (lightEngine == null) return;
        int pcx = clampInt((int)Math.floor(x) / CHUNK, 0, CHUNKS_X - 1);
        int pcz = clampInt((int)Math.floor(z) / CHUNK, 0, CHUNKS_Z - 1);
        for (int r = 0; r <= 6; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                int cx = pcx + dx, cz = pcz + dz;
                if (cx < 0 || cz < 0 || cx >= CHUNKS_X || cz >= CHUNKS_Z) continue;
                if (!generatedColumns[cx][cz] || litColumns[cx][cz]) continue;
                int minX = cx * CHUNK, minZ = cz * CHUNK;
                lightEngine.rebuildRegion(minX, minZ, minX + CHUNK - 1, minZ + CHUNK - 1);
                litColumns[cx][cz] = true;
                markChunkColumnDirty(cx, cz);
                return;
            }
        }
    }

    // ============================================================
    // ====== WIOSKI - z poprawionymi domkami + spawn raz ======
    // ============================================================

    static final int[][] HOUSE_DEFS = craft3dgl.world.VillageGenerator.HOUSE_DEFS;

    void addVillageInChunk(int minX, int minZ, int maxX, int maxZ, int[][] hmap, int[][] biomeMap) {
        int grid = 80;
        int cellMinX = Math.floorDiv(minX - 72, grid);
        int cellMaxX = Math.floorDiv(maxX + 72, grid);
        int cellMinZ = Math.floorDiv(minZ - 72, grid);
        int cellMaxZ = Math.floorDiv(maxZ + 72, grid);

        for (int gx = cellMinX; gx <= cellMaxX; gx++) {
            for (int gz = cellMinZ; gz <= cellMaxZ; gz++) {
                if (!villageExists(gx, gz)) continue;
                int[] c = villageCenter(gx, gz);
                int vcx = c[0], vcz = c[1];
                if (vcx < 8 || vcz < 8 || vcx >= WORLD_X - 8 || vcz >= WORLD_Z - 8) continue;
                int biome = biomeAt(vcx, vcz, worldSeed);
                if (biome == BIOME_MOUNTAINS) continue;
                int centerH = terrainHeightAt(vcx, vcz, biome, worldSeed);
                if (centerH <= WATER_LEVEL + 1) continue;

                // FAZA 1: per-chunk - kazdy chunk buduje swoja czesc
                for (int wx = minX; wx < maxX; wx++) {
                    for (int wz = minZ; wz < maxZ; wz++) {
                        buildVillageColumn(wx, wz, vcx, vcz, centerH, biome);
                    }
                }

                // FAZA 2: dla KAZDEGO domku ktorego JAKIKOLWIEK fragment wpada w nasz chunk,
                // przebuduj CALY domek (nawet czesci spoza chunka). Idempotent - nadpisze poprawnie.
                // To gwarantuje ze WSZYSTKIE sciany/dach beda na miejscu, niezaleznie od kolejnosci generacji.
                for (int[] h : HOUSE_DEFS) {
                    int hx = vcx + h[0], hz = vcz + h[1], hw = h[2], hd = h[3], htype = h[4];
                    // Czy dom dotyka tego chunka?
                    boolean overlaps = !(hx + hw <= minX || hx >= maxX || hz + hd <= minZ || hz >= maxZ);
                    if (!overlaps) continue;
                    // Przebuduj CALY dom (clamp do swiata)
                    for (int wx = Math.max(0, hx); wx < Math.min(WORLD_X, hx + hw); wx++) {
                        for (int wz = Math.max(0, hz); wz < Math.min(WORLD_Z, hz + hd); wz++) {
                            buildHouseColumn(wx, wz, hx, hz, hw, hd, htype, centerH, biome == BIOME_DESERT);
                        }
                    }
                }

                // Spawn villagerow TYLKO raz na wioske
                if (vcx >= minX && vcx < maxX && vcz >= minZ && vcz < maxZ) {
                    long cellKey = packCell(gx, gz);
                    if (!spawnedVillageCells.contains(cellKey)) {
                        spawnedVillageCells.add(cellKey);
                        spawnVillageVillagers(vcx, centerH + 1, vcz);
                    }
                }
            }
        }
    }

    /** Pomocnicza komenda /rebuildvillages - przebudowuje wszystkie wioski w pobliskich chunkach */
    void rebuildNearbyVillages() {
        int range = 80; // bloki w promieniu od gracza
        int grid = 80;
        int cellMinX = Math.floorDiv((int)x - range, grid);
        int cellMaxX = Math.floorDiv((int)x + range, grid);
        int cellMinZ = Math.floorDiv((int)z - range, grid);
        int cellMaxZ = Math.floorDiv((int)z + range, grid);
        int rebuilt = 0;
        for (int gx = cellMinX; gx <= cellMaxX; gx++) {
            for (int gz = cellMinZ; gz <= cellMaxZ; gz++) {
                if (!villageExists(gx, gz)) continue;
                int[] c = villageCenter(gx, gz);
                int vcx = c[0], vcz = c[1];
                if (vcx < 8 || vcz < 8 || vcx >= WORLD_X - 8 || vcz >= WORLD_Z - 8) continue;
                int biome = biomeAt(vcx, vcz, worldSeed);
                if (biome == BIOME_MOUNTAINS) continue;
                int centerH = terrainHeightAt(vcx, vcz, biome, worldSeed);
                if (centerH <= WATER_LEVEL + 1) continue;
                // Przebuduj WSZYSTKO: drogi, studnia, domki
                for (int[] h : HOUSE_DEFS) {
                    int hx = vcx + h[0], hz = vcz + h[1], hw = h[2], hd = h[3], htype = h[4];
                    for (int wx = Math.max(0, hx); wx < Math.min(WORLD_X, hx + hw); wx++) {
                        for (int wz = Math.max(0, hz); wz < Math.min(WORLD_Z, hz + hd); wz++) {
                            buildHouseColumn(wx, wz, hx, hz, hw, hd, htype, centerH, biome == BIOME_DESERT);
                        }
                    }
                }
                rebuilt++;
            }
        }
        // Mark all loaded chunks dirty zeby przerenderowac
        markAllChunksDirty();
        addChatMessage("Przebudowano wiosek: " + rebuilt);
    }

    boolean villageExists(int gx, int gz) { return craft3dgl.world.VillageGenerator.villageExists(gx, gz, worldSeed); }

    int[] villageCenter(int gx, int gz) { return craft3dgl.world.VillageGenerator.villageCenter(gx, gz, worldSeed); }

    void buildVillageColumn(int wx, int wz, int vcx, int vcz, int baseY, int biome) {
        int dx = wx - vcx, dz = wz - vcz;
        int adx = Math.abs(dx), adz = Math.abs(dz);
        boolean desert = biome == BIOME_DESERT;

        // Drogi krzyzowe
        if ((adx <= 2 && adz <= 30) || (adz <= 2 && adx <= 30)) {
            flattenColumn(wx, wz, baseY, desert ? SAND : DIRT);
            return;
        }

        for (int[] h : HOUSE_DEFS) {
            int hx = vcx + h[0], hz = vcz + h[1], hw = h[2], hd = h[3], htype = h[4];
            if (wx >= hx && wx < hx + hw && wz >= hz && wz < hz + hd) {
                buildHouseColumn(wx, wz, hx, hz, hw, hd, htype, baseY, desert);
                return;
            }
        }

        // Studnia w centrum
        if (adx <= 2 && adz <= 2) {
            flattenColumn(wx, wz, baseY, STONE);
            if (adx <= 1 && adz <= 1) {
                clearAbove(wx, wz, baseY + 1, baseY + 5);
                world[wx][baseY + 1][wz] = WATER;
                if (adx == 1 || adz == 1) world[wx][baseY + 2][wz] = STONE;
            }
        }
    }

    void flattenColumn(int wx, int wz, int y, int topBlock) {
        if (!inWorld(wx, y, wz)) return;
        for (int yy = 1; yy < y; yy++) if (world[wx][yy][wz] == AIR || world[wx][yy][wz] == WATER) world[wx][yy][wz] = DIRT;
        world[wx][y][wz] = (byte)topBlock;
        clearAbove(wx, wz, y + 1, y + 8);
    }

    void clearAbove(int wx, int wz, int y0, int y1) {
        for (int yy = Math.max(1, y0); yy <= Math.min(WORLD_Y - 1, y1); yy++) world[wx][yy][wz] = AIR;
    }

    /** Defensywna implementacja domku:
     *  - PELNE 4 sciany od baseY+1 do baseY+hWall
     *  - Slupki narozne z WOOD (wizualnie)
     *  - Dach na baseY+hWall+1 - PEWNIE rysowany dla KAZDEJ komorki domu
     *  - Drzwi na lz=0, lx=hw/2 - z BELKA i DACHEM nad nimi
     *  - Skrzynka i crafting table w narozach wnetrza + LOOT w skrzynce
     */
    void buildHouseColumn(int wx, int wz, int hx, int hz, int hw, int hd, int htype, int baseY, boolean desert) {
        // 1. PODLOGA - wyrownaj kolumne do baseY i postaw PLANKS jako podloge
        flattenColumn(wx, wz, baseY, PLANKS);

        int lx = wx - hx, lz = wz - hz;
        boolean cornerX = (lx == 0 || lx == hw - 1);
        boolean cornerZ = (lz == 0 || lz == hd - 1);
        boolean isCorner = cornerX && cornerZ;
        boolean edge = lx == 0 || lz == 0 || lx == hw - 1 || lz == hd - 1;
        // Drzwi POSRODKU SCIANY POLUDNIOWEJ. Drzwi NIE moga byc w narozniku.
        int doorLx = hw / 2;
        boolean isDoorCell = (lz == 0 && lx == doorLx && !isCorner);

        int wall = desert ? SAND : PLANKS;
        int post = desert ? SAND : WOOD;
        int hWall = 3;
        int roofY = baseY + hWall + 1;

        // 2. WYCZYSC WSZYSTKO POWYZEJ baseY DO roofY+1 - upewniamy sie ze nic nie wystaje
        //    (np. terrain, drzewa, woda) zanim postawimy sciany i dach
        for (int yy = baseY + 1; yy <= roofY + 2 && yy < WORLD_Y; yy++) {
            world[wx][yy][wz] = (byte) AIR;
        }

        // 3. SCIANY / SLUPY
        if (isCorner) {
            // Slupek narozny - cala wysokosc WOOD/POST
            for (int yy = baseY + 1; yy <= baseY + hWall; yy++) world[wx][yy][wz] = (byte) post;
        } else if (edge) {
            if (isDoorCell) {
                // Drzwi - dol + gora + BELKA NAD NIMI (zeby dach mial na czym sie wesprzec)
                world[wx][baseY + 1][wz] = DOOR_BOTTOM;
                world[wx][baseY + 2][wz] = DOOR_TOP;
                // Facing 2 = panel na +Z (do wewnatrz). Gracz wchodzi od strony -Z.
                int facing = 2;
                setDoorMeta(wx, baseY + 1, wz, facing);
                setDoorMeta(wx, baseY + 2, wz, facing);
                // Belka nad drzwiami zeby ZAMKNAC otwor i dac dachowi podpore
                world[wx][baseY + hWall][wz] = (byte) wall;
            } else {
                // PELNA SCIANA bez dziur od baseY+1 do baseY+hWall (3 bloki wysokosci)
                for (int yy = baseY + 1; yy <= baseY + hWall; yy++) world[wx][yy][wz] = (byte) wall;
            }
        }
        // Komorki wewnetrzne - juz wyczyszczone w kroku 2, zostawiamy AIR

        // 4. DACH - PEWNIE dla KAZDEJ komorki domu (edge+interior+corner)
        int roofMat = desert ? SAND : (htype == 1 ? WOOD : PLANKS);
        world[wx][roofY][wz] = (byte) roofMat;

        // 5. MEBLE w narozach wnetrza (NIE na drzwiach, NIE na slupkach)
        if (!isCorner && !isDoorCell && !edge) {
            // wnetrze - mozemy postawic chest/crafting
            if (lx == 1 && lz == hd - 2) {
                world[wx][baseY + 1][wz] = CHEST;
                // LOOT: slabe przedmioty w skrzynce wioskowej
                fillVillageChestLoot(wx, baseY + 1, wz);
            }
            if (lx == hw - 2 && lz == hd - 2) world[wx][baseY + 1][wz] = CRAFTING_TABLE;
        }
    }

    /** Loot do skrzynki wioskowej - SLABE przedmioty jak w MC village chest */
    void fillVillageChestLoot(int cx, int cy, int cz) {
        int[] ids = chestIdsAt(cx, cy, cz);
        int[] cnts = chestCountsAt(cx, cy, cz);
        // Deterministyczny seed dla danej skrzynki (zeby sie nie generowalo na nowo przy reloadzie)
        Random r = new Random(((long)cx * 91138233L) ^ ((long)cz * 27361291L) ^ ((long)cy * 19349663L) ^ worldSeed);
        // Pula slabego lootu (id, max ilosc, szansa 0..1)
        int[][] loot = {
            {ITEM_BREAD, 3, 60},      // 60% szans, 1-3 chleba
            {ITEM_STICK, 4, 50},      // 50% szans, 1-4 patyki
            {WOOD, 3, 45},            // 45% szans, 1-3 drewno
            {PLANKS, 4, 40},          // 40% szans, 1-4 deski
            {DIRT, 5, 30},            // 30% szans, 1-5 ziemia
            {ITEM_PORK, 2, 25},       // 25% szans, 1-2 mieso
            {ITEM_WOOD_PICKAXE, 1, 18}, // 18% szans, drewniany kilof
            {ITEM_WOOD_AXE, 1, 15},   // 15% szans, drewniana siekiera
            {ITEM_EMERALD, 1, 8},     // 8% szans, 1 szmaragd (rzadki!)
        };
        // Wybierz 2-4 sloty losowo i wsadz loot
        int filled = 0;
        int maxItems = 2 + r.nextInt(3); // 2-4 itemy w skrzynce
        for (int attempt = 0; attempt < 20 && filled < maxItems; attempt++) {
            int[] entry = loot[r.nextInt(loot.length)];
            if (r.nextInt(100) >= entry[2]) continue;
            // Znajdz pusty slot
            int slotIdx = -1;
            for (int s = 0; s < CHEST_SIZE; s++) {
                int idx = r.nextInt(CHEST_SIZE);
                if (ids[idx] == 0) { slotIdx = idx; break; }
            }
            if (slotIdx < 0) break;
            ids[slotIdx] = entry[0];
            cnts[slotIdx] = 1 + r.nextInt(entry[1]);
            filled++;
        }
    }

    void spawnVillageVillagers(int vx, int vy, int vz) {
        // Spawn 3-5 villagerow per dom = 15-25 villagerow na wioske. Raz na cala wioske,
        // po smierci NIE respawnuja sie (spawnedVillageCells trzyma "juz spawnowane").
        Random rand = new Random(((long) vx * 73428767L) ^ ((long) vz * 9122719L) ^ worldSeed);
        for (int i = 0; i < HOUSE_DEFS.length; i++) {
            int[] h = HOUSE_DEFS[i];
            int hx = vx + h[0], hz = vz + h[1], hw = h[2], hd = h[3];
            // Spawn 3-5 villagerow per dom w obszarze ~6x6 wokol domku
            int countPerHouse = 3 + rand.nextInt(3);
            int spawned = 0;
            for (int tries = 0; tries < 40 && spawned < countPerHouse; tries++) {
                // Losuj pozycje wokol drzwi (przed domkiem od strony -Z)
                int tx = hx + rand.nextInt(hw + 4) - 2;
                int tz = hz - 1 - rand.nextInt(4); // 1-4 bloki przed drzwiami
                if (tx < 2 || tz < 2 || tx >= WORLD_X - 2 || tz >= WORLD_Z - 2) continue;
                int ty = findSurfaceSpawnY(tx, tz);
                if (ty <= 0) continue;
                if (!validVillagerSpawn(tx, ty, tz)) continue;
                int prof = rand.nextInt(5); // 1.12: farmer, librarian, priest, smith, butcher
                VillagerGL v = new VillagerGL(tx + 0.5, ty, tz + 0.5, prof);
                v.onGround = true;
                v.homeX = vx;
                v.homeZ = vz;
                v.hasHome = true;
                villagers.add(v);
                spawned++;
            }
        }
    }

    boolean validVillagerSpawn(int bx, int by, int bz) {
        if (!inWorld(bx, by, bz) || by + 2 >= WORLD_Y) return false;
        if (!solid(bx, by - 1, bz)) return false;
        if (world[bx][by][bz] != AIR || world[bx][by + 1][bz] != AIR) return false;
        for (VillagerGL v : villagers) {
            double dx = v.x - (bx + 0.5), dz = v.z - (bz + 0.5);
            if (dx * dx + dz * dz < 1.5 * 1.5) return false;
        }
        return true;
    }

    void loop() {
        long last = System.nanoTime();
        while (!glfwWindowShouldClose(window)) {
            long now = System.nanoTime();
            double dt = (now - last) / 1_000_000_000.0;
            last = now;
            if (dt > 0.05) dt = 0.05;
            glfwPollEvents();
            try {
                if (inMainMenu) {
                    updateMenu();
                    if (inMainMenu) renderMenu();
                    else render();
                } else {
                    update(dt);
                    generateChunksAroundPlayer(3);
                    ensureLightingAroundPlayer();
                    rebuildDirtyChunks();
                    render();
                }
            } catch (Throwable t) {
                writeCrashLog(t);
                inMainMenu = true;
                menuScreen = 0;
                mouseCaptured = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                menuMessage = "Blad: zapisano crash.log";
                renderMenu();
            }
            glfwSwapBuffers(window);
            frames++;
            long t = System.currentTimeMillis();
            if (t - fpsTimer >= 1000) { fps = frames; frames = 0; fpsTimer = t; }
        }
        if (worldLoaded) saveWorld(currentWorldName);
    }

    void updateMenu() {
        boolean mouse = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        int mx = (int) mxA[0], my = (int) myA[0];
        if (mouse && !menuMouseWasDown) {
            sound.playClick();
            if (menuScreen == 0) handleMainMenuClick(mx, my);
            else handleWorldMenuClick(mx, my);
        }
        menuMouseWasDown = mouse;
    }

    void handleMainMenuClick(int mx, int my) {
        int bw = 320, bh = 46;
        int bx = width / 2 - bw / 2;
        int by = height / 2 - 40;
        if (inside(mx, my, bx, by, bw, bh)) { menuScreen = 1; refreshWorldList(); return; }
        if (inside(mx, my, bx, by + 58, bw, bh)) { createNewWorld(); return; }
        if (worldLoaded && inside(mx, my, bx, by + 116, bw, bh)) { saveWorld(currentWorldName); menuMessage = language.equals("en") ? "World saved" : "Swiat zapisany"; return; }
        if (inside(mx, my, bx, by + (worldLoaded ? 174 : 116), bw, bh)) glfwSetWindowShouldClose(window, true);
    }

    void handleWorldMenuClick(int mx, int my) {
        int listX = width / 2 - 260;
        int listY = 150;
        for (int i = 0; i < worldNames.length; i++) {
            int y0 = listY + i * 42;
            if (inside(mx, my, listX, y0, 520, 36)) { selectedWorldIndex = i; loadWorld(worldNames[i]); return; }
        }
        int bw = 240, bh = 42;
        int bx = width / 2 - bw / 2;
        int by = height - 190;
        if (inside(mx, my, bx, by, bw, bh)) { createNewWorld(); return; }
        if (inside(mx, my, bx, by + 54, bw, bh)) { menuScreen = 0; return; }
    }

    void renderMenu() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_FOG);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glDisable(GL_TEXTURE_2D);
        drawPrettyMenuBackground();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.02f, 0.02f, 0.02f, 0.38f);
        quad(width / 2 - 310, 42, 620, 92);
        glColor4f(1f, 1f, 1f, 0.22f);
        lineRect(width / 2 - 310, 42, 620, 92);
        glDisable(GL_BLEND);
        drawText("CRAFT3D OPENGL", width / 2 - 230, 64, 1.25f);
        drawText("LWJGL / OpenGL voxel survival", width / 2 - 210, 112, 0.72f);
        if (menuScreen == 0) drawMainMenu();
        else drawWorldMenu();
        if (menuMessage != null && !menuMessage.isEmpty()) drawText(menuMessage, 24, height - 32, 0.85f);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_FOG);
    }

    void drawPrettyMenuBackground() {
        MenuBackgroundRenderer.draw(width, height);
    }

    void drawMainMenu() {
        int bw = 320, bh = 46;
        int bx = width / 2 - bw / 2;
        int by = height / 2 - 40;
        drawButton(bx, by, bw, bh, tr("menu.singleplayer"));
        drawButton(bx, by + 58, bw, bh, tr("menu.newworld"));
        int quitY = by + 116;
        if (worldLoaded) { drawButton(bx, quitY, bw, bh, tr("menu.savecur")); quitY += 58; }
        drawButton(bx, quitY, bw, bh, tr("menu.quit"));
    }

    void drawWorldMenu() {
        drawText(tr("menu.selectworld"), width / 2 - 80, 118, 1.05f);
        int listX = width / 2 - 260;
        int listY = 150;
        if (worldNames.length == 0) drawText(tr("menu.noworlds"), listX + 120, listY + 20, 0.9f);
        for (int i = 0; i < worldNames.length; i++) {
            int y0 = listY + i * 42;
            drawButton(listX, y0, 520, 36, worldNames[i]);
        }
        int bw = 240, bh = 42;
        int bx = width / 2 - bw / 2;
        int by = height - 190;
        drawButton(bx, by, bw, bh, tr("menu.newworld"));
        drawButton(bx, by + 54, bw, bh, tr("menu.back"));
    }

    void drawButton(int x, int y, int w, int h, String text) {
        // Auto hover detection - sprawdz pozycje kursora
        double[] mxA = new double[1], myA = new double[1];
        try { glfwGetCursorPos(window, mxA, myA); } catch (Throwable ignored) {}
        int mx = (int) mxA[0], my = (int) myA[0];
        boolean hover = inside(mx, my, x, y, w, h);
        boolean pressed = hover && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        int state = pressed ? 2 : (hover ? 1 : 0);
        MenuButton.drawButtonState(fontRenderer, x, y, w, h, text, state);
    }

    /** Autofit button - dopasowuje szerokosc do tekstu. */
    int drawButtonAuto(int centerX, int y, String text, float scale) {
        double[] mxA = new double[1], myA = new double[1];
        try { glfwGetCursorPos(window, mxA, myA); } catch (Throwable ignored) {}
        int mx = (int) mxA[0], my = (int) myA[0];
        int tw = craft3dgl.ui.FontRenderer.textWidth(text, scale);
        int w = Math.max(120, tw + 48);
        int h = Math.max(32, (int)(craft3dgl.ui.FontRenderer.FONT_CELL * scale) + 14);
        int x = centerX - w / 2;
        boolean hover = inside(mx, my, x, y, w, h);
        boolean pressed = hover && glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        int state = pressed ? 2 : (hover ? 1 : 0);
        return MenuButton.drawButtonAuto(fontRenderer, centerX, y, text, scale, state);
    }

    void createNewWorld() {
        try {
            String name = "World_" + System.currentTimeMillis();
            currentWorldName = name;
            loadingScreenActive = true;
            worldLoaded = false;
            renderLoadingScreen(0.02, "Przygotowywanie nowego swiata...");
            clearAllState();
            generateWorld(System.currentTimeMillis());
            worldLoaded = true;
            renderLoadingScreen(0.64, "Budowanie terenu...");
            rebuildInitialChunksForLoading(3, 0.64, 0.91);
            renderLoadingScreen(0.93, "Zapisywanie swiata...");
            saveWorld(name);
            renderLoadingScreen(1.0, "Dolaczanie do swiata...");
            loadingScreenActive = false;
            inMainMenu = false;
            paused = false;
            inventoryOpen = false;
            deathScreen = false;
            mouseCaptured = true;
            firstMouse = true;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } catch (Throwable t) {
            bulkWorldGeneration = false;
            loadingScreenActive = false;
            writeCrashLog(t);
            inMainMenu = true;
            menuScreen = 0;
            mouseCaptured = false;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            menuMessage = "Nie udalo sie stworzyc swiata - crash.log";
        }
    }

    void refreshWorldList() {
        worldNames = craft3dgl.save.SaveIO.listWorlds();
        selectedWorldIndex = Math.min(selectedWorldIndex, Math.max(0, worldNames.length - 1));
    }

    void clearWorld() {
        for (int xx = 0; xx < WORLD_X; xx++) for (int yy = 0; yy < WORLD_Y; yy++) for (int zz = 0; zz < WORLD_Z; zz++) world[xx][yy][zz] = AIR;
    }

    void clearAllState() {
        clearWorld();
        for (int i = 0; i < INVENTORY_SIZE; i++) { invId[i] = 0; invCount[i] = 0; }
        for (int i = 0; i < 9; i++) { craftId[i] = 0; craftCount[i] = 0; }
        for (int i = 0; i < 5; i++) { equipId[i] = 0; equipCount[i] = 0; }
        drops.clear(); animals.clear(); villagers.clear();
        spawnedVillageCells.clear();
        cursorId = cursorCount = 0; selectedSlot = 0; cameraMode = 0; velY = 0; bodyYaw = yaw;
        health = maxHealth; hunger = 20; hungerTimer = regenTimer = starveTimer = 0;
        deathScreen = false; deathDropsDone = false; paused = false;
        villagerTradeOpen = false; tradingVillager = null; pauseScreen = 0;
        miningHit = null; miningProgress = 0; sneaking = false;
        eatingItemId = 0; eatingProgress = 0; eatingSoundStep = 0;
        waterSim.clear();
        for (int cx = 0; cx < CHUNKS_X; cx++) for (int cz = 0; cz < CHUNKS_Z; cz++) {
            generatedColumns[cx][cz] = false;
            litColumns[cx][cz] = false;
        }
        gameMode = GAMEMODE_SURVIVAL; flying = false;
        chatOpen = false; creativeInvOpen = false; creativeTab = 0;
        creativeSearch.setLength(0); chatInput.setLength(0); chatLog.clear();
        doorMeta.clear();
        chestStorage.clear(); chestOpen = false; chestLidProgress = 0f;
        swingTimer = 0; ticksSinceLastSwing = 1000.0;
        lastAttackItemId = Integer.MIN_VALUE; sprintDisableTimer = 0.0;
        walkPhase = 0; lastPosInit = false; wasInWater = false;
        gameTime = 600.0;
        rainSoundTimer = 0.0;
        weather.load(false, false, 12000, 12000, 0f, 0f);
        particleSystem.clear();
    }

    void saveWorld(String name) {
        if (name == null || name.isEmpty() || !worldLoaded) return;
        try {
            craft3dgl.save.SaveIO.ensureSaveDir(name);
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(craft3dgl.save.SaveIO.saveFile(name))))) {
                out.writeInt(craft3dgl.save.SaveIO.MAGIC_V2);  // bump magic (nowy format z spawnedVillageCells)
                out.writeInt(WORLD_X); out.writeInt(WORLD_Y); out.writeInt(WORLD_Z);
                out.writeDouble(x); out.writeDouble(y); out.writeDouble(z); out.writeDouble(velY);
                out.writeDouble(yaw); out.writeDouble(pitch); out.writeInt(selectedSlot); out.writeInt(cameraMode);
                for (int xx = 0; xx < WORLD_X; xx++) {
                    for (int yy = 0; yy < WORLD_Y; yy++) out.write(world[xx][yy], 0, WORLD_Z);
                    if (loadingScreenActive && (xx & 31) == 0) {
                        double part = (xx + 1) / (double)WORLD_X;
                        renderLoadingScreen(0.93 + Math.min(1.0, part) * 0.06, "Zapisywanie swiata...");
                    }
                }
                writeArray(out, invId); writeArray(out, invCount); writeArray(out, equipId); writeArray(out, equipCount);
                out.writeInt(health);
                out.writeInt(hunger);
                out.writeInt(gameMode);
                out.writeBoolean(flying);
                out.writeInt(doorMeta.size());
                for (java.util.Map.Entry<Long, Integer> e : doorMeta.entrySet()) {
                    out.writeLong(e.getKey());
                    out.writeInt(e.getValue());
                }
                out.writeInt(chestIds.size());
                for (java.util.Map.Entry<Long, int[]> e : chestIds.entrySet()) {
                    out.writeLong(e.getKey());
                    int[] ids = e.getValue();
                    int[] cnts = chestCounts.get(e.getKey());
                    if (cnts == null) cnts = new int[CHEST_SIZE];
                    for (int i = 0; i < CHEST_SIZE; i++) { out.writeInt(ids[i]); out.writeInt(cnts[i]); }
                }
                out.writeInt(animals.size());
                for (AnimalGL a : animals) {
                    out.writeInt(a.type);
                    out.writeDouble(a.x); out.writeDouble(a.y); out.writeDouble(a.z);
                    out.writeDouble(a.yaw);
                    out.writeInt(Math.max(0, Math.round(a.health)));
                }
                out.writeInt(villagers.size());
                for (VillagerGL v : villagers) {
                    out.writeDouble(v.x); out.writeDouble(v.y); out.writeDouble(v.z);
                    out.writeDouble(v.yaw);
                    out.writeInt(v.profession);
                    out.writeInt(Math.max(0, Math.round(v.health)));
                    out.writeDouble(v.homeX);
                    out.writeDouble(v.homeZ);
                    out.writeBoolean(v.hasHome);
                }
                // Spawnowane wioski.
                out.writeInt(spawnedVillageCells.size());
                for (Long k : spawnedVillageCells) out.writeLong(k);
                // Optional V2 tail. Older V2 saves simply end before this map.
                out.writeInt(chestFacings.size());
                for (java.util.Map.Entry<Long, Integer> e : chestFacings.entrySet()) {
                    out.writeLong(e.getKey());
                    out.writeInt(e.getValue());
                }
                // Optional V2 weather/time tail; old saves remain readable.
                out.writeDouble(gameTime);
                out.writeBoolean(weather.isRaining());
                out.writeBoolean(weather.isThundering());
                out.writeInt(weather.getRainTime());
                out.writeInt(weather.getThunderTime());
                out.writeFloat(weather.getRainStrength());
                out.writeFloat(weather.getRawThunderStrength());
            }
        } catch (Exception e) { menuMessage = "Save error: " + e.getMessage(); }
    }

    void writeArray(DataOutputStream out, int[] arr) throws IOException { craft3dgl.save.SaveIO.writeArray(out, arr); }
    void readArray(DataInputStream in, int[] arr) throws IOException { craft3dgl.save.SaveIO.readArray(in, arr); }

    boolean loadWorld(String name) {
        try {
            File file = craft3dgl.save.SaveIO.saveFile(name);
            if (!file.isFile()) return false;
            loadingScreenActive = true;
            worldLoaded = false;
            renderLoadingScreen(0.02, "Otwieranie swiata...");
            clearAllState();
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
                int magic = in.readInt();
                if (magic != craft3dgl.save.SaveIO.MAGIC_V1 && magic != craft3dgl.save.SaveIO.MAGIC_V2) throw new IOException("bad save");
                boolean v2 = (magic == craft3dgl.save.SaveIO.MAGIC_V2);
                int wx = in.readInt(), wy = in.readInt(), wz = in.readInt();
                x = in.readDouble(); y = in.readDouble(); z = in.readDouble(); velY = in.readDouble();
                yaw = in.readDouble(); pitch = in.readDouble(); selectedSlot = in.readInt(); cameraMode = in.readInt();
                if (wx <= 0 || wy <= 0 || wz <= 0 || wx > 4096 || wy > 512 || wz > 4096) {
                    throw new IOException("invalid world dimensions");
                }
                byte[] savedRow = new byte[wz];
                for (int xx = 0; xx < wx; xx++) {
                    for (int yy = 0; yy < wy; yy++) {
                        in.readFully(savedRow);
                        if (xx >= WORLD_X || yy >= WORLD_Y) continue;
                        int copy = Math.min(wz, WORLD_Z);
                        System.arraycopy(savedRow, 0, world[xx][yy], 0, copy);
                        int cx = xx / CHUNK;
                        for (int startZ = 0; startZ < copy; startZ += CHUNK) {
                            int cz = startZ / CHUNK;
                            if (generatedColumns[cx][cz]) continue;
                            int endZ = Math.min(copy, startZ + CHUNK);
                            for (int zz = startZ; zz < endZ; zz++) {
                                if ((savedRow[zz] & 0xff) != AIR) {
                                    generatedColumns[cx][cz] = true;
                                    break;
                                }
                            }
                        }
                    }
                    if ((xx & 15) == 0) {
                        double part = Math.min(1.0, (xx + 1) / (double)Math.max(1, wx));
                        renderLoadingScreen(0.07 + part * 0.39, "Wczytywanie terenu... " + (int)Math.round(part * 100.0) + "%");
                    }
                }
                readArray(in, invId); readArray(in, invCount); readArray(in, equipId); readArray(in, equipCount);
                if (in.available() >= 4) health = in.readInt();
                if (in.available() >= 4) hunger = in.readInt();
                if (in.available() >= 4) gameMode = in.readInt();
                if (in.available() >= 1) flying = in.readBoolean();
                if (in.available() >= 4) {
                    int dn = in.readInt();
                    for (int i = 0; i < dn; i++) {
                        long k = in.readLong();
                        int v = in.readInt();
                        doorMeta.put(k, v);
                    }
                }
                if (in.available() >= 4) {
                    int cn = in.readInt();
                    for (int i = 0; i < cn; i++) {
                        long k = in.readLong();
                        int[] ids = new int[CHEST_SIZE];
                        int[] cnts = new int[CHEST_SIZE];
                        for (int j = 0; j < CHEST_SIZE; j++) { ids[j] = in.readInt(); cnts[j] = in.readInt(); }
                        chestIds.put(k, ids);
                        chestCounts.put(k, cnts);
                    }
                }
                if (in.available() >= 4) {
                    int n = in.readInt();
                    for (int i = 0; i < n; i++) {
                        int type = in.readInt();
                        AnimalGL a = new AnimalGL(type, in.readDouble(), in.readDouble(), in.readDouble());
                        a.yaw = in.readDouble();
                        a.targetYaw = a.yaw;
                        a.health = Math.min(in.readInt(),
                                craft3dgl.entities.EntityConstants.animalMaxHealth(type));
                        unstickAnimal(a);
                        a.displayY = a.y;
                        a.displayInit = true;
                        animals.add(a);
                    }
                }
                if (in.available() >= 4) {
                    int vn = in.readInt();
                    for (int i = 0; i < vn; i++) {
                        double vx = in.readDouble();
                        double vy = in.readDouble();
                        double vz = in.readDouble();
                        double vyaw = in.readDouble();
                        int profession = in.readInt();
                        VillagerGL vv = new VillagerGL(vx, vy, vz, profession);
                        vv.yaw = vyaw;
                        vv.targetYaw = vyaw;
                        vv.health = in.readInt();
                        if (v2) {
                            vv.homeX = in.readDouble();
                            vv.homeZ = in.readDouble();
                            vv.hasHome = in.readBoolean();
                        }
                        villagers.add(vv);
                    }
                }
                if (v2 && in.available() >= 4) {
                    int sn = in.readInt();
                    for (int i = 0; i < sn; i++) spawnedVillageCells.add(in.readLong());
                }
                if (v2 && in.available() >= 4) {
                    int fn = in.readInt();
                    for (int i = 0; i < fn; i++) {
                        chestFacings.put(in.readLong(), Integer.valueOf(in.readInt() & 3));
                    }
                }
                if (v2 && in.available() >= 26) {
                    gameTime = in.readDouble();
                    boolean raining = in.readBoolean();
                    boolean thundering = in.readBoolean();
                    int rainTime = in.readInt();
                    int thunderTime = in.readInt();
                    float rain = in.readFloat();
                    float thunder = in.readFloat();
                    weather.load(raining, thundering, rainTime, thunderTime, rain, thunder);
                }
                if (animals.isEmpty()) spawnAnimalsInLoadedWorld(new Random(name.hashCode()));
            }
            currentWorldName = name;
            worldSeed = name.hashCode() * 341873128712L;
            deathScreen = false;
            deathDropsDone = false;
            if (health <= 0) health = maxHealth;

            // Corrupt/old saves may contain an invalid player height. Recover
            // near the stored position instead of entering the void.
            if (x < 1 || z < 1 || x >= WORLD_X - 1 || z >= WORLD_Z - 1 || y < 1 || y >= WORLD_Y - 2) {
                int[] safe = findSafePlayerSpawn(clampInt((int)x, 2, WORLD_X - 3),
                        clampInt((int)z, 2, WORLD_Z - 3), 64);
                x = safe[0] + 0.5;
                y = safe[1];
                z = safe[2] + 0.5;
                clearSpawnVegetation(safe[0], safe[1], safe[2]);
            }

            renderLoadingScreen(0.50, "Obliczanie oswietlenia...");
            rebuildLightAround(clampInt((int)Math.floor(x) / CHUNK, 0, CHUNKS_X - 1),
                    clampInt((int)Math.floor(z) / CHUNK, 0, CHUNKS_Z - 1), 6);
            renderLoadingScreen(0.59, "Przygotowywanie blokow...");
            registerMissingChestTileEntities();
            markAllChunksDirty();
            rebuildInitialChunksForLoading(3, 0.61, 0.91);
            renderLoadingScreen(0.93, "Przygotowywanie wody...");
            seedWaterQueue();
            worldLoaded = true;
            renderLoadingScreen(1.0, "Dolaczanie do swiata...");
            loadingScreenActive = false;
            inMainMenu = false;
            mouseCaptured = true;
            firstMouse = true;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            return true;
        } catch (Throwable e) {
            bulkWorldGeneration = false;
            loadingScreenActive = false;
            writeCrashLog(e);
            menuMessage = "Load error: " + e.getMessage();
            return false;
        }
    }

    void openPauseMenu() {
        paused = true;
        pauseScreen = 0;
        inventoryOpen = false;
        mouseCaptured = false;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        saveWorld(currentWorldName);
        sound.playClick();
    }

    void resumeGame() {
        paused = false;
        pauseScreen = 0;
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        sound.playClick();
    }

    void updatePauseMenu(boolean left, boolean esc) {
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        int mx = (int) mxA[0], my = (int) myA[0];
        if (esc && !escWasDown) {
            if (pauseScreen == 1) pauseScreen = 0;
            else resumeGame();
            return;
        }
        if (pauseScreen == 1) {
            updateSettingsScreen(left, mx, my);
            pauseMouseWasDown = left;
            return;
        }
        int bw = 320, bh = 46;
        int bx = width / 2 - bw / 2;
        int by = height / 2 - 76;
        if (left && !pauseMouseWasDown) {
            if (inside(mx, my, bx, by, bw, bh)) resumeGame();
            else if (inside(mx, my, bx, by + 58, bw, bh)) { pauseScreen = 1; settingsTab = 0; sound.playClick(); }
            else if (inside(mx, my, bx, by + 116, bw, bh)) {
                saveWorld(currentWorldName);
                paused = false;
                inMainMenu = true;
                menuScreen = 0;
                mouseCaptured = false;
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                refreshWorldList();
                menuMessage = (language.equals("en") ? "Saved: " : "Zapisano: ") + currentWorldName;
                sound.playClick();
            }
        }
        pauseMouseWasDown = left;
    }

    void updateSettingsScreen(boolean left, int mx, int my) {
        int panelW = 620, panelH = 420;
        int px = width / 2 - panelW / 2;
        int py = height / 2 - panelH / 2;
        String[] tabKeys = {"settings.title", "settings.sounds", "settings.language"};
        int tabW = (panelW - 32) / 3;
        for (int i = 0; i < 3; i++) {
            int tx = px + 16 + i * tabW;
            int ty = py + 36;
            if (left && !pauseMouseWasDown && inside(mx, my, tx, ty, tabW - 4, 30)) {
                settingsTab = i;
                sound.playClick();
                return;
            }
        }
        int bw = 220, bh = 40;
        int bx = px + panelW / 2 - bw / 2;
        int by = py + panelH - 60;
        if (left && !pauseMouseWasDown && inside(mx, my, bx, by, bw, bh)) {
            pauseScreen = 0;
            sound.playClick();
            return;
        }
        if (settingsTab == 0) {
            int sliderX = px + 80;
            int sliderY = py + 130;
            int sliderW = panelW - 160;
            if (left && (draggingVolume || inside(mx, my, sliderX - 8, sliderY - 14, sliderW + 16, 32))) {
                draggingVolume = true;
                double v = (mx - sliderX) / (double) sliderW;
                v = Math.round(clamp(v, 0, 1) * 100.0) / 100.0;
                sound.setVolume(v);
            }
            if (!left) draggingVolume = false;
        } else if (settingsTab == 1) {
            String[] cats = {"music", "blocks", "hostile", "animals", "players", "ambient", "ui"};
            int sliderX = px + 220;
            int sliderW = panelW - 280;
            for (int i = 0; i < cats.length; i++) {
                int sy = py + 90 + i * 36;
                if (left && (draggingSlider == i || (draggingSlider == -1 && inside(mx, my, sliderX - 6, sy - 4, sliderW + 12, 22)))) {
                    draggingSlider = i;
                    double v = (mx - sliderX) / (double) sliderW;
                    v = Math.round(clamp(v, 0, 1) * 100.0) / 100.0;
                    sound.setCategoryVolume(cats[i], v);
                }
            }
            if (!left) draggingSlider = -1;
        } else if (settingsTab == 2) {
            int lbw = 200, lbh = 50;
            int lbx = px + 60;
            int lby = py + 120;
            if (left && !pauseMouseWasDown) {
                if (inside(mx, my, lbx, lby, lbw, lbh)) { language = "pl"; sound.playClick(); }
                else if (inside(mx, my, lbx + lbw + 40, lby, lbw, lbh)) { language = "en"; sound.playClick(); }
            }
        }
    }

    void tickWorldClocks(double dt) {
        // Minecraft 1.12: 24000 ticks / 20 TPS = 1200 real seconds per day.
        gameTime += dt;
        weather.update(dt);

        float rain = weather.getRainStrength();
        if (rain <= 0.01f) {
            rainSoundTimer = 0.0;
            return;
        }
        rainSoundTimer -= dt;
        if (rainSoundTimer <= 0.0) {
            int bx = clampInt((int)Math.floor(x), 0, WORLD_X - 1);
            int bz = clampInt((int)Math.floor(z), 0, WORLD_Z - 1);
            int eyeY = clampInt((int)Math.floor(y + eyeHeight()), 0, WORLD_Y - 1);
            boolean covered = false;
            for (int yy = eyeY + 1; yy < WORLD_Y; yy++) {
                if ((world[bx][yy][bz] & 255) != AIR) { covered = true; break; }
            }
            sound.playRain(covered, rain);
            // The source clips are around two seconds long. Vanilla triggers
            // WEATHER_RAIN periodically from addRainParticles rather than looping it.
            rainSoundTimer = covered ? 3.6 : 1.8;
        }
    }

    void applyPassiveGravity(double dt) {
        if (flying || deathScreen) return;
        if (onGround) return;
        velY -= 18.0 * dt;
        if (velY < -24) velY = -24;
        moveVertical(velY * dt);
        if (y < -5) {
            if (gameMode == GAMEMODE_CREATIVE) { spawnPlayer(); velY = 0; }
            else damage(1000);
        }
    }

    void update(double dt) {
        // TileEntityChest.update(): move lid by 0.1 each 20 Hz tick.
        float lidStep = (float) (dt * 2.0);
        if (chestOpen) chestLidProgress = Math.min(1f, chestLidProgress + lidStep);
        else chestLidProgress = Math.max(0f, chestLidProgress - lidStep);

        boolean esc = glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS;
        boolean eKey = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        boolean qKey = glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS;
        boolean mKey = glfwGetKey(window, GLFW_KEY_M) == GLFW_PRESS;
        boolean f5Key = glfwGetKey(window, GLFW_KEY_F5) == GLFW_PRESS;
        boolean left = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        boolean right = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;

        if (deathScreen) {
            updateDeathScreen(left);
            updateDrops(dt);
            tickWaterAcc(dt);
            leftWasDown = left;
            rightWasDown = right;
            deathMouseWasDown = left;
            escWasDown = esc;
            return;
        }

        if (paused) {
            updatePauseMenu(left, esc);
            updateDrops(dt);
            tickWaterAcc(dt);
            leftWasDown = left;
            rightWasDown = right;
            escWasDown = esc;
            return;
        }

        // GuiContainer does not pause an integrated 1.12 world. Keep celestial
        // time, weather fades, precipitation animation and rain audio running
        // while inventory, crafting, chest, trade or chat screens are open.
        tickWorldClocks(dt);

        ticksSinceLastSwing += dt * 20.0;
        if (sprintDisableTimer > 0.0) sprintDisableTimer = Math.max(0.0, sprintDisableTimer - dt);

        // CHEST OPEN - obsluga zamykania (ESC, E, klikniecia w sloty)
        if (chestOpen) {
            handleChestInput(left, right);
            if (chestOpen) syncOpenChestInventory();
            applyPassiveGravity(dt);
            updateDrops(dt);
            updateAnimals(dt);
        updateXpOrbs(dt);
            updateVillagers(dt);
            tickWaterAcc(dt);
            leftWasDown = left;
            rightWasDown = right;
            escWasDown = esc;
            eWasDown = eKey;
            chestMouseWasDown = left;
            return;
        }

        if (!inventoryOpen && !chatOpen && !creativeInvOpen && !chestOpen && esc && !escWasDown) {
            openPauseMenu();
            escWasDown = esc;
            return;
        }

        if (chatOpen) {
            applyPassiveGravity(dt);
            updateDrops(dt);
            updateAnimals(dt);
        updateXpOrbs(dt);
            updateVillagers(dt);
            tickWaterAcc(dt);
            leftWasDown = left;
            rightWasDown = right;
            escWasDown = esc;
            return;
        }

        // NAPRAWIONE: villagerTradeOpen sprawdzany PRZED ogolnym handlerem E,
        // inaczej E od razu otwieraloby inventory zamiast zamknac trade.
        if (villagerTradeOpen) {
            handleVillagerTradeInput(left);
            applyPassiveGravity(dt);
            updateDrops(dt);
            updateAnimals(dt);
        updateXpOrbs(dt);
            updateVillagers(dt);
            tickWaterAcc(dt);
            leftWasDown = left;
            rightWasDown = right;
            escWasDown = esc;
            eWasDown = eKey;
            return;
        }

        if (eKey && !eWasDown) {
            if (inventoryOpen) closeInventory();
            else if (creativeInvOpen) closeCreativeInv();
            else if (gameMode == GAMEMODE_CREATIVE) openCreativeInv();
            else openInventory(false);
        }
        eWasDown = eKey;

        if (creativeInvOpen) {
            handleCreativeInvInput(left, right);
            applyPassiveGravity(dt);
            updateDrops(dt);
            updateAnimals(dt);
        updateXpOrbs(dt);
            updateVillagers(dt);
            tickWaterAcc(dt);
            leftWasDown = left;
            rightWasDown = right;
            escWasDown = esc;
            return;
        }

        if (f5Key && !f5WasDown) { cameraMode = (cameraMode + 1) % 3; sound.playClick(); }
        f5WasDown = f5Key;
        // Non-vanilla full-screen post-processing and the later JSON terrain
        // experiment are intentionally unavailable in the MCP 9.40 path.
        // F9 - toggle TOOL tuning mode (miecze/kilofy/siekiery/motyki)
        boolean f9Key = glfwGetKey(window, GLFW_KEY_F9) == GLFW_PRESS;
        if (f9Key && !f9WasDown) {
            itemTuneMode = (itemTuneMode == TUNE_TOOL) ? TUNE_OFF : TUNE_TOOL;
            System.out.println("[F9] TOOL TUNE MODE = " + (itemTuneMode == TUNE_TOOL));
            if (itemTuneMode == TUNE_TOOL) printTuneHelp();
            sound.playClick();
        }
        f9WasDown = f9Key;

        // F10 - toggle BLOCK tuning mode (dirt/stone/planks itd)
        boolean f10Key = glfwGetKey(window, GLFW_KEY_F10) == GLFW_PRESS;
        if (f10Key && !f10WasDown) {
            itemTuneMode = (itemTuneMode == TUNE_BLOCK) ? TUNE_OFF : TUNE_BLOCK;
            System.out.println("[F10] BLOCK TUNE MODE = " + (itemTuneMode == TUNE_BLOCK));
            if (itemTuneMode == TUNE_BLOCK) printTuneHelp();
            sound.playClick();
        }
        f10WasDown = f10Key;

        // F12 - toggle FOOD tuning mode (chleb/mieso/emerald/wheat/seeds)
        boolean f12Key = glfwGetKey(window, GLFW_KEY_F12) == GLFW_PRESS;
        if (f12Key && !f12WasDown) {
            itemTuneMode = (itemTuneMode == TUNE_FOOD) ? TUNE_OFF : TUNE_FOOD;
            System.out.println("[F12] FOOD TUNE MODE = " + (itemTuneMode == TUNE_FOOD));
            if (itemTuneMode == TUNE_FOOD) printTuneHelp();
            sound.playClick();
        }
        f12WasDown = f12Key;

        // Kontrolki tuning (aktywne dla aktualnego mode)
        if (itemTuneMode != TUNE_OFF) {
            float step = 0.02f;
            float rotStep = 2.0f;
            // Bierzemy referencje do wartosci aktualnie tuningowanych
            float x, y, z, rx, ry, rz, sc;
            if (itemTuneMode == TUNE_TOOL) {
                x = itemTuneX; y = itemTuneY; z = itemTuneZ;
                rx = itemTuneRotX; ry = itemTuneRotY; rz = itemTuneRotZ; sc = itemTuneScale;
            } else if (itemTuneMode == TUNE_BLOCK) {
                x = blockTuneX; y = blockTuneY; z = blockTuneZ;
                rx = blockTuneRotX; ry = blockTuneRotY; rz = blockTuneRotZ; sc = blockTuneScale;
            } else {   // TUNE_FOOD
                x = foodTuneX; y = foodTuneY; z = foodTuneZ;
                rx = foodTuneRotX; ry = foodTuneRotY; rz = foodTuneRotZ; sc = foodTuneScale;
            }
            if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)  x -= step;
            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) x += step;
            if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS)    y += step;
            if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS)  y -= step;
            if (glfwGetKey(window, GLFW_KEY_PAGE_UP) == GLFW_PRESS)   z += step;
            if (glfwGetKey(window, GLFW_KEY_PAGE_DOWN) == GLFW_PRESS) z -= step;
            if (glfwGetKey(window, GLFW_KEY_I) == GLFW_PRESS) rx += rotStep;
            if (glfwGetKey(window, GLFW_KEY_K) == GLFW_PRESS) rx -= rotStep;
            if (glfwGetKey(window, GLFW_KEY_J) == GLFW_PRESS) ry += rotStep;
            if (glfwGetKey(window, GLFW_KEY_L) == GLFW_PRESS) ry -= rotStep;
            if (glfwGetKey(window, GLFW_KEY_U) == GLFW_PRESS) rz += rotStep;
            if (glfwGetKey(window, GLFW_KEY_O) == GLFW_PRESS) rz -= rotStep;
            if (glfwGetKey(window, GLFW_KEY_EQUAL) == GLFW_PRESS) sc += 0.02f;
            if (glfwGetKey(window, GLFW_KEY_MINUS) == GLFW_PRESS) sc -= 0.02f;
            if (sc < 0.05f) sc = 0.05f;
            // Zapisz zmiany
            if (itemTuneMode == TUNE_TOOL) {
                itemTuneX = x; itemTuneY = y; itemTuneZ = z;
                itemTuneRotX = rx; itemTuneRotY = ry; itemTuneRotZ = rz;
                itemTuneScale = sc;
            } else if (itemTuneMode == TUNE_BLOCK) {
                blockTuneX = x; blockTuneY = y; blockTuneZ = z;
                blockTuneRotX = rx; blockTuneRotY = ry; blockTuneRotZ = rz;
                blockTuneScale = sc;
            } else {   // TUNE_FOOD
                foodTuneX = x; foodTuneY = y; foodTuneZ = z;
                foodTuneRotX = rx; foodTuneRotY = ry; foodTuneRotZ = rz;
                foodTuneScale = sc;
            }
            String modeName = itemTuneMode == TUNE_TOOL ? "TOOL" :
                              itemTuneMode == TUNE_BLOCK ? "BLOCK" : "FOOD";
            String prefix = itemTuneMode == TUNE_TOOL ? "item" :
                            itemTuneMode == TUNE_BLOCK ? "block" : "food";
            long now = System.currentTimeMillis();
            if (now - lastTuneLogTime > 500) {
                System.out.println(String.format(java.util.Locale.US,
                    "[TUNE %s] pos=(%.3f, %.3f, %.3f) rot=(%.1f, %.1f, %.1f) scale=%.3f",
                    modeName, x, y, z, rx, ry, rz, sc));
                lastTuneLogTime = now;
            }
            if (glfwGetKey(window, GLFW_KEY_P) == GLFW_PRESS) {
                System.out.println("======== TWOJE WARTOSCI (" + modeName + ") ========");
                System.out.println(String.format(java.util.Locale.US,
                    "%sTuneX = %.3ff; %sTuneY = %.3ff; %sTuneZ = %.3ff;",
                    prefix, x, prefix, y, prefix, z));
                System.out.println(String.format(java.util.Locale.US,
                    "%sTuneRotX = %.1ff; %sTuneRotY = %.1ff; %sTuneRotZ = %.1ff;",
                    prefix, rx, prefix, ry, prefix, rz));
                System.out.println(String.format(java.util.Locale.US,
                    "%sTuneScale = %.3ff;", prefix, sc));
                System.out.println("========================================");
            }
        }
        if (mKey && !mWasDown) { if (sound.masterVolume <= 0.001) sound.setVolume(1.0); else sound.setVolume(0.0); if (sound.enabled) sound.playClick(); }
        mWasDown = mKey;

        if (inventoryOpen) {
            handleInventoryInput(left, right, qKey);
            applyPassiveGravity(dt);
            updateDrops(dt);
            updateAnimals(dt);
        updateXpOrbs(dt);
            updateVillagers(dt);
            tickWaterAcc(dt);
            leftWasDown = left;
            rightWasDown = right;
            qWasDown = qKey;
            escWasDown = esc;
            return;
        }

        escWasDown = esc;

        if (left && !leftWasDown && !mouseCaptured) {
            mouseCaptured = true;
            firstMouse = true;
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }
        if (!mouseCaptured) {
            leftWasDown = left;
            rightWasDown = right;
            return;
        }

        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (glfwGetKey(window, GLFW_KEY_1 + i) == GLFW_PRESS) selectedSlot = i;
        }
        updateHotbarScroll();
        int attackItemId = selectedItemId();
        if (lastAttackItemId == Integer.MIN_VALUE) lastAttackItemId = attackItemId;
        else if (lastAttackItemId != attackItemId) {
            // EntityPlayer resets cooled strength when the held item type changes.
            lastAttackItemId = attackItemId;
            ticksSinceLastSwing = 0.0;
        }

        if (qKey && !qWasDown) dropSelected(glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS);
        qWasDown = qKey;

        double forward = 0, strafe = 0;
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) forward += 1;
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) forward -= 1;
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) strafe -= 1;
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) strafe += 1;
        double len = Math.sqrt(forward * forward + strafe * strafe);
        if (len > 0) { forward /= len; strafe /= len; }
        boolean inWater = playerTouchingWater();
        boolean enteredWater = inWater && !wasInWater;
        wasInWater = inWater;
        if (enteredWater) sound.playSplash();
        boolean ctrlDownH = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;
        boolean shiftDownH = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;
        // A player cannot stand up while a low ceiling occupies the standing hitbox.
        boolean wantsSneak = shiftDownH && !flying && !inWater;
        sneaking = wantsSneak || !playerFreeAtHeight(x, y, z, PLAYER_HEIGHT);
        // MC controls: Shift is sneak, Ctrl is sprint. Sneaking must never speed up walking.
        boolean sprinting = !flying && sprintDisableTimer <= 0.0
                && ctrlDownH && !sneaking && forward > 0.0 && !inWater;
        double speed;
        if (flying) {
            speed = ctrlDownH ? 22.0 : 12.0;
        } else if (sneaking) {
            speed = SNEAK_SPEED;
        } else if (sprinting) {
            speed = SPRINT_SPEED;
        } else {
            speed = WALK_SPEED;
        }
        if (inWater && !flying) speed *= 0.50;

        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double dx = (sin * forward + cos * strafe) * speed * dt;
        double dz = (cos * forward - sin * strafe) * speed * dt;
        boolean moving = Math.abs(dx) + Math.abs(dz) > 0.001;
        if (enteredWater) {
            double tickScale = 0.05 / Math.max(0.001, dt);
            spawnWaterEntryParticles(dx * tickScale, velY * 0.05, dz * tickScale);
        }
        moveHorizontal(dx, dz, sneaking && onGround && !flying);

        boolean spaceDown = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
        boolean shiftDown = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;
        double now = glfwGetTime();
        if (gameMode == GAMEMODE_CREATIVE && spaceDown && !spaceWasDown) {
            if (now - lastSpacePressTime < 0.30) {
                flying = !flying;
                if (flying) velY = 0;
            }
            lastSpacePressTime = now;
        }
        if (gameMode != GAMEMODE_CREATIVE) flying = false;
        if (flying) {
            velY = 0;
            double vertDir = 0;
            if (spaceDown) vertDir += 1;
            if (shiftDown) vertDir -= 1;
            double climbSpeed = 14.0;
            boolean ctrlDown = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;
            if (ctrlDown) climbSpeed = 28.0;
            double dy = vertDir * climbSpeed * dt;
            if (dy != 0 && playerFree(x, y + dy, z)) y += dy;
        } else {
            // MC-STYLE WATER PHYSICS:
            //   - w wodzie gracz nie ma onGround (unosi sie)
            //   - space = plywa w gore (kazda klatka)
            //   - bez space = opada wolno (gravity zmniejszona)
            //   - shift = opada szybciej (nurkowanie)
            //   - JUMP z brzegu wody: gdy dotykasz sciany/bloku w kierunku ruchu, space daje "hop up" zeby wyskoczyc
            if (inWater) {
                // MC-style water physics
                if (spaceDown) {
                    // Plyn w gore
                    velY += 12.0 * dt;
                    if (velY > 2.5) velY = 2.5;
                } else if (shiftDown) {
                    // Nurkowanie
                    velY -= 6.0 * dt;
                } else {
                    // Bez klawiszy - opada wolno (grawitacja wody)
                    velY -= 3.0 * dt;
                }
                // Terminal velocity w wodzie - szybciej niz wczesniej
                if (velY < -5.0) velY = -5.0;
                // Hamowanie tylko przy szybkim spadaniu (np. skok do wody z gory)
                // Wywala max down speed z -20 na -5 stopniowo
                if (velY < -3.0) velY *= 0.85;
                // Wyskakiwanie z wody na brzeg
                if (spaceDown && !spaceWasDown && isNextToLand()) {
                    velY = 5.5;
                }
            } else {
                // Na ladzie - normalna fizyka
                if (spaceDown && !spaceWasDown && onGround) {
                    velY = 6.8;
                    onGround = false;
                    sound.playJump();
                }
                velY -= 18.0 * dt;
                if (velY < -24) velY = -24;
            }
            moveVertical(velY * dt);
            if (y < -5) {
                if (gameMode == GAMEMODE_CREATIVE) { spawnPlayer(); velY = 0; }
                else damage(1000);
            }
        }
        spaceWasDown = spaceDown;

        updateSurvival(dt, moving);
        double movementTickScale = 0.05 / Math.max(0.001, dt);
        updateStepSound(dt, moving, sprinting, dx * movementTickScale, dz * movementTickScale);
        updateDrops(dt);
        updateAnimals(dt);
        updateXpOrbs(dt);
        updateVillagers(dt);
        tickWaterAcc(dt);
        tickWheatGrowth(dt);
        // Zanik screen shake (0.85 na sekunde)
        if (screenShake > 0) {
            screenShake -= dt * 4.0;
            if (screenShake < 0) screenShake = 0;
        }
        if (damageFlash > 0) {
            damageFlash -= dt * 2.5;
            if (damageFlash < 0) damageFlash = 0;
        }
        // Update damage numbers
        craft3dgl.ui.DamageNumbers.update(dt);

        particleSystem.update(dt, particleWorld);

        if (!lastPosInit) { lastX = x; lastZ = z; lastPosInit = true; }
        double moveDx = x - lastX, moveDz = z - lastZ;
        double moveLen = Math.sqrt(moveDx * moveDx + moveDz * moveDz);
        lastX = x; lastZ = z;
        // Update MC-style limbSwing/Amount dla renderu Steve
        float targetAmount = (float) Math.min(1.0, moveLen * 20.0);
        limbSwingAmount += (targetAmount - limbSwingAmount) * Math.min(1.0f, (float)(dt * 8.0));
        limbSwing += (float) moveLen * 4.0f;
        // ==== MC LivingEntity.aiStep() port 1:1 (linijki 2020-2130 z EntityLivingBase) ====
        // Wszystko w STOPNIACH (yaw*180/PI). MC uzywa yRot/yBodyRot w stopniach.
        double yawDeg = Math.toDegrees(yaw);
        double bodyYawDeg = Math.toDegrees(bodyYaw);

        double f = moveDx * moveDx + moveDz * moveDz;
        double g = bodyYawDeg; // target body yaw
        if (f > 0.0025) {
            // Idziemy - wyznacz kat kierunku ruchu
            double walkDir = Math.toDegrees(Math.atan2(moveDz, moveDx)) - 90.0;
            double l = Math.abs(wrapDeg(yawDeg) - walkDir);
            if (95.0 < l && l < 265.0) {
                g = walkDir - 180.0;  // idziemy tylem
            } else {
                g = walkDir;
            }
        }
        // Podczas ataku - body natychmiast dogania yaw
        if (swingTimer > 0) {
            g = yawDeg;
        }

        // ==== tickHeadTurn(g, ..) ====
        double dh = wrapDeg(g - bodyYawDeg);
        bodyYawDeg += dh * 0.3;  // MC: yBodyRot += h * 0.3F (per tick)
        double diff = wrapDeg(yawDeg - bodyYawDeg);
        if (diff < -75.0) diff = -75.0;
        if (diff >= 75.0) diff = 75.0;
        bodyYawDeg = yawDeg - diff;
        if (diff * diff > 2500.0) {
            bodyYawDeg += diff * 0.2;
        }
        bodyYaw = Math.toRadians(bodyYawDeg);

        // walkPhase (dla starego walk anim, glownie step sound)
        if (moveLen > 0.001 && (onGround || flying)) {
            walkPhase += moveLen * 7.0;
            if (walkPhase > Math.PI * 4) walkPhase -= Math.PI * 4;
        } else {
            double target = Math.round(walkPhase / Math.PI) * Math.PI;
            walkPhase += (target - walkPhase) * Math.min(1.0, dt * 8.0);
        }

        if (swingTimer > 0) {
            swingTimer -= dt / 0.30;
            if (swingTimer < 0) swingTimer = 0;
        }

        Hit hit = castRay(blockReach());
        double animalReach = hit.hit ? Math.min(entityReach(), hit.dist) : entityReach();
        AnimalGL targetAnimal = findTargetAnimal(animalReach);
        VillagerGL targetVillager = findTargetVillager(animalReach);
        if (targetAnimal != null && targetVillager != null) {
            double animalDistance = entityRayDistance(targetAnimal.x, targetAnimal.y, targetAnimal.z,
                    craft3dgl.entities.EntityConstants.ANIMAL_RADIUS,
                    craft3dgl.entities.EntityConstants.animalHeight(targetAnimal.type), animalReach);
            double villagerDistance = entityRayDistance(targetVillager.x, targetVillager.y, targetVillager.z,
                    craft3dgl.entities.EntityConstants.VILLAGER_RADIUS,
                    craft3dgl.entities.EntityConstants.VILLAGER_HEIGHT, animalReach);
            if (animalDistance < villagerDistance) targetVillager = null;
            else targetAnimal = null;
        }

        boolean movingTooFastToSweep = Math.hypot(dx, dz) / Math.max(0.001, dt) * 0.05 >= 0.1;
        // Minecraft#clickMouse always invokes EntityPlayer.swingArm, including
        // misses. This fixes LMB doing nothing when the crosshair points at air.
        if (left && !leftWasDown) swingTimer = 1.0;
        if (left && targetVillager != null && !leftWasDown) {
            attackVillager(targetVillager, sprinting, inWater, movingTooFastToSweep);
            swingTimer = 1.0;
            miningHit = null;
            miningProgress = 0;
        } else if (left && targetAnimal != null && !leftWasDown) {
            attackAnimal(targetAnimal, sprinting, inWater, movingTooFastToSweep);
            swingTimer = 1.0;
            miningHit = null;
            miningProgress = 0;
        } else if (left && hit.hit && targetAnimal == null) {
            if (!leftWasDown) swingTimer = 1.0;
            if (left && swingTimer <= 0 && gameMode != GAMEMODE_CREATIVE) swingTimer = 1.0;
            if (gameMode == GAMEMODE_CREATIVE) {
                if (!leftWasDown) updateMining(hit, dt);
            } else {
                updateMining(hit, dt);
            }
        } else { miningHit = null; miningProgress = 0; }

        updateEating(right, dt);
        if (right && !rightWasDown && eatingItemId == 0) {
            if (targetVillager != null) {
                openVillagerTrade(targetVillager);
                // SUCCESS from processRightClick(Entity) swings the active hand.
                swingTimer = 1.0;
            } else if (!startEatingSelected()) {
                boolean actionSucceeded = hit.hit && place(hit);
                // Minecraft#rightClickMouse only swings for a SUCCESS action
                // result. A miss, failed placement and creative pick do not swing.
                if (actionSucceeded) swingTimer = 1.0;
                // Vanilla pick-block is middle mouse only. Right-clicking with
                // an empty creative hand must not create a block or play UI audio.
            }
        }

        // MIDDLE MOUSE BUTTON = pick block (jak w MC) - dziala w obu trybach
        boolean middle = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_MIDDLE) == GLFW_PRESS;
        if (middle && !middleMouseWasDown && hit.hit) {
            pickBlockToHand(hit.block);
        }
        middleMouseWasDown = middle;

        leftWasDown = left;
        rightWasDown = right;
    }

    /** Pick block (jak MMB w MC): daje 1 sztuke bloku do reki.
     *  W creative: zawsze nowa sztuka.
     *  W survival: tylko jezeli juz mamy w inwentarzu (przesuwa do hotbara). */
    void pickBlockToHand(int blockId) {
        // Niektore bloki maja inny "item form" niz blok w swiecie
        int itemForBlock = blockId;
        if (blockId == WHEAT_0 || blockId == WHEAT_1 || blockId == WHEAT_2 || blockId == WHEAT_3) itemForBlock = ITEM_SEEDS;
        if (blockId == FARMLAND) itemForBlock = DIRT;
        if (blockId == GRASS) itemForBlock = GRASS;
        if (blockId == TALL_GRASS) itemForBlock = ITEM_SEEDS;

        if (gameMode == GAMEMODE_CREATIVE) {
            // Sprawdz czy juz mam w hotbarze
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                if (invId[i] == itemForBlock && invCount[i] > 0) {
                    selectedSlot = i;
                    return;
                }
            }
            // Sprawdz czy mam w plecaku
            for (int i = HOTBAR_SIZE; i < INVENTORY_SIZE; i++) {
                if (invId[i] == itemForBlock && invCount[i] > 0) {
                    // Przenies do hotbara (swap)
                    int tmpId = invId[selectedSlot];
                    int tmpCount = invCount[selectedSlot];
                    invId[selectedSlot] = invId[i];
                    invCount[selectedSlot] = invCount[i];
                    invId[i] = tmpId;
                    invCount[i] = tmpCount;
                    return;
                }
            }
            // Nie mam - daj nowa sztuke do hotbara
            // Jezeli w obecnym slocie jest cos innego, znajdz pusty slot w hotbarze
            if (invId[selectedSlot] == 0 || invCount[selectedSlot] <= 0) {
                invId[selectedSlot] = itemForBlock;
                invCount[selectedSlot] = 1;
            } else {
                // Znajdz pusty slot w hotbarze
                int empty = -1;
                for (int i = 0; i < HOTBAR_SIZE; i++) {
                    if (invId[i] == 0 || invCount[i] <= 0) { empty = i; break; }
                }
                if (empty >= 0) {
                    invId[empty] = itemForBlock;
                    invCount[empty] = 1;
                    selectedSlot = empty;
                } else {
                    // Hotbar pelny - nadpisz obecny slot
                    invId[selectedSlot] = itemForBlock;
                    invCount[selectedSlot] = 1;
                }
            }
        } else {
            // SURVIVAL: tylko przesun do hotbara jezeli juz mamy
            for (int i = 0; i < HOTBAR_SIZE; i++) {
                if (invId[i] == itemForBlock && invCount[i] > 0) {
                    selectedSlot = i;
                    return;
                }
            }
            for (int i = HOTBAR_SIZE; i < INVENTORY_SIZE; i++) {
                if (invId[i] == itemForBlock && invCount[i] > 0) {
                    int tmpId = invId[selectedSlot];
                    int tmpCount = invCount[selectedSlot];
                    invId[selectedSlot] = invId[i];
                    invCount[selectedSlot] = invCount[i];
                    invId[i] = tmpId;
                    invCount[i] = tmpCount;
                    return;
                }
            }
            // Survival - nie mamy itemu, nic nie robimy
        }
    }

    void updateHotbarScroll() {
        if (Math.abs(pendingScroll) < 0.01) return;
        int steps = (int)Math.round(pendingScroll);
        pendingScroll = 0;
        selectedSlot -= steps;
        while (selectedSlot < 0) selectedSlot += HOTBAR_SIZE;
        while (selectedSlot >= HOTBAR_SIZE) selectedSlot -= HOTBAR_SIZE;
    }

    void updateMining(Hit hit, double dt) {
        if (hit.block == WATER) return;
        if (miningHit == null || miningHit.x != hit.x || miningHit.y != hit.y || miningHit.z != hit.z) {
            miningHit = hit;
            miningProgress = 0;
        }
        if (gameMode == GAMEMODE_CREATIVE) {
            setBlock(hit.x, hit.y, hit.z, AIR);
            sound.playBreak(hit.block);
            miningProgress = 0;
            miningHit = null;
            return;
        }
        double waterPenalty = playerTouchingWater() ? 0.20 : 1.0;
        double prev = miningProgress;
        miningProgress += dt * miningSpeed(hit.block) / hardness(hit.block) * waterPenalty;
        // Spawn particles co ~0.12 sekundy podczas kopania (feedback wizualny)
        miningParticleTimer += dt;
        if (miningParticleTimer > 0.12) {
            miningParticleTimer = 0;
            spawnMiningParticles(hit.x, hit.y, hit.z, hit.block, hit.nx, hit.ny, hit.nz);
        }
        if (miningProgress >= 1.0) {
            int brokenId = hit.block;
            // Spawn okruchow bloku (particles) PRZED zniszczeniem (potrzeba koloru bloku)
            spawnBlockBreakParticles(hit.x, hit.y, hit.z, brokenId);
            // Minecraft 1.12 does not shake the camera when a block finishes breaking.
            craft3dgl.ui.Crosshair.triggerHit();
            setBlock(hit.x, hit.y, hit.z, AIR);
            sound.playBreak(brokenId);
            int drop = dropForBlock(brokenId);
            if (drop != AIR) spawnDrop(hit.x + 0.5, hit.y + 0.15, hit.z + 0.5, drop, 1);
            spawnSpecialBlockDrops(brokenId, hit.x, hit.y, hit.z);
            miningProgress = 0;
            miningHit = null;
        }
    }

    /** MCP Entity.doWaterSplashEffect: 13 bubbles and 13 splashes for width 0.6. */
    void spawnWaterEntryParticles(double motionX, double motionY, double motionZ) {
        final double width = 0.6;
        final int count = (int)(1.0 + width * 20.0);
        final double surfaceY = Math.floor(y) + 1.0;
        for (int i = 0; i < count; i++) {
            double offset = (random.nextDouble() * 2.0 - 1.0) * width;
            double px = x + offset;
            double pz = z + (random.nextDouble() * 2.0 - 1.0) * width;
            particleSystem.add(Particle.bubble(random, px, surfaceY, pz,
                    motionX, motionY - random.nextDouble() * 0.2, motionZ));
        }
        for (int i = 0; i < count; i++) {
            double offset = (random.nextDouble() * 2.0 - 1.0) * width;
            double px = x + offset;
            double pz = z + (random.nextDouble() * 2.0 - 1.0) * width;
            particleSystem.add(Particle.splash(random, px, surfaceY, pz,
                    motionX, motionY, motionZ));
        }
    }

    /** MCP ParticleManager.addBlockDestroyEffects: a complete 4x4x4 fragment grid. */
    void spawnBlockBreakParticles(int bx, int by, int bz, int blockId) {
        for (int ix = 0; ix < 4; ix++) {
            for (int iy = 0; iy < 4; iy++) {
                for (int iz = 0; iz < 4; iz++) {
                    double px = bx + (ix + 0.5) / 4.0;
                    double py = by + (iy + 0.5) / 4.0;
                    double pz = bz + (iz + 0.5) / 4.0;
                    particleSystem.add(Particle.blockCrack(random, px, py, pz,
                            px - bx - 0.5, py - by - 0.5, pz - bz - 0.5, blockId));
                }
            }
        }
    }

    /** MCP ParticleManager.addBlockHitEffects: one inset, scaled BLOCK_CRACK particle. */
    void spawnMiningParticles(int bx, int by, int bz, int blockId, int hitNx, int hitNy, int hitNz) {
        double px = bx + random.nextDouble() * 0.8 + 0.1;
        double py = by + random.nextDouble() * 0.8 + 0.1;
        double pz = bz + random.nextDouble() * 0.8 + 0.1;
        if (hitNx < 0) px = bx - 0.1;
        else if (hitNx > 0) px = bx + 1.1;
        else if (hitNy < 0) py = by - 0.1;
        else if (hitNy > 0) py = by + 1.1;
        else if (hitNz < 0) pz = bz - 0.1;
        else if (hitNz > 0) pz = bz + 1.1;
        particleSystem.add(Particle.blockCrack(random, px, py, pz,
                0.0, 0.0, 0.0, blockId).multiplyVelocity(0.2f).multiplyScale(0.6f));
    }

    /** Specjalne dropy ktore wypadaja dodatkowo lub zamiast podstawowych. */
    void spawnSpecialBlockDrops(int blockId, int bx, int by, int bz) {
        if (blockId == TALL_GRASS) {
            // 30% szansa na nasiona z trawy
            if (random.nextDouble() < 0.30) {
                int n = 1 + random.nextInt(2);
                spawnDrop(bx + 0.5, by + 0.15, bz + 0.5, ITEM_SEEDS, n);
            }
        } else if (blockId == WHEAT_3) {
            // Dojrzaly: 1 pszenica + 1-3 nasion
            spawnDrop(bx + 0.5, by + 0.15, bz + 0.5, ITEM_WHEAT, 1);
            int seedCount = 1 + random.nextInt(3);
            spawnDrop(bx + 0.5, by + 0.15, bz + 0.5, ITEM_SEEDS, seedCount);
        } else if (blockId == WHEAT_0 || blockId == WHEAT_1 || blockId == WHEAT_2) {
            // Niedojrzaly: tylko nasiona zwracane
            spawnDrop(bx + 0.5, by + 0.15, bz + 0.5, ITEM_SEEDS, 1);
        } else if (blockId == GRASS) {
            // Trawa moze drop nasion bardzo rzadko (jak w MC nie ma takiej mechaniki bezposrednio,
            // ale dla rownowagi - 5% szans)
            if (random.nextDouble() < 0.05) {
                spawnDrop(bx + 0.5, by + 0.15, bz + 0.5, ITEM_SEEDS, 1);
            }
        }
    }

    double hardness(int block) { return craft3dgl.world.MiningMechanics.hardness(block); }

    /** PlayerControllerMP#processRightClickBlock result: true only for SUCCESS. */
    boolean place(Hit hit) {
        if (hit.block == CRAFTING_TABLE) {
            openInventory(true);
            return true;
        }
        if (hit.block == CHEST) {
            openChest(hit.x, hit.y, hit.z);
            return true;
        }
        // === MOTYKA na DIRT/GRASS -> FARMLAND ===
        int curItem = selectedItemId();
        if ((curItem == ITEM_WOOD_HOE || curItem == ITEM_STONE_HOE) && selectedItemCount() > 0) {
            if ((hit.block == DIRT || hit.block == GRASS) && hit.ny > 0) {
                int abx = hit.x, aby = hit.y + 1, abz = hit.z;
                if (inWorld(abx, aby, abz) && (world[abx][aby][abz] & 0xff) == AIR) {
                    setBlock(hit.x, hit.y, hit.z, FARMLAND);
                    sound.playPlace(DIRT);
                    return true;
                }
            }
        }
        // === NASIONA na FARMLAND -> WHEAT_0 ===
        if (curItem == ITEM_SEEDS && selectedItemCount() > 0) {
            int px2 = hit.x + hit.nx, py2 = hit.y + hit.ny, pz2 = hit.z + hit.nz;
            if (inWorld(px2, py2, pz2) && (world[px2][py2][pz2] & 0xff) == AIR
                    && py2 > 0 && (world[px2][py2 - 1][pz2] & 0xff) == FARMLAND) {
                setBlock(px2, py2, pz2, WHEAT_0);
                sound.playPlace(DIRT);
                if (gameMode != GAMEMODE_CREATIVE) {
                    invCount[selectedSlot]--;
                    if (invCount[selectedSlot] <= 0) { invId[selectedSlot] = 0; invCount[selectedSlot] = 0; }
                }
                return true;
            }
        }
        if (hit.block == DOOR_BOTTOM || hit.block == DOOR_TOP) {
            int bx = hit.x;
            int byBot = hit.block == DOOR_BOTTOM ? hit.y : hit.y - 1;
            int byTop = byBot + 1;
            if (!inWorld(bx, byBot, hit.z)
                    || (world[bx][byBot][hit.z] & 0xff) != DOOR_BOTTOM) return false;

            int meta = getDoorMeta(bx, byBot, hit.z);
            boolean wasOpen = DoorSystem.isOpen(meta);
            int newMeta = DoorSystem.withOpen(meta, !wasOpen);
            setDoorMeta(bx, byBot, hit.z, newMeta);
            if (inWorld(bx, byTop, hit.z)
                    && (world[bx][byTop][hit.z] & 0xff) == DOOR_TOP) {
                setDoorMeta(bx, byTop, hit.z, newMeta);
            }
            markDirtyAround(bx, byBot, hit.z);
            markDirtyAround(bx, byTop, hit.z);
            if (wasOpen) sound.playDoorClose(); else sound.playDoorOpen();
            return true;
        }

        int item = selectedItemId();
        if (!isBlockItem(item) || selectedItemCount() <= 0) return false;

        // ItemDoor.onItemUse z MC 1.12: drzwi stawia sie wylacznie na gornej
        // scianie pelnego bloku, potrzebne sa dwie wymienialne komorki.
        if (item == DOOR_BOTTOM) {
            if (hit.ny != 1) return false;
            boolean replaceClicked = isReplaceableForDoor(hit.block);
            int px = hit.x, py = replaceClicked ? hit.y : hit.y + 1, pz = hit.z;
            if (!inWorld(px, py, pz) || !inWorld(px, py + 1, pz)) return false;
            if (!isNormalCubeForDoor(px, py - 1, pz)) return false;
            if (!isReplaceableForDoor(world[px][py][pz] & 0xff)
                    || !isReplaceableForDoor(world[px][py + 1][pz] & 0xff)) return false;

            double cosPitch = Math.cos(pitch);
            double hitWorldX = x + Math.sin(yaw) * cosPitch * hit.dist;
            double hitWorldZ = z + Math.cos(yaw) * cosPitch * hit.dist;
            double hitX = clamp(hitWorldX - hit.x, 0.0, 1.0);
            double hitZ = clamp(hitWorldZ - hit.z, 0.0, 1.0);
            int facing = yawToFacing(yaw);
            boolean rightHinge = DoorSystem.chooseRightHinge(
                    px, py, pz, facing, hitX, hitZ, doorPlacementWorld);
            int meta = DoorSystem.makeMeta(facing, false, rightHinge);

            setBlock(px, py, pz, DOOR_BOTTOM);
            setBlock(px, py + 1, pz, DOOR_TOP);
            setDoorMeta(px, py, pz, meta);
            setDoorMeta(px, py + 1, pz, meta);
            sound.playPlace(item);
            if (gameMode != GAMEMODE_CREATIVE) {
                invCount[selectedSlot]--;
                if (invCount[selectedSlot] <= 0) { invId[selectedSlot] = 0; invCount[selectedSlot] = 0; }
            }
            return true;
        }

        int px = hit.x + hit.nx, py = hit.y + hit.ny, pz = hit.z + hit.nz;
        if (!inWorld(px, py, pz)) return false;
        int existing = world[px][py][pz] & 0xff;
        if (existing != AIR && existing != WATER) return false;
        if (blockIntersectsPlayer(px, py, pz)) return false;
        if (item == CHEST && !canPlaceChestAt(px, py, pz)) return false;
        setBlock(px, py, pz, item);
        if (item == CHEST) {
            // BlockChest permits one horizontal neighbor, but never a triple
            // chest. A joined pair shares one facing perpendicular to its axis.
            int facing = (yawToFacing(yaw) + 2) & 3;
            long neighbor = adjacentChestKey(px, py, pz);
            if (neighbor != Long.MIN_VALUE) {
                int nx = ChestStorage.unpackX(neighbor), nz = ChestStorage.unpackZ(neighbor);
                if (nx != px) facing = facing == 2 ? 2 : 0;
                else facing = facing == 3 ? 3 : 1;
                setChestFacing(nx, py, nz, facing);
            }
            setChestFacing(px, py, pz, facing);
            // Rebuild both the chest section and (at a section boundary) the
            // support section immediately. This guarantees the supporting
            // block's top face is present before the inset 14/16 model appears.
            int ccx = px / CHUNK, ccy = py / CHUNK, ccz = pz / CHUNK;
            if (generatedColumns[ccx][ccz] && litColumns[ccx][ccz]) {
                rebuildChunk(ccx, ccy, ccz);
                if (py % CHUNK == 0 && ccy > 0) rebuildChunk(ccx, ccy - 1, ccz);
            }
        }
        sound.playPlace(item);
        if (gameMode != GAMEMODE_CREATIVE) {
            invCount[selectedSlot]--;
            if (invCount[selectedSlot] <= 0) { invId[selectedSlot] = 0; invCount[selectedSlot] = 0; }
        }
        return true;
    }

    /** BlockChest#canPlaceBlockAt: at most one neighbor and that neighbor is single. */
    boolean canPlaceChestAt(int x, int y, int z) {
        int neighbors = 0;
        final int[] dx = {-1, 1, 0, 0};
        final int[] dz = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i], nz = z + dz[i];
            if (!inWorld(nx, y, nz) || (world[nx][y][nz] & 255) != CHEST) continue;
            neighbors++;
            if (neighbors > 1 || chestHasNeighbor(nx, y, nz, x, z)) return false;
        }
        return true;
    }

    boolean chestHasNeighbor(int x, int y, int z, int exceptX, int exceptZ) {
        final int[] dx = {-1, 1, 0, 0};
        final int[] dz = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i], nz = z + dz[i];
            if (nx == exceptX && nz == exceptZ) continue;
            if (inWorld(nx, y, nz) && (world[nx][y][nz] & 255) == CHEST) return true;
        }
        return false;
    }

    long adjacentChestKey(int x, int y, int z) {
        final int[] dx = {-1, 1, 0, 0};
        final int[] dz = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i], nz = z + dz[i];
            if (inWorld(nx, y, nz) && (world[nx][y][nz] & 255) == CHEST) {
                return ChestStorage.packKey(nx, y, nz);
            }
        }
        return Long.MIN_VALUE;
    }

    int selectedItemId() { return invId[selectedSlot]; }
    int selectedItemCount() { return invCount[selectedSlot]; }
    boolean isBlockItem(int id) { return ItemRegistry.isBlockItem(id); }
    int maxStack(int id) { return ItemRegistry.maxStack(id); }
    int dropForBlock(int block) { return ItemRegistry.dropForBlock(block); }
    int blockCategory(int block) { return ItemRegistry.blockCategory(block); }
    int toolCategory(int item) { return ItemRegistry.toolCategory(item); }
    int toolTier(int item) { return ItemRegistry.toolTier(item); }
    double miningSpeed(int block) { return craft3dgl.world.MiningMechanics.miningSpeed(block, selectedItemId()); }

    void openInventory(boolean table) {
        returnCraftGridToInventory();
        inventoryOpen = true;
        usingCraftingTable = table;
        mouseCaptured = false;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        recalcCrafting();
    }

    void closeInventory() {
        returnCraftGridToInventory();
        inventoryOpen = false;
        usingCraftingTable = false;
        if (cursorId > 0 && cursorCount > 0) {
            spawnThrownDrop(cursorId, cursorCount);
            cursorId = 0;
            cursorCount = 0;
        }
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    void returnCraftGridToInventory() {
        for (int i = 0; i < craftId.length; i++) {
            if (craftId[i] > 0 && craftCount[i] > 0) {
                int id = craftId[i], c = craftCount[i];
                craftId[i] = 0;
                craftCount[i] = 0;
                if (!addItem(id, c)) spawnDrop(x, y + 1.2, z, id, c);
            }
        }
        craftResult = CraftingSystemGL.EMPTY;
    }

    void recalcCrafting() {
        craftResult = CraftingSystemGL.match(craftId, craftCount, usingCraftingTable ? 3 : 2);
    }

    boolean addItem(int id, int count) {
        if (id <= 0 || count <= 0) return true;
        int max = maxStack(id);
        for (int i = 0; i < INVENTORY_SIZE && count > 0; i++) {
            if (invId[i] == id && invCount[i] < max) {
                int add = Math.min(count, max - invCount[i]);
                invCount[i] += add;
                count -= add;
            }
        }
        for (int i = 0; i < INVENTORY_SIZE && count > 0; i++) {
            if (invId[i] == 0 || invCount[i] <= 0) {
                invId[i] = id;
                int add = Math.min(count, max);
                invCount[i] = add;
                count -= add;
            }
        }
        return count <= 0;
    }

    void handleInventoryInput(boolean left, boolean right, boolean qKey) {
        double[] mx = new double[1], my = new double[1];
        glfwGetCursorPos(window, mx, my);
        int mX = (int) mx[0];
        int mY = (int) my[0];
        if (left && !leftWasDown) clickInventory(mX, mY, false);
        if (right && cursorId > 0 && cursorCount > 0) {
            if (!rightWasDown) dragSlots.clear();
            int[] info = inventorySlotInfo(mX, mY);
            if (info != null && info[0] != 4) {
                long key = packSlotKey(info[0], info[1]);
                if (!dragSlots.contains(key)) {
                    dragSlots.add(key);
                    distributeIntoInvSlot(info[0], info[1]);
                }
            }
        } else if (right && !rightWasDown) {
            clickInventory(mX, mY, true);
        }
        if (!right) dragSlots.clear();
        if (qKey && !qWasDown) dropSelected(glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS);
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS && !escWasDown) closeInventory();
    }

    int[] inventorySlotInfo(int mx, int my) {
        // Uzywa tych samych helpers co InventoryUIRenderer - jedno zrodlo prawdy layoutu.
        int craftSize = usingCraftingTable ? 3 : 2;
        int slot = craft3dgl.ui.InventoryUIRenderer.SLOT_SIZE;

        // ContainerWorkbench has no equipment slots; ContainerPlayer has four
        // armour slots plus offhand at the MCP 9.40 coordinates.
        if (!usingCraftingTable) {
            int aX = craft3dgl.ui.InventoryUIRenderer.armorX(width);
            int aY = craft3dgl.ui.InventoryUIRenderer.armorY(height);
            int aPitch = craft3dgl.ui.InventoryUIRenderer.armorPitch();
            for (int i = 0; i < 4; i++) {
                int sx = aX, sy = aY + i * aPitch;
                if (inside(mx, my, sx, sy, slot, slot)) return new int[]{3, i};
            }
            int oX = craft3dgl.ui.InventoryUIRenderer.offhandX(width);
            int oY = craft3dgl.ui.InventoryUIRenderer.offhandY(height);
            if (inside(mx, my, oX, oY, slot, slot)) return new int[]{3, 4};
        }

        // Crafting grid (kind=0)
        int cx = craft3dgl.ui.InventoryUIRenderer.craftAreaX(width, craftSize);
        int cy = craft3dgl.ui.InventoryUIRenderer.craftY(height, craftSize);
        int cPitch = craft3dgl.ui.InventoryUIRenderer.craftPitch();
        for (int row = 0; row < craftSize; row++) for (int col = 0; col < craftSize; col++) {
            int idx = row * craftSize + col;
            int sx = cx + col * cPitch, sy = cy + row * cPitch;
            if (inside(mx, my, sx, sy, slot, slot)) return new int[]{0, idx};
        }
        // Output (kind=4 idx 0)
        int outX = craft3dgl.ui.InventoryUIRenderer.outputX(width, craftSize);
        int outY = craft3dgl.ui.InventoryUIRenderer.outputY(height, craftSize);
        if (inside(mx, my, outX, outY, slot, slot)) return new int[]{4, 0};

        // Backpack 3x9 (kind=1, idx 9..35)
        int iX = craft3dgl.ui.InventoryUIRenderer.invX(width);
        int iY = craft3dgl.ui.InventoryUIRenderer.invY(height);
        int iPitch = craft3dgl.ui.InventoryUIRenderer.invPitch();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = 9 + row * 9 + col;
            int sx = iX + col * iPitch, sy = iY + row * iPitch;
            if (inside(mx, my, sx, sy, slot, slot)) return new int[]{1, idx};
        }
        // Hotbar (kind=2, idx 0..8)
        int hY = craft3dgl.ui.InventoryUIRenderer.hotY(height);
        for (int col = 0; col < 9; col++) {
            int sx = iX + col * iPitch;
            if (inside(mx, my, sx, hY, slot, slot)) return new int[]{2, col};
        }
        return null;
    }

    void distributeIntoInvSlot(int kind, int idx) {
        if (cursorId <= 0 || cursorCount <= 0) return;
        int[] ids, cnts;
        switch (kind) {
            case 0: ids = craftId; cnts = craftCount; break;
            case 1: case 2: ids = invId; cnts = invCount; break;
            case 3: ids = equipId; cnts = equipCount; break;
            default: return;
        }
        if (ids[idx] == 0 || cnts[idx] == 0) {
            ids[idx] = cursorId;
            cnts[idx] = 1;
            cursorCount--;
        } else if (ids[idx] == cursorId && cnts[idx] < maxStack(cursorId)) {
            cnts[idx]++;
            cursorCount--;
        }
        if (cursorCount <= 0) { cursorId = 0; cursorCount = 0; dragging = false; }
        if (kind == 0) recalcCrafting();
    }

    void clickInventory(int mx, int my, boolean right) {
        boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS
                || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
        int[] info = inventorySlotInfo(mx, my);
        if (info == null) return;
        int kind = info[0], idx = info[1];
        if (kind == 4) { clickCraftOutput(); return; }
        int[] ids = (kind == 0) ? craftId : (kind == 1 || kind == 2) ? invId : (kind == 3) ? equipId : null;
        int[] cnts = (kind == 0) ? craftCount : (kind == 1 || kind == 2) ? invCount : (kind == 3) ? equipCount : null;
        if (ids == null) return;
        if (shift && !right && ids[idx] > 0) {
            if (kind == 0 || kind == 3) {
                quickMove(ids, cnts, idx, invId, invCount, 0, INVENTORY_SIZE);
                if (kind == 0) recalcCrafting();
            } else if (kind == 2) {
                quickMove(invId, invCount, idx, invId, invCount, 9, INVENTORY_SIZE);
            } else if (kind == 1) {
                quickMove(invId, invCount, idx, invId, invCount, 0, 9);
            }
            return;
        }
        clickStack(ids, cnts, idx, right);
        if (kind == 0) recalcCrafting();
        if (kind == 2) selectedSlot = idx;
    }

    boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    void clickStack(int[] ids, int[] counts, int idx, boolean right) {
        // GuiContainer slot interaction has no ui.button.click sound in 1.12.
        if (!right) {
            if (cursorId == 0 || cursorCount <= 0) {
                cursorId = ids[idx]; cursorCount = counts[idx]; ids[idx] = 0; counts[idx] = 0;
            } else if (ids[idx] == 0 || counts[idx] <= 0) {
                ids[idx] = cursorId; counts[idx] = cursorCount; cursorId = 0; cursorCount = 0;
            } else if (ids[idx] == cursorId && counts[idx] < maxStack(ids[idx])) {
                int add = Math.min(cursorCount, maxStack(ids[idx]) - counts[idx]);
                counts[idx] += add; cursorCount -= add;
                if (cursorCount <= 0) { cursorId = 0; cursorCount = 0; }
            } else {
                int ti = ids[idx], tc = counts[idx]; ids[idx] = cursorId; counts[idx] = cursorCount; cursorId = ti; cursorCount = tc;
            }
        } else {
            if (cursorId == 0 || cursorCount <= 0) {
                if (ids[idx] > 0 && counts[idx] > 0) {
                    cursorId = ids[idx]; cursorCount = (counts[idx] + 1) / 2; counts[idx] -= cursorCount;
                    if (counts[idx] <= 0) { ids[idx] = 0; counts[idx] = 0; }
                }
            } else {
                if (ids[idx] == 0 || counts[idx] <= 0) {
                    ids[idx] = cursorId; counts[idx] = 1; cursorCount--;
                    if (cursorCount <= 0) { cursorId = 0; cursorCount = 0; }
                } else if (ids[idx] == cursorId && counts[idx] < maxStack(ids[idx])) {
                    counts[idx]++; cursorCount--;
                    if (cursorCount <= 0) { cursorId = 0; cursorCount = 0; }
                }
            }
        }
    }

    void clickCraftOutput() {
        if (craftResult == null || craftResult.empty()) return;
        int id = craftResult.resultId, count = craftResult.resultCount;
        if (cursorId != 0 && cursorId != id) return;
        if (cursorId == id && cursorCount + count > maxStack(id)) return;
        if (cursorId == 0) { cursorId = id; cursorCount = count; }
        else cursorCount += count;
        for (int i = 0; i < craftResult.consume.length; i++) {
            if (craftResult.consume[i] > 0) {
                craftCount[i] -= craftResult.consume[i];
                if (craftCount[i] <= 0) { craftId[i] = 0; craftCount[i] = 0; }
            }
        }
        recalcCrafting();
    }

    void spawnDrop(double dx, double dy, double dz, int id, int count) {
        if (id <= 0 || count <= 0) return;
        drops.add(new DroppedItemGL(dx, dy, dz, id, count));
    }

    void spawnThrownDrop(int id, int count) {
        if (id <= 0 || count <= 0) return;
        double fx = Math.sin(yaw), fz = Math.cos(yaw);
        DroppedItemGL d = new DroppedItemGL(x + fx * 0.85, y + 1.25, z + fz * 0.85, id, count);
        d.vx = fx * 2.6; d.vz = fz * 2.6; d.vy = 1.2; d.age = -0.4;
        drops.add(d);
    }

    void updateDrops(double dt) {
        Iterator<DroppedItemGL> it = drops.iterator();
        while (it.hasNext()) {
            DroppedItemGL d = it.next();
            d.age += dt;
            d.vy -= 12.0 * dt;
            d.x += d.vx * dt;
            d.z += d.vz * dt;
            d.y += d.vy * dt;
            int ground = findDropGroundYBelow(d.x, d.y + 0.3, d.z);
            if (ground >= 0) {
                double restY = ground + 1.05;
                if (d.y <= restY) {
                    d.y = restY;
                    d.vy = 0;
                    d.vx *= 0.82;
                    d.vz *= 0.82;
                    if (Math.abs(d.vx) < 0.01) d.vx = 0;
                    if (Math.abs(d.vz) < 0.01) d.vz = 0;
                }
            } else if (d.y < 1) {
                d.y = 1;
                d.vy = 0;
            }
            double dist = Math.sqrt((d.x - x) * (d.x - x) + (d.y - (y + 1.0)) * (d.y - (y + 1.0)) + (d.z - z) * (d.z - z));
            if (d.age > 0.75 && dist < 1.35 && addItem(d.id, d.count)) { sound.playItemPickup(); it.remove(); }
            else if (d.age > 240) it.remove();
        }
    }

    int findDropGroundYBelow(double dx, double dy, double dz) {
        int bx = clampInt((int)Math.floor(dx), 0, WORLD_X - 1);
        int bz = clampInt((int)Math.floor(dz), 0, WORLD_Z - 1);
        int startY = clampInt((int)Math.floor(dy) - 1, 0, WORLD_Y - 1);
        for (int yy = startY; yy >= 0; yy--) {
            if (solid(bx, yy, bz)) return yy;
        }
        return -1;
    }

    void dropSelected(boolean wholeStack) {
        if (cursorId > 0 && cursorCount > 0) {
            int c = wholeStack ? cursorCount : 1;
            sound.playPlace(cursorId);
            spawnThrownDrop(cursorId, c);
            cursorCount -= c;
            if (cursorCount <= 0) { cursorId = 0; cursorCount = 0; }
            return;
        }
        int id = selectedItemId();
        if (id <= 0 || selectedItemCount() <= 0) return;
        int c = wholeStack ? selectedItemCount() : 1;
        sound.playPlace(id);
        spawnThrownDrop(id, c);
        invCount[selectedSlot] -= c;
        if (invCount[selectedSlot] <= 0) { invId[selectedSlot] = 0; invCount[selectedSlot] = 0; }
    }

    void updateStepSound(double dt, boolean moving, boolean sprinting,
                         double motionXPerTick, double motionZPerTick) {
        // Entity.createRunningParticles emits one BLOCK_CRACK per sprint tick, never in water.
        if (sprinting) {
            footstepDustTimer += dt;
            while (footstepDustTimer >= 0.05) {
                footstepDustTimer -= 0.05;
                spawnRunningParticle(motionXPerTick, motionZPerTick);
            }
        } else {
            footstepDustTimer = 0.0;
        }

        if (!onGround || !moving || deathScreen || inventoryOpen) {
            stepSoundTimer = 0;
            return;
        }
        stepSoundTimer -= dt;
        if (stepSoundTimer <= 0) {
            sound.playStep(blockUnderPlayer());
            stepSoundTimer = sneaking ? 0.72 : 0.38;
        }
    }

    /** MCP Entity.createRunningParticles. */
    void spawnRunningParticle(double motionXPerTick, double motionZPerTick) {
        int bx = (int)Math.floor(x);
        int by = (int)Math.floor(y - 0.2);
        int bz = (int)Math.floor(z);
        int blockId = inWorld(bx, by, bz) ? world[bx][by][bz] & 0xff : AIR;
        if (blockId == AIR || blockId == WATER) return;
        double px = x + (random.nextDouble() - 0.5) * 0.6;
        double py = y + 0.1;
        double pz = z + (random.nextDouble() - 0.5) * 0.6;
        particleSystem.add(Particle.blockCrack(random, px, py, pz,
                -motionXPerTick * 4.0, 1.5, -motionZPerTick * 4.0, blockId));
    }

    int blockUnderPlayer() {
        int bx = (int)Math.floor(x);
        int by = (int)Math.floor(y - 0.08);
        int bz = (int)Math.floor(z);
        if (inWorld(bx, by, bz)) return world[bx][by][bz] & 0xff;
        return STONE;
    }

    File findAssetDir(String child) { return craft3dgl.save.AssetFinder.findAssetDir(child, MinecraftGL.class); }

    void updateSurvival(double dt, boolean moving) {
        if (deathScreen) return;
        if (gameMode == GAMEMODE_CREATIVE) {
            health = maxHealth;
            hunger = 20;
            hungerTimer = regenTimer = starveTimer = 0;
            return;
        }
        hungerTimer += dt * (moving ? 1.0 : 0.22);
        if (hungerTimer >= 18.0) {
            hungerTimer = 0;
            if (hunger > 0) hunger--;
        }
        if (hunger >= 18 && health < maxHealth) {
            regenTimer += dt;
            if (regenTimer >= 4.0) {
                regenTimer = 0;
                health++;
            }
        } else regenTimer = 0;
        if (hunger <= 0) {
            starveTimer += dt;
            if (starveTimer >= 5.0) {
                starveTimer = 0;
                damage(1);
            }
        } else starveTimer = 0;
    }

    boolean startEatingSelected() {
        int id = selectedItemId();
        if (!craft3dgl.combat.DamageSystem.isFood(id) || hunger >= 20) return false;
        eatingItemId = id;
        eatingProgress = 0;
        eatingSoundStep = 0;
        return true;
    }

    void updateEating(boolean rightHeld, double dt) {
        if (eatingItemId == 0) return;
        // Food takes 32 game ticks (1.6 seconds) and is cancelled on release or item switch.
        if (!rightHeld || selectedItemId() != eatingItemId || selectedItemCount() <= 0 || hunger >= 20) {
            eatingItemId = 0;
            eatingProgress = 0;
            eatingSoundStep = 0;
            return;
        }
        eatingProgress += dt;
        // EntityLivingBase.updateActiveHand: while 25 ticks or fewer remain,
        // emit an eat event every four ticks (0.2 s).
        int soundsDue = Math.max(0, Math.min(6, (int)Math.floor((eatingProgress - 0.2) / 0.2)));
        while (eatingSoundStep < soundsDue) {
            sound.playEat();
            eatingSoundStep++;
        }
        if (eatingProgress < 1.6) return;
        int food = craft3dgl.combat.DamageSystem.foodValue(eatingItemId);
        hunger = Math.min(20, hunger + food);
        sound.playEat();
        sound.playBurp();
        regenTimer = 0;
        invCount[selectedSlot]--;
        if (invCount[selectedSlot] <= 0) { invId[selectedSlot] = 0; invCount[selectedSlot] = 0; }
        eatingItemId = 0;
        eatingProgress = 0;
        eatingSoundStep = 0;
    }

    // ====== VILLAGER LOGIKA ======

    void updateVillagers(double dt) {
        if (villagers.isEmpty()) return;
        for (VillagerGL v : villagers) {
            v.age += dt;
            v.hurtResistantTime = Math.max(0.0, v.hurtResistantTime - dt);
            v.hurtTime = Math.max(0.0, v.hurtTime - dt);
            v.walkTimer -= dt;
            if (v.walkTimer <= 0) {
                v.walkTimer = 1.6 + random.nextDouble() * 3.0;
                // Jezeli za daleko od domu - wracaj.
                if (v.hasHome) {
                    double dx = v.homeX - v.x;
                    double dz = v.homeZ - v.z;
                    double d2 = dx * dx + dz * dz;
                    if (d2 > 25 * 25) {
                        // wracaj do domu
                        double n = Math.sqrt(d2);
                        double sp = 0.7;
                        v.vx = (dx / n) * sp;
                        v.vz = (dz / n) * sp;
                        v.targetYaw = Math.atan2(v.vx, v.vz);
                    } else if (random.nextDouble() < 0.45) {
                        v.vx = 0; v.vz = 0;
                    } else {
                        double a = random.nextDouble() * Math.PI * 2;
                        double sp = 0.55;
                        v.vx = Math.sin(a) * sp;
                        v.vz = Math.cos(a) * sp;
                        v.targetYaw = a;
                    }
                } else {
                    if (random.nextDouble() < 0.45) { v.vx = 0; v.vz = 0; }
                    else {
                        double a = random.nextDouble() * Math.PI * 2;
                        double sp = 0.55;
                        v.vx = Math.sin(a) * sp;
                        v.vz = Math.cos(a) * sp;
                        v.targetYaw = a;
                    }
                }
            }
            if (v.onGround && v.vy < 0) v.vy = 0;
            if (!v.onGround) {
                v.vy -= 20 * dt;
                if (v.vy < -24) v.vy = -24;
            }
            double nx = v.x + (v.vx + v.knockbackX) * dt;
            double nz = v.z + (v.vz + v.knockbackZ) * dt;
            boolean bx = !villagerFreeAt(nx, v.y, v.z);
            boolean bz = !villagerFreeAt(v.x, v.y, nz);

            // === AUTO-JUMP + STEP ASSIST ===
            // Jezeli stoi na ziemi i jest blokada przed nim, ale 1 blok wyzej jest wolne:
            //   1. STEP UP - od razu przesun o 1 blok do gory + posun w bok (dziala dla schodow/krawednikow)
            //   2. Jezeli step up nie wystarczy, SKOK z duza predkoscia (vy=8.5 by bezpiecznie wlecial na 1.8 bloku)
            if (v.onGround && (bx || bz)) {
                double tryX = bx ? nx : v.x;
                double tryZ = bz ? nz : v.z;
                // STEP UP: villager wskakuje natychmiast na blok 1 wysoki (jak gracz w MC w trybie "auto-step")
                if (villagerFreeAt(tryX, v.y + 1.0, tryZ) && villagerFreeAt(tryX, v.y + 1.001, tryZ)) {
                    // Wskocz na blok od razu - przesun stopy do gory o 1
                    v.y = Math.floor(v.y) + 1.0;
                    v.x = tryX;
                    v.z = tryZ;
                    bx = false;
                    bz = false;
                    v.vy = 0;
                    v.onGround = true;
                } else if (villagerFreeAt(tryX, v.y + 1.0, tryZ)) {
                    // Fallback: skok normalny
                    v.vy = 8.5;
                    v.onGround = false;
                }
            }

            // Ruch w bok - jezeli wciaz blokowany ale jestesmy w powietrzu, sprobuj ze zaktualizowana wysokoscia
            if (bx && !v.onGround && villagerFreeAt(nx, v.y, v.z)) bx = false;
            if (bz && !v.onGround && villagerFreeAt(v.x, v.y, nz)) bz = false;

            if (!bx) v.x = nx; else v.knockbackX = 0.0;
            if (!bz) v.z = nz; else v.knockbackZ = 0.0;

            // Ruch pionowy
            double ny = v.y + v.vy * dt;
            if (v.vy <= 0) {
                double floor = findVillagerFloorBelow(v.x, v.y, v.z);
                if (floor >= 0 && ny <= floor + 1e-6) { v.y = floor; v.vy = 0; v.onGround = true; }
                else { v.y = ny; v.onGround = false; }
            } else {
                if (villagerFreeAt(v.x, ny, v.z)) { v.y = ny; v.onGround = false; } else v.vy = 0;
            }

            // ANTI-STUCK: jezeli villager utknal w bloku (np. domek zostal zbudowany na nim),
            // wypchnij w gore
            if (!villagerFreeAt(v.x, v.y, v.z)) {
                for (int up = 1; up <= 4; up++) {
                    if (villagerFreeAt(v.x, v.y + up, v.z)) {
                        v.y += up;
                        v.vy = 0;
                        v.onGround = false;
                        break;
                    }
                }
            }

            double knockbackDrag = Math.pow(v.onGround ? 0.546 : 0.91, dt * 20.0);
            v.knockbackX *= knockbackDrag;
            v.knockbackZ *= knockbackDrag;
            if (Math.abs(v.knockbackX) < 0.01) v.knockbackX = 0.0;
            if (Math.abs(v.knockbackZ) < 0.01) v.knockbackZ = 0.0;

            if (v.y < -3) { v.health = 0; }
            smoothVillager(v, dt);
        }
        villagers.removeIf(v -> v.health <= 0);
    }

    boolean villagerFreeAt(double vx, double vy, double vz) {
        return craft3dgl.entities.EntityCollision.isFreeAt(vx, vy, vz, 0.34, 1.80, entitySolidCheck, WORLD_X, WORLD_Y, WORLD_Z);
    }

    double findVillagerFloorBelow(double vx, double vy, double vz) {
        return craft3dgl.entities.EntityCollision.findFloorBelow(vx, vy, vz, 0.34, entitySolidCheck);
    }

    void smoothVillager(VillagerGL v, double dt) {
        if (!v.displayInit) { v.displayY = v.y; v.displayInit = true; }
        v.displayY = craft3dgl.entities.EntityCollision.smoothY(v.displayY, v.y, 18 * dt);
        v.yaw = craft3dgl.entities.EntityCollision.smoothYaw(v.yaw, v.targetYaw, 4.5 * dt);
    }

    VillagerGL findTargetVillager(double maxDist) {
        double cp = Math.cos(pitch);
        double dx = Math.sin(yaw) * cp;
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * cp;
        double ox = x, oy = y + eyeHeight(), oz = z;
        VillagerGL best = null;
        double bestT = maxDist;
        double radius = craft3dgl.entities.EntityConstants.VILLAGER_RADIUS;
        double height = craft3dgl.entities.EntityConstants.VILLAGER_HEIGHT;
        for (VillagerGL villager : villagers) {
            double[] interval = {0.0, bestT};
            if (!clipRayAxis(ox, dx, villager.x - radius, villager.x + radius, interval)) continue;
            if (!clipRayAxis(oy, dy, villager.y, villager.y + height, interval)) continue;
            if (!clipRayAxis(oz, dz, villager.z - radius, villager.z + radius, interval)) continue;
            if (interval[0] <= bestT) {
                best = villager;
                bestT = interval[0];
            }
        }
        return best;
    }

    void attackVillager(VillagerGL villager, boolean sprinting, boolean inWater,
                        boolean movingTooFastToSweep) {
        craft3dgl.combat.DamageSystem.AttackResult attack =
                beginMeleeAttack(sprinting, inWater, movingTooFastToSweep);
        craft3dgl.combat.DamageSystem.DamageResult damage =
                craft3dgl.combat.DamageSystem.resolveDamage(attack.damage,
                        villager.hurtResistantTime, villager.lastDamage);
        villager.hurtResistantTime = damage.hurtResistantTime;
        villager.lastDamage = damage.lastDamage;
        if (!damage.accepted) {
            sound.playAttackNoDamage();
            return;
        }

        villager.health -= damage.appliedDamage;
        if (damage.newHurt) {
            villager.hurtTime = craft3dgl.combat.DamageSystem.HURT_FLASH_SECONDS;
            knockBackVillager(villager, 0.4f);
            sound.playVillagerHurt();
        }
        if (attack.sprintKnockback) {
            knockBackVillager(villager, 0.5f);
            sprintDisableTimer = 0.05;
        }
        double villagerWidth = craft3dgl.entities.EntityConstants.VILLAGER_RADIUS * 2.0;
        double villagerHeight = craft3dgl.entities.EntityConstants.VILLAGER_HEIGHT;
        spawnEntityHitHearts(villager.x, villager.y, villager.z, villagerWidth, villagerHeight);
        if (attack.critical) spawnCriticalHitParticles(
                villager.x, villager.y, villager.z, villagerWidth, villagerHeight);
        craft3dgl.ui.Crosshair.triggerHit();
        playMeleeAttackSound(attack);
        if (attack.sweeping) performSweepAttack(villager.x, villager.y, villager.z, null, villager);
    }

    craft3dgl.combat.DamageSystem.AttackResult beginMeleeAttack(boolean sprinting, boolean inWater,
                                                               boolean movingTooFastToSweep) {
        int item = selectedItemId();
        boolean falling = !onGround && velY < 0.0;
        craft3dgl.combat.DamageSystem.AttackResult attack =
                craft3dgl.combat.DamageSystem.createAttack(ticksSinceLastSwing, item,
                        sprinting, falling, onGround, inWater, flying, movingTooFastToSweep);
        ticksSinceLastSwing = 0.0;
        return attack;
    }

    void playMeleeAttackSound(craft3dgl.combat.DamageSystem.AttackResult attack) {
        if (attack.critical) sound.playAttackCritical();
        else if (attack.sprintKnockback) sound.playAttackKnockback();
        else if (attack.sweeping) sound.playAttackSweep();
        else if (attack.strong) sound.playAttackStrong();
        else sound.playAttackWeak();
    }

    void knockBackVillager(VillagerGL villager, float strength) {
        double totalX = villager.vx + villager.knockbackX;
        double totalZ = villager.vz + villager.knockbackZ;
        craft3dgl.combat.DamageSystem.KnockbackResult result =
                craft3dgl.combat.DamageSystem.knockBack(totalX, villager.vy, totalZ,
                        villager.onGround, strength, x - villager.x, z - villager.z);
        villager.knockbackX = result.motionX - villager.vx;
        villager.knockbackZ = result.motionZ - villager.vz;
        villager.vy = result.motionY;
        if (result.motionY > 0.0) villager.onGround = false;
    }

    void performSweepAttack(double targetX, double targetY, double targetZ,
                            AnimalGL primaryAnimal, VillagerGL primaryVillager) {
        java.util.ArrayList<AnimalGL> nearbyAnimals = new java.util.ArrayList<>(animals);
        for (AnimalGL animal : nearbyAnimals) {
            if (animal == primaryAnimal || !insideSweepArea(animal.x, animal.y, animal.z,
                    targetX, targetY, targetZ)) continue;
            knockBackAnimal(animal, 0.4f); // explicit EntityPlayer sweep push
            craft3dgl.combat.DamageSystem.DamageResult result =
                    craft3dgl.combat.DamageSystem.resolveDamage(1.0f,
                            animal.hurtResistantTime, animal.lastDamage);
            animal.hurtResistantTime = result.hurtResistantTime;
            animal.lastDamage = result.lastDamage;
            if (!result.accepted) continue;
            animal.health -= result.appliedDamage;
            // A fresh hurt additionally performs normal EntityLivingBase knockback.
            if (result.newHurt) {
                animal.hurtTime = craft3dgl.combat.DamageSystem.HURT_FLASH_SECONDS;
                knockBackAnimal(animal, 0.4f);
                playAnimalHurtSound(animal);
            }
            startAnimalPanic(animal);
            spawnEntityHitHearts(animal.x, animal.y, animal.z,
                    craft3dgl.entities.EntityConstants.ANIMAL_RADIUS * 2.0,
                    craft3dgl.entities.EntityConstants.animalHeight(animal.type));
            if (animal.health <= 0.0f && animals.contains(animal)) killAnimal(animal);
        }
        java.util.ArrayList<VillagerGL> nearbyVillagers = new java.util.ArrayList<>(villagers);
        for (VillagerGL villager : nearbyVillagers) {
            if (villager == primaryVillager || !insideSweepArea(villager.x, villager.y, villager.z,
                    targetX, targetY, targetZ)) continue;
            knockBackVillager(villager, 0.4f); // explicit EntityPlayer sweep push
            craft3dgl.combat.DamageSystem.DamageResult result =
                    craft3dgl.combat.DamageSystem.resolveDamage(1.0f,
                            villager.hurtResistantTime, villager.lastDamage);
            villager.hurtResistantTime = result.hurtResistantTime;
            villager.lastDamage = result.lastDamage;
            if (!result.accepted) continue;
            villager.health -= result.appliedDamage;
            if (result.newHurt) {
                villager.hurtTime = craft3dgl.combat.DamageSystem.HURT_FLASH_SECONDS;
                knockBackVillager(villager, 0.4f);
                sound.playVillagerHurt();
            }
            spawnEntityHitHearts(villager.x, villager.y, villager.z,
                    craft3dgl.entities.EntityConstants.VILLAGER_RADIUS * 2.0,
                    craft3dgl.entities.EntityConstants.VILLAGER_HEIGHT);
        }
    }

    boolean insideSweepArea(double entityX, double entityY, double entityZ,
                            double targetX, double targetY, double targetZ) {
        double dxTarget = entityX - targetX;
        double dzTarget = entityZ - targetZ;
        double dxPlayer = entityX - x;
        double dzPlayer = entityZ - z;
        return dxTarget * dxTarget + dzTarget * dzTarget <= 2.25
                && dxPlayer * dxPlayer + dzPlayer * dzPlayer < 9.0
                && Math.abs(entityY - targetY) <= 1.25;
    }

    void spawnEntityHitHearts(double entityX, double entityY, double entityZ,
                              double width, double height) {
        for (int i = 0; i < 3; i++) {
            double px = entityX + (random.nextDouble() * 2.0 - 1.0) * width;
            double py = entityY + 0.5 + random.nextDouble() * height;
            double pz = entityZ + (random.nextDouble() * 2.0 - 1.0) * width;
            particleSystem.add(Particle.heart(random, px, py, pz));
        }
    }

    void spawnCriticalHitParticles(double entityX, double entityY, double entityZ,
                                   double width, double height) {
        for (int i = 0; i < 15; i++) {
            double px = entityX + (random.nextDouble() * 2.0 - 1.0) * width;
            double py = entityY + random.nextDouble() * height;
            double pz = entityZ + (random.nextDouble() * 2.0 - 1.0) * width;
            particleSystem.add(Particle.crit(random, px, py, pz,
                    random.nextGaussian() * 0.02,
                    random.nextGaussian() * 0.02,
                    random.nextGaussian() * 0.02));
        }
    }

    void openVillagerTrade(VillagerGL v) {
        tradingVillager = v;
        villagerTradeOpen = true;
        inventoryOpen = false;
        creativeInvOpen = false;
        chestOpen = false;
        mouseCaptured = false;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        // Zablokuj klawisze zeby tej samej milisekundy nie zamknac
        eWasDown = true;
        escWasDown = true;
        villagerMouseWasDown = true;
        rightWasDown = true;
        sound.playClick();
    }

    void closeVillagerTrade() {
        villagerTradeOpen = false;
        tradingVillager = null;
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        // Zablokuj E/ESC zeby od razu nie otworzyl sie inny ekran
        eWasDown = true;
        escWasDown = true;
        leftWasDown = true;
        rightWasDown = true;
        sound.playClick();
    }

    void handleVillagerTradeInput(boolean left) {
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS && !escWasDown) { closeVillagerTrade(); return; }
        // NAPRAWIONE: zamykanie przez E (jak w MC)
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS && !eWasDown) { closeVillagerTrade(); return; }
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        int mx = (int)mxA[0], my = (int)myA[0];
        if (left && !villagerMouseWasDown) {
            int trade = craft3dgl.ui.TradeUIRenderer.hitTest(mx, my, width, height);
            if (trade >= 0 && trade <= 2) doVillagerTrade(trade);
            else if (trade == 3) closeVillagerTrade();
        }
        villagerMouseWasDown = left;
    }

    void doVillagerTrade(int trade) {
        boolean success = false;
        if (trade == 0) {
            if (removeItems(WOOD, 8)) { addItem(ITEM_EMERALD, 1); success = true; }
        } else if (trade == 1) {
            if (removeItems(ITEM_EMERALD, 1)) { addItem(ITEM_BREAD, 3); success = true; }
        } else if (trade == 2) {
            // Pszenica -> szmaragd (20 wheat = 1 emerald, dobra cena dla rolnika)
            if (removeItems(ITEM_WHEAT, 20)) { addItem(ITEM_EMERALD, 1); success = true; }
        }
        if (success) sound.playVillagerYes();
        else sound.playVillagerNo();
    }

    int countItem(int id) { int c = 0; for (int i = 0; i < INVENTORY_SIZE; i++) if (invId[i] == id) c += invCount[i]; return c; }
    boolean removeItems(int id, int count) {
        if (countItem(id) < count) return false;
        for (int i = 0; i < INVENTORY_SIZE && count > 0; i++) if (invId[i] == id) {
            int take = Math.min(count, invCount[i]);
            invCount[i] -= take; count -= take;
            if (invCount[i] <= 0) { invId[i] = 0; invCount[i] = 0; }
        }
        return true;
    }

    // ====== NOWY, LADNIEJSZY MODEL VILLAGERA ======
    void drawVillagers() {
        // Rysujemy per-villager zeby ustawic swiatlo z bloku pod nim
        for (VillagerGL v : villagers) {
            applyEntityLight(v.x, v.y + 0.9, v.z, v.hurtTime > 0.0);
            java.util.ArrayList<VillagerGL> one = new java.util.ArrayList<>(1);
            one.add(v);
            craft3dgl.entities.VillagerRenderer.drawAll(one);
        }
        craft3dgl.ui.CuboidHelper.clearTint();
        glColor4f(1,1,1,1);
    }

    /** Applies combined sky/block lighting and the LivingBase hurt overlay to a model. */
    void applyEntityLight(double ex, double ey, double ez) {
        applyEntityLight(ex, ey, ez, false);
    }

    void applyEntityLight(double ex, double ey, double ez, boolean hurtFlash) {
        if (lightEngine == null) {
            if (hurtFlash) craft3dgl.ui.CuboidHelper.setTint(1.0f, 0.25f, 0.25f);
            else craft3dgl.ui.CuboidHelper.clearTint();
            glColor4f(1.0f, hurtFlash ? 0.25f : 1.0f,
                    hurtFlash ? 0.25f : 1.0f, 1.0f);
            return;
        }
        int bx = clampInt((int) Math.floor(ex), 0, WORLD_X - 1);
        int by = clampInt((int) Math.floor(ey), 0, WORLD_Y - 1);
        int bz = clampInt((int) Math.floor(ez), 0, WORLD_Z - 1);
        float s1 = lightEngine.sampleShade(bx, by, bz, currentDayMult);
        int by2 = Math.min(by + 1, WORLD_Y - 1);
        float s2 = lightEngine.sampleShade(bx, by2, bz, currentDayMult);
        float s = Math.max(s1, s2);
        // sampleShade already includes sky-day brightness; do not multiply it twice.
        // Night vision boost for fixed-function entity models.
        float entityAmbientMin = hasNightVision() ? 0.85f : (0.10f + currentDayMult * 0.20f);
        s = Math.max(entityAmbientMin, s);
        float greenBlue = hurtFlash ? s * 0.25f : s;
        craft3dgl.ui.CuboidHelper.setTint(s, greenBlue, greenBlue);
        glColor4f(s, greenBlue, greenBlue, 1f);
    }


    void drawVillagerTradeUI() { craft3dgl.ui.TradeUIRenderer.draw(fontRenderer, width, height, language); }

    // ====== ANIMALS ======

    void updateAnimals(double dt) {
        for (AnimalGL a : animals) {
            double previousX = a.x;
            double previousZ = a.z;
            a.age += dt;
            a.hurtResistantTime = Math.max(0.0, a.hurtResistantTime - dt);
            a.hurtTime = Math.max(0.0, a.hurtTime - dt);
            boolean inWater = animalInWater(a);
            boolean panicking = a.panicTimer > 0;
            if (panicking) {
                a.panicTimer -= dt;
                if (a.panicRedirect > 0) a.panicRedirect -= dt;
                a.panicRecalc -= dt;
                if (a.panicRecalc <= 0) {
                    a.panicRecalc = 0.30 + random.nextDouble() * 0.25;
                    double ddx = a.x - x;
                    double ddz = a.z - z;
                    double len = Math.sqrt(ddx * ddx + ddz * ddz);
                    if (len > 0.0001) { ddx /= len; ddz /= len; }
                    else { ddx = Math.sin(a.yaw); ddz = Math.cos(a.yaw); }
                    double jitter = (random.nextDouble() - 0.5) * 0.45;
                    double cs = Math.cos(jitter), sn = Math.sin(jitter);
                    a.panicDirX = ddx * cs - ddz * sn;
                    a.panicDirZ = ddx * sn + ddz * cs;
                }
                double panicSpeed = craft3dgl.entities.EntityConstants.animalPanicSpeed(a.type);
                if (inWater) panicSpeed *= 0.50;
                a.vx = a.panicDirX * panicSpeed;
                a.vz = a.panicDirZ * panicSpeed;
                a.targetYaw = Math.atan2(a.vx, a.vz);
                a.walkTimer = 0.5;
            } else {
                a.walkTimer -= dt;
                if (a.walkTimer <= 0) {
                    a.walkTimer = 1.2 + random.nextDouble() * 3.4;
                    if (random.nextDouble() < 0.42) {
                        a.vx = 0;
                        a.vz = 0;
                    } else {
                        double ty = random.nextDouble() * Math.PI * 2.0;
                        double sp = craft3dgl.entities.EntityConstants.animalSpeed(a.type);
                        if (inWater) sp *= 0.50;
                        a.vx = Math.sin(ty) * sp;
                        a.vz = Math.cos(ty) * sp;
                        a.targetYaw = ty;
                    }
                }
            }

            if (a.onGround && a.vy < 0) a.vy = 0;
            if (!a.onGround) {
                double g = inWater ? 4.0 : 22.0;
                a.vy -= g * dt;
                double terminal = inWater ? -2.5 : -26;
                if (a.vy < terminal) a.vy = terminal;
                if (inWater && a.vy < 1.2) a.vy += 6.0 * dt;
            }

            double nx = a.x + (a.vx + a.knockbackX) * dt;
            double nz = a.z + (a.vz + a.knockbackZ) * dt;
            boolean blockedX = !animalFreeAt(a, nx, a.y, a.z);
            boolean blockedZ = !animalFreeAt(a, a.x, a.y, nz);

            if (a.onGround && (blockedX || blockedZ)) {
                double tryX = blockedX ? nx : a.x;
                double tryZ = blockedZ ? nz : a.z;
                if (animalFreeAt(a, tryX, a.y + 1.0, tryZ)) {
                    a.vy = 7.0;
                    a.onGround = false;
                }
            }

            if (!blockedX) a.x = nx; else a.knockbackX = 0.0;
            if (!blockedZ) a.z = nz; else a.knockbackZ = 0.0;

            if (panicking && blockedX && blockedZ && a.onGround && a.panicRedirect <= 0) {
                double bestX = 0, bestZ = 0;
                double bestScore = -1e9;
                double baseAngle = Math.atan2(a.panicDirX, a.panicDirZ);
                double awayX = a.x - x;
                double awayZ = a.z - z;
                double awayLen = Math.sqrt(awayX*awayX + awayZ*awayZ);
                if (awayLen > 0.0001) { awayX /= awayLen; awayZ /= awayLen; }
                for (int k = 0; k < 8; k++) {
                    double ang = baseAngle + k * (Math.PI / 4);
                    double dx2 = Math.sin(ang), dz2 = Math.cos(ang);
                    double tx = a.x + dx2 * 0.55;
                    double tz = a.z + dz2 * 0.55;
                    if (!animalFreeAt(a, tx, a.y, tz)) continue;
                    double score = dx2 * awayX + dz2 * awayZ;
                    if (score > bestScore) { bestScore = score; bestX = dx2; bestZ = dz2; }
                }
                if (bestScore > -1e8) {
                    a.panicDirX = bestX;
                    a.panicDirZ = bestZ;
                    a.panicRedirect = 0.20;
                    a.panicRecalc = 0.40;
                }
            }

            double ny = a.y + a.vy * dt;
            if (a.vy <= 0) {
                double floorY = findAnimalFloorBelow(a, a.y);
                if (floorY >= 0 && ny <= floorY + 1e-6) {
                    a.y = floorY;
                    a.vy = 0;
                    a.onGround = true;
                } else if (floorY >= 0 && Math.abs(a.y - floorY) < 1e-4 && a.vy == 0) {
                    a.y = floorY;
                    a.onGround = true;
                } else {
                    a.y = ny;
                    a.onGround = false;
                }
            } else {
                if (animalFreeAt(a, a.x, ny, a.z)) {
                    a.y = ny;
                    a.onGround = false;
                } else {
                    a.vy = 0;
                }
            }

            if (a.onGround) {
                double floorCheck = findAnimalFloorBelow(a, a.y);
                if (floorCheck < 0 || Math.abs(a.y - floorCheck) > 0.05) {
                    a.onGround = false;
                }
            }

            double knockbackDrag = Math.pow(a.onGround ? 0.546 : 0.91, dt * 20.0);
            a.knockbackX *= knockbackDrag;
            a.knockbackZ *= knockbackDrag;
            if (Math.abs(a.knockbackX) < 0.01) a.knockbackX = 0.0;
            if (Math.abs(a.knockbackZ) < 0.01) a.knockbackZ = 0.0;

            if (a.y < -3) {
                int sy = -1;
                int bx = clampInt((int)Math.floor(a.x), 1, WORLD_X - 2);
                int bz = clampInt((int)Math.floor(a.z), 1, WORLD_Z - 2);
                for (int yy = WORLD_Y - 2; yy >= 1; yy--) {
                    if (solid(bx, yy, bz) && !solid(bx, yy + 1, bz) && !solid(bx, yy + 2, bz)) { sy = yy + 1; break; }
                }
                if (sy > 0) { a.y = sy; a.displayY = sy; a.vy = 0; a.onGround = true; }
            }
            smoothAnimalVisual(a, dt);
            updateAnimalModelAnimation(a, previousX, previousZ, dt);
        }
    }

    /**
     * EntityLivingBase limbSwing + EntityAIWatchClosest (zasieg 6 blokow),
     * przeliczone na niezalezny od FPS krok czasu.
     */
    void updateAnimalModelAnimation(AnimalGL animal, double previousX, double previousZ, double dt) {
        double movedX = animal.x - previousX;
        double movedZ = animal.z - previousZ;
        double speed = Math.sqrt(movedX * movedX + movedZ * movedZ) / Math.max(0.0001, dt);
        float targetAmount = (float)Math.min(1.0, speed * 0.2);
        float blend = (float)(1.0 - Math.pow(0.6, dt * 20.0));
        animal.limbSwingAmount += (targetAmount - animal.limbSwingAmount) * blend;
        animal.limbSwing += animal.limbSwingAmount * dt * 20.0;

        double toPlayerX = x - animal.x;
        double toPlayerZ = z - animal.z;
        double horizontal = Math.sqrt(toPlayerX * toPlayerX + toPlayerZ * toPlayerZ);
        double targetHeadYaw = 0.0;
        double targetHeadPitch = 0.0;
        if (horizontal <= 6.0) {
            double lookYaw = Math.atan2(toPlayerX, toPlayerZ);
            targetHeadYaw = -Math.atan2(Math.sin(lookYaw - animal.yaw),
                    Math.cos(lookYaw - animal.yaw));
            targetHeadYaw = clamp(targetHeadYaw, -Math.PI / 3.0, Math.PI / 3.0);
            double animalEye = animal.y + craft3dgl.entities.EntityConstants.animalEyeHeight(animal.type);
            double playerEye = y + eyeHeight();
            targetHeadPitch = -Math.atan2(playerEye - animalEye, Math.max(0.001, horizontal));
            targetHeadPitch = clamp(targetHeadPitch, -Math.PI / 4.0, Math.PI / 4.0);
        }
        animal.headYaw = craft3dgl.entities.EntityCollision.smoothYaw(
                animal.headYaw, targetHeadYaw, Math.min(1.0, dt * 7.0));
        animal.headPitch += (targetHeadPitch - animal.headPitch) * Math.min(1.0, dt * 7.0);
    }

    boolean animalInWater(AnimalGL a) {
        return craft3dgl.entities.EntityCollision.inWater(a.x, a.y, a.z,
                craft3dgl.entities.EntityConstants.ANIMAL_RADIUS,
                craft3dgl.entities.EntityConstants.animalHeight(a.type), entitySolidCheck);
    }

    boolean animalFreeAt(AnimalGL animal, double ax, double ay, double az) {
        return craft3dgl.entities.EntityCollision.isFreeAt(ax, ay, az,
                craft3dgl.entities.EntityConstants.ANIMAL_RADIUS,
                craft3dgl.entities.EntityConstants.animalHeight(animal.type),
                entitySolidCheck, WORLD_X, WORLD_Y, WORLD_Z);
    }

    double findAnimalFloorBelow(AnimalGL animal, double startY) {
        return craft3dgl.entities.EntityCollision.findFloorBelow(animal.x, startY, animal.z,
                craft3dgl.entities.EntityConstants.ANIMAL_RADIUS, entitySolidCheck);
    }

    void unstickAnimal(AnimalGL a) {
        if (animalFreeAt(a, a.x, a.y, a.z)) return;
        for (int i = 1; i <= 32; i++) {
            double ny = Math.floor(a.y) + i;
            if (animalFreeAt(a, a.x, ny, a.z)) {
                a.y = ny;
                a.displayY = ny;
                a.vy = 0;
                a.onGround = true;
                return;
            }
        }
        for (int r = 1; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                double tx = a.x + dx, tz = a.z + dz;
                for (int i = 0; i <= 32; i++) {
                    double ny = Math.floor(a.y) + i;
                    if (animalFreeAt(a, tx, ny, tz)) {
                        a.x = tx; a.z = tz; a.y = ny;
                        a.displayY = ny; a.vy = 0; a.onGround = true;
                        return;
                    }
                }
            }
        }
    }

    void smoothAnimalVisual(AnimalGL a, double dt) {
        if (!a.displayInit) { a.displayY = a.y; a.displayInit = true; }
        double rate = (a.y > a.displayY) ? 14.0 : 28.0;
        a.displayY = craft3dgl.entities.EntityCollision.smoothY(a.displayY, a.y, rate * dt);
        double turnRate = a.panicTimer > 0 ? 9.0 : 5.0;
        a.yaw = craft3dgl.entities.EntityCollision.smoothYaw(a.yaw, a.targetYaw, turnRate * dt);
    }

    AnimalGL findTargetAnimal(double maxDist) {
        double cp = Math.cos(pitch);
        double dx = Math.sin(yaw) * cp;
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * cp;
        double ox = x, oy = y + eyeHeight(), oz = z;
        AnimalGL best = null;
        double bestT = maxDist;
        for (AnimalGL animal : animals) {
            double radius = craft3dgl.entities.EntityConstants.ANIMAL_RADIUS;
            double height = craft3dgl.entities.EntityConstants.animalHeight(animal.type);
            double[] interval = {0.0, bestT};
            if (!clipRayAxis(ox, dx, animal.x - radius, animal.x + radius, interval)) continue;
            if (!clipRayAxis(oy, dy, animal.y, animal.y + height, interval)) continue;
            if (!clipRayAxis(oz, dz, animal.z - radius, animal.z + radius, interval)) continue;
            if (interval[0] <= bestT) {
                best = animal;
                bestT = interval[0];
            }
        }
        return best;
    }

    double entityRayDistance(double entityX, double entityY, double entityZ,
                             double radius, double entityHeight, double maxDistance) {
        double cp = Math.cos(pitch);
        double dx = Math.sin(yaw) * cp;
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * cp;
        double[] interval = {0.0, maxDistance};
        if (!clipRayAxis(x, dx, entityX - radius, entityX + radius, interval)) return Double.POSITIVE_INFINITY;
        if (!clipRayAxis(y + eyeHeight(), dy, entityY, entityY + entityHeight, interval)) return Double.POSITIVE_INFINITY;
        if (!clipRayAxis(z, dz, entityZ - radius, entityZ + radius, interval)) return Double.POSITIVE_INFINITY;
        return interval[0];
    }

    /** Slab test jednej osi dla ray kontra AABB. */
    static boolean clipRayAxis(double origin, double direction, double min, double max,
                               double[] interval) {
        if (Math.abs(direction) < 1.0e-9) return origin >= min && origin <= max;
        double t0 = (min - origin) / direction;
        double t1 = (max - origin) / direction;
        if (t0 > t1) { double swap = t0; t0 = t1; t1 = swap; }
        interval[0] = Math.max(interval[0], t0);
        interval[1] = Math.min(interval[1], t1);
        return interval[1] >= interval[0];
    }

    void attackAnimal(AnimalGL animal, boolean sprinting, boolean inWater,
                      boolean movingTooFastToSweep) {
        craft3dgl.combat.DamageSystem.AttackResult attack =
                beginMeleeAttack(sprinting, inWater, movingTooFastToSweep);
        craft3dgl.combat.DamageSystem.DamageResult damage =
                craft3dgl.combat.DamageSystem.resolveDamage(attack.damage,
                        animal.hurtResistantTime, animal.lastDamage);
        animal.hurtResistantTime = damage.hurtResistantTime;
        animal.lastDamage = damage.lastDamage;
        if (!damage.accepted) {
            sound.playAttackNoDamage();
            return;
        }

        animal.health -= damage.appliedDamage;
        if (damage.newHurt) {
            animal.hurtTime = craft3dgl.combat.DamageSystem.HURT_FLASH_SECONDS;
            knockBackAnimal(animal, 0.4f);
            playAnimalHurtSound(animal);
        }
        if (attack.sprintKnockback) {
            knockBackAnimal(animal, 0.5f);
            sprintDisableTimer = 0.05;
        }
        double animalWidth = craft3dgl.entities.EntityConstants.ANIMAL_RADIUS * 2.0;
        double animalHeight = craft3dgl.entities.EntityConstants.animalHeight(animal.type);
        spawnEntityHitHearts(animal.x, animal.y, animal.z, animalWidth, animalHeight);
        if (attack.critical) spawnCriticalHitParticles(
                animal.x, animal.y, animal.z, animalWidth, animalHeight);
        craft3dgl.ui.Crosshair.triggerHit();
        playMeleeAttackSound(attack);
        startAnimalPanic(animal);
        if (attack.sweeping) performSweepAttack(animal.x, animal.y, animal.z, animal, null);

        if (animal.health <= 0.0f) killAnimal(animal);
    }

    void knockBackAnimal(AnimalGL animal, float strength) {
        double totalX = animal.vx + animal.knockbackX;
        double totalZ = animal.vz + animal.knockbackZ;
        craft3dgl.combat.DamageSystem.KnockbackResult result =
                craft3dgl.combat.DamageSystem.knockBack(totalX, animal.vy, totalZ,
                        animal.onGround, strength, x - animal.x, z - animal.z);
        animal.knockbackX = result.motionX - animal.vx;
        animal.knockbackZ = result.motionZ - animal.vz;
        animal.vy = result.motionY;
        if (result.motionY > 0.0) animal.onGround = false;
    }

    void playAnimalHurtSound(AnimalGL animal) {
        if (animal.type == AnimalGL.COW) sound.playCow();
        else if (animal.type == AnimalGL.PIG) sound.playPig();
        else if (animal.type == AnimalGL.SHEEP) sound.playSheep();
        else sound.playAnimal();
    }

    void startAnimalPanic(AnimalGL animal) {
        double awayX = animal.x - x;
        double awayZ = animal.z - z;
        double length = Math.sqrt(awayX * awayX + awayZ * awayZ);
        if (length > 0.0001) {
            animal.panicDirX = awayX / length;
            animal.panicDirZ = awayZ / length;
        } else {
            animal.panicDirX = -Math.sin(yaw);
            animal.panicDirZ = -Math.cos(yaw);
        }
        animal.panicTimer = craft3dgl.entities.EntityConstants.ANIMAL_PANIC_TIME;
        animal.panicRecalc = 0.0;
        animal.panicRedirect = 0.0;
        animal.targetYaw = Math.atan2(animal.panicDirX, animal.panicDirZ);
    }

    void killAnimal(AnimalGL animal) {
        int drop = animal.type == AnimalGL.COW ? ITEM_BEEF
                : animal.type == AnimalGL.SHEEP ? ITEM_MUTTON : ITEM_PORK;
        spawnDrop(animal.x, animal.y + 0.45, animal.z, drop, 1 + random.nextInt(3));
        int orbCount = 1 + random.nextInt(3);
        int totalXp = 1 + random.nextInt(3);
        for (int i = 0; i < orbCount; i++) {
            int xpPerOrb = Math.max(1, totalXp / orbCount);
            xpOrbs.add(new craft3dgl.entities.ExperienceOrb(
                    animal.x, animal.y + 0.6, animal.z, xpPerOrb));
        }
        animals.remove(animal);
    }

    void updateXpOrbs(double dt) {
        double gravity = 12.0;
        double drag = 0.85;
        java.util.Iterator<craft3dgl.entities.ExperienceOrb> it = xpOrbs.iterator();
        while (it.hasNext()) {
            craft3dgl.entities.ExperienceOrb orb = it.next();
            orb.age += dt;
            orb.lifetime += dt;
            // Usun po 5 minutach
            if (orb.lifetime > 300.0) { it.remove(); continue; }
            // Magnes - gdy blisko gracza, przyciagaj
            double dx = x - orb.x;
            double dy = (y + 1.0) - orb.y;
            double dz = z - orb.z;
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 1.0) {
                // Zebrany! Dodaj XP i usun
                addPlayerXp(orb.xpValue);
                sound.playXpPickup();
                it.remove();
                continue;
            }
            if (dist < 6.0) {
                // Przyciag - im blizej tym mocniej
                orb.magneted = true;
                double pull = 8.0 / Math.max(0.5, dist);
                orb.vx += (dx / dist) * pull * dt;
                orb.vy += (dy / dist) * pull * dt;
                orb.vz += (dz / dist) * pull * dt;
                // Wylacz grawitacje (unosi sie do gracza)
            } else {
                // Grawitacja normalna gdy daleko
                orb.vy -= gravity * dt;
            }
            // Move + collision (osobno per axis)
            orb.x += orb.vx * dt;
            orb.z += orb.vz * dt;
            double newY = orb.y + orb.vy * dt;
            // Ground collision - sprawdz blok POD orbam. Orb ma ~0.15 radius,
            // wiec dolna czesc = newY. Blok pod orbe = (int)floor(newY - epsilon)
            int bxG = (int) Math.floor(orb.x);
            int byG = (int) Math.floor(newY);
            int bzG = (int) Math.floor(orb.z);
            if (inWorld(bxG, byG, bzG) && solid(bxG, byG, bzG) && orb.vy <= 0) {
                // Odbicie od gornej sciany bloku - orb.y ustawiona 0.05 nad blokiem
                orb.y = byG + 1.05;
                orb.vy = 0;
                orb.vx *= 0.6;
                orb.vz *= 0.6;
            } else {
                orb.y = newY;
            }
            // Drag horizontal
            if (!orb.magneted) {
                orb.vx *= Math.pow(drag, dt);
                orb.vz *= Math.pow(drag, dt);
            } else {
                // Silny drag przy magnesie zeby nie latal wokol
                orb.vx *= Math.pow(0.5, dt);
                orb.vy *= Math.pow(0.5, dt);
                orb.vz *= Math.pow(0.5, dt);
            }
        }
    }

    void drawXpOrbs() {
        if (xpOrbs.isEmpty()) return;
        craft3dgl.entities.ExperienceOrbRenderer.drawAll(xpOrbs, x, y, z, yaw, pitch);
    }

    void drawAnimals() {
        for (AnimalGL a : animals) {
            applyEntityLight(a.x, a.y + 0.5, a.z, a.hurtTime > 0.0);
            java.util.ArrayList<AnimalGL> one = new java.util.ArrayList<>(1);
            one.add(a);
            craft3dgl.entities.AnimalRenderer.drawAll(one);
        }
        craft3dgl.ui.CuboidHelper.clearTint();
        glColor4f(1,1,1,1);
    }


    void damage(int amount) {
        if (amount <= 0 || deathScreen) return;
        if (gameMode == GAMEMODE_CREATIVE) return;
        sound.playHurt();
        // Screen shake proporcjonalny do damage
        screenShake = Math.max(screenShake, Math.min(1.0, 0.3 + amount * 0.05));
        // Czerwony blysk overlay
        damageFlash = Math.min(1.0, damageFlash + 0.5 + amount * 0.08);
        // Damage number nad graczem
        craft3dgl.ui.DamageNumbers.spawn(x, y + 1.9, z, "-" + amount, 1f, 0.35f, 0.35f);
        // Screen shake proporcjonalny do damage
        screenShake = Math.max(screenShake, Math.min(0.6, amount * 0.15));
        health -= amount;
        if (health <= 0) openDeathScreen();
    }

    void openDeathScreen() {
        health = 0;
        deathScreen = true;
        inventoryOpen = false;
        mouseCaptured = false;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        if (!deathDropsDone) {
            dropInventoryOnDeath();
            deathDropsDone = true;
        }
        saveWorld(currentWorldName);
    }

    void dropInventoryOnDeath() {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (invId[i] > 0 && invCount[i] > 0) {
                spawnDrop(x, y + 1.0, z, invId[i], invCount[i]);
                invId[i] = 0;
                invCount[i] = 0;
            }
        }
        for (int i = 0; i < equipId.length; i++) {
            if (equipId[i] > 0 && equipCount[i] > 0) {
                spawnDrop(x, y + 1.0, z, equipId[i], equipCount[i]);
                equipId[i] = 0;
                equipCount[i] = 0;
            }
        }
        cursorId = cursorCount = 0;
    }

    void respawnAfterDeath() {
        health = maxHealth;
        hunger = 20;
        deathScreen = false;
        deathDropsDone = false;
        spawnPlayer();
        velY = 0;
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        saveWorld(currentWorldName);
    }

    void updateDeathScreen(boolean left) {
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        int mx = (int)mxA[0], my = (int)myA[0];
        if (left && !deathMouseWasDown) {
            int bw = 300, bh = 46;
            int bx = width / 2 - bw / 2;
            int by = height / 2 + 62;
            if (inside(mx, my, bx, by, bw, bh)) {
                respawnAfterDeath();
            } else if (inside(mx, my, bx, by + 58, bw, bh)) {
                health = maxHealth;
                hunger = 20;
                deathScreen = false;
                deathDropsDone = false;
                spawnPlayer();
                velY = 0;
                saveWorld(currentWorldName);
                inMainMenu = true;
                menuScreen = 0;
                refreshWorldList();
                menuMessage = language.equals("en") ? "World saved after death" : "Swiat zapisany po smierci";
            }
        }
    }

    void moveHorizontal(double dx, double dz, boolean sneaking) {
        // Shift in Minecraft prevents the player from walking off a ledge.
        if (playerFree(x + dx, y, z) && (!sneaking || hasGroundBelow(x + dx, z))) x += dx;
        if (playerFree(x, y, z + dz) && (!sneaking || hasGroundBelow(x, z + dz))) z += dz;
    }

    boolean hasGroundBelow(double px, double pz) {
        int by = (int)Math.floor(y - 0.08);
        int minX = (int)Math.floor(px - PLAYER_RADIUS + 0.04);
        int maxX = (int)Math.floor(px + PLAYER_RADIUS - 0.04);
        int minZ = (int)Math.floor(pz - PLAYER_RADIUS + 0.04);
        int maxZ = (int)Math.floor(pz + PLAYER_RADIUS - 0.04);
        for (int bx = minX; bx <= maxX; bx++) for (int bz = minZ; bz <= maxZ; bz++) {
            if (solid(bx, by, bz)) return true;
        }
        return false;
    }

    void moveVertical(double dy) {
        onGround = false;
        if (playerFree(x, y + dy, z)) y += dy;
        else {
            if (dy < 0) {
                onGround = true;
                if (velY < -13.0 && !playerTouchingWater()) damage((int)Math.ceil((-velY - 12.0) * 1.15));
            }
            velY = 0;
        }
    }

    double playerHeight() { return sneaking ? SNEAK_HEIGHT : PLAYER_HEIGHT; }
    double eyeHeight() { return sneaking ? SNEAK_EYE_HEIGHT : EYE_HEIGHT; }

    boolean playerTouchingWater() {
        int minX = (int)Math.floor(x - PLAYER_RADIUS);
        int maxX = (int)Math.floor(x + PLAYER_RADIUS);
        int minY = (int)Math.floor(y + 0.02);
        int maxY = (int)Math.floor(y + playerHeight() - 0.02);
        int minZ = (int)Math.floor(z - PLAYER_RADIUS);
        int maxZ = (int)Math.floor(z + PLAYER_RADIUS);
        for (int bx = minX; bx <= maxX; bx++) for (int by = minY; by <= maxY; by++) for (int bz = minZ; bz <= maxZ; bz++) {
            if (inWorld(bx, by, bz) && (world[bx][by][bz] & 0xff) == WATER) return true;
        }
        return false;
    }

    boolean playerFree(double nx, double ny, double nz) {
        return playerFreeAtHeight(nx, ny, nz, playerHeight());
    }

    boolean playerFreeAtHeight(double nx, double ny, double nz, double height) {
        int minX = (int) Math.floor(nx - PLAYER_RADIUS);
        int maxX = (int) Math.floor(nx + PLAYER_RADIUS);
        int minY = (int) Math.floor(ny + 0.02);
        int maxY = (int) Math.floor(ny + height - 0.02);
        int minZ = (int) Math.floor(nz - PLAYER_RADIUS);
        int maxZ = (int) Math.floor(nz + PLAYER_RADIUS);
        double px0 = nx - PLAYER_RADIUS, px1 = nx + PLAYER_RADIUS;
        double pz0 = nz - PLAYER_RADIUS, pz1 = nz + PLAYER_RADIUS;
        for (int bx = minX; bx <= maxX; bx++) for (int by = minY; by <= maxY; by++) for (int bz = minZ; bz <= maxZ; bz++) {
            if (solid(bx, by, bz)) return false;
            if (inWorld(bx, by, bz)) {
                int id = world[bx][by][bz] & 0xff;
                if (id == DOOR_BOTTOM || id == DOOR_TOP) {
                    int meta = getDoorMeta(bx, by, bz);
                    double[] a = doorAabb(meta);
                    double dx0 = bx + a[0], dz0 = bz + a[1];
                    double dx1 = bx + a[2], dz1 = bz + a[3];
                    if (px1 > dx0 && px0 < dx1 && pz1 > dz0 && pz0 < dz1) return false;
                }
            }
        }
        return true;
    }

    boolean blockIntersectsPlayer(int bx, int by, int bz) {
        return bx + 1 > x - PLAYER_RADIUS && bx < x + PLAYER_RADIUS
                && bz + 1 > z - PLAYER_RADIUS && bz < z + PLAYER_RADIUS
                && by + 1 > y && by < y + playerHeight();
    }

    void render() {
        double dayTime = gameTime / 1200.0;
        double dayFraction = dayTime % 1.0;
        float rainStrength = weather.getRainStrength();
        float thunderStrength = weather.getThunderStrength();
        currentDayMult = craft3dgl.world.LightEngine.skyDayMultiplier(
                dayFraction, rainStrength, thunderStrength);
        float[] fogC = craft3dgl.world.SkyRenderer.getFogColor(
                dayFraction, rainStrength, thunderStrength);
        boolean camUnderwater = isWaterAt(x, y + eyeHeight(), z);
        if (camUnderwater) {
            // EntityRenderer.updateFogColor/setupFog for Material.WATER.
            fogC = new float[]{0.02f, 0.02f, 0.20f, 1.0f};
            glFogi(GL_FOG_MODE, GL_EXP);
            glFogf(GL_FOG_DENSITY, 0.10f);
        } else {
            // Vanilla 1.12 uses linear terrain fog, not the custom EXP2 fog.
            glFogi(GL_FOG_MODE, GL_LINEAR);
            glFogf(GL_FOG_START, 64.0f);
            glFogf(GL_FOG_END, 80.0f);
        }
        java.nio.FloatBuffer fbC = org.lwjgl.BufferUtils.createFloatBuffer(4).put(fogC); fbC.flip();
        glFogfv(GL_FOG_COLOR, fbC);
        glClearColor(fogC[0], fogC[1], fogC[2], 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        setupProjection();
        craft3dgl.world.SkyRenderer.drawSky(
                yaw, pitch, dayTime, rainStrength, thunderStrength);
        setupCamera();
        drawClouds(dayFraction, rainStrength, thunderStrength);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);

        // Minecraft 1.12 render order: opaque/cutout terrain, entities, translucent
        // terrain, then particles. Rendering particles before terrain erased their colour.
        if (gameRenderer.getLightmapTexture() != null) {
            gameRenderer.getLightmapTexture().update(currentDayMult, hasNightVision() ? 0.90f : 0f);
        }
        if (gameRenderer.getWaterTexture() != null) gameRenderer.getWaterTexture().update();
        if (USE_MODERN_RENDERER) drawModernChunks(false);
        else drawChunks(false);
        // Doors are CUTOUT geometry and belong before entities/translucency.
        drawDoors();

        // Tile entities are rendered after the opaque/cutout block layer in 1.12.
        drawChests();
        drawDroppedItems3D();
        drawAnimals();
        drawXpOrbs();
        drawVillagers();
        if (cameraMode != 0) {
            applyEntityLight(x, y + 0.9, z);
            drawPlayerModel();
            craft3dgl.ui.CuboidHelper.clearTint();
            glColor4f(1,1,1,1);
        }
        if (!USE_MODERN_RENDERER) drawChunks(true);
        if (USE_MODERN_RENDERER) drawModernChunks(true);
        craft3dgl.world.WeatherRenderer.draw(world, worldSeed, x, y + eyeHeight(), z,
                gameTime, rainStrength);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
        drawParticles();

        Hit hit = castRay(blockReach());
        if (hit.hit) drawFaceOutline(hit);
        drawUI();
    }

    void setupProjection() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        double near = 0.05;
        double far = 180.0;
        double fov = Math.toRadians(70); // vanilla 1.12 normal-FOV default
        double top = near * Math.tan(fov / 2.0);
        double right = top * ((double) width / height);
        glFrustum(-right, right, -top, top, near, far);
        glMatrixMode(GL_MODELVIEW);
    }

    /** MC Mth.wrapDegrees - normalize kat do [-180, 180]. */
    static double wrapDeg(double deg) {
        deg = deg % 360.0;
        if (deg >= 180.0) deg -= 360.0;
        if (deg < -180.0) deg += 360.0;
        return deg;
    }

    void setupCamera() {
        glLoadIdentity();
        // Screen shake - lekki jitter na yaw/pitch
        double sy = 0, sp = 0;
        if (screenShake > 0.01) {
            sy = (Math.random() - 0.5) * screenShake * 0.06;
            sp = (Math.random() - 0.5) * screenShake * 0.04;
        }
        double useYaw = yaw + sy;
        double usePitch = pitch + sp;
        double cp = Math.cos(usePitch);
        double fx = Math.sin(useYaw) * cp;
        double fy = Math.sin(usePitch);
        double fz = Math.cos(useYaw) * cp;
        double eyeX = x;
        double eyeY = y + eyeHeight();
        double eyeZ = z;
        if (cameraMode == 0) {
            lookAt(eyeX, eyeY, eyeZ, eyeX + fx, eyeY + fy, eyeZ + fz, 0, 1, 0);
        } else if (cameraMode == 1) {
            double maxDist = 4.5;
            double[] cam = safeThirdPersonOffset(-fx, -fy, -fz, maxDist);
            lookAt(eyeX + cam[0], eyeY + cam[1] + 0.45, eyeZ + cam[2], eyeX, y + 1.15, eyeZ, 0, 1, 0);
        } else {
            double maxDist = 4.5;
            double[] cam = safeThirdPersonOffset(fx, fy, fz, maxDist);
            lookAt(eyeX + cam[0], eyeY + cam[1] + 0.35, eyeZ + cam[2], eyeX, y + 1.15, eyeZ, 0, 1, 0);
        }
    }

    double[] safeThirdPersonOffset(double dx, double dy, double dz, double maxDist) {
        double ox = x, oy = y + eyeHeight(), oz = z;
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (len < 1e-9) return new double[]{0, 0, 0};
        double nx = dx/len, ny = dy/len, nz = dz/len;
        double padding = 0.25;
        double best = maxDist;
        double[][] offsets = {
            {0, 0, 0},
            {0.18, 0, 0}, {-0.18, 0, 0},
            {0, 0.18, 0}, {0, -0.18, 0},
            {0, 0, 0.18}, {0, 0, -0.18},
            {0.13, 0.13, 0}, {-0.13, -0.13, 0}
        };
        for (double[] off : offsets) {
            double t = rayHitDist(ox + off[0], oy + off[1], oz + off[2], nx, ny, nz, maxDist);
            if (t < best) best = t;
        }
        double dist = Math.max(0.6, best - padding);
        return new double[]{nx * dist, ny * dist, nz * dist};
    }

    double rayHitDist(double ox, double oy, double oz, double dx, double dy, double dz, double maxDist) {
        return craft3dgl.physics.RayCaster.castDist(ox, oy, oz, dx, dy, dz, maxDist, rayWorld, LEAVES);
    }

    void lookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
        double fx = centerX - eyeX, fy = centerY - eyeY, fz = centerZ - eyeZ;
        double fl = Math.sqrt(fx * fx + fy * fy + fz * fz); fx /= fl; fy /= fl; fz /= fl;
        double sx = fy * upZ - fz * upY, sy = fz * upX - fx * upZ, sz = fx * upY - fy * upX;
        double sl = Math.sqrt(sx * sx + sy * sy + sz * sz); sx /= sl; sy /= sl; sz /= sl;
        double ux = sy * fz - sz * fy, uy = sz * fx - sx * fz, uz = sx * fy - sy * fx;
        FloatBuffer m = BufferUtils.createFloatBuffer(16);
        m.put((float) sx).put((float) ux).put((float) -fx).put(0);
        m.put((float) sy).put((float) uy).put((float) -fy).put(0);
        m.put((float) sz).put((float) uz).put((float) -fz).put(0);
        m.put(0).put(0).put(0).put(1).flip();
        glMultMatrixf(m);
        glTranslated(-eyeX, -eyeY, -eyeZ);
    }

    void drawChunks(boolean leaves) {
        int pcx = clampInt((int) x / CHUNK, 0, CHUNKS_X - 1);
        int pcz = clampInt((int) z / CHUNK, 0, CHUNKS_Z - 1);
        int range = 5;
        // A VBO pass must not inherit sky/entity/UI state from the prior frame.
        // In particular, cutout grass uses double-sided crossed quads: leaked
        // culling made its back side disappear when the camera looked upward.
        org.lwjgl.opengl.GL20.glUseProgram(0);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDepthMask(true);
        glDisable(GL_CULL_FACE);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);
        glDisable(GL_BLEND);
        glColor4f(1f, 1f, 1f, 1f);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        enableFixedFunctionLightmap();
        if (leaves) {
            int waterTexture = gameRenderer.getWaterTexId();
            if (waterTexture > 0) glBindTexture(GL_TEXTURE_2D, waterTexture);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDepthMask(false);
        }
        // Wlacz smooth shading dla AO na krawedziach blokow (a/b/c/d w face())
        glShadeModel(GL_SMOOTH);
        if (leaves) {
            // Render translucent water chunks back-to-front as RenderGlobal's
            // TRANSLUCENT pass does; the previous fixed iteration caused blend
            // order seams whenever several water surfaces overlapped.
            java.util.ArrayList<int[]> order = new java.util.ArrayList<>();
            for (int cx = Math.max(0, pcx - range); cx <= Math.min(CHUNKS_X - 1, pcx + range); cx++) {
                for (int cy = 0; cy < CHUNKS_Y; cy++) {
                    for (int cz = Math.max(0, pcz - range); cz <= Math.min(CHUNKS_Z - 1, pcz + range); cz++) {
                        if (generatedColumns[cx][cz] && litColumns[cx][cz]
                                && chunks[cx][cy][cz].modernVboTranslucent != null) order.add(new int[]{cx, cy, cz});
                    }
                }
            }
            order.sort((a, b) -> Double.compare(chunkDistanceSq(b), chunkDistanceSq(a)));
            for (int[] pos : order) {
                drawFixedChunkVbo(chunks[pos[0]][pos[1]][pos[2]].modernVboTranslucent);
            }
        } else {
            for (int cx = Math.max(0, pcx - range); cx <= Math.min(CHUNKS_X - 1, pcx + range); cx++) {
                for (int cy = 0; cy < CHUNKS_Y; cy++) {
                    for (int cz = Math.max(0, pcz - range); cz <= Math.min(CHUNKS_Z - 1, pcz + range); cz++) {
                        if (!generatedColumns[cx][cz] || !litColumns[cx][cz]) continue;
                        Chunk ch = chunks[cx][cy][cz];
                        drawFixedChunkVbo(ch.modernVboSolid);
                        drawFixedChunkVbo(ch.modernVboCutout);
                    }
                }
            }
        }
        craft3dgl.blaze3d.vertex.VertexBuffer.unbind();
        glShadeModel(GL_FLAT);
        if (leaves) { glDepthMask(true); glDisable(GL_BLEND); }
        disableFixedFunctionLightmap();
    }

    void drawFixedChunkVbo(craft3dgl.blaze3d.vertex.VertexBuffer vbo) {
        if (vbo == null || vbo.getVertexCount() <= 0) return;
        craft3dgl.blaze3d.vertex.VertexFormat fmt = craft3dgl.blaze3d.vertex.DefaultVertexFormat.BLOCK;
        vbo.bind();
        fmt.setupBufferState(0L);
        vbo.draw(GL_QUADS);
        fmt.clearBufferState();
    }

    double chunkDistanceSq(int[] pos) {
        double dx = (pos[0] + 0.5) * CHUNK - x;
        double dy = (pos[1] + 0.5) * CHUNK - (y + eyeHeight());
        double dz = (pos[2] + 0.5) * CHUNK - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /** EntityRenderer.enableLightmap/disableLightmap from MCP 9.40. */
    void enableFixedFunctionLightmap() {
        int lightmap = gameRenderer.getLightmapTexId();
        if (lightmap <= 0) return;
        org.lwjgl.opengl.GL20.glUseProgram(0);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, lightmap);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glMatrixMode(GL_TEXTURE);
        glPushMatrix();
        glLoadIdentity();
        // OpenGlHelper light coordinates are in 0..255 units.
        glScalef(1f / 256f, 1f / 256f, 1f);
        glMatrixMode(GL_MODELVIEW);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
    }

    void disableFixedFunctionLightmap() {
        if (gameRenderer.getLightmapTexId() <= 0) return;
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE1);
        glMatrixMode(GL_TEXTURE);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glDisable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
    }

    void setFixedFunctionLightmapCoords(int blockX, int blockY, int blockZ) {
        int sky = 15;
        int block = 0;
        if (lightEngine != null && inWorld(blockX, blockY, blockZ)) {
            sky = lightEngine.getSky(blockX, blockY, blockZ);
            block = lightEngine.getBlockLight(blockX, blockY, blockZ);
        }
        float lightX = (block + 0.5f) * 16f;
        float lightY = (sky + 0.5f) * 16f;
        org.lwjgl.opengl.GL13.glMultiTexCoord2f(org.lwjgl.opengl.GL13.GL_TEXTURE1, lightX, lightY);
    }

    void drawClouds(double dayFraction, float rainStrength, float thunderStrength) {
        CloudRenderer.drawClouds(x, z, dayFraction, rainStrength, thunderStrength);
    }

    void drawDroppedItems3D() {
        craft3dgl.entities.DroppedItemRenderer.drawAll(drops, textureAtlas, this::face,
                craft3dgl.ui.ToolTextures::getTexId);
    }

    void drawPlayerModel() {
        int held = selectedItemId();
        int heldCount = selectedItemCount();
        if (craft3dgl.entities.SteveRenderer.isLoaded()) {
            // Nowy renderer z tekstura Steve'a
            float swingProgress = swingTimer > 0 ? (float)(1.0 - swingTimer) : 0f;
            // Przekaz bodyYaw i pelen yaw jako headYaw (SteveRenderer sam liczy netHeadYaw)
            float ageInTicks = (float)(System.nanoTime() / 50_000_000.0);
            // attackTime: nasz swingTimer 1=start, 0=koniec. MC uzywa 0=start, 1=koniec.
            // Konwersja: mcAttack = 1 - swingTimer
            float attackTime = swingTimer > 0 ? (float)(1.0 - swingTimer) : 0f;
            craft3dgl.entities.SteveRenderer.drawPlayer(x, y, z, bodyYaw, yaw, pitch,
                this.limbSwing, this.limbSwingAmount, ageInTicks, attackTime, sneaking);
            if (held > 0 && heldCount > 0) {
                drawPlayerHeldItem3D(held, ageInTicks, attackTime);
            }
        } else {
            // Fallback do starego
            craft3dgl.entities.PlayerRenderer.drawPlayerModel(x, y, z, yaw, pitch, walkPhase, swingTimer, held, heldCount, textureAtlas, this::face);
        }
    }

    /** Item w rece gracza 3rd person - dlon prawej reki Steve'a. */
    void drawPlayerHeldItem3D(int held, float ageInTicks, float attackTime) {
        glPushMatrix();
        glTranslated(x, y, z);
        glRotated(Math.toDegrees(bodyYaw), 0, 1, 0);
        // Ten sam pivot i animacja co prawe ramie modelu, rowniez przy skradaniu.
        craft3dgl.entities.SteveRenderer.applyThirdPersonRightHandTransform(
                limbSwing, limbSwingAmount, ageInTicks, attackTime, sneaking);
        if (isBlockItem(held)) {
            glEnable(GL_TEXTURE_2D);
            glColor4f(1, 1, 1, 1);
            glScaled(0.30, 0.30, 0.30);
            glTranslated(-0.5, -0.5, -0.5);
            if (held == CHEST && craft3dgl.world.ChestRenderer.drawItemModel()) {
                // Same tile-entity model/texture as the placed chest.
            } else {
                glBindTexture(GL_TEXTURE_2D, textureAtlas);
                glBegin(GL_QUADS);
                for (int dir = 0; dir < 6; dir++) face(0, 0, 0, held, dir);
                glEnd();
            }
            glDisable(GL_TEXTURE_2D);
        } else if (toolCategory(held) > 0) {
            glRotated(-30, 1, 0, 0);
            glRotated(45, 0, 0, 1);
            glScaled(0.55, 0.55, 0.55);
            if (!drawToolSpriteInHand(held)) {
                glDisable(GL_TEXTURE_2D);
                drawToolModel3D(held);
            }
        } else if (held == ITEM_STICK) {
            glDisable(GL_TEXTURE_2D);
            glRotated(-30, 1, 0, 0);
            glRotated(45, 0, 0, 1);
            color(0.55f, 0.32f, 0.15f);
            drawCuboid(-0.018, -0.05, -0.025, 0.018, 0.30, 0.025);
        }
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
        glPopMatrix();
    }

    void color(float r, float g, float b) { glColor3f(r, g, b); }

    void drawCuboid(double x0, double y0, double z0, double x1, double y1, double z1) {
        glBegin(GL_QUADS);
        glVertex3d(x0,y0,z1); glVertex3d(x1,y0,z1); glVertex3d(x1,y1,z1); glVertex3d(x0,y1,z1);
        glVertex3d(x1,y0,z0); glVertex3d(x0,y0,z0); glVertex3d(x0,y1,z0); glVertex3d(x1,y1,z0);
        glVertex3d(x1,y0,z1); glVertex3d(x1,y0,z0); glVertex3d(x1,y1,z0); glVertex3d(x1,y1,z1);
        glVertex3d(x0,y0,z0); glVertex3d(x0,y0,z1); glVertex3d(x0,y1,z1); glVertex3d(x0,y1,z0);
        glVertex3d(x0,y1,z1); glVertex3d(x1,y1,z1); glVertex3d(x1,y1,z0); glVertex3d(x0,y1,z0);
        glVertex3d(x0,y0,z0); glVertex3d(x1,y0,z0); glVertex3d(x1,y0,z1); glVertex3d(x0,y0,z1);
        glEnd();
    }

    void drawFaceOutline(Hit h) { craft3dgl.world.BlockBreakRenderer.draw(h, miningHit, miningProgress, doorSystem); }



    void drawUI() {
        // GuiIngame/GuiContainer in 1.12 are fixed-function passes. Isolate them
        // from terrain shaders, lightmap texture unit, entity tint and blend state.
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        org.lwjgl.opengl.GL20.glUseProgram(0);
        craft3dgl.blaze3d.renderer.RenderSystem.setShader(null);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_FOG);
        glDisable(GL_LIGHTING);
        glDepthMask(true);
        glColor4f(1f, 1f, 1f, 1f);
        glMatrixMode(GL_TEXTURE);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        if (deathScreen) {
            drawDeathScreen();
        } else if (paused) {
            drawPauseOverlay();
        } else {
            // Czerwony blysk gdy jest damage
            craft3dgl.ui.DamageOverlay.draw(width, height, damageFlash);
            if (!inventoryOpen && !creativeInvOpen && !chestOpen) drawCrosshair();
            if (!inventoryOpen && !creativeInvOpen && !chestOpen && cameraMode == 0) drawHandOverlay();
            if (gameMode == GAMEMODE_SURVIVAL) drawSurvivalBars();
            drawHotbar();
            drawDamageNumbers();
            String hud = "Mode: " + (gameMode == GAMEMODE_CREATIVE ? "Creative" : "Survival") + (flying ? "  (Flying)" : "");
            drawText(hud, 10, 10, 0.55f);
            drawText("FPS: " + fps, 10, 32, 0.50f);
            if (inventoryOpen) drawInventoryUI();
            if (creativeInvOpen) drawCreativeInvUI();
            if (chestOpen) drawChestUI();
            if (villagerTradeOpen) drawVillagerTradeUI();
            drawChatUI();
        }
        glMatrixMode(GL_TEXTURE);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glColor4f(1,1,1,1);
        glPopAttrib();
    }

    /**
     * Etap 6b: Test modern renderer pipeline.
     * Rysuje kolorowy pasek na dole ekranu przez shader position_color + BufferBuilder.
     * Pokazuje ze RenderType/RenderStates/GameRenderer/RenderSystem dziala end-to-end.
     */
    void drawModernRendererTest() {
        try {
            craft3dgl.blaze3d.shaders.EffectInstance shader = gameRenderer.positionColorShader();
            if (shader == null) return;

            // GUARD: zapisz caly stan OpenGL zeby nie zepsuc reszty UI/game
            org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glPushClientAttrib(org.lwjgl.opengl.GL11.GL_CLIENT_ALL_ATTRIB_BITS);

            // Setup RenderType (GUI_NO_TEX - position_color, alpha blend, no depth)
            craft3dgl.blaze3d.renderer.RenderType type = craft3dgl.blaze3d.renderer.RenderTypes.GUI_NO_TEX;
            type.setupRenderState();

            // Ustaw i aplikuj shader
            craft3dgl.blaze3d.renderer.RenderSystem.setShader(shader);
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            craft3dgl.blaze3d.renderer.RenderSystem.applyShader();

            // Rysuj gradient bar u dolu ekranu przez BufferBuilder
            craft3dgl.blaze3d.vertex.Tesselator tess = craft3dgl.blaze3d.vertex.Tesselator.getInstance();
            craft3dgl.blaze3d.vertex.BufferBuilder bb = tess.getBuilder();
            int y0 = height - 30;
            int y1 = height - 10;
            int x0 = width / 2 - 200;
            int x1 = width / 2 + 200;
            bb.begin(org.lwjgl.opengl.GL11.GL_QUADS, craft3dgl.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR);
            // Gradient: fioletowy -> cyjan
            bb.vertex(x0, y1, 0).color(0.6f, 0.2f, 0.9f, 0.85f).endVertex();
            bb.vertex(x1, y1, 0).color(0.2f, 0.8f, 1.0f, 0.85f).endVertex();
            bb.vertex(x1, y0, 0).color(0.2f, 0.8f, 1.0f, 0.85f).endVertex();
            bb.vertex(x0, y0, 0).color(0.6f, 0.2f, 0.9f, 0.85f).endVertex();
            tess.end();

            // KRYTYCZNE: wyjdz z shader mode zanim wroci fixed-function
            // (podwojne dla bezpieczenstwa - NVIDIA cache program id)
            org.lwjgl.opengl.GL20.glUseProgram(0);
            org.lwjgl.opengl.GL20.glUseProgram(0);
            craft3dgl.blaze3d.renderer.RenderSystem.setShader(null);
            type.clearRenderState();

            // Przywroc caly stan OpenGL sprzed testu
            org.lwjgl.opengl.GL11.glPopClientAttrib();
            org.lwjgl.opengl.GL11.glPopAttrib();

            // Napis nad paskiem (rysuje starym fontrenderer - fixed-function, po pop attrib)
            glColor4f(1f, 1f, 1f, 1f);
            drawText("MODERN SHADER PIPELINE ACTIVE (F7)", width / 2 - 180, height - 50, 0.55f);

            // ===== TEST 2: TEKSTUROWANY QUAD (position_tex_color shader) =====
            drawModernTexturedQuadTest();
        } catch (Throwable t) {
            // awaryjny cleanup gdyby cos wybuchlo
            try { org.lwjgl.opengl.GL20.glUseProgram(0); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopClientAttrib(); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopAttrib(); } catch (Throwable ignored) {}
            if (!modernRendererTestErrLogged) {
                System.err.println("[ModernTest] error: " + t);
                t.printStackTrace();
                modernRendererTestErrLogged = true;
            }
        }
    }
    private boolean modernRendererTestErrLogged = false;
    private boolean modernTexTestErrLogged = false;

    /**
     * Etap 6c: teksturowany quad rysowany przez position_tex_color shader.
     * Renderuje glowe Steve'a jako logo w lewym gornym rogu.
     * Testuje: Sampler0 uniform + UV pipeline + vertex color modulation.
     */
    void drawModernTexturedQuadTest() {
        try {
            craft3dgl.blaze3d.shaders.EffectInstance shader = gameRenderer.positionTexColorShader();
            if (shader == null) return;
            int texId = craft3dgl.entities.SteveRenderer.textureId();
            if (texId <= 0) return;

            // GUARD stanu OpenGL
            org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glPushClientAttrib(org.lwjgl.opengl.GL11.GL_CLIENT_ALL_ATTRIB_BITS);

            // RenderType GUI - position_tex_color, alpha blend, no depth, tex 2d
            craft3dgl.blaze3d.renderer.RenderType type = craft3dgl.blaze3d.renderer.RenderTypes.GUI;
            type.setupRenderState();

            // Bind steve texture unit 0
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, texId);

            // Setup shader + uniformy (Sampler0 -> unit 0, ColorModulator -> lekki tint)
            craft3dgl.blaze3d.renderer.RenderSystem.setShader(shader);
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            craft3dgl.blaze3d.renderer.RenderSystem.applyShader();
            // Sampler0 -> 0 (unit 0)
            shader.safeGetUniform("Sampler0").set(0.0f);

            // Rysuj quad 128x128 w lewym gornym rogu z UV glowy Steve'a
            // Steve texture 64x64, glowa front = (8,8)-(16,16), UV znormalizowany
            float u0 = 8f  / 64f;
            float v0 = 8f  / 64f;
            float u1 = 16f / 64f;
            float v1 = 16f / 64f;
            int qx = 20, qy = 60, qs = 128;

            craft3dgl.blaze3d.vertex.Tesselator tess = craft3dgl.blaze3d.vertex.Tesselator.getInstance();
            craft3dgl.blaze3d.vertex.BufferBuilder bb = tess.getBuilder();
            bb.begin(org.lwjgl.opengl.GL11.GL_QUADS, craft3dgl.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR);
            // Uwaga na kolejnosc atrybutow: POSITION, UV0, COLOR
            bb.vertex(qx,      qy + qs, 0).uv(u0, v1).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(qx + qs, qy + qs, 0).uv(u1, v1).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(qx + qs, qy,      0).uv(u1, v0).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(qx,      qy,      0).uv(u0, v0).color(1f, 1f, 1f, 1f).endVertex();
            tess.end();

            // KRYTYCZNE cleanup
            org.lwjgl.opengl.GL20.glUseProgram(0);
            org.lwjgl.opengl.GL20.glUseProgram(0);
            craft3dgl.blaze3d.renderer.RenderSystem.setShader(null);
            type.clearRenderState();

            org.lwjgl.opengl.GL11.glPopClientAttrib();
            org.lwjgl.opengl.GL11.glPopAttrib();

            glColor4f(1f, 1f, 1f, 1f);
            drawText("<- Steve head via position_tex_color shader", qx + qs + 10, qy + qs / 2 - 6, 0.50f);
        } catch (Throwable t) {
            try { org.lwjgl.opengl.GL20.glUseProgram(0); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopClientAttrib(); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopAttrib(); } catch (Throwable ignored) {}
            if (!modernTexTestErrLogged) {
                System.err.println("[ModernTexTest] error: " + t);
                t.printStackTrace();
                modernTexTestErrLogged = true;
            }
        }
    }

    private boolean modernBlockTestErrLogged = false;
    private boolean modernBlockPosLogged = false;

    /**
     * Etap 7a: test rendertype_solid shader - kolorowy blok unoszacy sie w powietrzu.
     * Testuje: BLOCK vertex format (pos+color+uv0+uv1), fog per-pixel, sampler0 (atlas), sampler2 (lightmap).
     * Rysowany w kontekscie 3D (kamera + projekcja perspective) - stad wywolanie w render(), nie drawUI().
     */
    void drawModernBlockTest() {
        try {
            craft3dgl.blaze3d.shaders.EffectInstance shader = gameRenderer.rendertypeSolidShader();
            if (shader == null) return;

            // GUARD stan OpenGL
            org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glPushClientAttrib(org.lwjgl.opengl.GL11.GL_CLIENT_ALL_ATTRIB_BITS);

            craft3dgl.blaze3d.renderer.RenderType type = craft3dgl.blaze3d.renderer.RenderTypes.SOLID;
            type.setupRenderState();

            // MC-way: przekaz textury przez setSampler, EffectInstance.apply() zrobi resze
            // (glActiveTexture + glBindTexture + glUniform1i wg extractSamplerUnit)
            shader.setSampler("Sampler0", Integer.valueOf(textureAtlas));
            int lightmapId = gameRenderer.getWhiteLightmapTexId();
            shader.setSampler("Sampler1", Integer.valueOf(lightmapId));

            craft3dgl.blaze3d.renderer.RenderSystem.setShader(shader);
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            double dayFraction = (gameTime / 1200.0) % 1.0;
            float[] fogC = craft3dgl.world.SkyRenderer.getFogColor(dayFraction,
                    weather.getRainStrength(), weather.getThunderStrength());
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderFogColor(fogC[0], fogC[1], fogC[2], 1f);
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderFogStart(20f);
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderFogEnd(120f);
            craft3dgl.blaze3d.renderer.RenderSystem.setFogEnabled(true);
            craft3dgl.blaze3d.renderer.RenderSystem.applyShader();

            // FIXED world position - blok stoi na miejscu (nie leci za graczem)
            // Spawn = (WORLD_X/2, ?, WORLD_Z/2) = (256, ?, 256). Wysoko nad terenem ale w WORLD_Y=64
            double bx = WORLD_X / 2.0 + 2;   // 258
            double by = 45.0;                 // wysoko nad terenem (spawn Y ~=30-35)
            double bz = WORLD_Z / 2.0 + 2;   // 258
            // DEBUG: raz na 60 klatek pisz gdzie jest blok i gracz
            if ((System.currentTimeMillis() / 1000) % 5 == 0 && !modernBlockPosLogged) {
                System.out.println("[ModernBlockTest] block at (" + bx + "," + by + "," + bz
                    + ") player at (" + String.format("%.1f", x) + "," + String.format("%.1f", y) + "," + String.format("%.1f", z)
                    + ") dist=" + String.format("%.1f", Math.sqrt((bx-x)*(bx-x) + (by-y)*(by-y) + (bz-z)*(bz-z))));
                modernBlockPosLogged = true;
            }

            // UV planks.png z atlas - uzywam TextureAtlas mapping
            // Zamiast tego uzyjemy calego atlasu (u0=0,v0=0,u1=1,v1=1) - blok bedzie mial ca\ly atlas jako texture
            // (test - efekt psychodeliczny ale pokazuje ze sampler0 dziala)
            float u0 = 0f, v0 = 0f, u1 = 1f, v1 = 1f;
            // Lightmap uv (255,255) -> pelne swiatlo (bo lightmap texture 1x1 = white)
            int lu = 240, lv = 240;

            craft3dgl.blaze3d.vertex.Tesselator tess = craft3dgl.blaze3d.vertex.Tesselator.getInstance();
            craft3dgl.blaze3d.vertex.BufferBuilder bb = tess.getBuilder();
            bb.begin(org.lwjgl.opengl.GL11.GL_QUADS, craft3dgl.blaze3d.vertex.DefaultVertexFormat.BLOCK);

            // BLOCK format: POSITION (3f) + COLOR (4ub) + UV0 (2f) + UV1 (2s)
            // 6 scian po 4 wierzcholki, kazdy ma normal color 255,255,255
            // Sciana +Y (top) - jasniejsza
            addBlockVertex(bb, bx,   by+1, bz,   255, 255, 255, u0, v0, lu, lv);
            addBlockVertex(bb, bx,   by+1, bz+1, 255, 255, 255, u0, v1, lu, lv);
            addBlockVertex(bb, bx+1, by+1, bz+1, 255, 255, 255, u1, v1, lu, lv);
            addBlockVertex(bb, bx+1, by+1, bz,   255, 255, 255, u1, v0, lu, lv);
            // Sciana -Y (bottom) - ciemniejsza (fake AO)
            addBlockVertex(bb, bx,   by,   bz+1, 150, 150, 150, u0, v1, lu, lv);
            addBlockVertex(bb, bx,   by,   bz,   150, 150, 150, u0, v0, lu, lv);
            addBlockVertex(bb, bx+1, by,   bz,   150, 150, 150, u1, v0, lu, lv);
            addBlockVertex(bb, bx+1, by,   bz+1, 150, 150, 150, u1, v1, lu, lv);
            // +Z
            addBlockVertex(bb, bx,   by,   bz+1, 200, 200, 200, u0, v1, lu, lv);
            addBlockVertex(bb, bx+1, by,   bz+1, 200, 200, 200, u1, v1, lu, lv);
            addBlockVertex(bb, bx+1, by+1, bz+1, 200, 200, 200, u1, v0, lu, lv);
            addBlockVertex(bb, bx,   by+1, bz+1, 200, 200, 200, u0, v0, lu, lv);
            // -Z
            addBlockVertex(bb, bx+1, by,   bz,   200, 200, 200, u0, v1, lu, lv);
            addBlockVertex(bb, bx,   by,   bz,   200, 200, 200, u1, v1, lu, lv);
            addBlockVertex(bb, bx,   by+1, bz,   200, 200, 200, u1, v0, lu, lv);
            addBlockVertex(bb, bx+1, by+1, bz,   200, 200, 200, u0, v0, lu, lv);
            // +X
            addBlockVertex(bb, bx+1, by,   bz+1, 180, 180, 180, u0, v1, lu, lv);
            addBlockVertex(bb, bx+1, by,   bz,   180, 180, 180, u1, v1, lu, lv);
            addBlockVertex(bb, bx+1, by+1, bz,   180, 180, 180, u1, v0, lu, lv);
            addBlockVertex(bb, bx+1, by+1, bz+1, 180, 180, 180, u0, v0, lu, lv);
            // -X
            addBlockVertex(bb, bx,   by,   bz,   180, 180, 180, u0, v1, lu, lv);
            addBlockVertex(bb, bx,   by,   bz+1, 180, 180, 180, u1, v1, lu, lv);
            addBlockVertex(bb, bx,   by+1, bz+1, 180, 180, 180, u1, v0, lu, lv);
            addBlockVertex(bb, bx,   by+1, bz,   180, 180, 180, u0, v0, lu, lv);

            tess.end();

            // Cleanup
            org.lwjgl.opengl.GL20.glUseProgram(0);
            org.lwjgl.opengl.GL20.glUseProgram(0);
            craft3dgl.blaze3d.renderer.RenderSystem.setShader(null);
            // Wyczysc lightmap unit 2
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + 2);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0);
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            type.clearRenderState();

            org.lwjgl.opengl.GL11.glPopClientAttrib();
            org.lwjgl.opengl.GL11.glPopAttrib();
        } catch (Throwable t) {
            try { org.lwjgl.opengl.GL20.glUseProgram(0); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopClientAttrib(); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopAttrib(); } catch (Throwable ignored) {}
            if (!modernBlockTestErrLogged) {
                System.err.println("[ModernBlockTest] error: " + t);
                t.printStackTrace();
                modernBlockTestErrLogged = true;
            }
        }
    }

    /** Helper - jedna vertex w BLOCK format: pos(3f) + color(4ub) + uv0(2f) + uv1(2s). */
    private void addBlockVertex(craft3dgl.blaze3d.vertex.BufferBuilder bb,
                                double px, double py, double pz,
                                int r, int g, int b,
                                float u, float v,
                                int lu, int lv) {
        bb.vertex(px, py, pz).color(r, g, b, 255).uv(u, v).uv2(lu, lv).endVertex();
    }

    private boolean modernChunksErrLogged = false;
    private long modernChunksFrameCount = 0;

    /** Draws either the opaque/cutout terrain pass or the later translucent pass. */
    void drawModernChunks(boolean translucentOnly) {
        try {
            craft3dgl.blaze3d.shaders.EffectInstance solidSh = gameRenderer.rendertypeSolidShader();
            craft3dgl.blaze3d.shaders.EffectInstance cutoutSh = gameRenderer.rendertypeCutoutShader();
            craft3dgl.blaze3d.shaders.EffectInstance translucentSh = gameRenderer.rendertypeTranslucentShader();
            if (!translucentOnly && solidSh == null) return;
            if (translucentOnly && translucentSh == null) return;

            org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ALL_ATTRIB_BITS);
            org.lwjgl.opengl.GL11.glPushClientAttrib(org.lwjgl.opengl.GL11.GL_CLIENT_ALL_ATTRIB_BITS);

            int pcx = clampInt((int) x / CHUNK, 0, CHUNKS_X - 1);
            int pcz = clampInt((int) z / CHUNK, 0, CHUNKS_Z - 1);
            int range = 5;
            craft3dgl.blaze3d.vertex.Tesselator tess = craft3dgl.blaze3d.vertex.Tesselator.getInstance();
            craft3dgl.blaze3d.vertex.BufferBuilder bb = tess.getBuilder();
            craft3dgl.blaze3d.vertex.VertexFormat fmt = craft3dgl.blaze3d.vertex.DefaultVertexFormat.BLOCK;

            int rebuildBudget = 2;
            int rebuiltCount = 0;

            // Rebuild all three meshes once, before the opaque pass.
            if (!translucentOnly) {
                for (int cx = Math.max(0, pcx - range); cx <= Math.min(CHUNKS_X - 1, pcx + range) && rebuildBudget > 0; cx++) {
                    for (int cy = 0; cy < CHUNKS_Y && rebuildBudget > 0; cy++) {
                        for (int cz = Math.max(0, pcz - range); cz <= Math.min(CHUNKS_Z - 1, pcz + range) && rebuildBudget > 0; cz++) {
                            Chunk ch = chunks[cx][cy][cz];
                            if (!ch.modernDirty && ch.modernVboSolid != null) continue;
                            // SOLID
                            bb.begin(org.lwjgl.opengl.GL11.GL_QUADS, fmt);
                            int qs = addChunkToModernMesh(bb, cx, cy, cz, MODERN_LAYER_SOLID);
                            bb.end();
                            if (ch.modernVboSolid == null) ch.modernVboSolid = new craft3dgl.blaze3d.vertex.VertexBuffer(fmt);
                            ch.modernVboSolid.upload(bb.getBuffer());
                            ch.modernVertexCountSolid = qs * 4;
                            bb.clear();
                            // CUTOUT
                            bb.begin(org.lwjgl.opengl.GL11.GL_QUADS, fmt);
                            int qc = addChunkToModernMesh(bb, cx, cy, cz, MODERN_LAYER_CUTOUT);
                            bb.end();
                            if (ch.modernVboCutout == null) ch.modernVboCutout = new craft3dgl.blaze3d.vertex.VertexBuffer(fmt);
                            ch.modernVboCutout.upload(bb.getBuffer());
                            ch.modernVertexCountCutout = qc * 4;
                            bb.clear();
                            // TRANSLUCENT
                            bb.begin(org.lwjgl.opengl.GL11.GL_QUADS, fmt);
                            int qt = addChunkToModernMesh(bb, cx, cy, cz, MODERN_LAYER_TRANSLUCENT);
                            bb.end();
                            if (ch.modernVboTranslucent == null) ch.modernVboTranslucent = new craft3dgl.blaze3d.vertex.VertexBuffer(fmt);
                            ch.modernVboTranslucent.upload(bb.getBuffer());
                            ch.modernVertexCountTranslucent = qt * 4;
                            bb.clear();
                            ch.modernDirty = false;
                            rebuiltCount++;
                            rebuildBudget--;
                        }
                    }
                }
            }

            // Wspolny setup uniformow (fog) dla wszystkich passow
            double dayFraction = (gameTime / 1200.0) % 1.0;
            float[] fogC = craft3dgl.world.SkyRenderer.getFogColor(dayFraction,
                    weather.getRainStrength(), weather.getThunderStrength());
            // Underwater fog - ciemnoniebieski, KROTKI zasieg (widoczność ograniczona)
            boolean underwater = isWaterAt(x, y + eyeHeight(), z);
            if (underwater) {
                fogC = new float[]{0.05f, 0.15f, 0.35f, 1.0f};
            }
            int atlasTex = textureAtlas;
            int lightmapTex = gameRenderer.getLightmapTexId();

            int drawnSolid = 0, drawnCutout = 0, drawnTranslucent = 0;

            if (!translucentOnly) {
                // Vanilla order begins with opaque and cutout terrain.
                drawnSolid = drawModernChunkLayer(solidSh, craft3dgl.blaze3d.renderer.RenderTypes.SOLID,
                    pcx, pcz, range, fmt, atlasTex, lightmapTex, fogC, MODERN_LAYER_SOLID);
                if (cutoutSh != null) {
                    drawnCutout = drawModernChunkLayer(cutoutSh, craft3dgl.blaze3d.renderer.RenderTypes.CUTOUT,
                        pcx, pcz, range, fmt, atlasTex, lightmapTex, fogC, MODERN_LAYER_CUTOUT);
                }
            } else {
                // Water/translucent terrain is rendered after opaque entities, before particles.
                int waterTex = gameRenderer.getWaterTexId();
                if (waterTex > 0) gameRenderer.getWaterTexture().update();
                drawnTranslucent = drawModernChunkLayer(translucentSh, craft3dgl.blaze3d.renderer.RenderTypes.TRANSLUCENT,
                    pcx, pcz, range, fmt, waterTex > 0 ? waterTex : atlasTex, lightmapTex, fogC, MODERN_LAYER_TRANSLUCENT);
            }
            craft3dgl.blaze3d.vertex.VertexBuffer.unbind();

            if (!translucentOnly && (modernChunksFrameCount++ % 1800) == 0) {   // co ~30s
                System.out.println("[ModernChunks] rebuilt=" + rebuiltCount
                    + " solid=" + drawnSolid + " cutout=" + drawnCutout + " translucent=" + drawnTranslucent
                    + " frame=" + modernChunksFrameCount);
            }

            org.lwjgl.opengl.GL20.glUseProgram(0);
            craft3dgl.blaze3d.renderer.RenderSystem.setShader(null);

            org.lwjgl.opengl.GL11.glPopClientAttrib();
            org.lwjgl.opengl.GL11.glPopAttrib();
        } catch (Throwable t) {
            try { org.lwjgl.opengl.GL20.glUseProgram(0); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopClientAttrib(); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL11.glPopAttrib(); } catch (Throwable ignored) {}
            if (!modernChunksErrLogged) {
                System.err.println("[ModernChunks] error: " + t);
                t.printStackTrace();
                modernChunksErrLogged = true;
            }
        }
    }

    /** Rysuje wszystkie VBO danego layera z chunkow w range. Zwraca liczbe drawnietych VBO. */
    int drawModernChunkLayer(craft3dgl.blaze3d.shaders.EffectInstance shader,
                              craft3dgl.blaze3d.renderer.RenderType type,
                              int pcx, int pcz, int range,
                              craft3dgl.blaze3d.vertex.VertexFormat fmt,
                              int atlasTex, int lightmapTex, float[] fogC, int layer) {
        type.setupRenderState();
        shader.setSampler("Sampler0", Integer.valueOf(atlasTex));
        shader.setSampler("Sampler1", Integer.valueOf(lightmapTex));
        craft3dgl.blaze3d.renderer.RenderSystem.setShader(shader);
        // Day/night brightness comes exclusively from the MCP-style lightmap. Multiplying
        // ColorModulator by it again made the scene unnaturally dark and colour-graded.
        if (layer == MODERN_LAYER_TRANSLUCENT) {
            // Plains biome water colour (0x3F76E4); source texture supplies transparency.
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderColor(0.247f, 0.463f, 0.894f, 1.0f);
        } else {
            craft3dgl.blaze3d.renderer.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
        craft3dgl.blaze3d.renderer.RenderSystem.setShaderFogColor(fogC[0], fogC[1], fogC[2], 1f);
        boolean uw = isWaterAt(x, y + eyeHeight(), z);
        float fogEnd = range * CHUNK;
        craft3dgl.blaze3d.renderer.RenderSystem.setShaderFogStart(uw ? 2f : fogEnd * 0.75f);
        craft3dgl.blaze3d.renderer.RenderSystem.setShaderFogEnd(uw ? 20f : fogEnd);
        craft3dgl.blaze3d.renderer.RenderSystem.setFogEnabled(true);
        craft3dgl.blaze3d.renderer.RenderSystem.applyShader();

        // Apply final layer state after EffectInstance has applied its JSON defaults.
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        if (layer == MODERN_LAYER_TRANSLUCENT) {
            org.lwjgl.opengl.GL11.glDepthMask(false);
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            org.lwjgl.opengl.GL11.glDepthMask(true);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        }
        // Crossed plants are two-sided; opaque blocks and water use corrected CCW faces.
        if (layer == MODERN_LAYER_CUTOUT) org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        else org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        // Cutout alpha is discarded directly by rendertype_cutout.fsh.
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_ALPHA_TEST);

        int drawn = 0;
        for (int cx = Math.max(0, pcx - range); cx <= Math.min(CHUNKS_X - 1, pcx + range); cx++) {
            for (int cy = 0; cy < CHUNKS_Y; cy++) {
                for (int cz = Math.max(0, pcz - range); cz <= Math.min(CHUNKS_Z - 1, pcz + range); cz++) {
                    Chunk ch = chunks[cx][cy][cz];
                    craft3dgl.blaze3d.vertex.VertexBuffer vbo;
                    int vc;
                    if (layer == MODERN_LAYER_SOLID) { vbo = ch.modernVboSolid; vc = ch.modernVertexCountSolid; }
                    else if (layer == MODERN_LAYER_CUTOUT) { vbo = ch.modernVboCutout; vc = ch.modernVertexCountCutout; }
                    else { vbo = ch.modernVboTranslucent; vc = ch.modernVertexCountTranslucent; }
                    if (vbo == null || vc == 0) continue;
                    vbo.bind();
                    fmt.setupBufferState(0L);
                    vbo.draw(org.lwjgl.opengl.GL11.GL_QUADS);
                    fmt.clearBufferState();
                    drawn++;
                }
            }
        }
        type.clearRenderState();
        // Przywroc rozsadny defaultowy stan po layerze
        org.lwjgl.opengl.GL11.glDepthMask(true);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        return drawn;
    }

    /**
     * MC 1.14.4 Block.canOcclude() port 1:1.
     * Zwraca true TYLKO dla blokow z renderLayer=SOLID i hasCollision=true.
     * To decyduje czy sasiad moze zakryc nasza sciane. Liscie/woda/trawa NIE occlude.
     */
    boolean canOccludeModern(int id) {
        if (id == AIR) return false;
        // Wszystkie NIE-solid layers (CUTOUT + TRANSLUCENT + brak collision) -> NIE occlude
        if (id == LEAVES) return false;                        // CUTOUT_MIPPED
        if (id == TALL_GRASS) return false;                    // CUTOUT (cross)
        if (id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3) return false;  // CUTOUT
        if (id == WATER) return false;                         // TRANSLUCENT
        if (id == DOOR_BOTTOM || id == DOOR_TOP) return false; // CUTOUT (rysowane inaczej)
        if (id == CHEST) return false;                          // TileEntityChest model 14/16 wide
        // Wszystko inne (stone/dirt/grass/wood/planks/sand/farmland/crafting_table itd) = SOLID
        return true;
    }

    /**
     * MC 1.14.4 Block.shouldRenderFace() port.
     * Sprawdzamy czy sasiad w kierunku 'dir' zakrywa nasza sciane.
     * Jesli sasiad !canOcclude -> nasza sciana widoczna.
     * Dodatkowo: skipRendering dla tego samego layera (LEAVES obok LEAVES, WATER obok WATER).
     */
    boolean faceVisibleModern(int id, int nx, int ny, int nz) {
        if (!inWorld(nx, ny, nz)) return true;
        int other = world[nx][ny][nz] & 0xff;
        // MC: skipRendering - LEAVES obok LEAVES nie rysujemy internal faces
        if (id == LEAVES && other == LEAVES) return false;
        // MC: WATER obok WATER - internal culling (nie rysujemy powierzchni miedzy blokami wody)
        if (id == WATER && other == WATER) return false;
        // GLOWNA REGULA MC: sasiad musi occlude zeby zakryc nasza sciane
        return !canOccludeModern(other);
    }

    // Etap 7d: constants dla identyfikacji layera
    static final int MODERN_LAYER_SOLID = 0;
    static final int MODERN_LAYER_CUTOUT = 1;
    static final int MODERN_LAYER_TRANSLUCENT = 2;

    /**
     * Dodaje wszystkie widoczne sciany blokow chunka (cx,cy,cz) do BufferBuildera dla danego layera.
     *   SOLID       = kamien, ziemia, drewno, planks itd (opaque)
     *   CUTOUT      = liscie, tall_grass, wheat (alpha discard w shaderze)
     *   TRANSLUCENT = woda, szklo (alpha blend, sorting)
     * Zwraca liczbe dodanych quadow.
     */
    static int dbgDir0=0, dbgDir1=0, dbgDir2=0, dbgDir3=0, dbgDir4=0, dbgDir5=0;
    int addChunkToModernMesh(craft3dgl.blaze3d.vertex.BufferBuilder bb, int cx, int cy, int cz, int layer) {
        int minX = cx * CHUNK, minY = cy * CHUNK, minZ = cz * CHUNK;
        int maxX = Math.min(WORLD_X, minX + CHUNK);
        int maxY = Math.min(WORLD_Y, minY + CHUNK);
        int maxZ = Math.min(WORLD_Z, minZ + CHUNK);
        int quads = 0;
        for (int bx = minX; bx < maxX; bx++) {
            for (int by = minY; by < maxY; by++) {
                for (int bz = minZ; bz < maxZ; bz++) {
                    int id = world[bx][by][bz] & 0xff;
                    if (id == AIR) continue;
                    if (id == DOOR_BOTTOM || id == DOOR_TOP || id == CHEST) continue;
                    boolean isCross = (id == TALL_GRASS || id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3);
                    boolean isLeaves = (id == LEAVES);
                    boolean isWater = (id == WATER);
                    // Klasyfikacja per layer
                    if (layer == MODERN_LAYER_SOLID) {
                        if (isCross || isLeaves || isWater) continue;
                    } else if (layer == MODERN_LAYER_CUTOUT) {
                        if (!isCross && !isLeaves) continue;
                    } else if (layer == MODERN_LAYER_TRANSLUCENT) {
                        if (!isWater) continue;
                    }
                    // Cross meshe (tall_grass, wheat) - X-shape
                    if (isCross) {
                        quads += addCrossFacesModern(bb, bx, by, bz, id);
                        continue;
                    }
                    // Woda - specjalny mesh z lower top
                    if (isWater) {
                        quads += addWaterFacesModern(bb, bx, by, bz);
                        continue;
                    }
                    // Normalny blok - 6 scian z MC-style face culling (canOccludeModern)
                    if (faceVisibleModern(id, bx + 1, by, bz)) { addFaceModern(bb, bx, by, bz, id, 0); quads++; dbgDir0++; }
                    if (faceVisibleModern(id, bx - 1, by, bz)) { addFaceModern(bb, bx, by, bz, id, 1); quads++; dbgDir1++; }
                    if (faceVisibleModern(id, bx, by + 1, bz)) { addFaceModern(bb, bx, by, bz, id, 2); quads++; dbgDir2++; }
                    if (faceVisibleModern(id, bx, by - 1, bz)) { addFaceModern(bb, bx, by, bz, id, 3); quads++; dbgDir3++; }
                    if (faceVisibleModern(id, bx, by, bz + 1)) { addFaceModern(bb, bx, by, bz, id, 4); quads++; dbgDir4++; }
                    if (faceVisibleModern(id, bx, by, bz - 1)) { addFaceModern(bb, bx, by, bz, id, 5); quads++; dbgDir5++; }
                }
            }
        }
        return quads;
    }

    /** Cross mesh (X-shape) dla tall_grass, wheat - dwa quady krzyzujace sie. */
    int addCrossFacesModern(craft3dgl.blaze3d.vertex.BufferBuilder bb, int x, int y, int z, int id) {
        int tile = tileFor(id, 0);
        float u0 = (float) atlasU0(tile);
        float u1 = (float) atlasU1(tile);
        float v0 = (float) atlasV0();
        float v1 = (float) atlasV1();
        int skyLv = 15, blLv = 0;
        if (lightEngine != null && inWorld(x, y, z)) {
            skyLv = lightEngine.getSky(x, y, z);
            blLv = lightEngine.getBlockLight(x, y, z);
        }
        int lu = (int)((blLv + 0.5f) * 16f);
        int lv = (int)((skyLv + 0.5f) * 16f);
        // Diagonal 1: (x,z)->(x+1,z+1), front and back like block/cross.json.
        vtxModern(bb, u0,v1, x,y,z,       1.0f, lu, lv);
        vtxModern(bb, u1,v1, x+1,y,z+1,   1.0f, lu, lv);
        vtxModern(bb, u1,v0, x+1,y+1,z+1, 1.0f, lu, lv);
        vtxModern(bb, u0,v0, x,y+1,z,     1.0f, lu, lv);
        vtxModern(bb, u0,v1, x,y,z,       1.0f, lu, lv);
        vtxModern(bb, u0,v0, x,y+1,z,     1.0f, lu, lv);
        vtxModern(bb, u1,v0, x+1,y+1,z+1, 1.0f, lu, lv);
        vtxModern(bb, u1,v1, x+1,y,z+1,   1.0f, lu, lv);
        // Diagonal 2: (x+1,z)->(x,z+1), front and back.
        vtxModern(bb, u0,v1, x+1,y,z,     1.0f, lu, lv);
        vtxModern(bb, u1,v1, x,y,z+1,     1.0f, lu, lv);
        vtxModern(bb, u1,v0, x,y+1,z+1,   1.0f, lu, lv);
        vtxModern(bb, u0,v0, x+1,y+1,z,   1.0f, lu, lv);
        vtxModern(bb, u0,v1, x+1,y,z,     1.0f, lu, lv);
        vtxModern(bb, u0,v0, x+1,y+1,z,   1.0f, lu, lv);
        vtxModern(bb, u1,v0, x,y+1,z+1,   1.0f, lu, lv);
        vtxModern(bb, u1,v1, x,y,z+1,     1.0f, lu, lv);
        return 4;
    }

    /** Water mesh - blok wody, top nizej (0.88) jesli nie ma wody nad. */
    int addWaterFacesModern(craft3dgl.blaze3d.vertex.BufferBuilder bb, int x, int y, int z) {
        // Woda uzywa OSOBNEJ tekstury waterTexture (16x16 animowana) - pelen zakres UV 0..1
        // NIE atlas UV - shader/RenderSystem podmienia Sampler0 dla translucent na waterTex
        float u0 = 0.0f, u1 = 1.0f;
        float v0 = 0.0f, v1 = 1.0f;
        float top = y + (isWater(x, y + 1, z) ? 1.0f : 0.88f);
        int skyLv = 15, blLv = 0;
        if (lightEngine != null && inWorld(x, y, z)) {
            skyLv = lightEngine.getSky(x, y, z);
            blLv = lightEngine.getBlockLight(x, y, z);
        }
        int lu = (int)((blLv + 0.5f) * 16f);
        int lv = (int)((skyLv + 0.5f) * 16f);
        int quads = 0;
        // Top (jesli nie ma wody nad)
        if (!isWater(x, y + 1, z)) {
            vtxWater(bb, u0,v0, x,   top, z,   0.95f, lu, lv);
            vtxWater(bb, u0,v1, x,   top, z+1, 0.95f, lu, lv);
            vtxWater(bb, u1,v1, x+1, top, z+1, 0.95f, lu, lv);
            vtxWater(bb, u1,v0, x+1, top, z,   0.95f, lu, lv);
            quads++;
        }
        // +X
        if (isAirForWaterSide(x + 1, y, z)) {
            vtxWater(bb, u0,v1, x+1, y,   z+1, 0.68f, lu, lv);
            vtxWater(bb, u1,v1, x+1, y,   z,   0.68f, lu, lv);
            vtxWater(bb, u1,v0, x+1, top, z,   0.68f, lu, lv);
            vtxWater(bb, u0,v0, x+1, top, z+1, 0.68f, lu, lv);
            quads++;
        }
        // -X
        if (isAirForWaterSide(x - 1, y, z)) {
            vtxWater(bb, u0,v1, x, y,   z,   0.68f, lu, lv);
            vtxWater(bb, u1,v1, x, y,   z+1, 0.68f, lu, lv);
            vtxWater(bb, u1,v0, x, top, z+1, 0.68f, lu, lv);
            vtxWater(bb, u0,v0, x, top, z,   0.68f, lu, lv);
            quads++;
        }
        // +Z
        if (isAirForWaterSide(x, y, z + 1)) {
            vtxWater(bb, u0,v1, x,   y,   z+1, 0.68f, lu, lv);
            vtxWater(bb, u1,v1, x+1, y,   z+1, 0.68f, lu, lv);
            vtxWater(bb, u1,v0, x+1, top, z+1, 0.68f, lu, lv);
            vtxWater(bb, u0,v0, x,   top, z+1, 0.68f, lu, lv);
            quads++;
        }
        // -Z
        if (isAirForWaterSide(x, y, z - 1)) {
            vtxWater(bb, u0,v1, x+1, y,   z,   0.68f, lu, lv);
            vtxWater(bb, u1,v1, x,   y,   z,   0.68f, lu, lv);
            vtxWater(bb, u1,v0, x,   top, z,   0.68f, lu, lv);
            vtxWater(bb, u0,v0, x+1, top, z,   0.68f, lu, lv);
            quads++;
        }
        return quads;
    }

    /** Analog face() ale wysyla do BufferBuildera zamiast glVertex3d. */
    void addFaceModern(craft3dgl.blaze3d.vertex.BufferBuilder bb, int x, int y, int z, int id, int dir) {
        int tile = tileFor(id, dir);
        float u0 = (float) atlasU0(tile);
        float u1 = (float) atlasU1(tile);
        float v0 = (float) atlasV0();
        float v1 = (float) atlasV1();
        // BlockModelRenderer's vanilla face brightness: down=.5, up=1, Z=.8, X=.6.
        float dirShade = dir == 2 ? 1.0f : dir == 3 ? 0.5f : dir < 2 ? 0.6f : 0.8f;
        int nx = x, ny = y, nz = z;
        switch (dir) {
            case 0: nx = x + 1; break;
            case 1: nx = x - 1; break;
            case 2: ny = y + 1; break;
            case 3: ny = y - 1; break;
            case 4: nz = z + 1; break;
            case 5: nz = z - 1; break;
        }
        // No decorative per-corner gradient: vanilla smooth lighting derives AO from
        // neighbouring opaque blocks, never from a fixed pattern on every face.
        float light = dirShade;
        float a = 1.0f, b = 1.0f, c = 1.0f, d = 1.0f;
        // MC-style lightmap UV: X = block light (torch), Y = sky light
        // Poziomy 0..15 - shader podzieli przez 16
        int skyLv = 15, blLv = 0;
        if (lightEngine != null && inWorld(nx, ny, nz)) {
            skyLv = lightEngine.getSky(nx, ny, nz);
            blLv = lightEngine.getBlockLight(nx, ny, nz);
        }
        // W lightmap texture (16x16): U axis = blocklight, V axis = skylight
        // Nasze UV1 idzie jako SHORT (0..65535), shader podzieli przez 256 (juz w rendertype_solid.vsh)
        // Aby trafic w texel (blLv, skyLv) w 16x16 texturze:
        // UV normalized = (blLv+0.5)/16 dla U, (skyLv+0.5)/16 dla V
        // W SHORT: 256 * (blLv+0.5)/16 = 16*(blLv+0.5) = 8, 24, 40, 56...
        int lu = (int)((blLv + 0.5f) * 16f);   // 8, 24, 40... 248
        int lv = (int)((skyLv + 0.5f) * 16f);  // 8, 24, 40... 248
        switch (dir) {
            case 0:
                vtxModern(bb, u0,v1, x+1,y,z+1,   light*c, lu, lv);
                vtxModern(bb, u1,v1, x+1,y,z,     light*d, lu, lv);
                vtxModern(bb, u1,v0, x+1,y+1,z,   light*a, lu, lv);
                vtxModern(bb, u0,v0, x+1,y+1,z+1, light*b, lu, lv);
                break;
            case 1:
                vtxModern(bb, u0,v1, x,y,z,       light*c, lu, lv);
                vtxModern(bb, u1,v1, x,y,z+1,     light*d, lu, lv);
                vtxModern(bb, u1,v0, x,y+1,z+1,   light*a, lu, lv);
                vtxModern(bb, u0,v0, x,y+1,z,     light*b, lu, lv);
                break;
            case 2:
                vtxModern(bb, u0,v0, x,y+1,z,     light*b, lu, lv);
                vtxModern(bb, u0,v1, x,y+1,z+1,   light*a, lu, lv);
                vtxModern(bb, u1,v1, x+1,y+1,z+1, light*b, lu, lv);
                vtxModern(bb, u1,v0, x+1,y+1,z,   light*a, lu, lv);
                break;
            case 3:
                vtxModern(bb, u0,v1, x,y,z+1,     light*c, lu, lv);
                vtxModern(bb, u0,v0, x,y,z,       light*d, lu, lv);
                vtxModern(bb, u1,v0, x+1,y,z,     light*d, lu, lv);
                vtxModern(bb, u1,v1, x+1,y,z+1,   light*c, lu, lv);
                break;
            case 4:
                vtxModern(bb, u0,v1, x,y,z+1,     light*c, lu, lv);
                vtxModern(bb, u1,v1, x+1,y,z+1,   light*d, lu, lv);
                vtxModern(bb, u1,v0, x+1,y+1,z+1, light*a, lu, lv);
                vtxModern(bb, u0,v0, x,y+1,z+1,   light*b, lu, lv);
                break;
            case 5:
                vtxModern(bb, u0,v1, x+1,y,z,     light*c, lu, lv);
                vtxModern(bb, u1,v1, x,y,z,       light*d, lu, lv);
                vtxModern(bb, u1,v0, x,y+1,z,     light*a, lu, lv);
                vtxModern(bb, u0,v0, x+1,y+1,z,   light*b, lu, lv);
                break;
        }
    }

    /** Writes one fixed-function BLOCK vertex. BufferBuilder's SHORT UV path
     * stores its second argument first, hence uv2(sky, block) in memory becomes
     * the OpenGL (block, sky) lightmap coordinate pair. */
    private void vtxModern(craft3dgl.blaze3d.vertex.BufferBuilder bb,
                           float u, float v, double px, double py, double pz,
                           float shade, int lu, int lv) {
        int col = (int) Math.max(0, Math.min(255, shade * 255f));
        bb.vertex(px, py, pz).color(col, col, col, 255).uv(u, v).uv2(lv, lu).endVertex();
    }

    private void vtxWater(craft3dgl.blaze3d.vertex.BufferBuilder bb,
                          float u, float v, double px, double py, double pz,
                          float shade, int lu, int lv) {
        int red = (int)Math.max(0, Math.min(255, 0x3f * shade));
        int green = (int)Math.max(0, Math.min(255, 0x76 * shade));
        int blue = (int)Math.max(0, Math.min(255, 0xe4 * shade));
        bb.vertex(px, py, pz).color(red, green, blue, 255).uv(u, v).uv2(lv, lu).endVertex();
    }

    /** Rysuje unoszace popup damage numbers nad entities (w 2D nad HUD). */
    void drawDamageNumbers() {
        java.util.ArrayList<craft3dgl.ui.DamageNumbers.Popup> pops = craft3dgl.ui.DamageNumbers.getPopups();
        if (pops.isEmpty()) return;
        craft3dgl.ui.WorldToScreen proj = new craft3dgl.ui.WorldToScreen();
        double aspect = (double) width / (double) Math.max(1, height);
        double camX = x, camY = y + eyeHeight(), camZ = z;
        for (craft3dgl.ui.DamageNumbers.Popup pp : pops) {
            proj.project(pp.worldX, pp.worldY, pp.worldZ, camX, camY, camZ, yaw, pitch, 72.0, aspect, width, height);
            if (!proj.visible) continue;
            float t = (float) (pp.age / pp.maxAge);
            float alpha = 1f - t * t;
            if (alpha <= 0) continue;
            float dist = (float) Math.max(2.0, proj.depth);
            float scale = (float) (Math.min(1.1, 5.5 / dist) * (1.0 + Math.min(0.4, (0.2 - t) * 2)));
            if (scale < 0.35f) scale = 0.35f;
            int sx = (int) (proj.screenX);
            int sy = (int) (proj.screenY - pp.offsetY);
            String txt = pp.text;
            int tw = craft3dgl.ui.FontRenderer.textWidth(txt, scale);
            int th = (int)(craft3dgl.ui.FontRenderer.FONT_CELL * scale);
            // Podklad ciemny dla czytelnosci
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(0f, 0f, 0f, alpha * 0.65f);
            craft3dgl.ui.UIStyle.quad(sx - tw / 2 - 4, sy - 2, tw + 8, th + 4);
            // Kolorowy border (typ damage)
            glColor4f(pp.r, pp.g, pp.b, alpha * 0.9f);
            craft3dgl.ui.UIStyle.lineRect(sx - tw / 2 - 4, sy - 2, tw + 8, th + 4);
            glEnable(GL_TEXTURE_2D);
            // Tekst - hack: renderujemy przez glColor4f + drawText (drawText wewnetrznie ustawi kolor 1,1,1)
            // Wiec uzyjemy fontRenderer.drawText direct ale z shadowem
            fontRenderer.drawText(txt, sx - tw / 2, sy, scale);
            // Overlay kolorem - drugi passing z kolorem popup
            glBindTexture(GL_TEXTURE_2D, fontTexture);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(pp.r, pp.g, pp.b, alpha);
            drawTextColoredRaw(txt, sx - tw / 2, sy, scale);
            glBindTexture(GL_TEXTURE_2D, textureAtlas);
        }
        glColor4f(1, 1, 1, 1);
    }

    /** Rysuje tekst tylko kwady - bez ustawiania koloru (kolor musi byc juz wczesniej ustawiony). */
    void drawTextColoredRaw(String text, int x, int y, float scale) {
        float size = craft3dgl.ui.FontRenderer.FONT_CELL * scale;
        glBegin(GL_QUADS);
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            if (c < 32 || c > 126) c = '?';
            int tx = c & 15;
            int ty = c >> 4;
            float u0 = tx / 16f;
            float v0 = ty / 16f;
            float u1 = (tx + 1) / 16f;
            float v1 = (ty + 1) / 16f;
            float px = x + i * size * 0.66f;
            float py = y;
            glTexCoord2f(u0, v0); glVertex2f(px, py);
            glTexCoord2f(u1, v0); glVertex2f(px + size, py);
            glTexCoord2f(u1, v1); glVertex2f(px + size, py + size);
            glTexCoord2f(u0, v1); glVertex2f(px, py + size);
        }
        glEnd();
    }

    void drawCrosshair() {
        double mp = (miningHit != null) ? Math.min(1.0, miningProgress) : 0.0;
        float attackStrength = craft3dgl.combat.DamageSystem.cooledAttackStrength(
                ticksSinceLastSwing, selectedItemId(), 0.0f);
        craft3dgl.ui.Crosshair.draw(width, height, mp,
                org.lwjgl.glfw.GLFW.glfwGetTime(), attackStrength);
    }

    void drawDeathScreen() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBegin(GL_QUADS);
        glColor4f(0.32f, 0.00f, 0.00f, 0.76f); glVertex2i(0, 0); glVertex2i(width, 0);
        glColor4f(0.02f, 0.00f, 0.00f, 0.88f); glVertex2i(width, height); glVertex2i(0, height);
        glEnd();
        int panelW = 520;
        int panelH = 265;
        int px = width / 2 - panelW / 2;
        int py = height / 2 - 125;
        glColor4f(0.03f, 0.00f, 0.00f, 0.72f); quad(px, py, panelW, panelH);
        glColor4f(0.80f, 0.08f, 0.06f, 0.70f); lineRect(px, py, panelW, panelH);
        drawCenteredText(tr("death.title"), width / 2, py + 34, 1.35f);
        drawCenteredText(tr("death.dropped"), width / 2, py + 115, 0.62f);
        int bw = 300, bh = 46;
        int bx = width / 2 - bw / 2;
        int by = height / 2 + 62;
        drawDeathButton(bx, by, bw, bh, tr("death.respawn"));
        drawDeathButton(bx, by + 58, bw, bh, tr("death.titlescr"));
        glDisable(GL_BLEND);
        glColor4f(1,1,1,1);
    }

    void drawDeathButton(int x, int y, int w, int h, String text) { MenuButton.drawDeathButton(fontRenderer, x, y, w, h, text); }

    void drawPauseOverlay() {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.00f, 0.00f, 0.00f, 0.48f); quad(0, 0, width, height);
        if (pauseScreen == 1) drawSettingsOverlay();
        else drawPauseButtons();
        glDisable(GL_BLEND);
        glColor4f(1,1,1,1);
    }

    void drawPauseButtons() {
        int panelW = 430, panelH = 265;
        int px = width / 2 - panelW / 2;
        int py = height / 2 - 145;
        glColor4f(0.03f, 0.03f, 0.04f, 0.70f); quad(px, py, panelW, panelH);
        glColor4f(0.80f, 0.80f, 0.86f, 0.42f); lineRect(px, py, panelW, panelH);
        drawCenteredText(tr("pause.title"), px + panelW / 2, py + 18, 0.90f);
        int bw = 320, bh = 46;
        int bx = width / 2 - bw / 2;
        int by = height / 2 - 76;
        drawButton(bx, by, bw, bh, tr("pause.continue"));
        drawButton(bx, by + 58, bw, bh, tr("pause.settings"));
        drawButton(bx, by + 116, bw, bh, tr("pause.exit"));
        drawText(tr("pause.esc"), bx + 40, by + 176, 0.58f);
    }

    void drawSettingsOverlay() {
        int panelW = 620, panelH = 420;
        int px = width / 2 - panelW / 2;
        int py = height / 2 - panelH / 2;
        glColor4f(0.03f, 0.03f, 0.04f, 0.80f); quad(px, py, panelW, panelH);
        glColor4f(0.80f, 0.80f, 0.86f, 0.50f); lineRect(px, py, panelW, panelH);
        drawCenteredText(tr("settings.title"), px + panelW / 2, py + 12, 0.90f);
        String[] tabLabels = {tr("settings.title"), tr("settings.sounds"), tr("settings.language")};
        int tabW = (panelW - 32) / 3;
        for (int i = 0; i < 3; i++) {
            int tx = px + 16 + i * tabW;
            int ty = py + 36;
            glColor4f(i == settingsTab ? 0.30f : 0.15f, i == settingsTab ? 0.30f : 0.15f, i == settingsTab ? 0.32f : 0.17f, 1f);
            quad(tx, ty, tabW - 4, 30);
            glColor4f(0.85f, 0.85f, 0.88f, 1f); lineRect(tx, ty, tabW - 4, 30);
            drawCenteredText(tabLabels[i], tx + (tabW - 4) / 2, ty + 6, 0.55f);
        }
        if (settingsTab == 0) {
            int sliderX = px + 80;
            int sliderY = py + 130;
            int sliderW = panelW - 160;
            drawText(tr("settings.master") + ": " + sound.volumePercent() + "%", sliderX, sliderY - 30, 0.65f);
            drawVolumeSlider(sliderX, sliderY, sliderW, 18, sound.masterVolume);
        } else if (settingsTab == 1) {
            String[] cats = {"music", "blocks", "hostile", "animals", "players", "ambient", "ui"};
            String[] catKeys = {"settings.music", "settings.blocks", "settings.hostile", "settings.animals", "settings.players", "settings.ambient", "settings.ui"};
            int sliderX = px + 220;
            int sliderW = panelW - 280;
            for (int i = 0; i < cats.length; i++) {
                int sy = py + 90 + i * 36;
                drawText(tr(catKeys[i]), px + 30, sy - 2, 0.55f);
                int pct = (int) Math.round(sound.catVol(cats[i]) * 100);
                drawText(pct + "%", px + panelW - 60, sy - 2, 0.55f);
                drawVolumeSlider(sliderX, sy, sliderW, 14, sound.catVol(cats[i]));
            }
        } else if (settingsTab == 2) {
            int lbw = 200, lbh = 50;
            int lbx = px + 60;
            int lby = py + 120;
            glColor4f(language.equals("pl") ? 0.30f : 0.15f, language.equals("pl") ? 0.45f : 0.15f, language.equals("pl") ? 0.30f : 0.17f, 1f);
            quad(lbx, lby - 6, lbw, lbh);
            glColor4f(0.85f, 0.85f, 0.88f, 1f); lineRect(lbx, lby - 6, lbw, lbh);
            drawCenteredText("Polski", lbx + lbw / 2, lby + 10, 0.78f);
            glColor4f(language.equals("en") ? 0.30f : 0.15f, language.equals("en") ? 0.45f : 0.15f, language.equals("en") ? 0.30f : 0.17f, 1f);
            quad(lbx + lbw + 40, lby - 6, lbw, lbh);
            glColor4f(0.85f, 0.85f, 0.88f, 1f); lineRect(lbx + lbw + 40, lby - 6, lbw, lbh);
            drawCenteredText("English", lbx + lbw + 40 + lbw / 2, lby + 10, 0.78f);
        }
        int bw = 220, bh = 40;
        int bx = px + panelW / 2 - bw / 2;
        int by = py + panelH - 60;
        drawButton(bx, by, bw, bh, tr("settings.done"));
    }

    void drawVolumeSlider(int x, int y, int w, int h, double value) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.02f, 0.02f, 0.025f, 0.98f); quad(x, y, w, h);
        glColor4f(0.62f, 0.62f, 0.66f, 1f); lineRect(x, y, w, h);
        int fill = (int)Math.round(w * value);
        glColor4f(0.18f, 0.62f, 0.18f, 0.95f); quad(x + 2, y + 2, Math.max(0, fill - 4), h - 4);
        int knobX = x + fill;
        glColor4f(0.92f, 0.92f, 0.92f, 1f); quad(knobX - 7, y - 4, 14, h + 8);
        glColor4f(0.15f, 0.15f, 0.16f, 1f); lineRect(knobX - 7, y - 4, 14, h + 8);
    }

    void drawSurvivalBars() {
        // MC-style layout od dolu do gory: hotbar -> XP bar -> serca/hunger -> zbroja
        int barW = craft3dgl.ui.HotbarRenderer.HOTBAR_W;
        int barH = craft3dgl.ui.HotbarRenderer.HOTBAR_H;
        int start = width / 2 - barW / 2;
        int hotbarY = height - barH - 8;
        int iconSize = 27;   // MC 9*3
        int spacing = 24;    // MC 8*3

        // XP BAR - tuz nad hotbarem (5px odstep), pod sercami
        int xpBarH = 15;     // MC 5*3
        int xpY = hotbarY - xpBarH - 3;
        int xpX = start + 4;
        craft3dgl.ui.HudIcons.drawXpBar(xpX, xpY, barW - 8, xpBarH, xpProgress);
        if (xpLevel > 0) {
            // MC-style level: 4x czarny obrys + zielony #80FF20 centered
            String lvl = String.valueOf(xpLevel);
            int centerX = xpX + (barW - 8) / 2;
            fontRenderer.drawXpLevel(lvl, centerX, xpY - 28, 3);
        }

        // SERCA i HUNGER - nad XP barem (8px odstep zeby odsunac od baru)
        int yBase = xpY - iconSize - 8;
        int leftX = start + 4;
        int rightX = start + barW - 4;
        for (int i = 0; i < 10; i++) {
            drawHeart(leftX + i * spacing, yBase, health >= (i + 1) * 2, health == i * 2 + 1);
        }
        for (int i = 0; i < 10; i++) {
            drawHunger(rightX - (i + 1) * spacing - iconSize + spacing, yBase,
                       hunger >= (i + 1) * 2, hunger == i * 2 + 1);
        }

        // ZBROJA - nad sercami
        int armor = armorPoints();
        if (armor > 0) {
            for (int i = 0; i < 10; i++) {
                drawArmor(leftX + i * spacing, yBase - iconSize - 4,
                          armor >= (i + 1) * 2, armor == i * 2 + 1);
            }
        }
    }

    int armorPoints() {
        int p = 0;
        for (int i = 0; i < 4; i++) if (equipId[i] > 0 && equipCount[i] > 0) p += 2;
        return p;
    }

    void drawHeart(int x, int y, boolean full, boolean half) { craft3dgl.ui.HudIcons.drawHeart(x, y, full, half); }

    void drawHunger(int x, int y, boolean full, boolean half) { craft3dgl.ui.HudIcons.drawHunger(x, y, full, half); }

    void drawArmor(int x, int y, boolean full, boolean half) { craft3dgl.ui.HudIcons.drawArmor(x, y, full, half); }

    void drawHotbar() {
        // Cala ramka hotbara w stylu MC jest narysowana przez HotbarRenderer.
        HotbarRenderer.drawHotbarFrame(width, height, selectedSlot, HOTBAR_SIZE);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);

        int slot = HotbarRenderer.SLOT_SIZE;
        int iconSize = HotbarRenderer.ICON_SIZE;
        int iconOffset = HotbarRenderer.iconOffset();
        int by = HotbarRenderer.slotY(height) - 1;  // -1 zeby pozycje pasowaly do reszty kodu
        int barW = HOTBAR_SIZE * slot + 2;
        int start = width / 2 - barW / 2;
        int barH = slot + 2;

        for (int i = 0; i < HOTBAR_SIZE; i++) {
            int sx = start + 1 + i * slot;
            int sy = by + 1;
            drawStackIcon(invId[i], invCount[i], sx + iconOffset, sy + iconOffset, iconSize);
        }

        // === 9. NAZWA TRZYMANEGO PRZEDMIOTU ponad hotbarem ===
        int item = selectedItemId();
        String name = item > 0 ? itemName(item) + " x" + selectedItemCount() : "";
        if (!name.isEmpty()) {
            drawCenteredText(name, width / 2, by - 22, 0.65f);
        }
    }

    void drawInventoryUI() {
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        translationSys.setLanguage(language);
        craft3dgl.ui.InventoryUIRenderer.draw(fontRenderer, this::drawStackIcon, translationSys,
                width, height, (int)mxA[0], (int)myA[0],
                usingCraftingTable, craftId, craftCount, craftResult,
                equipId, equipCount, invId, invCount, selectedSlot,
                cursorId, cursorCount);
    }

    /** Czarne tlo z prostym podgladem postaci (2D avatar) */
    /** Nowa wersja hover dla nowego layoutu */
    int inventoryHoverItem(int mx, int my, int px, int py, int panelW, int panelH, int craftSize, int slot, int cx, int cy, int outX, int outY, int invX, int invY, int hotY, int eqX, int eqY, int offX, int offY) {
        for (int row = 0; row < craftSize; row++) for (int col = 0; col < craftSize; col++) {
            int idx = row * craftSize + col;
            int sx = cx + col * slot, sy = cy + row * slot;
            if (inside(mx, my, sx, sy, 40, 40) && craftId[idx] > 0) return craftId[idx];
        }
        if (inside(mx, my, outX, outY, 40, 40) && !craftResult.empty()) return craftResult.resultId;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = 9 + row * 9 + col;
            int sx = invX + col * slot, sy = invY + row * slot;
            if (inside(mx, my, sx, sy, 40, 40) && invId[idx] > 0) return invId[idx];
        }
        for (int col = 0; col < 9; col++) {
            int sx = invX + col * slot;
            if (inside(mx, my, sx, hotY, 40, 40) && invId[col] > 0) return invId[col];
        }
        for (int i = 0; i < 4; i++) {
            int sx = eqX, sy = eqY + i * slot;
            if (inside(mx, my, sx, sy, 40, 40) && equipId[i] > 0) return equipId[i];
        }
        if (inside(mx, my, offX, offY, 40, 40) && equipId[4] > 0) return equipId[4];
        return 0;
    }

    void drawSlotRect(int x, int y, int s, boolean selected) {
        // Slot w stylu MC: jasne szare tlo z wglebionym srodkiem
        glDisable(GL_TEXTURE_2D);
        if (selected) {
            // Biala bold obwodka dla wybranego slotu
            glColor4f(1.00f, 1.00f, 1.00f, 1f); quad(x - 2, y - 2, s + 4, s + 4);
            glColor4f(0.0f, 0.0f, 0.0f, 1f); quad(x - 1, y - 1, s + 2, s + 2);
        }
        // Ciemna ramka (zewnetrzna obwodka slotu)
        glColor4f(0.30f, 0.30f, 0.30f, 1f);
        quad(x, y, s, s);
        // Wewnetrzne ciemniejsze tlo (efekt wglebienia)
        glColor4f(0.55f, 0.55f, 0.55f, 1f);
        quad(x + 1, y + 1, s - 2, s - 2);
        // Jasniejszy gradient w srodku
        glBegin(GL_QUADS);
        glColor4f(0.50f, 0.50f, 0.50f, 1f); glVertex2i(x + 1, y + 1); glVertex2i(x + s - 1, y + 1);
        glColor4f(0.65f, 0.65f, 0.65f, 1f); glVertex2i(x + s - 1, y + s - 1); glVertex2i(x + 1, y + s - 1);
        glEnd();
        // Wglebienie: ciemna linia gora+lewa
        glColor4f(0.30f, 0.30f, 0.30f, 1f);
        quad(x + 1, y + 1, s - 2, 1); quad(x + 1, y + 1, 1, s - 2);
        // Jasna linia dol+prawa
        glColor4f(0.85f, 0.85f, 0.85f, 1f);
        quad(x + 1, y + s - 2, s - 2, 1); quad(x + s - 2, y + 1, 1, s - 2);
        glEnable(GL_TEXTURE_2D);
    }

    void drawSlotHover(int x, int y, int s, int mx, int my) {
        if (!inside(mx, my, x, y, s, s)) return;
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 0.30f);
        quad(x + 2, y + 2, s - 4, s - 4);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
    }

    void drawPanel(int x, int y, int w, int h) {
        // Panel w stylu Minecraft: jasne szare tlo + czarna gruba ramka + bevel
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Cien pod panelem
        glColor4f(0f, 0f, 0f, 0.55f);
        quad(x + 6, y + 8, w, h);
        glDisable(GL_BLEND);
        // Glowne tlo - jasne szare (jak w MC)
        glColor4f(0.78f, 0.78f, 0.78f, 1f);
        quad(x, y, w, h);
        // Czarna zewnetrzna ramka (3px gruba)
        glColor4f(0.0f, 0.0f, 0.0f, 1f);
        quad(x, y, w, 3); quad(x, y, 3, h);
        quad(x, y + h - 3, w, 3); quad(x + w - 3, y, 3, h);
        // Jasna wewnetrzna obwodka (top + left) - bevel up
        glColor4f(1.0f, 1.0f, 1.0f, 1f);
        quad(x + 3, y + 3, w - 6, 2); quad(x + 3, y + 3, 2, h - 6);
        // Ciemna wewnetrzna obwodka (bottom + right) - bevel down
        glColor4f(0.45f, 0.45f, 0.45f, 1f);
        quad(x + 3, y + h - 5, w - 6, 2); quad(x + w - 5, y + 3, 2, h - 6);
        glEnable(GL_TEXTURE_2D);
    }

    void drawTitleBar(String title, int x, int y, int w) {
        // Tytul - prosto czarny tekst bez tla (jak w MC inventory: tylko napis "Crafting")
        drawCenteredTextDark(title, x + w / 2, y + 4, 0.85f);
    }

    void drawCenteredTextDark(String text, int centerX, int y, float scale) { fontRenderer.drawCenteredTextDark(text, centerX, y, scale); }

    void drawTextDark(String text, int x, int y, float scale) { fontRenderer.drawTextDark(text, x, y, scale); }

    void drawSectionLabel(String text, int x, int y) {
        // Etykieta sekcji - ciemny tekst, bez tla (jak w MC)
        drawTextDark(text, x, y, 0.60f);
    }

    void drawCraftArrow(int x, int y) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.45f, 0.43f, 0.40f, 1f);
        quad(x, y + 14, 32, 8);
        glBegin(GL_TRIANGLES);
        glColor4f(0.45f, 0.43f, 0.40f, 1f);
        glVertex2i(x + 30, y + 8); glVertex2i(x + 30, y + 28); glVertex2i(x + 42, y + 18);
        glEnd();
        glColor4f(0.18f, 0.16f, 0.14f, 1f);
        quad(x, y + 14, 32, 1);
        quad(x, y + 21, 32, 1);
        glEnable(GL_TEXTURE_2D);
    }

    void drawTooltip(String text, int mx, int my) { Tooltip.draw(fontRenderer, text, mx, my, width, height); }

    void quad(int x, int y, int w, int h) {
        glBegin(GL_QUADS);
        glVertex2i(x, y); glVertex2i(x + w, y); glVertex2i(x + w, y + h); glVertex2i(x, y + h);
        glEnd();
    }

    void lineRect(int x, int y, int w, int h) {
        glBegin(GL_LINE_LOOP);
        glVertex2i(x, y); glVertex2i(x + w, y); glVertex2i(x + w, y + h); glVertex2i(x, y + h);
        glEnd();
    }

    void drawStackIcon(int id, int count, int x, int y, int s) {
        if (id <= 0 || count <= 0) return;
        if (isBlockItem(id)) drawIcon(id, x, y, s);
        else drawToolIcon(id, x, y, s);
        if (count > 1) drawCountDots(count, x, y, s);
    }

    void drawCountDots(int count, int x, int y, int iconSize) {
        String text = String.valueOf(count);
        int pixelScale = Math.max(1, Math.round(iconSize / 16f));
        int textX = x + 17 * pixelScale - FontRenderer.mcTextWidth(text, pixelScale);
        int textY = y + 9 * pixelScale;
        fontRenderer.drawVanillaStackCount(text, textX, textY, pixelScale);
    }

    void drawToolIcon(int id, int x, int y, int s) {
        // Etap: PNG textures dla narzedzi (kilofy/siekiery/miecze/motyki)
        if (craft3dgl.ui.ToolTextures.drawToolIcon(id, x, y, s)) return;
        glDisable(GL_TEXTURE_2D);
        if (id == ITEM_EMERALD) {
            glColor3f(0.10f, 0.85f, 0.45f);
            quad(x + 9, y + 6, s - 18, s - 12);
            glColor3f(0.55f, 1.0f, 0.72f);
            quad(x + 13, y + 10, 8, 6);
            glEnable(GL_TEXTURE_2D);
            return;
        }
        if (id == ITEM_BREAD) {
            glColor3f(0.76f, 0.48f, 0.18f);
            quad(x + 6, y + 10, s - 12, s - 18);
            glColor3f(0.95f, 0.70f, 0.32f);
            quad(x + 9, y + 12, s - 18, 5);
            glEnable(GL_TEXTURE_2D);
            return;
        }
        if (id == ITEM_PORK || id == ITEM_BEEF || id == ITEM_MUTTON) {
            if (id == ITEM_BEEF) glColor3f(0.58f, 0.12f, 0.10f);
            else if (id == ITEM_MUTTON) glColor3f(0.82f, 0.30f, 0.32f);
            else glColor3f(0.95f, 0.38f, 0.42f);
            quad(x + 7, y + 9, s - 12, s - 15);
            glColor3f(1.0f, 0.62f, 0.64f);
            quad(x + 11, y + 12, 8, 6);
            glEnable(GL_TEXTURE_2D);
            return;
        }
        if (id == ITEM_SEEDS) {
            glColor3f(0.5f, 0.7f, 0.2f);
            quad(x + 8, y + 8, 4, 4);
            quad(x + 18, y + 10, 4, 4);
            quad(x + 10, y + 18, 4, 4);
            quad(x + 20, y + 20, 4, 4);
            glColor3f(0.7f, 0.9f, 0.4f);
            quad(x + 9, y + 9, 2, 2);
            quad(x + 19, y + 11, 2, 2);
            quad(x + 11, y + 19, 2, 2);
            quad(x + 21, y + 21, 2, 2);
            glEnable(GL_TEXTURE_2D);
            return;
        }
        if (id == ITEM_WHEAT) {
            glColor3f(0.85f, 0.65f, 0.20f);
            quad(x + 9, y + 6, 2, 20);
            quad(x + 15, y + 6, 2, 20);
            quad(x + 21, y + 6, 2, 20);
            glColor3f(0.6f, 0.45f, 0.10f);
            quad(x + 8, y + 6, 4, 4);
            quad(x + 14, y + 6, 4, 4);
            quad(x + 20, y + 6, 4, 4);
            glEnable(GL_TEXTURE_2D);
            return;
        }
        if (id == ITEM_STICK) {
            glColor3f(0.55f, 0.32f, 0.15f);
            for (int i = 0; i < s - 8; i++) {
                int px2 = x + 6 + i;
                int py2 = y + s - 8 - i;
                quad(px2, py2, 3, 3);
            }
            glEnable(GL_TEXTURE_2D);
            return;
        }
        int tCat = toolCategory(id);
        int tier = toolTier(id);
        if (tCat > 0 && tier > 0) {
            glColor3f(0.50f, 0.30f, 0.13f);
            for (int i = 0; i < s - 10; i++) {
                int px2 = x + 6 + i;
                int py2 = y + s - 9 - i;
                quad(px2, py2, 3, 3);
            }
            float hr, hg, hb;
            if (tier == 2) { hr = 0.65f; hg = 0.65f; hb = 0.70f; }
            else { hr = 0.78f; hg = 0.55f; hb = 0.28f; }
            glColor3f(hr, hg, hb);
            int hx = x + s - 18;
            int hy = y + 4;
            if (tCat == 1) {
                quad(hx, hy, 14, 4);
                quad(hx + 2, hy + 4, 10, 2);
                quad(hx + 5, hy + 6, 4, 2);
            } else if (tCat == 2) {
                quad(hx + 3, hy, 8, 10);
                quad(hx + 4, hy + 10, 6, 2);
            } else if (tCat == 3) {
                quad(hx, hy + 2, 12, 8);
                quad(hx, hy + 1, 8, 1);
                quad(hx, hy + 10, 8, 1);
                quad(hx - 2, hy + 4, 2, 4);
            } else if (tCat == 4) {
                glColor3f(hr, hg, hb);
                for (int i = 0; i < s - 14; i++) {
                    int px2 = x + s - 8 - i;
                    int py2 = y + 6 + i;
                    quad(px2, py2, 3, 3);
                }
                glColor3f(0.40f, 0.25f, 0.10f);
                quad(x + 6, y + s - 11, 7, 3);
            } else if (tCat == 5) {
                // Motyka - poziomy kafelek + krotka pionowa nasada
                glColor3f(hr, hg, hb);
                quad(hx, hy, 12, 4);
                quad(hx + 9, hy + 4, 3, 6);
            }
            glEnable(GL_TEXTURE_2D);
            return;
        }
        glColor3f(0.65f, 0.65f, 0.70f);
        quad(x + 8, y + 8, s - 16, s - 16);
        glEnable(GL_TEXTURE_2D);
    }

    int textWidth(String text, float scale) { return FontRenderer.textWidth(text, scale); }

    void drawCenteredText(String text, int centerX, int y, float scale) { fontRenderer.drawCenteredText(text, centerX, y, scale); }

    void drawText(String text, int x, int y, float scale) { fontRenderer.drawText(text, x, y, scale); }

    String itemName(int id) { return ItemNames.itemName(id, language); }


    void drawHandOverlay() {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        double near = 0.05, far = 10.0;
        double fov = Math.toRadians(70);
        double top = near * Math.tan(fov / 2.0);
        double right = top * ((double) width / height);
        glFrustum(-right, right, -top, top, near, far);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        // Overlay reki korzysta z fixed-function OpenGL 2.1, nie z shadera chunkow.
        org.lwjgl.opengl.GL20.glUseProgram(0);
        craft3dgl.blaze3d.renderer.RenderSystem.setShader(null);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glDisable(GL_CULL_FACE);
        glDisable(GL_FOG);
        // Czyszczenie depth jest respektowane tylko przy wlaczonym zapisie depth.
        glDepthMask(true);
        glClearDepth(1.0);
        glClear(GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        // BEZ blendingu - inaczej moga byc problemy z alpha
        glDisable(GL_BLEND);
        glColor4f(1, 1, 1, 1);

        double bobY = Math.sin(walkPhase) * 0.025;
        double bobX = Math.cos(walkPhase * 0.5) * 0.020;
        double baseX = 0.62 + bobX;
        double baseYpos = -0.55 + bobY;
        double baseZ = -0.95;
        double sw = swingTimer;
        double t = 1.0 - sw;
        double swingRot = 0, swingZ = 0, swingX = 0, swingTwist = 0;
        if (sw > 0) {
            // MC-style swing: sin(sqrt(t)*PI) - ostre uderzenie, powolny powrot
            // sqrt(t) rozciaga poczatek (szybki peak) i sciska koniec (lagodny powrot)
            double curve = Math.sin(Math.sqrt(t) * Math.PI);
            swingRot = curve * 60.0;
            swingZ = -curve * 0.18;
            swingX = -Math.sin(Math.sqrt(t) * Math.PI * 2.0) * 0.08;
            swingTwist = Math.sin(Math.sqrt(t) * Math.PI) * 25.0;
        }
        int item = selectedItemId();
        int held = (item > 0 && selectedItemCount() > 0) ? item : 0;
        boolean blockItem = held > 0 && isBlockItem(held);
        boolean foodItem = (held == ITEM_PORK || held == ITEM_BEEF || held == ITEM_MUTTON || held == ITEM_BREAD);
        boolean spriteItem = (held == ITEM_EMERALD || held == ITEM_WHEAT || held == ITEM_SEEDS || foodItem);
        boolean toolItem = held > 0 && toolCategory(held) > 0;

        if (held == 0 && craft3dgl.entities.SteveRenderer.isLoaded()) {
            // ItemRenderer.renderArmFirstPerson: pusty main hand, equipProgress=0.
            // Transformacja jest kompletna, wiec nie wolno dodawac baseX/baseY/baseZ drugi raz.
            float swingProgress = swingTimer > 0 ? (float)(1.0 - swingTimer) : 0f;
            craft3dgl.entities.SteveRenderer.drawMinecraftFirstPersonArm(swingProgress, 0f);
            finishHandOverlay();
            return;
        }

        boolean eating = eatingItemId == held && eatingProgress > 0;
        float swingProgress = swingTimer > 0 ? (float)(1.0 - swingTimer) : 0f;
        glPushMatrix();
        applyMinecraftFirstPersonItemTransform(swingProgress, eating);
        boolean renderedMinecraftItem = drawMinecraftFirstPersonItem(held);
        glPopMatrix();
        if (renderedMinecraftItem) {
            finishHandOverlay();
            return;
        }

        // Awaryjna sciezka dla przedmiotow bez tekstury/modelu.
        // Craft3D use is 0..1 over 32 ticks, so remaining ticks are 32*(1-use).
        if (eating) {
            double use = Math.min(1.0, eatingProgress / 1.6);
            double remaining = 32.0 * (1.0 - use);
            double fractionRemaining = remaining / 32.0;
            if (fractionRemaining < 0.8) {
                double bob = Math.abs(Math.cos(remaining / 4.0 * Math.PI) * 0.1);
                glTranslated(0.0, bob, 0.0);
            }
            double progress = 1.0 - Math.pow(fractionRemaining, 27.0);
            glTranslated(progress * 0.6, progress * -0.5, 0.0);
            glRotated(progress * 90.0, 0, 1, 0);
            glRotated(progress * 10.0, 1, 0, 0);
            glRotated(progress * 30.0, 0, 0, 1);
        }

        glTranslated(baseX + swingX, baseYpos, baseZ + swingZ);

        if (blockItem) {
            // FIX "czarny blok w rece": face() samplouje envLight z world[0][0][0]
            // co daje 0 (bo tam nie ma swiatla). Rozwiazanie: tymczasowo wywalamy lightEngine
            // zeby sampleShade zawsze zwracalo 1.0 (max jasnosc dla drawn item).
            craft3dgl.world.LightEngine savedLE = lightEngine;
            float savedDay = currentDayMult;
            lightEngine = null;              // sampleShade w face() bedzie return 1.0f
            currentDayMult = 1.0f;

            glPushMatrix();
            glRotated(swingRot, 1, 0, 0);
            glRotated(swingTwist, 0, 0, 1);
            glTranslated(blockTuneX, blockTuneY, blockTuneZ);
            if (Math.abs(blockTuneRotX) > 0.01f) glRotated(blockTuneRotX, 1, 0, 0);
            if (Math.abs(blockTuneRotY) > 0.01f) glRotated(blockTuneRotY, 0, 1, 0);
            if (Math.abs(blockTuneRotZ) > 0.01f) glRotated(blockTuneRotZ, 0, 0, 1);
            glScaled(blockTuneScale, blockTuneScale, blockTuneScale);
            glTranslated(-0.5, -0.5, -0.5);
            glColor4f(1, 1, 1, 1);
            glEnable(GL_TEXTURE_2D);
            glDisable(GL_ALPHA_TEST);
            glDisable(GL_BLEND);
            glBindTexture(GL_TEXTURE_2D, textureAtlas);
            glBegin(GL_QUADS);
            for (int dir = 0; dir < 6; dir++) face(0, 0, 0, held, dir);
            glEnd();
            glColor4f(1, 1, 1, 1);
            glPopMatrix();

            // Przywroc lightEngine
            lightEngine = savedLE;
            currentDayMult = savedDay;
        } else if (toolItem) {
            // Sprawdz czy mamy PNG teksture - jesli tak, uzyj MC-style renderu (BEZ arm model!)
            int toolTex = craft3dgl.ui.ToolTextures.getTexId(held);
            if (toolTex > 0) {
                // KROK 3: nie rysujemy arm modelu bo miecz sam sobie stoi w 3D voxel
                // (arm model powodowal ze reka Steve'a przenikala z mieczem)
                glPushMatrix();
                glRotated(swingRot, 1, 0, 0);
                glRotated(swingTwist, 0, 0, 1);
                drawToolSpriteInHand(held);
                glPopMatrix();
            } else {
                // Fallback do starego proceduralnego 3D modelu
                glRotated(swingRot, 1, 0, 0);
                glRotated(-15, 0, 1, 0);
                glRotated(swingTwist, 0, 0, 1);
                drawHandArmModel();
                glPushMatrix();
                glTranslated(0.04, -0.05, 0.10);
                glRotated(45, 0, 0, 1);
                drawToolModel3D(held);
                glPopMatrix();
            }
        } else if (held == ITEM_STICK) {
            glRotated(swingRot, 1, 0, 0);
            glRotated(-15, 0, 1, 0);
            drawHandArmModel();
            glPushMatrix();
            glTranslated(0.04, 0.0, 0.08);
            glRotated(40, 0, 0, 1);
            glDisable(GL_TEXTURE_2D);
            glColor4f(0.55f, 0.32f, 0.15f, 1f);
            drawCuboid(-0.025, -0.05, -0.05, 0.025, 0.40, 0.05);
            glPopMatrix();
        } else if (spriteItem && craft3dgl.ui.ToolTextures.getTexId(held) > 0) {
            // Food needs a visible arm while it moves to the mouth; other sprites stay item-only.
            glPushMatrix();
            glRotated(swingRot, 1, 0, 0);
            glRotated(swingTwist, 0, 0, 1);
            // Vanilla ItemInHandRenderer renders the food model here; the arm is not a second overlay.
            drawFoodSpriteInHand(held);
            glPopMatrix();
        } else if (foodItem) {
            // JEDZENIE (mieso/chleb) - DUZE i WYRAZNE w dloni, NA PEWNO widoczne
            glRotated(swingRot, 1, 0, 0);
            glRotated(-15, 0, 1, 0);
            drawHandArmModel();
            glPushMatrix();
            // Przesun przedmiot bardziej do przodu i ku gorze zeby byl WYRAZNY
            glTranslated(0.05, -0.02, 0.18);
            glRotated(15, 1, 0, 0);
            glDisable(GL_TEXTURE_2D);
            if (held == ITEM_BREAD) {
                // CHLEB - duzy bochenek
                glColor4f(0.76f, 0.48f, 0.18f, 1f);
                drawCuboid(-0.14, -0.04, -0.09, 0.14, 0.16, 0.09);
                // Jasny srodek (gorny i dolny pasek)
                glColor4f(0.95f, 0.70f, 0.32f, 1f);
                drawCuboid(-0.12, 0.04, -0.091, 0.12, 0.08, -0.085);
                drawCuboid(-0.12, 0.04, 0.085, 0.12, 0.08, 0.091);
                // Ciemne skorka na koncach
                glColor4f(0.52f, 0.30f, 0.10f, 1f);
                drawCuboid(-0.145, -0.05, -0.10, -0.13, 0.17, 0.10);
                drawCuboid(0.13, -0.05, -0.10, 0.145, 0.17, 0.10);
            } else {
                // MIESO
                if (held == ITEM_BEEF) glColor4f(0.58f, 0.12f, 0.10f, 1f);
                else if (held == ITEM_MUTTON) glColor4f(0.82f, 0.30f, 0.32f, 1f);
                else glColor4f(0.95f, 0.38f, 0.42f, 1f);
                drawCuboid(-0.14, -0.04, -0.09, 0.14, 0.18, 0.09);
                // Jasniejszy srodek (rozowy)
                glColor4f(1.0f, 0.62f, 0.64f, 1f);
                drawCuboid(-0.10, 0.04, -0.091, 0.10, 0.14, -0.085);
                // Kosc (jasny pasek z gory)
                glColor4f(0.95f, 0.92f, 0.85f, 1f);
                drawCuboid(-0.03, 0.18, -0.04, 0.03, 0.22, 0.04);
            }
            glPopMatrix();
        } else if (held == ITEM_EMERALD) {
            // SZMARAGD - duzy zielony krysztal w dloni
            glRotated(swingRot, 1, 0, 0);
            glRotated(-15, 0, 1, 0);
            drawHandArmModel();
            glPushMatrix();
            glTranslated(0.04, -0.02, 0.15);
            glRotated(25, 0, 1, 0);
            glRotated(20, 1, 0, 0);
            glDisable(GL_TEXTURE_2D);
            // Ciemna baza
            glColor4f(0.05f, 0.50f, 0.25f, 1f);
            drawCuboid(-0.07, 0.0, -0.06, 0.07, 0.18, 0.06);
            // Jasniejsza fasetka
            glColor4f(0.10f, 0.85f, 0.45f, 1f);
            drawCuboid(-0.06, 0.01, -0.05, 0.06, 0.17, 0.05);
            // Najjasniejszy blik (rog)
            glColor4f(0.65f, 1.00f, 0.78f, 1f);
            drawCuboid(-0.035, 0.05, 0.049, 0.025, 0.13, 0.052);
            // Drugi blik z gory
            glColor4f(0.55f, 1.00f, 0.72f, 1f);
            drawCuboid(-0.04, 0.155, -0.04, 0.04, 0.19, 0.04);
            glPopMatrix();
        } else {
            glRotated(swingRot, 1, 0, 0);
            glRotated(-15, 0, 1, 0);
            drawHandArmModel();
        }

        finishHandOverlay();
    }

    /** ItemRenderer.transformSideFirstPerson/transformFirstPerson z MCP 9.40. */
    void applyMinecraftFirstPersonItemTransform(float swingProgress, boolean eating) {
        if (eating) {
            double use = Math.min(1.0, eatingProgress / 1.6);
            double remaining = 32.0 * (1.0 - use);
            double fractionRemaining = remaining / 32.0;
            if (fractionRemaining < 0.8) {
                glTranslated(0, Math.abs(Math.cos(remaining / 4.0 * Math.PI) * 0.1), 0);
            }
            double progress = 1.0 - Math.pow(fractionRemaining, 27.0);
            glTranslated(progress * 0.6, progress * -0.5, 0);
            glRotated(progress * 90.0, 0, 1, 0);
            glRotated(progress * 10.0, 1, 0, 0);
            glRotated(progress * 30.0, 0, 0, 1);
        } else {
            double root = Math.sqrt(swingProgress);
            glTranslated(-0.4 * Math.sin(root * Math.PI),
                    0.2 * Math.sin(root * Math.PI * 2.0),
                    -0.2 * Math.sin(swingProgress * Math.PI));
        }

        // transformSideFirstPerson(RIGHT, equipProgress=0)
        glTranslated(0.56, -0.52, -0.72);
        if (!eating) {
            // transformFirstPerson(RIGHT, swingProgress)
            double turn = Math.sin(swingProgress * swingProgress * Math.PI);
            double swing = Math.sin(Math.sqrt(swingProgress) * Math.PI);
            glRotated(45.0 - turn * 20.0, 0, 1, 0);
            glRotated(swing * -20.0, 0, 0, 1);
            glRotated(swing * -80.0, 1, 0, 0);
            glRotated(-45.0, 0, 1, 0);
        }
    }

    /** Render modelu FIRST_PERSON_RIGHT_HAND z block.json/generated.json. */
    boolean drawMinecraftFirstPersonItem(int held) {
        if (held == CHEST) {
            glPushMatrix();
            glRotated(45.0, 0, 1, 0);
            glScaled(0.40, 0.40, 0.40);
            glTranslated(-0.5, -0.5, -0.5);
            boolean rendered = craft3dgl.world.ChestRenderer.drawItemModel();
            glPopMatrix();
            if (rendered) return true;
        }
        if (isBlockItem(held) && held != DOOR_BOTTOM) {
            craft3dgl.world.LightEngine savedLE = lightEngine;
            float savedDay = currentDayMult;
            lightEngine = null;
            currentDayMult = 1.0f;
            try {
                glPushMatrix();
                // assets/minecraft/models/block/block.json
                glRotated(45.0, 0, 1, 0);
                glScaled(0.40, 0.40, 0.40);
                glTranslated(-0.5, -0.5, -0.5);
                glColor4f(1, 1, 1, 1);
                glEnable(GL_TEXTURE_2D);
                glDisable(GL_ALPHA_TEST);
                glDisable(GL_BLEND);
                glBindTexture(GL_TEXTURE_2D, textureAtlas);
                glBegin(GL_QUADS);
                for (int dir = 0; dir < 6; dir++) face(0, 0, 0, held, dir);
                glEnd();
                glPopMatrix();
            } finally {
                lightEngine = savedLE;
                currentDayMult = savedDay;
            }
            return true;
        }

        if (toolCategory(held) > 0) {
            if (drawToolSpriteInHand(held)) return true;
            glPushMatrix();
            applyGeneratedItemModelTransform();
            glDisable(GL_TEXTURE_2D);
            drawToolModel3D(held);
            glEnable(GL_TEXTURE_2D);
            glPopMatrix();
            return true;
        }

        if (held == ITEM_STICK) {
            glPushMatrix();
            applyGeneratedItemModelTransform();
            glDisable(GL_TEXTURE_2D);
            glColor4f(0.55f, 0.32f, 0.15f, 1f);
            drawCuboid(-0.025, -0.45, -0.025, 0.025, 0.45, 0.025);
            glEnable(GL_TEXTURE_2D);
            glPopMatrix();
            return true;
        }

        // generated.json: jedzenie, emerald, nasiona, pszenica i drzwi jako item.
        return drawFoodSpriteInHand(held);
    }

    void applyGeneratedItemModelTransform() {
        // item/generated.json i item/handheld.json: firstperson_righthand
        glTranslated(1.13 / 16.0, 3.2 / 16.0, 1.13 / 16.0);
        glRotated(-90.0, 0, 1, 0);
        glRotated(25.0, 0, 0, 1);
        glScaled(0.68, 0.68, 0.68);
    }

    void finishHandOverlay() {
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_FOG);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1,1,1,1);
    }

    void drawHandArmModel() {
        if (craft3dgl.entities.SteveRenderer.isLoaded()) {
            // Wywolujacy ustawil juz transformacje przedmiotu/reki.
            craft3dgl.entities.SteveRenderer.drawMinecraftFirstPersonArm();
        } else {
            craft3dgl.entities.PlayerRenderer.drawHandArmModel();
        }
    }

    void drawToolModel3D(int item) { craft3dgl.entities.PlayerRenderer.drawToolModel3D(item); }

    /**
     * Rysuje item w rece jako teksturowany flat quad (MC-style item in hand).
     * Zwraca true jesli udalo sie znalezc PNG teksture, false = trzeba uzyc fallback.
     */
    /**
     * Rysuje item w rece jako PRAWDZIWY 3D voxel model.
     * Voxelizer: kazdy nieprzezroczysty pixel tekstury = maly szescian (bez tekstury, kolor bezposredni).
     * Boki generowane tylko dla pixeli obok przezroczystych sasiadow (MC ItemModelGenerator style).
     * Wynik: item wyglada 3D z kazdego katu, bez dziur, bez UV bleedingu.
     */
    void printTuneHelp() {
        System.out.println("  STRZALKI: X/Y pozycja | PgUp/PgDn: Z pozycja");
        System.out.println("  I/K J/L U/O: rotacja X/Y/Z (I/J/U = +, K/L/O = -)");
        System.out.println("  + / -: skala");
        System.out.println("  P: wypisz aktualne wartosci");
    }

    boolean drawToolSpriteInHand(int itemId) {
        craft3dgl.ui.ToolMeshBuilder.VoxelMesh mesh = craft3dgl.ui.ToolMeshBuilder.getMesh(itemId);
        if (mesh == null) return false;

        boolean cullWas = glIsEnabled(GL_CULL_FACE);
        glDisable(GL_CULL_FACE);
        glDisable(GL_TEXTURE_2D);       // Rysujemy per-vertex kolorami, bez tekstury
        glDisable(GL_BLEND);
        glDisable(GL_ALPHA_TEST);

        glPushMatrix();
        applyGeneratedItemModelTransform();

        // Voxel grid: 16x16 pixel -> 1x1 blok (0..1)
        int w = mesh.width, h = mesh.height;
        double px = 1.0 / w;                 // wielkosc jednego pixela = 1/16 = 0.0625
        double thick = 1.0 / 16.0;           // KROK 2: explicit 1 pixel MC-style
        double offX = -0.5;
        double offY = -0.5;

        glBegin(GL_QUADS);
        int n = mesh.voxels.length;
        for (int i = 0; i < n; i++) {
            craft3dgl.ui.ToolMeshBuilder.Voxel v = mesh.voxels[i];
            // Pixel image: y=0 na gorze. My chcemy y=0 na dole (OpenGL)
            double px0 = offX + v.x * px;
            double px1 = px0 + px;
            double py1 = offY + (h - v.y) * px;    // odwrocone Y
            double py0 = py1 - px;
            // Kolor pixela
            int argb = v.rgba;
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >> 8) & 0xFF) / 255f;
            float b = (argb & 0xFF) / 255f;
            glColor3f(r, g, b);

            double z0 = -thick/2, z1 = thick/2;
            // PRZOD (z=+z1)
            glVertex3d(px0, py0, z1);
            glVertex3d(px1, py0, z1);
            glVertex3d(px1, py1, z1);
            glVertex3d(px0, py1, z1);
            // TYL (z=-z1)
            glVertex3d(px0, py0, z0);
            glVertex3d(px0, py1, z0);
            glVertex3d(px1, py1, z0);
            glVertex3d(px1, py0, z0);
            // BOKI - tylko tam gdzie sasiad jest przezroczysty (optymalizacja + MC-style)
            // GORA (obok pixela nad = przezroczysty -> renderuj gorna sciane)
            if (mesh.hasUp[i]) {
                // MC gora = 1.0 (niebo swieci na wierzch)
                glColor3f(r, g, b);
                glVertex3d(px0, py1, z0);
                glVertex3d(px0, py1, z1);
                glVertex3d(px1, py1, z1);
                glVertex3d(px1, py1, z0);
            }
            // DOL - MC 0.5 (najciemniejsze)
            if (mesh.hasDown[i]) {
                glColor3f(r * 0.5f, g * 0.5f, b * 0.5f);
                glVertex3d(px0, py0, z1);
                glVertex3d(px0, py0, z0);
                glVertex3d(px1, py0, z0);
                glVertex3d(px1, py0, z1);
                glColor3f(r, g, b);
            }
            // LEWO
            if (mesh.hasLeft[i]) {
                glColor3f(r * 0.8f, g * 0.8f, b * 0.8f);
                glVertex3d(px0, py0, z0);
                glVertex3d(px0, py1, z0);
                glVertex3d(px0, py1, z1);
                glVertex3d(px0, py0, z1);
                glColor3f(r, g, b);
            }
            // PRAWO
            if (mesh.hasRight[i]) {
                glColor3f(r * 0.8f, g * 0.8f, b * 0.8f);
                glVertex3d(px1, py0, z1);
                glVertex3d(px1, py1, z1);
                glVertex3d(px1, py1, z0);
                glVertex3d(px1, py0, z0);
                glColor3f(r, g, b);
            }
        }
        glEnd();
        glColor4f(1, 1, 1, 1);
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);
        if (cullWas) glEnable(GL_CULL_FACE);
        return true;
    }

    boolean drawFoodSpriteInHand(int itemId) {
        craft3dgl.ui.ToolMeshBuilder.VoxelMesh mesh = craft3dgl.ui.ToolMeshBuilder.getMesh(itemId);
        int tex = craft3dgl.ui.ToolTextures.getTexId(itemId);
        if (mesh == null && tex <= 0) return false;
        boolean cullWas = glIsEnabled(GL_CULL_FACE);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        glDisable(GL_ALPHA_TEST);
        glPushMatrix();
        applyGeneratedItemModelTransform();
        if (mesh != null) {
            glDisable(GL_TEXTURE_2D);
            int w = mesh.width, h = mesh.height;
            double px = 1.0 / w;
            double thick = 1.0 / 16.0;
            double offX = -0.5, offY = -0.5;
            glBegin(GL_QUADS);
            int n = mesh.voxels.length;
            for (int i = 0; i < n; i++) {
                craft3dgl.ui.ToolMeshBuilder.Voxel v = mesh.voxels[i];
                double px0 = offX + v.x * px;
                double px1 = px0 + px;
                double py1 = offY + (h - v.y) * px;
                double py0 = py1 - px;
                int argb = v.rgba;
                float r = ((argb >> 16) & 0xFF) / 255f;
                float g = ((argb >> 8) & 0xFF) / 255f;
                float b = (argb & 0xFF) / 255f;
                glColor3f(r, g, b);
                double z0 = -thick/2, z1 = thick/2;
                glVertex3d(px0, py0, z1); glVertex3d(px1, py0, z1);
                glVertex3d(px1, py1, z1); glVertex3d(px0, py1, z1);
                glVertex3d(px0, py0, z0); glVertex3d(px0, py1, z0);
                glVertex3d(px1, py1, z0); glVertex3d(px1, py0, z0);
                if (mesh.hasUp[i]) {
                    glVertex3d(px0, py1, z0); glVertex3d(px0, py1, z1);
                    glVertex3d(px1, py1, z1); glVertex3d(px1, py1, z0);
                }
                if (mesh.hasDown[i]) {
                    glColor3f(r * 0.5f, g * 0.5f, b * 0.5f);
                    glVertex3d(px0, py0, z1); glVertex3d(px0, py0, z0);
                    glVertex3d(px1, py0, z0); glVertex3d(px1, py0, z1);
                    glColor3f(r, g, b);
                }
                if (mesh.hasLeft[i]) {
                    glColor3f(r * 0.8f, g * 0.8f, b * 0.8f);
                    glVertex3d(px0, py0, z0); glVertex3d(px0, py1, z0);
                    glVertex3d(px0, py1, z1); glVertex3d(px0, py0, z1);
                    glColor3f(r, g, b);
                }
                if (mesh.hasRight[i]) {
                    glColor3f(r * 0.8f, g * 0.8f, b * 0.8f);
                    glVertex3d(px1, py0, z1); glVertex3d(px1, py1, z1);
                    glVertex3d(px1, py1, z0); glVertex3d(px1, py0, z0);
                    glColor3f(r, g, b);
                }
            }
            glEnd();
            glColor4f(1, 1, 1, 1);
            glEnable(GL_TEXTURE_2D);
        } else {
            glEnable(GL_TEXTURE_2D);
            glColor4f(1f, 1f, 1f, 1f);
            glBindTexture(GL_TEXTURE_2D, tex);
            double sz = 0.5;
            glBegin(GL_QUADS);
            glTexCoord2f(0, 1); glVertex3d(-sz, -sz, 0);
            glTexCoord2f(1, 1); glVertex3d( sz, -sz, 0);
            glTexCoord2f(1, 0); glVertex3d( sz,  sz, 0);
            glTexCoord2f(0, 0); glVertex3d(-sz,  sz, 0);
            glEnd();
        }
        glPopMatrix();
        if (cullWas) glEnable(GL_CULL_FACE);
        return true;
    }

    double atlasU0(int tile) { return TextureAtlas.atlasU0(tile); }
    double atlasU1(int tile) { return TextureAtlas.atlasU1(tile); }
    double atlasV0() { return TextureAtlas.atlasV0(); }
    double atlasV1() { return TextureAtlas.atlasV1(); }

    void drawIcon(int block, int x, int y, int s) {
        if (block == CHEST
                && craft3dgl.world.ChestRenderer.drawGuiItem(x, y, s, width, height)) {
            return;
        }
        // Every item draw must bind its own texture. Previously a block rendered
        // after a tool/font/container sampled whichever texture happened to be
        // left bound, corrupting icons differently in hotbar and inventory.
        org.lwjgl.opengl.GL20.glUseProgram(0);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
        int tile = tileFor(block, 1);
        double u0 = atlasU0(tile);
        double u1 = atlasU1(tile);
        double v0 = atlasV0();
        double v1 = atlasV1();
        glColor3f(1,1,1);
        glBegin(GL_QUADS);
        glTexCoord2d(u0, v0); glVertex2i(x, y);
        glTexCoord2d(u1, v0); glVertex2i(x + s, y);
        glTexCoord2d(u1, v1); glVertex2i(x + s, y + s);
        glTexCoord2d(u0, v1); glVertex2i(x, y + s);
        glEnd();
    }

    void rebuildDirtyChunks() {
        // Never compile all 16,384 chunks in one frame. Besides freezing the
        // window, that exhausted native OpenGL display-list memory on Windows.
        int budget = 8;
        int pcx = clampInt((int)Math.floor(x) / CHUNK, 0, CHUNKS_X - 1);
        int pcz = clampInt((int)Math.floor(z) / CHUNK, 0, CHUNKS_Z - 1);
        for (int r = 0; r <= 6 && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) for (int dz = -r; dz <= r && budget > 0; dz++) {
                if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                int cx = pcx + dx, cz = pcz + dz;
                if (cx < 0 || cz < 0 || cx >= CHUNKS_X || cz >= CHUNKS_Z) continue;
                if (!generatedColumns[cx][cz] || !litColumns[cx][cz]) continue;
                for (int cy = 0; cy < CHUNKS_Y && budget > 0; cy++) {
                    if (chunks[cx][cy][cz].dirty) {
                        rebuildChunk(cx, cy, cz);
                        budget--;
                    }
                }
            }
        }
    }

    void rebuildInitialChunksForLoading(int radius, double progressStart, double progressEnd) {
        int pcx = clampInt((int)Math.floor(x) / CHUNK, 0, CHUNKS_X - 1);
        int pcz = clampInt((int)Math.floor(z) / CHUNK, 0, CHUNKS_Z - 1);
        int total = 0;
        for (int cx = Math.max(0, pcx - radius); cx <= Math.min(CHUNKS_X - 1, pcx + radius); cx++) {
            for (int cz = Math.max(0, pcz - radius); cz <= Math.min(CHUNKS_Z - 1, pcz + radius); cz++) {
                if (generatedColumns[cx][cz] && litColumns[cx][cz]) total += CHUNKS_Y;
            }
        }
        int done = 0;
        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;
                int cx = pcx + dx, cz = pcz + dz;
                if (cx < 0 || cz < 0 || cx >= CHUNKS_X || cz >= CHUNKS_Z) continue;
                if (!generatedColumns[cx][cz] || !litColumns[cx][cz]) continue;
                for (int cy = 0; cy < CHUNKS_Y; cy++) {
                    if (chunks[cx][cy][cz].dirty) rebuildChunk(cx, cy, cz);
                    done++;
                }
                double part = total == 0 ? 1.0 : Math.min(1.0, done / (double)total);
                renderLoadingScreen(progressStart + (progressEnd - progressStart) * part,
                        "Budowanie terenu... " + (int)Math.round(part * 100.0) + "%");
            }
        }
        System.out.println("[ChunkBuilder] initial terrain VBOs ready: " + done + " sections");
    }

    void rebuildChunk(int cx, int cy, int cz) {
        // Minecraft 1.12 uses chunk VBOs when OpenGlHelper.useVbo() is true.
        // The old display-list path crashes in some Windows OpenGL drivers
        // while compiling the first terrain section (0xC0000005).
        Chunk ch = chunks[cx][cy][cz];
        if (!terrainVboLogged) {
            terrainVboLogged = true;
            System.out.println("[ChunkBuilder] using Minecraft 1.12 fixed-function VBO terrain path");
        }
        craft3dgl.blaze3d.vertex.Tesselator tess = craft3dgl.blaze3d.vertex.Tesselator.getInstance();
        craft3dgl.blaze3d.vertex.BufferBuilder bb = tess.getBuilder();
        craft3dgl.blaze3d.vertex.VertexFormat fmt = craft3dgl.blaze3d.vertex.DefaultVertexFormat.BLOCK;
        buildingTerrainMesh = true;
        try {
            bb.begin(GL_QUADS, fmt);
            int solid = addChunkToModernMesh(bb, cx, cy, cz, MODERN_LAYER_SOLID);
            bb.end();
            ch.modernVboSolid = uploadChunkLayer(ch.modernVboSolid, bb, fmt, solid);
            ch.modernVertexCountSolid = solid * 4;
            bb.clear();

            bb.begin(GL_QUADS, fmt);
            int cutout = addChunkToModernMesh(bb, cx, cy, cz, MODERN_LAYER_CUTOUT);
            bb.end();
            ch.modernVboCutout = uploadChunkLayer(ch.modernVboCutout, bb, fmt, cutout);
            ch.modernVertexCountCutout = cutout * 4;
            bb.clear();

            bb.begin(GL_QUADS, fmt);
            int water = addChunkToModernMesh(bb, cx, cy, cz, MODERN_LAYER_TRANSLUCENT);
            bb.end();
            ch.modernVboTranslucent = uploadChunkLayer(ch.modernVboTranslucent, bb, fmt, water);
            ch.modernVertexCountTranslucent = water * 4;
            bb.clear();
            ch.modernDirty = false;
            ch.dirty = false;
        } finally {
            buildingTerrainMesh = false;
        }
    }

    craft3dgl.blaze3d.vertex.VertexBuffer uploadChunkLayer(
            craft3dgl.blaze3d.vertex.VertexBuffer vbo,
            craft3dgl.blaze3d.vertex.BufferBuilder bb,
            craft3dgl.blaze3d.vertex.VertexFormat fmt, int quads) {
        if (quads <= 0) {
            if (vbo != null) vbo.delete();
            return null;
        }
        if (vbo == null) vbo = new craft3dgl.blaze3d.vertex.VertexBuffer(fmt);
        if (vbo.getId() <= 0) throw new IllegalStateException("OpenGL VBO allocation failed");
        vbo.upload(bb.getBuffer());
        return vbo;
    }

    void addVisibleFaces(int bx, int by, int bz, int id) {
        if (id == WATER) { addWaterVisibleFaces(bx, by, bz); return; }
        if (faceVisible(id, bx + 1, by, bz)) face(bx, by, bz, id, 0);
        if (faceVisible(id, bx - 1, by, bz)) face(bx, by, bz, id, 1);
        if (faceVisible(id, bx, by + 1, bz)) face(bx, by, bz, id, 2);
        if (faceVisible(id, bx, by - 1, bz)) face(bx, by, bz, id, 3);
        if (faceVisible(id, bx, by, bz + 1)) face(bx, by, bz, id, 4);
        if (faceVisible(id, bx, by, bz - 1)) face(bx, by, bz, id, 5);
    }

    void addWaterVisibleFaces(int bx, int by, int bz) {
        if (!isWater(bx, by + 1, bz)) waterFace(bx, by, bz, 2);
        if (isAirForWaterSide(bx + 1, by, bz)) waterFace(bx, by, bz, 0);
        if (isAirForWaterSide(bx - 1, by, bz)) waterFace(bx, by, bz, 1);
        if (isAirForWaterSide(bx, by, bz + 1)) waterFace(bx, by, bz, 4);
        if (isAirForWaterSide(bx, by, bz - 1)) waterFace(bx, by, bz, 5);
    }

    /** Rysuje X-krzyzowy mesh dla tall_grass i wheat (jak w MC). */
    void addCrossFaces(int bx, int by, int bz, int id) {
        int tile = tileFor(id, 0);
        double u0 = atlasU0(tile), u1 = atlasU1(tile);
        double v0 = atlasV0(), v1 = atlasV1();
        setFixedFunctionLightmapCoords(bx, by, bz);
        glColor4f(1f, 1f, 1f, 1f);
        double x0 = bx, x1 = bx + 1, z0 = bz, z1 = bz + 1;
        double y0 = by, y1 = by + 1;
        // Pierwszy diagonal (NW-SE) - dwustronnie
        glTexCoord2d(u0,v1); glVertex3d(x0,y0,z0); glTexCoord2d(u1,v1); glVertex3d(x1,y0,z1);
        glTexCoord2d(u1,v0); glVertex3d(x1,y1,z1); glTexCoord2d(u0,v0); glVertex3d(x0,y1,z0);
        glTexCoord2d(u0,v1); glVertex3d(x1,y0,z1); glTexCoord2d(u1,v1); glVertex3d(x0,y0,z0);
        glTexCoord2d(u1,v0); glVertex3d(x0,y1,z0); glTexCoord2d(u0,v0); glVertex3d(x1,y1,z1);
        // Drugi diagonal (NE-SW) - dwustronnie
        glTexCoord2d(u0,v1); glVertex3d(x1,y0,z0); glTexCoord2d(u1,v1); glVertex3d(x0,y0,z1);
        glTexCoord2d(u1,v0); glVertex3d(x0,y1,z1); glTexCoord2d(u0,v0); glVertex3d(x1,y1,z0);
        glTexCoord2d(u0,v1); glVertex3d(x0,y0,z1); glTexCoord2d(u1,v1); glVertex3d(x1,y0,z0);
        glTexCoord2d(u1,v0); glVertex3d(x1,y1,z0); glTexCoord2d(u0,v0); glVertex3d(x0,y1,z1);
    }

    boolean isWater(int x, int y, int z) {
        return inWorld(x, y, z) && (world[x][y][z] & 0xff) == WATER;
    }

    boolean isAirForWaterSide(int x, int y, int z) {
        return !inWorld(x, y, z) || (world[x][y][z] & 0xff) == AIR;
    }

    boolean faceVisible(int id, int nx, int ny, int nz) {
        if (!inWorld(nx, ny, nz)) return true;
        int other = world[nx][ny][nz] & 0xff;
        if (other == AIR) return true;
        if (id == WATER && other == WATER) return false;
        if (id == WATER) return true;
        if (other == WATER) return true;
        if (other == DOOR_BOTTOM || other == DOOR_TOP || other == CHEST) return true;
        if (id == LEAVES && other == LEAVES) return false;
        // tall_grass i wheat sa "cross" - inne bloki widoczne za nimi
        if (other == TALL_GRASS || other == WHEAT_0 || other == WHEAT_1 || other == WHEAT_2 || other == WHEAT_3) return true;
        return false;
    }

    void face(int x, int y, int z, int id, int dir) {
        if (id == WATER) { waterFace(x, y, z, dir); return; }
        int tile = tileFor(id, dir);
        double u0 = atlasU0(tile);
        double u1 = atlasU1(tile);
        double v0 = atlasV0(), v1 = atlasV1();
        // BlockModelRenderer directional diffuse values: DOWN=.5, UP=1,
        // NORTH/SOUTH=.8 and WEST/EAST=.6.
        float dirShade = dir == 2 ? 1.0f : dir == 3 ? 0.5f : dir < 2 ? 0.6f : 0.8f;
        // Sample light z sasiada po stronie ktora patrzy face (blok POWIETRZA obok)
        int nx = x, ny = y, nz = z;
        switch (dir) {
            case 0: nx = x + 1; break;
            case 1: nx = x - 1; break;
            case 2: ny = y + 1; break;
            case 3: ny = y - 1; break;
            case 4: nz = z + 1; break;
            case 5: nz = z - 1; break;
        }
        // Fixed-function unit 1 supplies dynamic MCP sky/block light. Vertex
        // colour only carries directional diffuse shading and local AO.
        setFixedFunctionLightmapCoords(nx, ny, nz);
        float light = dirShade;
        switch (dir) {
            case 0:
                faceVtx(u0,v1,x+1,y,z+1,light,x,y,z,dir); faceVtx(u1,v1,x+1,y,z,light,x,y,z,dir); faceVtx(u1,v0,x+1,y+1,z,light,x,y,z,dir); faceVtx(u0,v0,x+1,y+1,z+1,light,x,y,z,dir); break;
            case 1:
                faceVtx(u0,v1,x,y,z,light,x,y,z,dir); faceVtx(u1,v1,x,y,z+1,light,x,y,z,dir); faceVtx(u1,v0,x,y+1,z+1,light,x,y,z,dir); faceVtx(u0,v0,x,y+1,z,light,x,y,z,dir); break;
            case 2:
                faceVtx(u0,v0,x,y+1,z,light,x,y,z,dir); faceVtx(u1,v0,x+1,y+1,z,light,x,y,z,dir); faceVtx(u1,v1,x+1,y+1,z+1,light,x,y,z,dir); faceVtx(u0,v1,x,y+1,z+1,light,x,y,z,dir); break;
            case 3:
                faceVtx(u0,v1,x,y,z+1,light,x,y,z,dir); faceVtx(u1,v1,x+1,y,z+1,light,x,y,z,dir); faceVtx(u1,v0,x+1,y,z,light,x,y,z,dir); faceVtx(u0,v0,x,y,z,light,x,y,z,dir); break;
            case 4:
                faceVtx(u0,v1,x,y,z+1,light,x,y,z,dir); faceVtx(u1,v1,x+1,y,z+1,light,x,y,z,dir); faceVtx(u1,v0,x+1,y+1,z+1,light,x,y,z,dir); faceVtx(u0,v0,x,y+1,z+1,light,x,y,z,dir); break;
            case 5:
                faceVtx(u0,v1,x+1,y,z,light,x,y,z,dir); faceVtx(u1,v1,x,y,z,light,x,y,z,dir); faceVtx(u1,v0,x,y+1,z,light,x,y,z,dir); faceVtx(u0,v0,x+1,y+1,z,light,x,y,z,dir); break;
        }
    }

    void faceVtx(double u, double v, double vx, double vy, double vz, float light,
                 int blockX, int blockY, int blockZ, int dir) {
        vtx(u, v, vx, vy, vz, light,
                ambientOcclusion(blockX, blockY, blockZ, dir, vx, vy, vz));
    }

    /**
     * Corner AO from the two edge neighbours and their diagonal. This is the
     * fixed-function equivalent of BlockModelRenderer.AmbientOcclusionFace;
     * unlike the old constant diagonal it reacts to actual surrounding cubes.
     */
    float ambientOcclusion(int x, int y, int z, int dir, double vx, double vy, double vz) {
        if (!buildingTerrainMesh) return 1.0f;
        int dx = vx <= x ? -1 : 1;
        int dy = vy <= y ? -1 : 1;
        int dz = vz <= z ? -1 : 1;
        boolean sideA, sideB, corner;
        if (dir == 0 || dir == 1) {
            int ox = x + (dir == 0 ? 1 : -1);
            sideA = occludesAo(ox, y + dy, z);
            sideB = occludesAo(ox, y, z + dz);
            corner = occludesAo(ox, y + dy, z + dz);
        } else if (dir == 2 || dir == 3) {
            int oy = y + (dir == 2 ? 1 : -1);
            sideA = occludesAo(x + dx, oy, z);
            sideB = occludesAo(x, oy, z + dz);
            corner = occludesAo(x + dx, oy, z + dz);
        } else {
            int oz = z + (dir == 4 ? 1 : -1);
            sideA = occludesAo(x + dx, y, oz);
            sideB = occludesAo(x, y + dy, oz);
            corner = occludesAo(x + dx, y + dy, oz);
        }
        int obstruction = (sideA ? 1 : 0) + (sideB ? 1 : 0) + (corner ? 1 : 0);
        if (sideA && sideB) obstruction = 3;
        return 1.0f - obstruction * 0.2f;
    }

    boolean occludesAo(int x, int y, int z) {
        if (y < 0 || x < 0 || z < 0 || x >= WORLD_X || z >= WORLD_Z) return true;
        if (y >= WORLD_Y) return false;
        int id = world[x][y][z] & 0xff;
        return id == GRASS || id == DIRT || id == STONE || id == WOOD
                || id == SAND || id == PLANKS || id == CRAFTING_TABLE;
    }

    void vtx(double u, double v, double x, double y, double z, float light, float shade) {
        float l = light * shade;
        glColor3f(l, l, l);
        glTexCoord2d(u, v);
        glVertex3d(x, y, z);
    }

    void waterFace(int x, int y, int z, int dir) {
        boolean animated = gameRenderer.getWaterTexId() > 0;
        int tile = 11;
        double u0 = animated ? 0.0 : atlasU0(tile);
        double u1 = animated ? 1.0 : atlasU1(tile);
        double v0 = animated ? 0.0 : atlasV0();
        double v1 = animated ? 1.0 : atlasV1();
        double top = y + (isWater(x, y + 1, z) ? 1.0 : 0.88);
        float dirShade = dir == 2 ? 1.0f : dir == 3 ? 0.5f : dir < 2 ? 0.6f : 0.8f;
        int nxw = x, nyw = y, nzw = z;
        switch (dir) {
            case 0: nxw = x + 1; break;
            case 1: nxw = x - 1; break;
            case 2: nyw = y + 1; break;
            case 3: nyw = y - 1; break;
            case 4: nzw = z + 1; break;
            case 5: nzw = z - 1; break;
        }
        setFixedFunctionLightmapCoords(nxw, nyw, nzw);
        float light = dirShade;
        // BlockColors WATER_COLOR (0x3F76E4) multiplied with vanilla
        // water_still.png, exactly as the 1.12 translucent block layer.
        glColor4f((0x3f / 255.0f) * light,
                (0x76 / 255.0f) * light,
                (0xe4 / 255.0f) * light, 1.0f);
        switch (dir) {
            case 0:
                glTexCoord2d(u0,v1); glVertex3d(x+1,y,z+1); glTexCoord2d(u1,v1); glVertex3d(x+1,y,z); glTexCoord2d(u1,v0); glVertex3d(x+1,top,z); glTexCoord2d(u0,v0); glVertex3d(x+1,top,z+1); break;
            case 1:
                glTexCoord2d(u0,v1); glVertex3d(x,y,z); glTexCoord2d(u1,v1); glVertex3d(x,y,z+1); glTexCoord2d(u1,v0); glVertex3d(x,top,z+1); glTexCoord2d(u0,v0); glVertex3d(x,top,z); break;
            case 2:
                glTexCoord2d(u0,v0); glVertex3d(x,top,z); glTexCoord2d(u1,v0); glVertex3d(x+1,top,z); glTexCoord2d(u1,v1); glVertex3d(x+1,top,z+1); glTexCoord2d(u0,v1); glVertex3d(x,top,z+1); break;
            case 3:
                glTexCoord2d(u0,v1); glVertex3d(x,y,z+1); glTexCoord2d(u1,v1); glVertex3d(x+1,y,z+1); glTexCoord2d(u1,v0); glVertex3d(x+1,y,z); glTexCoord2d(u0,v0); glVertex3d(x,y,z); break;
            case 4:
                glTexCoord2d(u0,v1); glVertex3d(x,y,z+1); glTexCoord2d(u1,v1); glVertex3d(x+1,y,z+1); glTexCoord2d(u1,v0); glVertex3d(x+1,top,z+1); glTexCoord2d(u0,v0); glVertex3d(x,top,z+1); break;
            case 5:
                glTexCoord2d(u0,v1); glVertex3d(x+1,y,z); glTexCoord2d(u1,v1); glVertex3d(x,y,z); glTexCoord2d(u1,v0); glVertex3d(x,top,z); glTexCoord2d(u0,v0); glVertex3d(x+1,top,z); break;
        }
    }

    int tileFor(int id, int dir) { return craft3dgl.world.BlockTextures.tileFor(id, dir); }

    // Minecraft reach: survival blocks 4.5, creative blocks 5.0; melee 3.0 / 5.0.
    double blockReach() { return gameMode == GAMEMODE_CREATIVE ? 5.0 : 4.5; }
    double entityReach() { return gameMode == GAMEMODE_CREATIVE ? 5.0 : 3.0; }

    Hit castRay(double maxDist) {
        double cp = Math.cos(pitch);
        double dx = Math.sin(yaw) * cp;
        double dy = Math.sin(pitch);
        double dz = Math.cos(yaw) * cp;
        return craft3dgl.physics.RayCaster.cast(x, y + eyeHeight(), z, dx, dy, dz, maxDist, rayWorld);
    }

    /** Usuwa drzwi stojace bez podloza i tworzy pojedynczy drop. */
    void removeUnsupportedDoorAbove(int supportX, int supportY, int supportZ) {
        int lowerY = supportY + 1;
        if (!inWorld(supportX, lowerY, supportZ)
                || (world[supportX][lowerY][supportZ] & 0xff) != DOOR_BOTTOM
                || isNormalCubeForDoor(supportX, supportY, supportZ)) return;

        world[supportX][lowerY][supportZ] = (byte)AIR;
        removeDoorMeta(supportX, lowerY, supportZ);
        markDirtyAround(supportX, lowerY, supportZ);
        if (inWorld(supportX, lowerY + 1, supportZ)
                && (world[supportX][lowerY + 1][supportZ] & 0xff) == DOOR_TOP) {
            world[supportX][lowerY + 1][supportZ] = (byte)AIR;
            removeDoorMeta(supportX, lowerY + 1, supportZ);
            markDirtyAround(supportX, lowerY + 1, supportZ);
        }
        spawnDrop(supportX + 0.5, lowerY + 0.15, supportZ + 0.5, DOOR_BOTTOM, 1);
    }

    void setBlock(int bx, int by, int bz, int id) {
        if (!inWorld(bx, by, bz)) return;
        int old = world[bx][by][bz] & 0xff;
        if (old == CHEST && id != CHEST) {
            int[] ids = chestIds.get(packChestKey(bx, by, bz));
            int[] cnts = chestCounts.get(packChestKey(bx, by, bz));
            if (ids != null && cnts != null) {
                for (int i = 0; i < CHEST_SIZE; i++) {
                    if (ids[i] > 0 && cnts[i] > 0) spawnDrop(bx + 0.5, by + 0.55, bz + 0.5, ids[i], cnts[i]);
                }
            }
            removeChest(bx, by, bz);
            if (chestOpen && chestOpenX == bx && chestOpenY == by && chestOpenZ == bz) closeChest();
        }
        if ((old == DOOR_BOTTOM || old == DOOR_TOP) && id != old) {
            int partnerY = old == DOOR_BOTTOM ? by + 1 : by - 1;
            if (inWorld(bx, partnerY, bz)) {
                int p = world[bx][partnerY][bz] & 0xff;
                if (p == DOOR_BOTTOM || p == DOOR_TOP) {
                    world[bx][partnerY][bz] = (byte) AIR;
                    removeDoorMeta(bx, partnerY, bz);
                    markDirtyAround(bx, partnerY, bz);
                }
            }
            removeDoorMeta(bx, by, bz);
        }
        world[bx][by][bz] = (byte) id;
        // Every chest is a tile entity, including a newly placed empty chest.
        // Register it immediately so the ModelChest renderer does not depend on
        // opening the container once before it becomes visible.
        if (id == CHEST && old != CHEST) {
            chestIdsAt(bx, by, bz);
            chestCountsAt(bx, by, bz);
        }
        // BlockDoor.neighborChanged: po utracie pelnego podloza obie polowki
        // znikaja, a dolna polowka upuszcza dokladnie jeden item drzwi.
        if (id != old) removeUnsupportedDoorAbove(bx, by, bz);

        // FIX: gdy niszczymy blok pod rosliną (tall_grass, wheat), roslinka tez znika (drop)
        // W MC to sie nazywa "block update" - roslina wymaga podloza pod soba
        if (id == AIR && inWorld(bx, by + 1, bz)) {
            int above = world[bx][by + 1][bz] & 0xff;
            if (above == TALL_GRASS || above == WHEAT_0 || above == WHEAT_1 || above == WHEAT_2 || above == WHEAT_3) {
                // Drop wheat seeds gdy tall_grass zniszczona
                if (above == TALL_GRASS && random.nextInt(8) == 0) {
                    spawnDrop(bx + 0.5, by + 1.15, bz + 0.5, ITEM_SEEDS, 1);
                }
                // Drop wheat + seeds gdy dojrzale wheat
                if (above == WHEAT_3) {
                    spawnDrop(bx + 0.5, by + 1.15, bz + 0.5, ITEM_WHEAT, 1);
                    if (random.nextInt(2) == 0) spawnDrop(bx + 0.5, by + 1.15, bz + 0.5, ITEM_SEEDS, 1);
                }
                world[bx][by + 1][bz] = (byte) AIR;
                markDirtyAround(bx, by + 1, bz);
            }
        }
        markDirtyAround(bx, by, bz);
        if (lightEngine != null) lightEngine.rebuildRegion(bx - 8, bz - 8, bx + 8, bz + 8);
        if (id == AIR) {
            scheduleWater(bx, by, bz);
            scheduleWater(bx, by + 1, bz);
            scheduleWater(bx + 1, by, bz);
            scheduleWater(bx - 1, by, bz);
            scheduleWater(bx, by, bz + 1);
            scheduleWater(bx, by, bz - 1);
        } else if (id == WATER) {
            scheduleWater(bx, by - 1, bz);
            scheduleWater(bx + 1, by, bz);
            scheduleWater(bx - 1, by, bz);
            scheduleWater(bx, by, bz + 1);
            scheduleWater(bx, by, bz - 1);
        }
    }

    void setBlockRaw(int bx, int by, int bz, int id) {
        if (inWorld(bx, by, bz)) world[bx][by][bz] = (byte) id;
    }

    void seedWaterQueue() {
        // Seed only exposed water near the player. Queueing five neighbours for
        // every ocean block allocated millions of tiny arrays during loading.
        waterSim.clear();
        int radius = 7 * CHUNK;
        int minX = Math.max(1, (int)Math.floor(x) - radius);
        int maxX = Math.min(WORLD_X - 2, (int)Math.floor(x) + radius);
        int minZ = Math.max(1, (int)Math.floor(z) - radius);
        int maxZ = Math.min(WORLD_Z - 2, (int)Math.floor(z) + radius);
        for (int bx = minX; bx <= maxX; bx++) for (int bz = minZ; bz <= maxZ; bz++) {
            if (!generatedColumns[bx / CHUNK][bz / CHUNK]) continue;
            for (int by = 1; by < WORLD_Y - 1; by++) {
                if ((world[bx][by][bz] & 0xff) != WATER) continue;
                if ((world[bx][by - 1][bz] & 0xff) == AIR) scheduleWater(bx, by - 1, bz);
                if ((world[bx + 1][by][bz] & 0xff) == AIR) scheduleWater(bx + 1, by, bz);
                if ((world[bx - 1][by][bz] & 0xff) == AIR) scheduleWater(bx - 1, by, bz);
                if ((world[bx][by][bz + 1] & 0xff) == AIR) scheduleWater(bx, by, bz + 1);
                if ((world[bx][by][bz - 1] & 0xff) == AIR) scheduleWater(bx, by, bz - 1);
            }
        }
    }

    void scheduleWater(int x, int y, int z) {
        waterSim.schedule(x, y, z, WORLD_X, WORLD_Y, WORLD_Z);
    }

    void tickWaterAcc(double dt) {
        waterSim.tickAcc(dt, (x, y, z) -> {
            if (!inWorld(x, y, z)) return false;
            int id = world[x][y][z] & 0xff;
            if (id != AIR) return false;
            boolean fromAbove = inWorld(x, y + 1, z) && (world[x][y + 1][z] & 0xff) == WATER;
            boolean fromSide =
                    (inWorld(x + 1, y, z) && (world[x + 1][y][z] & 0xff) == WATER) ||
                    (inWorld(x - 1, y, z) && (world[x - 1][y][z] & 0xff) == WATER) ||
                    (inWorld(x, y, z + 1) && (world[x][y][z + 1] & 0xff) == WATER) ||
                    (inWorld(x, y, z - 1) && (world[x][y][z - 1] & 0xff) == WATER);
            if (!fromAbove && !fromSide) return false;
            world[x][y][z] = (byte) WATER;
            markDirtyAround(x, y, z);
            scheduleWater(x, y - 1, z);
            scheduleWater(x + 1, y, z);
            scheduleWater(x - 1, y, z);
            scheduleWater(x, y, z + 1);
            scheduleWater(x, y, z - 1);
            return true;
        });
    }

    void tickWheatGrowth(double dt) {
        farming.tick(dt, x, y, z, WORLD_X, WORLD_Y, WORLD_Z, random, (bx, by, bz) -> {
            int id = world[bx][by][bz] & 0xff;
            if (id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2) {
                setBlock(bx, by, bz, id + 1);
                return true;
            }
            return false;
        });
    }

    void markAllChunksDirty() {
        // Uncreated columns must not allocate OpenGL terrain buffers. The old
        // implementation marked the whole 1024x1024 world and caused a native
        // 0xC0000005 crash immediately after creating a world on Windows.
        for (int cx = 0; cx < CHUNKS_X; cx++) for (int cz = 0; cz < CHUNKS_Z; cz++) {
            if (!generatedColumns[cx][cz]) continue;
            for (int cy = 0; cy < CHUNKS_Y; cy++) {
                chunks[cx][cy][cz].dirty = true;
                chunks[cx][cy][cz].modernDirty = true;
            }
        }
    }

    void markDirtyAround(int bx, int by, int bz) {
        markChunk(bx / CHUNK, by / CHUNK, bz / CHUNK);
        if (bx % CHUNK == 0) markChunk(bx / CHUNK - 1, by / CHUNK, bz / CHUNK);
        if (bx % CHUNK == CHUNK - 1) markChunk(bx / CHUNK + 1, by / CHUNK, bz / CHUNK);
        if (by % CHUNK == 0) markChunk(bx / CHUNK, by / CHUNK - 1, bz / CHUNK);
        if (by % CHUNK == CHUNK - 1) markChunk(bx / CHUNK, by / CHUNK + 1, bz / CHUNK);
        if (bz % CHUNK == 0) markChunk(bx / CHUNK, by / CHUNK, bz / CHUNK - 1);
        if (bz % CHUNK == CHUNK - 1) markChunk(bx / CHUNK, by / CHUNK, bz / CHUNK + 1);
    }

    void markChunk(int cx, int cy, int cz) {
        if (cx >= 0 && cy >= 0 && cz >= 0 && cx < CHUNKS_X && cy < CHUNKS_Y && cz < CHUNKS_Z) { chunks[cx][cy][cz].dirty = true; chunks[cx][cy][cz].modernDirty = true; }
    }

    /** Czy obok gracza jest solid land (do wyskoku z wody). */
    boolean isNextToLand() {
        // Sprawdz 4 kierunki na wysokosci nog + 1 blok wyzej (do wskoczenia)
        double r = PLAYER_RADIUS + 0.1;
        int by = (int) Math.floor(y);
        int byUp = by + 1;
        double[][] dirs = {{r,0},{-r,0},{0,r},{0,-r}};
        for (double[] d : dirs) {
            int bx = (int) Math.floor(x + d[0]);
            int bz = (int) Math.floor(z + d[1]);
            // Land = solidny blok obok, i AIR nad nim (miejsce na wskoczenie)
            if (solid(bx, by, bz) && !solid(bx, byUp, bz)) return true;
        }
        return false;
    }

    /** Czy w podanym world-position jest woda (blok w tej pozycji). */
    boolean isWaterAt(double wx, double wy, double wz) {
        int bx = (int) Math.floor(wx);
        int by = (int) Math.floor(wy);
        int bz = (int) Math.floor(wz);
        if (!inWorld(bx, by, bz)) return false;
        return (world[bx][by][bz] & 0xff) == WATER;
    }

    /** Bloki, ktorych gorna sciana moze podpierac drzwi (MC: isTopSolid). */
    boolean isNormalCubeForDoor(int bx, int by, int bz) {
        if (!inWorld(bx, by, bz)) return false;
        int id = world[bx][by][bz] & 0xff;
        return id == GRASS || id == DIRT || id == STONE || id == WOOD
                || id == LEAVES || id == SAND || id == PLANKS
                || id == CRAFTING_TABLE;
    }

    /** Materialy replaceable, ktore ItemDoor moze zastapic. */
    boolean isReplaceableForDoor(int id) {
        return id == AIR || id == WATER || id == TALL_GRASS
                || id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3;
    }

    boolean solid(int bx, int by, int bz) {
        if (by < 0) return true;
        if (bx < 0 || bz < 0 || bx >= WORLD_X || bz >= WORLD_Z) return true;
        if (by >= WORLD_Y) return false;
        int id = world[bx][by][bz] & 0xff;
        // Drzwi maja czesciowy AABB 3/16, obslugiwany osobno przez fizyke.
        if (id == DOOR_BOTTOM || id == DOOR_TOP) return false;
        // Trawka i pszenica - przejscie przez nie (jak w MC)
        if (id == TALL_GRASS || id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3) return false;
        return id != AIR && id != LEAVES && id != WATER;
    }

    boolean inWorld(int bx, int by, int bz) {
        return bx >= 0 && by >= 0 && bz >= 0 && bx < WORLD_X && by < WORLD_Y && bz < WORLD_Z;
    }

    int hash(int a, int b) {
        int h = a * 73428767 ^ b * 9122719;
        h ^= h >>> 13;
        h *= 1274126177;
        return h;
    }

    double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
    int clampInt(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }

    void writeCrashLog(Throwable t) { craft3dgl.save.CrashLogger.write(t); }

    // ============================================================
    // ====== SKRZYNIE - POPRAWIONA OBSLUGA ======
    // ============================================================

    void openChest(int bx, int by, int bz) {
        chestOpen = true;
        chestOpenX = bx; chestOpenY = by; chestOpenZ = bz;
        java.util.Arrays.fill(openChestIds, 0);
        java.util.Arrays.fill(openChestCounts, 0);
        long clickedKey = packChestKey(bx, by, bz);
        long pairKey = adjacentChestKey(bx, by, bz);
        // InventoryLargeChest has stable left/right halves regardless of which
        // physical half the player activates. West/north comes first here.
        openChestPrimaryKey = clickedKey;
        openChestSecondaryKey = pairKey;
        if (pairKey != Long.MIN_VALUE) {
            int pairX = ChestStorage.unpackX(pairKey), pairZ = ChestStorage.unpackZ(pairKey);
            if (pairX < bx || (pairX == bx && pairZ < bz)) {
                openChestPrimaryKey = pairKey;
                openChestSecondaryKey = clickedKey;
            }
        }
        openChestSlots = pairKey == Long.MIN_VALUE ? CHEST_SIZE : CHEST_SIZE * 2;
        int fx = ChestStorage.unpackX(openChestPrimaryKey);
        int fy = ChestStorage.unpackY(openChestPrimaryKey);
        int fz = ChestStorage.unpackZ(openChestPrimaryKey);
        int[] firstIds = chestIdsAt(fx, fy, fz);
        int[] firstCounts = chestCountsAt(fx, fy, fz);
        System.arraycopy(firstIds, 0, openChestIds, 0, CHEST_SIZE);
        System.arraycopy(firstCounts, 0, openChestCounts, 0, CHEST_SIZE);
        if (openChestSecondaryKey != Long.MIN_VALUE) {
            int sx = ChestStorage.unpackX(openChestSecondaryKey);
            int sy = ChestStorage.unpackY(openChestSecondaryKey);
            int sz = ChestStorage.unpackZ(openChestSecondaryKey);
            System.arraycopy(chestIdsAt(sx, sy, sz), 0, openChestIds, CHEST_SIZE, CHEST_SIZE);
            System.arraycopy(chestCountsAt(sx, sy, sz), 0, openChestCounts, CHEST_SIZE, CHEST_SIZE);
        }
        mouseCaptured = false;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        // Zablokuj E zeby otwarcie skrzyni nie zlapal sie razem z eWasDown=true.
        eWasDown = true;
        escWasDown = true;
        chestMouseWasDown = true;  // wazne - inaczej od razu zarejestrowalibysmy klikniecie
        rightWasDown = true;
        sound.playChestOpen();
    }

    void closeChest() {
        syncOpenChestInventory();
        chestOpen = false;
        if (cursorId > 0 && cursorCount > 0) {
            if (!addItem(cursorId, cursorCount)) spawnDrop(x, y + 1.2, z, cursorId, cursorCount);
            cursorId = 0; cursorCount = 0;
        }
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        eWasDown = true;
        escWasDown = true;
        leftWasDown = true;
        rightWasDown = true;
        sound.playChestClose();
    }

    void syncOpenChestInventory() {
        if (openChestPrimaryKey == Long.MIN_VALUE) return;
        int px = ChestStorage.unpackX(openChestPrimaryKey);
        int py = ChestStorage.unpackY(openChestPrimaryKey);
        int pz = ChestStorage.unpackZ(openChestPrimaryKey);
        System.arraycopy(openChestIds, 0, chestIdsAt(px, py, pz), 0, CHEST_SIZE);
        System.arraycopy(openChestCounts, 0, chestCountsAt(px, py, pz), 0, CHEST_SIZE);
        if (openChestSecondaryKey != Long.MIN_VALUE) {
            int sx = ChestStorage.unpackX(openChestSecondaryKey);
            int sy = ChestStorage.unpackY(openChestSecondaryKey);
            int sz = ChestStorage.unpackZ(openChestSecondaryKey);
            System.arraycopy(openChestIds, CHEST_SIZE, chestIdsAt(sx, sy, sz), 0, CHEST_SIZE);
            System.arraycopy(openChestCounts, CHEST_SIZE, chestCountsAt(sx, sy, sz), 0, CHEST_SIZE);
        }
    }

    void handleChestInput(boolean left, boolean right) {
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        int mx = (int) mxA[0], my = (int) myA[0];
        boolean eNow = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        boolean escNow = glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS;
        if (escNow && !escWasDown) { closeChest(); return; }
        if (eNow && !eWasDown) { closeChest(); return; }
        boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
        // Same MCP 9.40 slot coordinates as ChestUIRenderer/ContainerChest.
        int slot = craft3dgl.ui.ChestUIRenderer.SLOT_PITCH;
        int rows = openChestSlots / 9;
        int startX = craft3dgl.ui.ChestUIRenderer.startX(width);
        int chestY = craft3dgl.ui.ChestUIRenderer.chestY(height, rows);
        int invY = craft3dgl.ui.ChestUIRenderer.invY(height, rows);
        int hotY = craft3dgl.ui.ChestUIRenderer.hotY(height, rows);
        int[] cIds = openChestIds;
        int[] cCnts = openChestCounts;

        // PPM + drag = rozdzielanie po 1
        if (right && cursorId > 0 && cursorCount > 0) {
            if (!rightWasDown) dragSlots.clear();
            int idx = chestSlotAt(mx, my, startX, chestY, slot, invY, hotY);
            int kind = chestSlotKind(mx, my, startX, chestY, slot, invY, hotY);
            if (idx >= 0 && kind >= 0) {
                long key = packSlotKey(kind, idx);
                if (!dragSlots.contains(key)) {
                    dragSlots.add(key);
                    distributeIntoChestSlot(kind, idx, cIds, cCnts);
                }
            }
            return;
        }
        if (!right) dragSlots.clear();
        boolean leftClick = left && !leftWasDown;
        boolean rightClick = right && !rightWasDown;
        if (!leftClick && !rightClick) return;
        int hoverIdx = chestSlotAt(mx, my, startX, chestY, slot, invY, hotY);
        int hoverKind = chestSlotKind(mx, my, startX, chestY, slot, invY, hotY);
        if (hoverIdx < 0) return;
        int[] tgtIds = hoverKind == 0 ? cIds : invId;
        int[] tgtCnts = hoverKind == 0 ? cCnts : invCount;
        int item = tgtIds[hoverIdx];

        if (leftClick && shift && item > 0) {
            if (hoverKind == 0) quickMove(cIds, cCnts, hoverIdx, invId, invCount, 0, INVENTORY_SIZE);
            else quickMove(invId, invCount, hoverIdx, cIds, cCnts, 0, openChestSlots);
            return;
        }

        long now = System.currentTimeMillis();
        if (leftClick && cursorId > 0 && lastClickItem == cursorId && now - lastClickTime < 350) {
            collectAllInto(cursorId, cIds, cCnts);
            collectAllInto(cursorId, invId, invCount);
            lastClickTime = 0;
            return;
        }
        if (leftClick) {
            lastClickTime = now;
            lastClickItem = item > 0 ? item : cursorId;
        }
        if (hoverKind == 2) selectedSlot = hoverIdx;
        clickStack(tgtIds, tgtCnts, hoverIdx, rightClick);
    }

    int chestSlotAt(int mx, int my, int startX, int chestY, int slot, int invY, int hotY) {
        int size = craft3dgl.ui.ChestUIRenderer.SLOT_SIZE;
        for (int row = 0; row < openChestSlots / 9; row++) for (int col = 0; col < 9; col++) {
            int sx = startX + col * slot, sy = chestY + row * slot;
            if (inside(mx, my, sx, sy, size, size)) return row * 9 + col;
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int sx = startX + col * slot, sy = invY + row * slot;
            if (inside(mx, my, sx, sy, size, size)) return 9 + row * 9 + col;
        }
        for (int col = 0; col < 9; col++) {
            int sx = startX + col * slot;
            if (inside(mx, my, sx, hotY, size, size)) return col;
        }
        return -1;
    }

    int chestSlotKind(int mx, int my, int startX, int chestY, int slot, int invY, int hotY) {
        int size = craft3dgl.ui.ChestUIRenderer.SLOT_SIZE;
        for (int row = 0; row < openChestSlots / 9; row++) for (int col = 0; col < 9; col++) {
            int sx = startX + col * slot, sy = chestY + row * slot;
            if (inside(mx, my, sx, sy, size, size)) return 0;
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int sx = startX + col * slot, sy = invY + row * slot;
            if (inside(mx, my, sx, sy, size, size)) return 1;
        }
        for (int col = 0; col < 9; col++) {
            int sx = startX + col * slot;
            if (inside(mx, my, sx, hotY, size, size)) return 2;
        }
        return -1;
    }

    void distributeIntoChestSlot(int kind, int idx, int[] cIds, int[] cCnts) {
        if (cursorId <= 0 || cursorCount <= 0) return;
        int[] tIds = kind == 0 ? cIds : invId;
        int[] tCnts = kind == 0 ? cCnts : invCount;
        if (tIds[idx] == 0 || tCnts[idx] == 0) {
            tIds[idx] = cursorId;
            tCnts[idx] = 1;
            cursorCount--;
        } else if (tIds[idx] == cursorId && tCnts[idx] < maxStack(cursorId)) {
            tCnts[idx]++;
            cursorCount--;
        }
        if (cursorCount <= 0) { cursorId = 0; cursorCount = 0; dragging = false; }
    }

    void quickMove(int[] srcIds, int[] srcCnts, int srcIdx, int[] tgtIds, int[] tgtCnts, int tStart, int tEnd) {
        int id = srcIds[srcIdx];
        int cnt = srcCnts[srcIdx];
        if (id <= 0 || cnt <= 0) return;
        int max = maxStack(id);
        for (int i = tStart; i < tEnd && cnt > 0; i++) {
            if (tgtIds[i] == id && tgtCnts[i] < max) {
                int add = Math.min(cnt, max - tgtCnts[i]);
                tgtCnts[i] += add;
                cnt -= add;
            }
        }
        for (int i = tStart; i < tEnd && cnt > 0; i++) {
            if (tgtIds[i] == 0 || tgtCnts[i] <= 0) {
                tgtIds[i] = id;
                int add = Math.min(cnt, max);
                tgtCnts[i] = add;
                cnt -= add;
            }
        }
        srcCnts[srcIdx] = cnt;
        if (cnt <= 0) srcIds[srcIdx] = 0;
    }

    void collectAllInto(int id, int[] sIds, int[] sCnts) {
        if (cursorId != id) return;
        int max = maxStack(id);
        for (int i = 0; i < sIds.length && cursorCount < max; i++) {
            if (sIds[i] == id && sCnts[i] > 0) {
                int add = Math.min(sCnts[i], max - cursorCount);
                cursorCount += add;
                sCnts[i] -= add;
                if (sCnts[i] <= 0) sIds[i] = 0;
            }
        }
    }

    void drawChestUI() {
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        translationSys.setLanguage(language);
        craft3dgl.ui.ChestUIRenderer.draw(fontRenderer, this::drawStackIcon, translationSys,
                width, height, (int)mxA[0], (int)myA[0],
                openChestIds, openChestCounts, openChestSlots,
                invId, invCount, selectedSlot, cursorId, cursorCount);
    }

    void drawChests() {
        int openX = (chestOpen || chestLidProgress > 0f) ? chestOpenX : -1;
        int openY = (chestOpen || chestLidProgress > 0f) ? chestOpenY : -1;
        int openZ = (chestOpen || chestLidProgress > 0f) ? chestOpenZ : -1;
        craft3dgl.world.ChestRenderer.drawAll(world, chestIds.keySet(), chestFacings, CHEST,
                x, z, 5 * CHUNK, openX, openY, openZ, chestLidProgress,
                lightEngine, currentDayMult);
        org.lwjgl.opengl.GL20.glUseProgram(0);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
        glColor4f(1f, 1f, 1f, 1f);
    }

    void drawDoors() {
        // Night vision: podbij dayMult zeby drzwi tez byly jasniejsze
        float doorDayMult = hasNightVision() ? Math.max(currentDayMult, 0.85f) : currentDayMult;
        craft3dgl.world.DoorRenderer.drawAll(world, doorSystem, x, z, textureAtlas, 5 * CHUNK, lightEngine, doorDayMult);
    }


    // ====== CHAT + KOMENDY ======
    void openChat(boolean withSlash) {
        chatOpen = true;
        chatInput.setLength(0);
        if (withSlash) chatInput.append('/');
        chatCursor = chatInput.length();
        chatSelection = chatCursor;
        chatScroll = 0;
        sentHistoryCursor = sentChatHistory.size();
        chatHistoryDraft = "";
        resetChatCompletion();
        mouseCaptured = false;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    void closeChat(boolean send) {
        if (send) { submitChat(); return; }
        chatOpen = false;
        chatInput.setLength(0);
        chatCursor = 0;
        chatSelection = 0;
        resetChatCompletion();
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    void submitChat() {
        String text = chatInput.toString().trim();
        if (!text.isEmpty() && (sentChatHistory.isEmpty()
                || !text.equals(sentChatHistory.get(sentChatHistory.size() - 1)))) {
            sentChatHistory.add(text);
            if (sentChatHistory.size() > 100) sentChatHistory.remove(0);
        }
        chatInput.setLength(0);
        chatCursor = 0;
        chatSelection = 0;
        chatOpen = false;
        resetChatCompletion();
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (text.isEmpty()) return;
        if (text.startsWith("/")) handleCommand(text);
        else addChatMessage("<Player> " + text);
    }

    void navigateChatHistory(int direction) {
        int size = sentChatHistory.size();
        if (size == 0) return;
        if (sentHistoryCursor == size && direction < 0) chatHistoryDraft = chatInput.toString();
        sentHistoryCursor = Math.max(0, Math.min(size, sentHistoryCursor + direction));
        String value = sentHistoryCursor == size ? chatHistoryDraft : sentChatHistory.get(sentHistoryCursor);
        chatInput.setLength(0);
        chatInput.append(value);
        chatCursor = chatInput.length();
        chatSelection = chatCursor;
        resetChatCompletion();
    }

    String selectedChatText() {
        if (chatCursor == chatSelection) return "";
        int from = Math.min(chatCursor, chatSelection);
        int to = Math.max(chatCursor, chatSelection);
        return chatInput.substring(from, to);
    }

    boolean deleteChatSelection() {
        if (chatCursor == chatSelection) return false;
        int from = Math.min(chatCursor, chatSelection);
        int to = Math.max(chatCursor, chatSelection);
        chatInput.delete(from, to);
        chatCursor = from;
        chatSelection = from;
        resetChatCompletion();
        return true;
    }

    void insertChatText(String text) {
        String clean = text.replace('\n', ' ').replace('\r', ' ');
        deleteChatSelection();
        int room = 256 - chatInput.length();
        if (room <= 0) return;
        if (clean.length() > room) clean = clean.substring(0, room);
        chatInput.insert(chatCursor, clean);
        chatCursor += clean.length();
        chatSelection = chatCursor;
        resetChatCompletion();
    }

    void resetChatCompletion() {
        chatCompletions = java.util.Collections.emptyList();
        chatCompletionIndex = -1;
    }

    void completeChatInput() {
        if (chatCompletions.isEmpty()) {
            String original = chatInput.toString();
            chatCompletions = craft3dgl.commands.ChatCommands.complete(original);
            chatCompletionIndex = -1;
            if (chatCompletions.size() > 1) {
                String common = chatCompletions.get(0);
                for (int i = 1; i < chatCompletions.size(); i++) {
                    String candidate = chatCompletions.get(i);
                    int n = 0, max = Math.min(common.length(), candidate.length());
                    while (n < max && common.charAt(n) == candidate.charAt(n)) n++;
                    common = common.substring(0, n);
                }
                if (common.length() > original.length()) {
                    chatInput.setLength(0);
                    chatInput.append(common);
                    chatCursor = chatInput.length();
                    chatSelection = chatCursor;
                    return;
                }
            }
        }
        if (chatCompletions.isEmpty()) return;
        chatCompletionIndex = (chatCompletionIndex + 1) % chatCompletions.size();
        String value = chatCompletions.get(chatCompletionIndex);
        chatInput.setLength(0);
        chatInput.append(value);
        chatCursor = chatInput.length();
        chatSelection = chatCursor;
    }

    void addChatMessage(String msg) {
        chatLog.add(new ChatMessage(msg));
        if (chatLog.size() > 60) chatLog.remove(0);
    }

    void handleCommand(String cmd) {
        craft3dgl.commands.ChatCommands.handle(cmd, new craft3dgl.commands.ChatCommands.CommandContext() {
            @Override public void addChatMessage(String msg) { MinecraftGL.this.addChatMessage(msg); }
            @Override public void setGameMode(int mode) { MinecraftGL.this.setGameMode(mode); }
            @Override public void toggleFly() {
                flying = !flying;
                addChatMessage("Latanie: " + (flying ? "ON" : "OFF"));
            }
            @Override public boolean isCreative() { return gameMode == GAMEMODE_CREATIVE; }
            @Override public void teleport(double tx, double ty, double tz) {
                x = tx; y = ty; z = tz; velY = 0;
            }
            @Override public boolean giveItem(int id, int count) { return addItem(id, count); }
            @Override public int getFps() { return fps; }
            @Override public void rebuildVillages() { rebuildNearbyVillages(); }
            @Override public boolean applyEffect(String type, int seconds) {
                if (type.equals("night_vision") || type.equals("nv") || type.equals("nightvision")) {
                    nightVisionExpireMs = System.currentTimeMillis() + seconds * 1000L;
                    return true;
                }
                return false;
            }
            @Override public void clearEffects() {
                nightVisionExpireMs = 0L;
            }
            @Override public boolean setTime(String value) {
                try {
                    long ticks;
                    if ("day".equalsIgnoreCase(value)) ticks = 1000L;
                    else if ("night".equalsIgnoreCase(value)) ticks = 13000L;
                    else ticks = Long.parseLong(value);
                    long inDay = ((ticks % 24000L) + 24000L) % 24000L;
                    double dayFraction = (inDay / 24000.0 + 0.25) % 1.0;
                    gameTime = dayFraction * 1200.0;
                    return true;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
            @Override public boolean setWeather(String value) {
                return weather.setWeather(value);
            }
        });
    }

    int parseItemName(String name) { return ItemNames.parseItemName(name); }


    void setGameMode(int mode) {
        gameMode = mode;
        if (mode == GAMEMODE_CREATIVE) {
            health = maxHealth;
            hunger = 20;
        } else {
            flying = false;
        }
    }

    // ====== CREATIVE ======
    void openCreativeInv() {
        creativeInvOpen = true;
        creativeTab = 0;
        creativeScroll = 0;
        creativeSearch.setLength(0);
        mouseCaptured = false;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    void closeCreativeInv() {
        creativeInvOpen = false;
        if (cursorId > 0 && cursorCount > 0) {
            cursorId = 0; cursorCount = 0;
        }
        mouseCaptured = true;
        firstMouse = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
    }

    int[] creativeItemsForTab() {
        int[] all = CREATIVE_ITEMS;
        int[] blocks = {GRASS, DIRT, STONE, SAND, WOOD, PLANKS, LEAVES, CRAFTING_TABLE, DOOR_BOTTOM, CHEST, WATER, FARMLAND, TALL_GRASS};
        int[] tools = {ITEM_STICK, ITEM_WOOD_PICKAXE, ITEM_STONE_PICKAXE, ITEM_WOOD_AXE, ITEM_STONE_AXE, ITEM_WOOD_SHOVEL, ITEM_STONE_SHOVEL, ITEM_WOOD_SWORD, ITEM_STONE_SWORD, ITEM_WOOD_HOE, ITEM_STONE_HOE};
        int[] food = {ITEM_PORK, ITEM_BEEF, ITEM_MUTTON, ITEM_BREAD, ITEM_WHEAT};
        int[] base = creativeTab == 1 ? blocks : creativeTab == 2 ? tools : creativeTab == 3 ? food : all;
        String q = creativeSearch.toString().trim().toLowerCase();
        if (q.isEmpty()) return base;
        int[] tmp = new int[base.length];
        int n = 0;
        for (int id : base) if (itemName(id).toLowerCase().contains(q)) tmp[n++] = id;
        int[] out = new int[n];
        System.arraycopy(tmp, 0, out, 0, n);
        return out;
    }

    void handleCreativeInvInput(boolean left, boolean right) {
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        int mx = (int) mxA[0], my = (int) myA[0];
        boolean eNow = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        boolean back = glfwGetKey(window, GLFW_KEY_BACKSPACE) == GLFW_PRESS;
        if (back && !backspaceWasDown && creativeSearch.length() > 0) creativeSearch.deleteCharAt(creativeSearch.length() - 1);
        backspaceWasDown = back;
        if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS && !escWasDown) { closeCreativeInv(); return; }
        if (eNow && !eWasDown) { closeCreativeInv(); return; }
        // Uzywamy pozycji z CreativeUIRenderer (ten sam layout co draw)
        int slot = craft3dgl.ui.CreativeUIRenderer.SLOT_PITCH;
        int tabWSmall = craft3dgl.ui.CreativeUIRenderer.tabWidthSmall();
        int tabHSmall = craft3dgl.ui.CreativeUIRenderer.tabHeightSmall();
        int panelYFinal = craft3dgl.ui.CreativeUIRenderer.panelY(height);
        int tabY = panelYFinal - tabHSmall + 4;
        // Kliki w male tabs (4 tabs z ikonami nad panelem)
        if (left && !leftWasDown) {
            for (int i = 0; i < 4; i++) {
                int tx = craft3dgl.ui.CreativeUIRenderer.tabX(width, i);
                if (inside(mx, my, tx, tabY, tabWSmall, tabHSmall)) {
                    creativeTab = i; creativeScroll = 0; creativeSearch.setLength(0);
                    return;
                }
            }
        }
        // Grid slotow 9x5 = 45
        int gridX = craft3dgl.ui.CreativeUIRenderer.gridX(width);
        int gridY = craft3dgl.ui.CreativeUIRenderer.gridY(height);
        int[] items = creativeItemsForTab();
        if (left && !leftWasDown) {
            for (int i = 0; i < 45 && i < items.length; i++) {
                int col = i % 9;
                int row = i / 9;
                int sx = gridX + col * slot;
                int sy = gridY + row * slot;
                if (inside(mx, my, sx, sy, slot, slot)) {
                    cursorId = items[i]; cursorCount = maxStack(cursorId);
                    return;
                }
            }
        }
        // Hotbar (9 slotow na dole)
        int invX = craft3dgl.ui.CreativeUIRenderer.invX(width);
        int invY = craft3dgl.ui.CreativeUIRenderer.invY(height);
        // Trash slot
        int trashX = craft3dgl.ui.CreativeUIRenderer.trashX(width);
        int trashY = craft3dgl.ui.CreativeUIRenderer.trashY(height);
        int trashW = craft3dgl.ui.CreativeUIRenderer.TRASH_W;
        if (left && !leftWasDown && inside(mx, my, trashX, trashY, trashW, trashW)) {
            boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
            if (shift) {
                for (int i = 0; i < INVENTORY_SIZE; i++) { invId[i] = 0; invCount[i] = 0; }
                for (int i = 0; i < equipId.length; i++) { equipId[i] = 0; equipCount[i] = 0; }
                cursorId = 0; cursorCount = 0;
            } else {
                cursorId = 0; cursorCount = 0;
            }
            return;
        }
        // Kliki w hotbar
        if (left && !leftWasDown) {
            for (int col = 0; col < 9; col++) {
                int sx = invX + col * slot;
                if (inside(mx, my, sx, invY, slot, slot)) {
                    // Kliknac na slot hotbar - jesli mamy cursor item, wsadz do slotu
                    if (cursorId > 0 && cursorCount > 0) {
                        invId[col] = cursorId;
                        invCount[col] = cursorCount;
                        cursorId = 0; cursorCount = 0;
                    } else if (invId[col] > 0) {
                        // Pusty cursor - podnies item ze slota
                        cursorId = invId[col];
                        cursorCount = invCount[col];
                        invId[col] = 0; invCount[col] = 0;
                    }
                    selectedSlot = col;
                    return;
                }
            }
        }
        // Prawy klik na slot = wyrzuc jeden item z cursor
        if (right && cursorId > 0 && cursorCount > 0) {
            for (int col = 0; col < 9; col++) {
                int sx = invX + col * slot;
                if (inside(mx, my, sx, invY, slot, slot)) {
                    if (invId[col] == 0 || (invId[col] == cursorId && invCount[col] < maxStack(cursorId))) {
                        invId[col] = cursorId;
                        invCount[col] = (invId[col] == cursorId ? invCount[col] + 1 : 1);
                        return;
                    }
                }
            }
        }
    }

    void handleCreativeInventorySlots(int mx, int my, boolean left, boolean right, int invX, int invY, int slot) {
        if (right && cursorId > 0 && cursorCount > 0) {
            if (!rightWasDown) dragSlots.clear();
            int idx = creativeInventoryIndexAt(mx, my, invX, invY, slot);
            if (idx >= 0) {
                long key = idx;
                if (!dragSlots.contains(key)) { dragSlots.add(key); depositOne(invId, invCount, idx); }
            }
            return;
        }
        if (!right) dragSlots.clear();
        if (left && !leftWasDown) {
            int idx = creativeInventoryIndexAt(mx, my, invX, invY, slot);
            if (idx >= 0) { clickStack(invId, invCount, idx, false); if (idx < HOTBAR_SIZE) selectedSlot = idx; }
        }
        if (right && !rightWasDown && (cursorId == 0 || cursorCount <= 0)) {
            int idx = creativeInventoryIndexAt(mx, my, invX, invY, slot);
            if (idx >= 0) clickStack(invId, invCount, idx, true);
        }
    }

    int creativeInventoryIndexAt(int mx, int my, int invX, int invY, int slot) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = 9 + row * 9 + col;
            if (inside(mx, my, invX + col * slot, invY + row * slot, 40, 40)) return idx;
        }
        int hotY = invY + 160;
        for (int col = 0; col < 9; col++) if (inside(mx, my, invX + col * slot, hotY, 40, 40)) return col;
        return -1;
    }

    void depositOne(int[] ids, int[] counts, int idx) {
        if (cursorId <= 0 || cursorCount <= 0) return;
        if (ids[idx] == 0 || counts[idx] <= 0) { ids[idx] = cursorId; counts[idx] = 1; cursorCount--; }
        else if (ids[idx] == cursorId && counts[idx] < maxStack(cursorId)) { counts[idx]++; cursorCount--; }
        if (cursorCount <= 0) { cursorId = 0; cursorCount = 0; }
    }

    void drawCreativeInvUI() {
        double[] mxA = new double[1], myA = new double[1];
        glfwGetCursorPos(window, mxA, myA);
        translationSys.setLanguage(language);
        craft3dgl.ui.CreativeUIRenderer.draw(fontRenderer, this::drawStackIcon, translationSys,
                width, height, (int)mxA[0], (int)myA[0],
                creativeItemsForTab(), creativeTab, creativeSearch.toString(),
                invId, invCount, selectedSlot, cursorId, cursorCount);
    }

    void drawChatUI() {
        long now = System.currentTimeMillis();
        final int fontScale = 2;
        final int lineHeight = 9 * fontScale;
        final int chatWidth = Math.min(width - 4, 320 * fontScale);
        final int maxLines = chatOpen ? Math.max(10, (height - 48) / lineHeight) : 10;
        int bottom = height - 40;
        int newest = chatLog.size() - 1 - (chatOpen ? chatScroll : 0);
        int shown = 0;
        for (int i = newest; i >= 0 && shown < maxLines; i--) {
            ChatMessage message = chatLog.get(i);
            long age = now - message.shownAt;
            if (!chatOpen && age >= 10000L) continue;
            float alpha = 1f;
            if (!chatOpen) {
                float remaining = 1f - age / 10000f;
                remaining = Math.max(0f, Math.min(1f, remaining * 10f));
                alpha = remaining * remaining;
            }
            int lineY = bottom - (shown + 1) * lineHeight;
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(0f, 0f, 0f, 0.5f * alpha);
            quad(2, lineY, chatWidth, lineHeight);
            fontRenderer.drawVanillaText(message.text, 4, lineY + fontScale, fontScale, alpha);
            shown++;
        }

        if (chatOpen) {
            // GuiChat's GuiTextField: a simple translucent black strip at the
            // bottom, no custom title, frame, prompt, or neon decoration.
            int inputY = height - 24;
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(0f, 0f, 0f, 0.5f);
            quad(2, inputY, width - 4, 20);

            int maxChars = Math.max(1, (width - 12) / (6 * fontScale));
            int start = Math.max(0, chatCursor - maxChars + 1);
            int end = Math.min(chatInput.length(), start + maxChars);
            String visible = chatInput.substring(start, end);
            int selectionFrom = Math.max(start, Math.min(chatCursor, chatSelection));
            int selectionTo = Math.min(end, Math.max(chatCursor, chatSelection));
            if (selectionTo > selectionFrom) {
                int selectionX = 4 + craft3dgl.ui.FontRenderer.mcTextWidth(
                        chatInput.substring(start, selectionFrom), fontScale);
                int selectionW = craft3dgl.ui.FontRenderer.mcTextWidth(
                        chatInput.substring(selectionFrom, selectionTo), fontScale);
                glDisable(GL_TEXTURE_2D);
                glColor4f(0.25f, 0.40f, 1f, 0.65f);
                quad(selectionX, inputY + 1, selectionW, 17);
            }
            fontRenderer.drawVanillaText(visible, 4, inputY + 2, fontScale, 1f);
            if ((now / 500L) % 2L == 0L) {
                int cursorChars = Math.max(0, Math.min(chatCursor - start, visible.length()));
                int cursorX = 4 + craft3dgl.ui.FontRenderer.mcTextWidth(
                        visible.substring(0, cursorChars), fontScale);
                fontRenderer.drawVanillaText("_", cursorX, inputY + 2, fontScale, 1f);
            }

            if (chatCompletions.size() > 1) {
                StringBuilder options = new StringBuilder();
                for (int i = 0; i < chatCompletions.size() && i < 8; i++) {
                    if (i > 0) options.append("  ");
                    options.append(chatCompletions.get(i));
                }
                int suggestionY = inputY - lineHeight;
                glDisable(GL_TEXTURE_2D);
                glColor4f(0f, 0f, 0f, 0.5f);
                quad(2, suggestionY, chatWidth, lineHeight);
                fontRenderer.drawVanillaText(options.toString(), 4, suggestionY + fontScale,
                        fontScale, 1f);
            }
        }
        glColor4f(1f, 1f, 1f, 1f);
    }

    void drawParticles() {
        particleSystem.draw(textureAtlas, particleWorld);
    }

    /** One manager owns both particle simulation and rendering. */
    final ParticleSystem particleSystem = new ParticleSystem();

    
    
    void cleanup() {
        for (int cx = 0; cx < CHUNKS_X; cx++) for (int cy = 0; cy < CHUNKS_Y; cy++) for (int cz = 0; cz < CHUNKS_Z; cz++) {
            Chunk c = chunks[cx][cy][cz];
            if (c != null) {
                if (c.modernVboSolid != null) c.modernVboSolid.delete();
                if (c.modernVboCutout != null) c.modernVboCutout.delete();
                if (c.modernVboTranslucent != null) c.modernVboTranslucent.delete();
            }
        }
        // Delete GL resources while the context is still current.
        gameRenderer.cleanup();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    static final class ChatMessage {
        final String text;
        final long shownAt;
        ChatMessage(String text) { this.text = text; this.shownAt = System.currentTimeMillis(); }
    }

    // Particle przeniesiony do craft3dgl.entities.Particle

    static final class Chunk {
        boolean dirty = true;
        // Minecraft 1.12-style fixed-function VBO cache per render layer.
        craft3dgl.blaze3d.vertex.VertexBuffer modernVboSolid;
        int modernVertexCountSolid;
        craft3dgl.blaze3d.vertex.VertexBuffer modernVboCutout;
        int modernVertexCountCutout;
        craft3dgl.blaze3d.vertex.VertexBuffer modernVboTranslucent;
        int modernVertexCountTranslucent;
        boolean modernDirty = true;
    }

    // class Hit przeniesiona do craft3dgl.combat.Hit

    /** Block/material access used by MCP-style per-tick particle physics. */
    final ParticleSystem.WorldAccess particleWorld = new ParticleSystem.WorldAccess() {
        @Override public boolean isSolid(double px, double py, double pz) {
            int bx = (int)Math.floor(px);
            int by = (int)Math.floor(py);
            int bz = (int)Math.floor(pz);
            if (by < 0 || bx < 0 || bz < 0 || bx >= WORLD_X || bz >= WORLD_Z) return true;
            if (by >= WORLD_Y) return false;
            int id = world[bx][by][bz] & 0xff;
            if (isDoor(id)) {
                double[] box = DoorSystem.doorBox(getDoorMeta(bx, by, bz));
                double lx = px - bx;
                double ly = py - by;
                double lz = pz - bz;
                return lx >= box[0] && lx <= box[3] && ly >= box[1] && ly <= box[4]
                        && lz >= box[2] && lz <= box[5];
            }
            return id != AIR && id != WATER && id != TALL_GRASS
                    && id != WHEAT_0 && id != WHEAT_1 && id != WHEAT_2 && id != WHEAT_3;
        }

        @Override public boolean isWater(double px, double py, double pz) {
            return isWaterAt(px, py, pz);
        }

        @Override public float brightness(double px, double py, double pz) {
            if (lightEngine == null) return 1.0f;
            int bx = clampInt((int)Math.floor(px), 0, WORLD_X - 1);
            int by = clampInt((int)Math.floor(py), 0, WORLD_Y - 1);
            int bz = clampInt((int)Math.floor(pz), 0, WORLD_Z - 1);
            return lightEngine.sampleShade(bx, by, bz, currentDayMult);
        }
    };

    /** Adapter RayCastera, lacznie z dokladnym selection boxem drzwi. */
    final craft3dgl.physics.RayCaster.WorldAccessor rayWorld = new craft3dgl.physics.RayCaster.WorldAccessor() {
        @Override public boolean inWorld(int x, int y, int z) { return MinecraftGL.this.inWorld(x, y, z); }
        @Override public int getBlock(int x, int y, int z) { return world[x][y][z] & 0xff; }
        @Override public double[] getSelectionBounds(int x, int y, int z) {
            int id = world[x][y][z] & 0xff;
            return MinecraftGL.this.isDoor(id)
                    ? DoorSystem.doorBox(getDoorMeta(x, y, z)) : null;
        }
    };
}
