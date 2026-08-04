package craft3dgl.entities;

import craft3dgl.AnimalGL;

import java.util.List;
import java.util.Random;

import static craft3dgl.world.WorldConstants.*;

/**
 * Spawnowanie zwierzat: per-chunk + przy ladowaniu zapisanego swiata.
 * Wymaga callbacka SolidCheck dla testow kolizji + dostepu do tablicy world.
 */
public final class AnimalSpawner {
    private AnimalSpawner() {}

    public interface SpawnContext {
        EntityCollision.SolidCheck solidCheck();
        byte[][][] world();
        long worldSeed();
        List<AnimalGL> animals();
        /** Wypchnij zwierze w gore jezeli utknelo w bloku. */
        void unstickAnimal(AnimalGL a);
    }

    /** Sprobuj wyspawnowac kilka zwierzat w obrebie chunka. */
    public static void spawnInChunk(int minX, int minZ, int maxX, int maxZ,
                                     int[][] hmap, int[][] biomeMap,
                                     SpawnContext ctx) {
        Random rand = new Random(ctx.worldSeed() ^ (minX * 17L) ^ (minZ * 1337L));
        if (rand.nextDouble() > 0.32) return;
        int count = 1 + rand.nextInt(3);
        for (int i = 0; i < count; i++) {
            int type = rand.nextInt(3) == 0 ? AnimalGL.COW
                     : rand.nextInt(2) == 0 ? AnimalGL.SHEEP : AnimalGL.PIG;
            for (int tries = 0; tries < 20; tries++) {
                int wx = minX + 2 + rand.nextInt(Math.max(1, maxX - minX - 4));
                int wz = minZ + 2 + rand.nextInt(Math.max(1, maxZ - minZ - 4));
                int h = hmap[wx - minX][wz - minZ];
                if (isValidSpawn(wx, h + 1, wz, ctx)) {
                    AnimalGL a = new AnimalGL(type, wx + 0.5, h + 1, wz + 0.5);
                    a.onGround = true;
                    ctx.unstickAnimal(a);
                    ctx.animals().add(a);
                    break;
                }
            }
        }
    }

    /** Wyspawnuj 28 zwierzat w juz wczytanym swiecie (jezeli brak save'owanych). */
    public static void spawnInLoadedWorld(SpawnContext ctx, Random rand) {
        for (int i = 0; i < 28; i++) {
            int type = i % 3 == 0 ? AnimalGL.COW : i % 3 == 1 ? AnimalGL.SHEEP : AnimalGL.PIG;
            for (int tries = 0; tries < 140; tries++) {
                int ax = 6 + rand.nextInt(WORLD_X - 12);
                int az = 6 + rand.nextInt(WORLD_Z - 12);
                int ay = findSurfaceSpawnY(ctx.world(), ax, az);
                if (ay > 0 && isValidSpawn(ax, ay, az, ctx)) {
                    AnimalGL a = new AnimalGL(type, ax + 0.5, ay, az + 0.5);
                    a.onGround = true;
                    ctx.unstickAnimal(a);
                    ctx.animals().add(a);
                    break;
                }
            }
        }
    }

    /** Znajdz najwyzsza pozycje na trawie z 2 wolnymi blokami nad nia. */
    public static int findSurfaceSpawnY(byte[][][] world, int ax, int az) {
        for (int ay = WORLD_Y - 2; ay >= 1; ay--) {
            if (world[ax][ay][az] == GRASS
                && ay + 2 < WORLD_Y
                && world[ax][ay + 1][az] == AIR
                && world[ax][ay + 2][az] == AIR) {
                return ay + 1;
            }
        }
        return -1;
    }

    /** Czy mozna wyspawnowac zwierze w danej pozycji. */
    public static boolean isValidSpawn(int ax, int ay, int az, SpawnContext ctx) {
        byte[][][] world = ctx.world();
        if (ax < 0 || ay < 0 || az < 0 || ax >= WORLD_X || ay + 2 >= WORLD_Y || az >= WORLD_Z) return false;
        if (world[ax][ay - 1][az] != GRASS) return false;
        if (world[ax][ay][az] != AIR || world[ax][ay + 1][az] != AIR || world[ax][ay + 2][az] != AIR) return false;
        EntityCollision.SolidCheck sc = ctx.solidCheck();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int nx = ax + dx, nz = az + dz;
            if (!sc.inWorld(nx, ay, nz)) return false;
            if (sc.isSolid(nx, ay, nz) || sc.isSolid(nx, ay + 1, nz)) return false;
        }
        double sx = WORLD_X / 2.0, sz = WORLD_Z / 2.0;
        if ((ax - sx) * (ax - sx) + (az - sz) * (az - sz) < 12 * 12) return false;
        for (AnimalGL a : ctx.animals()) {
            double dx = a.x - (ax + 0.5), dz = a.z - (az + 0.5);
            if (dx * dx + dz * dz < 2.2 * 2.2) return false;
        }
        return true;
    }
}
