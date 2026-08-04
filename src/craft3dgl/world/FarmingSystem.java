package craft3dgl.world;

import java.util.Random;

/**
 * System rolnictwa: tick wzrostu pszenicy w obszarze wokol gracza,
 * sprawdzanie warunkow do sadzenia, itp.
 *
 * Uzywany przez MinecraftGL.tickWheatGrowth() z callbackiem do setBlock().
 */
public final class FarmingSystem {
    public static final double WHEAT_TICK = 4.0;  // co 4 sekundy
    public static final int GROWTH_RADIUS = 24;   // bloki w promieniu od gracza
    public static final int MAX_GROWS_PER_TICK = 20;
    public static final double GROW_CHANCE = 0.60;

    public double acc = 0;

    public void clear() {
        acc = 0;
    }

    /**
     * Probuje rosnac pszenice w obszarze wokol gracza.
     * @param dt delta time
     * @param playerX X gracza
     * @param playerY Y gracza
     * @param playerZ Z gracza
     * @param random RNG
     * @param cb callback - dla danej pozycji sprawdza id i (jezeli pszenica)
     *           promotuje do nastepnego stadium, zwraca true gdy "grown"
     */
    public void tick(double dt, double playerX, double playerY, double playerZ,
                     int worldX, int worldY, int worldZ,
                     Random random, GrowCallback cb) {
        acc += dt;
        if (acc < WHEAT_TICK) return;
        acc = 0;

        int minBx = Math.max(0, (int)playerX - GROWTH_RADIUS);
        int maxBx = Math.min(worldX - 1, (int)playerX + GROWTH_RADIUS);
        int minBz = Math.max(0, (int)playerZ - GROWTH_RADIUS);
        int maxBz = Math.min(worldZ - 1, (int)playerZ + GROWTH_RADIUS);
        int minBy = Math.max(0, (int)playerY - 8);
        int maxBy = Math.min(worldY - 1, (int)playerY + 8);

        int attempts = 200;
        int grown = 0;
        while (attempts-- > 0 && grown < MAX_GROWS_PER_TICK) {
            int bx = minBx + random.nextInt(maxBx - minBx + 1);
            int bz = minBz + random.nextInt(maxBz - minBz + 1);
            int by = minBy + random.nextInt(maxBy - minBy + 1);
            if (random.nextDouble() < GROW_CHANCE) {
                if (cb.tryGrow(bx, by, bz)) grown++;
            }
        }
    }

    public interface GrowCallback {
        /** Sprobuj rosnac wheat na (x,y,z). Zwraca true gdy faktycznie rosla. */
        boolean tryGrow(int x, int y, int z);
    }
}
