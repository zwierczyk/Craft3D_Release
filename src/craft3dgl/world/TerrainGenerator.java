package craft3dgl.world;

import static craft3dgl.world.WorldConstants.*;

/**
 * Generator terenu v3 - obsluguje nowe biomy (OCEAN, BEACH, RIVER) + jaskinie + wodospady.
 */
public final class TerrainGenerator {
    private TerrainGenerator() {}

    /** Wypelnij jedna kolumne terenu na podstawie height + biome. */
    public static void fillColumn(byte[][][] world, int wx, int wz, int h, int biome) {
        boolean sandy = biome == BIOME_DESERT || biome == BIOME_BEACH || (biome == BIOME_OCEAN && h > WATER_LEVEL - 6);
        boolean underwater = biome == BIOME_OCEAN || biome == BIOME_RIVER;
        for (int wy = 0; wy <= h; wy++) {
            if (wy == 0) world[wx][wy][wz] = STONE;
            else if (wy == h) {
                if (underwater) world[wx][wy][wz] = (byte)(h < WATER_LEVEL - 4 ? DIRT : SAND);
                else if (sandy) world[wx][wy][wz] = SAND;
                else world[wx][wy][wz] = GRASS;
            }
            else if (wy >= h - 3) {
                if (underwater && h < WATER_LEVEL - 4) world[wx][wy][wz] = DIRT;
                else if (sandy) world[wx][wy][wz] = SAND;
                else world[wx][wy][wz] = DIRT;
            }
            else world[wx][wy][wz] = STONE;
        }
        if (h < WATER_LEVEL) {
            for (int wy = h + 1; wy <= WATER_LEVEL && wy < WORLD_Y; wy++) world[wx][wy][wz] = WATER;
        }
    }

    /** Jaskinie: worm tunnels + big caverns + shallow pockets. */
    public static void carveCaves(byte[][][] world, int wx, int wz, int surface, long seed) {
        int maxY = Math.min(surface - 3, WORLD_Y - 1);
        int minY = 2;

        for (int wy = minY; wy <= maxY; wy++) {
            // Worm tunnels - dlugie tunele
            double wormA = BiomeGenerator.fbm(wx * 0.045, wz * 0.045 + wy * 0.08, seed + 3000, 2);
            double wormB = BiomeGenerator.fbm(wx * 0.045 + wy * 0.10, wz * 0.045 + 500, seed + 4000, 2);
            double wormDist = Math.abs(wormA - 0.5) + Math.abs(wormB - 0.5);
            boolean inWorm = wormDist < 0.10;

            // Duze jaskinie
            double cavernNoise = BiomeGenerator.fbm(wx * 0.035, wz * 0.035 + wy * 0.05, seed + 5500 + wy * 7, 3);
            double cavernDepth = 1.0 - Math.abs(wy - 10.0) / 12.0;
            if (cavernDepth < 0) cavernDepth = 0;
            boolean inCavern = cavernNoise > (0.78 - cavernDepth * 0.15);

            // Shallow pockets
            double pocketNoise = BiomeGenerator.fbm(wx * 0.085, wz * 0.085 + wy * 0.12, seed + 6000, 2);
            boolean inPocket = pocketNoise > 0.82 && wy > surface - 10 && wy < surface - 4;

            if (inWorm || inCavern || inPocket) {
                world[wx][wy][wz] = AIR;
            }
        }
    }

    /** Wodospady - wypelnij jaskinie woda gdy pod water level. */
    public static void addWaterfalls(byte[][][] world, int wx, int wz, int surface) {
        if (surface >= WATER_LEVEL) return;
        for (int wy = WATER_LEVEL - 1; wy >= 1; wy--) {
            int id = world[wx][wy][wz] & 0xff;
            if (id == AIR) world[wx][wy][wz] = WATER;
        }
    }

    /** Bedrock warstwa Y=0-1. */
    public static void placeBedrock(byte[][][] world, int wx, int wz, long seed) {
        world[wx][0][wz] = STONE;
        int h = (int)(BiomeGenerator.smoothNoise(wx * 0.7, wz * 0.7, seed + 111) * 100);
        if ((h % 3) < 2) world[wx][1][wz] = STONE;
    }

    /** Po carve - przywroc trawe/piasek. */
    public static void repairSurface(byte[][][] world, int minX, int minZ, int maxX, int maxZ,
                                     int[][] hmap, int[][] biomeMap) {
        for (int wx = minX; wx < maxX; wx++) for (int wz = minZ; wz < maxZ; wz++) {
            int h = hmap[wx - minX][wz - minZ];
            int biome = biomeMap[wx - minX][wz - minZ];
            boolean sandy = biome == BIOME_DESERT || biome == BIOME_BEACH;
            boolean underwater = biome == BIOME_OCEAN || biome == BIOME_RIVER;
            if (underwater) {
                world[wx][h][wz] = (byte)(h < WATER_LEVEL - 4 ? DIRT : SAND);
            } else {
                world[wx][h][wz] = (byte)(sandy ? SAND : GRASS);
            }
            for (int wy = h - 1; wy >= Math.max(1, h - 5); wy--) {
                if (world[wx][wy][wz] == AIR || world[wx][wy][wz] == WATER) {
                    world[wx][wy][wz] = (byte)(sandy || underwater ? SAND : DIRT);
                }
            }
        }
    }

    /** Czy mozna postawic drzewo. */
    public static boolean canPlaceTree(byte[][][] world, int x, int y, int z, int radius,
                                       int worldWidth, int worldDepth) {
        if (y < 1 || y + 12 >= WORLD_Y) return false;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int bx = x + dx, bz = z + dz;
                if (bx < 2 || bz < 2 || bx >= worldWidth - 2 || bz >= worldDepth - 2) return false;
                for (int yy = Math.max(1, y - 1); yy < Math.min(WORLD_Y, y + 12); yy++) {
                    int id = world[bx][yy][bz] & 0xff;
                    if (id == WOOD || id == LEAVES || id == WATER) return false;
                    if (id == PLANKS || id == DOOR_BOTTOM || id == DOOR_TOP || id == CHEST || id == CRAFTING_TABLE) return false;
                    if (id == FARMLAND || id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3) return false;
                }
            }
        }
        return true;
    }
}
