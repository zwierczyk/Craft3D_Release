package craft3dgl.world;

/**
 * Generator wiosek: definicja domków + sprawdzanie obecności wioski w grid cell.
 */
public final class VillageGenerator {
    private VillageGenerator() {}

    /** Definicje 5 domków per wioska: {dx, dz, w, d, type} - offset od centrum wioski. */
    public static final int[][] HOUSE_DEFS = {
        {-17, -15, 7, 7, 0},
        { 14, -13, 8, 7, 1},
        {-18,  14, 7, 8, 0},
        { 13,  14, 7, 7, 1},
        { -4,  23, 8, 7, 0}
    };

    public static final int VILLAGE_GRID = 80;          // co ile bloków grid komórki
    public static final double VILLAGE_CHANCE = 260 / 1024.0;  // ~25% szans na wioskę w komórce

    /** Czy istnieje wioska w danej komórce gridu (deterministycznie z seed-a). */
    public static boolean villageExists(int gx, int gz, long seed) {
        int h = BiomeGenerator.hash(gx * 928371 + (int)seed, gz * 12377 + (int)(seed >>> 32));
        return (h & 1023) < 260;
    }

    /** Środek wioski (offset od grid corner). */
    public static int[] villageCenter(int gx, int gz, long seed) {
        int h = BiomeGenerator.hash(gx ^ (int)seed, gz ^ (int)(seed >>> 32));
        int ox = 18 + Math.abs(h & 31);
        int oz = 18 + Math.abs((h >> 8) & 31);
        return new int[]{gx * VILLAGE_GRID + ox, gz * VILLAGE_GRID + oz};
    }
}
