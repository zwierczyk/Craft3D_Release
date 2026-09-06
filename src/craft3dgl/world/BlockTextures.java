package craft3dgl.world;

import static craft3dgl.world.WorldConstants.*;

/**
 * Mapowanie ID bloku + kierunek ściany na indeks tile w atlasie tekstur.
 */
public final class BlockTextures {
    private BlockTextures() {}

    /** Texture selected by the Minecraft block model's \"particle\" entry. */
    public static int particleTileFor(int id) {
        // Vanilla grass and farmland models deliberately use dirt for fragments.
        if (id == GRASS || id == FARMLAND) return 2;
        return tileFor(id, 0);
    }

    /** Direction: 0=+X, 1=-X, 2=+Y(top), 3=-Y(bottom), 4=+Z, 5=-Z */
    public static int tileFor(int id, int dir) {
        if (id == GRASS) return dir == 2 ? 0 : dir == 3 ? 2 : 1;
        if (id == DIRT) return 2;
        if (id == STONE) return 3;
        if (id == WOOD) return dir == 2 || dir == 3 ? 5 : 4;
        if (id == LEAVES) return 6;
        if (id == SAND) return 7;
        if (id == PLANKS) return 8;
        if (id == CRAFTING_TABLE) return dir == 2 ? 9 : 10;
        if (id == WATER) return 11;
        if (id == DOOR_BOTTOM) return 12;
        if (id == DOOR_TOP) return 13;
        if (id == CHEST) return dir == 2 || dir == 3 ? 14 : 15;
        if (id == FARMLAND) return dir == 2 ? 16 : 2;
        if (id == TALL_GRASS) return 17;
        if (id == WHEAT_0) return 18;
        if (id == WHEAT_1) return 19;
        if (id == WHEAT_2) return 20;
        if (id == WHEAT_3) return 21;
        return 3;
    }
}
