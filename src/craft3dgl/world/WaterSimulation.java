package craft3dgl.world;

import java.util.ArrayDeque;

/**
 * Symulacja przeplywu wody. Trzyma kolejke komorek do sprawdzenia
 * i co WATER_TICK sekund probuje rozlac wode do sasiednich pustych komorek.
 *
 * Wymaga dostepu do tablicy swiata - przekazywanej przez parametry.
 */
public final class WaterSimulation {
    public static final double WATER_TICK = 0.08;
    public static final int WATER_UPDATES_PER_TICK = 768;

    public final ArrayDeque<int[]> queue = new ArrayDeque<>();
    public double acc = 0;

    public void clear() {
        queue.clear();
        acc = 0;
    }

    public void schedule(int x, int y, int z, int worldX, int worldY, int worldZ) {
        if (x < 0 || y < 0 || z < 0 || x >= worldX || y >= worldY || z >= worldZ) return;
        queue.add(new int[]{x, y, z});
    }

    /** Po wygenerowaniu/wczytaniu mapy - wrzuc do kolejki sasiadow kazdej wody. */
    public void seedFromWorld(byte[][][] world, int worldX, int worldY, int worldZ, int WATER_ID) {
        queue.clear();
        for (int x = 0; x < worldX; x++)
            for (int y = 0; y < worldY; y++)
                for (int z = 0; z < worldZ; z++) {
                    if ((world[x][y][z] & 0xff) != WATER_ID) continue;
                    schedule(x, y - 1, z, worldX, worldY, worldZ);
                    schedule(x + 1, y, z, worldX, worldY, worldZ);
                    schedule(x - 1, y, z, worldX, worldY, worldZ);
                    schedule(x, y, z + 1, worldX, worldY, worldZ);
                    schedule(x, y, z - 1, worldX, worldY, worldZ);
                }
    }

    /** Wywolywane co klatke - akumuluje czas i odpala dyskretne tiki. */
    public void tickAcc(double dt, WaterTickCallback cb) {
        acc += dt;
        int safety = 3;
        while (acc >= WATER_TICK && safety-- > 0) {
            acc -= WATER_TICK;
            tickOnce(cb);
        }
        if (acc > WATER_TICK * 3) acc = WATER_TICK;
    }

    /** Pojedynczy tick - przetwarza do WATER_UPDATES_PER_TICK komorek. */
    private void tickOnce(WaterTickCallback cb) {
        int budget = WATER_UPDATES_PER_TICK;
        int processed = 0;
        int hardLimit = budget * 8;
        while (budget > 0 && processed < hardLimit && !queue.isEmpty()) {
            int[] p = queue.pollFirst();
            processed++;
            if (cb.tryFlood(p[0], p[1], p[2])) {
                budget--;
            }
        }
        while (queue.size() > 80000) queue.pollFirst();
    }

    /** Callback wywolywany dla kazdej komorki w kolejce - powinien sprawdzic
     *  czy mozna ja zalac woda i jezeli tak - zalewa i dodaje sasiadow do kolejki.
     *  Zwraca true gdy zalano komorke (dla budget tracking). */
    public interface WaterTickCallback {
        boolean tryFlood(int x, int y, int z);
    }
}
