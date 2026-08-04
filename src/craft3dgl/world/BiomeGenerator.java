package craft3dgl.world;

import static craft3dgl.world.WorldConstants.*;

/**
 * Generator biomów v2 - MC-inspired:
 *  - Continental mask (kontynenty vs oceany, wielka skala)
 *  - Temperature + humidity mapy - kombinacja daje biom
 *  - Domain warping (perturbacja przed sample - lamie prostoliniowosc)
 *  - Meandrowate rzeki (ridge noise)
 *  - Beach na kontakcie ladu z oceanem
 */
public final class BiomeGenerator {
    private BiomeGenerator() {}

    public static int biomeAt(int x, int z, long seed) {
        // ---- 1. DOMAIN WARPING - perturbuj wspolrzedne przed sampling ----
        // Dzieki temu biomy maja "organiczne" nieregularne krawedzie
        double warpX = fbm(x * 0.008, z * 0.008, seed + 40001, 3) * 60;
        double warpZ = fbm(x * 0.008 + 200, z * 0.008 - 300, seed + 40002, 3) * 60;
        double wx = x + warpX;
        double wz = z + warpZ;

        // ---- 2. CONTINENTAL MASK ----
        // Wielka skala noise (0.003) - wielkie kontynenty ~300 blokow
        double continent = fbm(wx * 0.0030, wz * 0.0030, seed + 100, 4);
        // Ocean gdzie continent < 0.42
        if (continent < 0.42) return BIOME_OCEAN;
        // Beach na krawedzi (continent w waskim pasie)
        if (continent < 0.46) return BIOME_BEACH;

        // ---- 3. RIVER - meandrowate przez lad ----
        double river = riverValue(wx, wz, seed);
        if (river < 0.03 && continent < 0.72) return BIOME_RIVER;

        // ---- 4. TEMPERATURE + HUMIDITY ----
        double temp = fbm(wx * 0.012, wz * 0.012, seed + 200, 3);
        double humid = fbm(wx * 0.014 + 500, wz * 0.014 - 500, seed + 500, 3);

        // ---- 5. MOUNTAIN mask (osobna wielka skala) ----
        double mountain = fbm(wx * 0.006, wz * 0.006, seed + 900, 4);
        if (mountain > 0.68) return BIOME_MOUNTAINS;

        // ---- 6. Klasyfikacja temp/humid ----
        if (temp > 0.62 && humid < 0.46) return BIOME_DESERT;
        if (humid > 0.55) return BIOME_FOREST;
        return BIOME_PLAINS;
    }

    public static int terrainHeightAt(int wx, int wz, int biome, long seed) {
        // Domain warping tez dla wysokosci
        double warpX = fbm(wx * 0.008, wz * 0.008, seed + 40001, 3) * 60;
        double warpZ = fbm(wx * 0.008 + 200, wz * 0.008 - 300, seed + 40002, 3) * 60;
        double sx = wx + warpX;
        double sz = wz + warpZ;

        // Continental height blend (basowa wysokosc z continent mask)
        double continent = fbm(sx * 0.0030, sz * 0.0030, seed + 100, 4);

        // Detale terenu
        double hill = fbm(sx * 0.028, sz * 0.028, seed + 11, 4);
        double detail = fbm(sx * 0.09, sz * 0.09, seed + 29, 2);
        double roughness = fbm(sx * 0.15, sz * 0.15, seed + 33, 2) * 0.3;

        int base;
        switch (biome) {
            case BIOME_OCEAN: {
                // Dno morza - glebokie w srodku oceanu, plytkie na brzegach
                double depth = (0.42 - continent) * 40;   // 0..16 blokow glebokosci
                base = (int)(WATER_LEVEL - 4 - depth + detail * 2);
                break;
            }
            case BIOME_BEACH:
                // Plaska plaza tuz nad water level
                base = WATER_LEVEL + 1 + (int)(detail * 2);
                break;
            case BIOME_RIVER:
                // Rzeka - 2 bloki pod water level
                base = WATER_LEVEL - 3 + (int)(detail * 1);
                break;
            case BIOME_MOUNTAINS: {
                // WYSOKIE gory - continental mask boostuje wysokosc
                double mountFactor = fbm(sx * 0.006, sz * 0.006, seed + 900, 4);
                double peak = Math.max(0, mountFactor - 0.68) * 3.5;  // 0..1
                base = WATER_LEVEL + 8 + (int)(hill * 20 + peak * 20 + detail * 6 + roughness * 4);
                break;
            }
            case BIOME_DESERT:
                base = WATER_LEVEL + 2 + (int)(hill * 4 + detail * 2);
                break;
            case BIOME_FOREST:
                base = WATER_LEVEL + 4 + (int)(hill * 6 + detail * 3);
                break;
            case BIOME_PLAINS:
            default:
                base = WATER_LEVEL + 3 + (int)(hill * 4 + detail * 2);
                break;
        }
        return clampInt(base, 3, WORLD_Y - 5);
    }

    // ===== NOISE =====

    public static double fbm(double x, double z, long seed, int octaves) {
        double sum = 0, amp = 1, scale = 1, div = 0;
        for (int i = 0; i < octaves; i++) {
            sum += smoothNoise(x * scale, z * scale, seed + i * 1013) * amp;
            div += amp;
            amp *= 0.5;
            scale *= 2.0;
        }
        return sum / div;
    }

    public static double smoothNoise(double x, double z, long seed) {
        int x0 = (int)Math.floor(x), z0 = (int)Math.floor(z);
        double tx = x - x0, tz = z - z0;
        // Perlin fade - lepsza smoothness niz hermite
        tx = tx * tx * tx * (tx * (tx * 6 - 15) + 10);
        tz = tz * tz * tz * (tz * (tz * 6 - 15) + 10);
        double a = randomValue(x0, z0, seed);
        double b = randomValue(x0 + 1, z0, seed);
        double c = randomValue(x0, z0 + 1, seed);
        double d = randomValue(x0 + 1, z0 + 1, seed);
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    public static double randomValue(int x, int z, long seed) {
        int h = hash(x ^ (int)seed, z ^ (int)(seed >>> 32));
        return (h & 0xffff) / 65535.0;
    }

    /** Meandrowate rzeki: absolutna wartosc ridge noise -> 0 = w centrum rzeki. */
    public static double riverValue(double x, double z, long seed) {
        // Duza skala - dlugie rzeki
        double n = fbm(x * 0.005, z * 0.005, seed + 7500, 4);
        // Ridge: |n - 0.5| = odleglosc od centrum "grzbietu" ridge
        return Math.abs(n - 0.5);
    }

    // Backward compat aliases
    public static double riverNoise(int x, int z, long seed) {
        return riverValue(x, z, seed);
    }

    public static double lakeNoise(int x, int z, long seed) {
        double n = fbm(x * 0.045 + 200, z * 0.045 - 200, seed + 7000, 3);
        double mask = fbm(x * 0.015, z * 0.015, seed + 7100, 2);
        return n * 0.7 + mask * 0.3;
    }

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static int hash(int a, int b) {
        int h = a * 73428767 ^ b * 9122719;
        h ^= h >>> 13;
        h *= 1274126177;
        return h;
    }

    public static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
