package craft3dgl.world;

import craft3dgl.items.ItemRegistry;

import static craft3dgl.world.WorldConstants.*;

/**
 * Mechanika kopania: hardness bloków + szybkość narzędzi.
 */
public final class MiningMechanics {
    private MiningMechanics() {}

    /** Twardość bloku - im więcej, tym dłużej trzeba kopać. */
    public static double hardness(int block) {
        if (block == LEAVES) return 0.25;
        if (block == TALL_GRASS) return 0.05;
        if (block == WHEAT_0 || block == WHEAT_1 || block == WHEAT_2 || block == WHEAT_3) return 0.10;
        if (block == DIRT || block == GRASS || block == SAND || block == FARMLAND) return 0.55;
        if (block == WOOD || block == PLANKS || block == CRAFTING_TABLE) return 1.0;
        return 1.8;
    }

    /** Szybkość kopania dla danego bloku i trzymanego narzędzia. */
    public static double miningSpeed(int block, int toolItem) {
        int bCat = ItemRegistry.blockCategory(block);
        int tCat = ItemRegistry.toolCategory(toolItem);
        int tier = ItemRegistry.toolTier(toolItem);

        // Kamień - tylko kilof normalnie kopie
        if (block == STONE) {
            if (tCat == 1) {
                if (tier == 2) return 5.0;
                if (tier == 1) return 3.0;
            }
            return 0.40;
        }

        // Pasujace narzedzie - przyspieszenie
        if (tCat == bCat && bCat > 0) {
            if (tier == 2) return 4.2;  // kamienne
            if (tier == 1) return 2.8;  // drewniane
        }

        return 1.0;  // bez bonusu
    }
}
