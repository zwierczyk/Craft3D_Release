package craft3dgl.world;

/**
 * Stałe świata: ID bloków, itemów, rozmiary, biomy.
 * Wyciągnięte z MinecraftGL żeby inne pakiety mogły referencjonować.
 */
public final class WorldConstants {
    private WorldConstants() {}

    // === Rozmiary świata ===
    public static final int WORLD_X = 1024;
    public static final int WORLD_Y = 64;
    public static final int WORLD_Z = 1024;
    public static final int CHUNK = 16;
    public static final int WATER_LEVEL = 24;

    // === Block IDs ===
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
    public static final int GLASS = 31;
    public static final int GLASS_PANE = 32;

    // Farming blocks
    public static final int FARMLAND = 50;
    public static final int TALL_GRASS = 51;
    public static final int WHEAT_0 = 52;
    public static final int WHEAT_1 = 53;
    public static final int WHEAT_2 = 54;
    public static final int WHEAT_3 = 55;

    // === Item IDs ===
    public static final int ITEM_STICK = 9;
    public static final int ITEM_WOOD_PICKAXE = 10;
    public static final int ITEM_STONE_PICKAXE = 11;
    public static final int ITEM_PORK = 12;
    public static final int ITEM_BEEF = 13;
    public static final int ITEM_MUTTON = 14;
    public static final int ITEM_WOOD_AXE = 19;
    public static final int ITEM_STONE_AXE = 20;
    public static final int ITEM_WOOD_SHOVEL = 21;
    public static final int ITEM_STONE_SHOVEL = 22;
    public static final int ITEM_WOOD_SWORD = 23;
    public static final int ITEM_STONE_SWORD = 24;
    public static final int ITEM_EMERALD = 25;
    public static final int ITEM_BREAD = 26;
    public static final int ITEM_SEEDS = 27;
    public static final int ITEM_WHEAT = 28;
    public static final int ITEM_WOOD_HOE = 29;
    public static final int ITEM_STONE_HOE = 30;

    // === Biomy ===
    public static final int BIOME_PLAINS = 0;
    public static final int BIOME_FOREST = 1;
    public static final int BIOME_DESERT = 2;
    public static final int BIOME_MOUNTAINS = 3;
    public static final int BIOME_OCEAN = 4;
    public static final int BIOME_BEACH = 5;
    public static final int BIOME_RIVER = 6;

    // === Inwentarz ===
    public static final int HOTBAR_SIZE = 9;
    public static final int INVENTORY_SIZE = 36;

    /** Czy id reprezentuje blok stawialny (nie item). */
    public static boolean isBlockItem(int id) {
        return (id >= GRASS && id <= CRAFTING_TABLE) || id == DOOR_BOTTOM || id == CHEST || id == GLASS || id == GLASS_PANE;
    }
}
