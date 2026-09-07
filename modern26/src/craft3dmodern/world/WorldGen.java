package craft3dmodern.world;

import java.util.Random;

/**
 * Generowanie swiata demonstracyjnego M3: teren z wartosciowego szumu,
 * trawa/ziemia/kamien, plaze z piasku, drzewa (debowe: pien + korona lisci)
 * i glazy z cobblestone.
 */
public final class WorldGen {
    public static final int SX = 64;
    public static final int SZ = 64;
    public static final int SY = 48;

    private WorldGen() {}

    public static World generate(long seed) {
        World w = new World(SX, SY, SZ);
        int[][] h = new int[SX][SZ];
        for (int x = 0; x < SX; x++) {
            for (int z = 0; z < SZ; z++) {
                double n = 6.0
                        + valueNoise(x * 0.045, z * 0.045, seed) * 4.5
                        + valueNoise(x * 0.12 + 37.7, z * 0.12 + 9.3, seed) * 2.2
                        + valueNoise(x * 0.26 + 11.1, z * 0.26 + 73.5, seed) * 1.0;
                // plaska polana w centrum (spawn)
                double dx = x - SX / 2.0;
                double dz = z - SZ / 2.0;
                double d = Math.sqrt(dx * dx + dz * dz);
                double flat = Math.max(0.0, 1.0 - d / 9.0);
                n = n * (1.0 - flat) + 8.0 * flat;
                int hn = (int) Math.round(n);
                if (hn < 3) hn = 3;
                if (hn > 15) hn = 15;
                h[x][z] = hn;
            }
        }

        for (int x = 0; x < SX; x++) {
            for (int z = 0; z < SZ; z++) {
                int top = h[x][z];
                for (int y = 0; y <= top; y++) {
                    int id;
                    if (top <= 4) {
                        id = y == top ? BlockIds.SAND : (y <= top - 2 ? BlockIds.STONE : BlockIds.SAND);
                    } else if (y == top) {
                        id = BlockIds.GRASS_BLOCK;
                    } else if (y >= top - 3) {
                        id = BlockIds.DIRT;
                    } else {
                        id = BlockIds.STONE;
                    }
                    w.set(x, y, z, id);
                }
            }
        }

        Random rnd = new Random(seed);
        int trees = 7 + rnd.nextInt(4);
        for (int i = 0; i < trees; i++) {
            int tx = 6 + rnd.nextInt(SX - 12);
            int tz = 6 + rnd.nextInt(SZ - 12);
            int ground = h[tx][tz];
            if (ground < 5) continue;
            if (Math.abs(tx - SX / 2) < 6 && Math.abs(tz - SZ / 2) < 6) continue;
            tree(w, tx, ground, tz, rnd);
        }
        int boulders = 5 + rnd.nextInt(3);
        for (int i = 0; i < boulders; i++) {
            int bx = 3 + rnd.nextInt(SX - 6);
            int bz = 3 + rnd.nextInt(SZ - 6);
            int by = h[bx][bz];
            if (by <= 3) continue;
            int n = 1 + rnd.nextInt(3);
            for (int k = 0; k < n; k++) {
                int rx = bx + rnd.nextInt(3) - 1;
                int rz = bz + rnd.nextInt(3) - 1;
                w.set(rx, h[Math.max(0, Math.min(SX - 1, rx))][Math.max(0, Math.min(SZ - 1, rz))], rz, BlockIds.COBBLESTONE);
            }
        }
        return w;
    }

    private static void tree(World w, int x, int ground, int z, Random rnd) {
        int h = 4 + rnd.nextInt(2);
        int topLog = ground + h - 1;
        for (int y = ground + 1; y <= topLog; y++) {
            w.set(x, y, z, BlockIds.OAK_LOG);
        }
        // korona lisci: wokol i ponad wierzcholkiem pnia
        for (int ly = 0; ly < 3; ly++) {
            int y = topLog + ly;
            int r = ly == 0 ? 1 : (ly == 1 ? 2 : 1);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0) {
                        // nad pniem (nie nadpisujemy ostatniego kloca)
                        if (ly >= 1 && w.get(x, y, z) == BlockIds.AIR) {
                            w.set(x, y, z, BlockIds.OAK_LEAVES);
                        }
                        continue;
                    }
                    // losowo przycinamy narozniki, korona nie musi byc idealnym prostopadloscianem
                    if (Math.abs(dx) == r && Math.abs(dz) == r && rnd.nextInt(3) == 0) continue;
                    if (w.get(x + dx, y, z + dz) == BlockIds.AIR) {
                        w.set(x + dx, y, z + dz, BlockIds.OAK_LEAVES);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // wartosciowy szum
    // ------------------------------------------------------------------

    private static double hash2(int x, int z, long seed) {
        long h = x * 374761393L + z * 668265263L + seed * 974634307L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return ((h & 0x7fffffffL) / (double) 0x40000000L) - 1.0; // -1..1
    }

    private static double valueNoise(double x, double z, long seed) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double xf = x - xi;
        double zf = z - zi;
        xf = xf * xf * (3 - 2 * xf);
        zf = zf * zf * (3 - 2 * zf);
        double n00 = hash2(xi, zi, seed);
        double n10 = hash2(xi + 1, zi, seed);
        double n01 = hash2(xi, zi + 1, seed);
        double n11 = hash2(xi + 1, zi + 1, seed);
        double nx0 = n00 + (n10 - n00) * xf;
        double nx1 = n01 + (n11 - n01) * xf;
        return nx0 + (nx1 - nx0) * zf;
    }
}
