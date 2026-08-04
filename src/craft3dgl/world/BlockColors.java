package craft3dgl.world;

import static craft3dgl.world.WorldConstants.*;

/**
 * Prezentacyjne kolory bloków - używane do particles (okruchów), map, minimap itp.
 * Bazowe kolory tekstur, nie sample z atlasu.
 */
public final class BlockColors {
    private BlockColors() {}

    /** Zwraca RGB [r,g,b] w [0,1] dla danego bloku. */
    public static float[] colorFor(int block) {
        switch (block) {
            case GRASS: return new float[]{0.31f, 0.68f, 0.24f};
            case DIRT: return new float[]{0.48f, 0.30f, 0.18f};
            case STONE: return new float[]{0.47f, 0.48f, 0.51f};
            case WOOD: return new float[]{0.55f, 0.34f, 0.16f};
            case LEAVES: return new float[]{0.15f, 0.56f, 0.24f};
            case SAND: return new float[]{0.86f, 0.77f, 0.48f};
            case PLANKS: return new float[]{0.66f, 0.43f, 0.22f};
            case CRAFTING_TABLE: return new float[]{0.61f, 0.39f, 0.19f};
            case WATER: return new float[]{0.24f, 0.47f, 0.85f};
            case DOOR_BOTTOM:
            case DOOR_TOP: return new float[]{0.65f, 0.42f, 0.20f};
            case CHEST: return new float[]{0.61f, 0.43f, 0.21f};
            case FARMLAND: return new float[]{0.35f, 0.22f, 0.14f};
            case TALL_GRASS: return new float[]{0.31f, 0.67f, 0.22f};
            case WHEAT_0: return new float[]{0.31f, 0.67f, 0.22f};
            case WHEAT_1: return new float[]{0.44f, 0.72f, 0.22f};
            case WHEAT_2: return new float[]{0.62f, 0.72f, 0.22f};
            case WHEAT_3: return new float[]{0.91f, 0.76f, 0.28f};
            default: return new float[]{0.5f, 0.5f, 0.5f};
        }
    }
}
