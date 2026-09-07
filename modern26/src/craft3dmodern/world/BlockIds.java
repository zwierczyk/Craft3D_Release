package craft3dmodern.world;

import java.util.HashMap;
import java.util.Map;

/** Prosty rejestr blokow (id nazw) uzywanych przez M3. */
public final class BlockIds {
    public static final int AIR = 0;
    public static final int STONE = 1;
    public static final int DIRT = 2;
    public static final int SAND = 3;
    public static final int COBBLESTONE = 4;
    public static final int OAK_PLANKS = 5;
    public static final int GRASS_BLOCK = 6;
    public static final int OAK_LOG = 7;
    public static final int OAK_LEAVES = 8;

    /** kolor tintu trawy (plains) */
    public static final int TINT_GRASS = 0xFF91BD59;
    /** kolor tintu lisci (foliage, plains) */
    public static final int TINT_FOLIAGE = 0xFF6AA234;

    private static final String[] NAMES = {
        "air", "stone", "dirt", "sand", "cobblestone", "oak_planks",
        "grass_block", "oak_log", "oak_leaves"
    };
    private static final Map<String, Integer> BY_NAME = new HashMap<String, Integer>();
    private static final int[] TINTS = new int[NAMES.length];

    static {
        for (int i = 0; i < NAMES.length; i++) BY_NAME.put(NAMES[i], i);
        TINTS[GRASS_BLOCK] = TINT_GRASS;
        TINTS[OAK_LEAVES] = TINT_FOLIAGE;
    }

    private BlockIds() {}

    public static int count() {
        return NAMES.length;
    }

    public static int id(String name) {
        Integer i = BY_NAME.get(name);
        return i == null ? AIR : i.intValue();
    }

    public static String name(int id) {
        if (id < 0 || id >= NAMES.length) return "air";
        return NAMES[id];
    }

    /** Kazdy nie-powietrzny blok zaslania sasiednie sciany (M3: brak transparentnych poza air). */
    public static boolean occludes(int id) {
        return id != AIR;
    }

    /** Tint bloku (0 = bialy / brak) wg nazwy bloku; tekstura szara dostaje kolor. */
    public static int tintRgb(int id) {
        if (id < 0 || id >= NAMES.length) return 0xFFFFFF;
        int t = TINTS[id];
        return t == 0 ? 0xFFFFFF : t;
    }
}
