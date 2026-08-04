package craft3dgl.items;

import static craft3dgl.world.WorldConstants.*;

/**
 * Centralna rejestracja itemów: max stack, kategorie narzędzi, tiery, dropy.
 */
public final class ItemRegistry {
    private ItemRegistry() {}

    /** Maksymalna ilość w slocie. */
    public static int maxStack(int id) {
        if (id == ITEM_WOOD_PICKAXE || id == ITEM_STONE_PICKAXE) return 1;
        if (id == ITEM_WOOD_AXE || id == ITEM_STONE_AXE) return 1;
        if (id == ITEM_WOOD_SHOVEL || id == ITEM_STONE_SHOVEL) return 1;
        if (id == ITEM_WOOD_SWORD || id == ITEM_STONE_SWORD) return 1;
        if (id == ITEM_WOOD_HOE || id == ITEM_STONE_HOE) return 1;
        return 64;
    }

    /** Czy item jest stawialnym blokiem (delegator do WorldConstants). */
    public static boolean isBlockItem(int id) {
        return craft3dgl.world.WorldConstants.isBlockItem(id);
    }

    /** Drop dla zniszczonego bloku. */
    public static int dropForBlock(int block) {
        if (block == WATER) return AIR;
        if (block == GRASS) return DIRT;
        if (block == DOOR_BOTTOM || block == DOOR_TOP) return DOOR_BOTTOM;
        if (block == FARMLAND) return DIRT;
        if (block == TALL_GRASS) return AIR;
        if (block == WHEAT_0 || block == WHEAT_1 || block == WHEAT_2 || block == WHEAT_3) return AIR;
        return block;
    }

    /** Kategoria bloku: 1=pickaxe (stone), 2=shovel (dirt/sand), 3=axe (wood/planks), 0=brak. */
    public static int blockCategory(int block) {
        if (block == STONE || block == CRAFTING_TABLE) return 1;
        if (block == DIRT || block == GRASS || block == SAND) return 2;
        if (block == WOOD || block == PLANKS || block == DOOR_BOTTOM || block == DOOR_TOP || block == CHEST) return 3;
        return 0;
    }

    /** Kategoria narzędzia: 1=pickaxe, 2=shovel, 3=axe, 4=sword, 5=hoe, 0=brak. */
    public static int toolCategory(int item) {
        if (item == ITEM_WOOD_PICKAXE || item == ITEM_STONE_PICKAXE) return 1;
        if (item == ITEM_WOOD_SHOVEL || item == ITEM_STONE_SHOVEL) return 2;
        if (item == ITEM_WOOD_AXE || item == ITEM_STONE_AXE) return 3;
        if (item == ITEM_WOOD_SWORD || item == ITEM_STONE_SWORD) return 4;
        if (item == ITEM_WOOD_HOE || item == ITEM_STONE_HOE) return 5;
        return 0;
    }

    /** Tier narzędzia: 1=drewno, 2=kamień, 0=brak. */
    public static int toolTier(int item) {
        if (item == ITEM_WOOD_PICKAXE || item == ITEM_WOOD_AXE || item == ITEM_WOOD_SHOVEL
                || item == ITEM_WOOD_SWORD || item == ITEM_WOOD_HOE) return 1;
        if (item == ITEM_STONE_PICKAXE || item == ITEM_STONE_AXE || item == ITEM_STONE_SHOVEL
                || item == ITEM_STONE_SWORD || item == ITEM_STONE_HOE) return 2;
        return 0;
    }
}
