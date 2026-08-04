package craft3dgl.world;

import static craft3dgl.world.WorldConstants.*;

/**
 * Silnik oswietlenia typu Minecraft: sky light + block light w jednym byte
 * (sky = gorne 4 bity, block = dolne 4 bity, 0-15 kazde).
 *
 * Sky light: propaguje z gory - blok solid blokuje, transparent przepuszcza.
 * Sky w dol NIE zanika (columny slonca). Horyzontalnie -1 na krok.
 *
 * Block light: emisja ze zrodel (obecnie brak - kod przygotowany) z zanikiem -1.
 *
 * Rebuild uzywa BFS. Zaokraglone regiony (padding 15) zeby BFS na krawedzi
 * dobrze propagowal light z sasiednich chunkow.
 */
public final class LightEngine {
    public final byte[][][] light;
    private final byte[][][] world;

    public LightEngine(byte[][][] world) {
        this.world = world;
        this.light = new byte[WORLD_X][WORLD_Y][WORLD_Z];
    }

    /** Blok pozwala przepuscic swiatlo. */
    public static boolean isTransparent(int id) {
        return id == AIR || id == WATER || id == LEAVES
                || id == TALL_GRASS
                || id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3
                || id == DOOR_BOTTOM || id == DOOR_TOP;
    }

    /** Ile swiatla emituje blok (0 = nie emituje). */
    public static int emissionOf(int id) {
        return 0; // przygotowane pod przyszle bloki (torch, glowstone)
    }

    public int getSky(int x, int y, int z) { return (light[x][y][z] >> 4) & 0x0F; }
    public int getBlockLight(int x, int y, int z) { return light[x][y][z] & 0x0F; }
    public void setSky(int x, int y, int z, int v) {
        light[x][y][z] = (byte)((light[x][y][z] & 0x0F) | ((v & 0x0F) << 4));
    }
    public void setBlockLight(int x, int y, int z, int v) {
        light[x][y][z] = (byte)((light[x][y][z] & 0xF0) | (v & 0x0F));
    }

    /** Combined shade (0.05..1.0) - do renderowania faces. */
    public float sampleShade(int x, int y, int z, float dayMult) {
        if (x < 0 || x >= WORLD_X || y < 0 || y >= WORLD_Y || z < 0 || z >= WORLD_Z) return dayMult;
        int sky = getSky(x, y, z);
        int bl = getBlockLight(x, y, z);
        float skyEff = sky * dayMult;
        float lv = Math.max(skyEff, bl);
        float t = lv / 15f;
        // Krzywa gamma - ciemne wyrazniej, jasne stabilnie
        return Math.max(0.06f, (float) Math.pow(t, 1.35) * 0.94f + 0.06f);
    }

    /**
     * Pelen rebuild regionu (min..max) w XZ, cala kolumna Y.
     * Rozszerza BFS o padding 15 zeby dobrze propagowac swiatlo z sasiadow.
     */
    public void rebuildRegion(int minX, int minZ, int maxX, int maxZ) {
        minX = Math.max(0, minX);
        minZ = Math.max(0, minZ);
        maxX = Math.min(WORLD_X - 1, maxX);
        maxZ = Math.min(WORLD_Z - 1, maxZ);

        int padMinX = Math.max(0, minX - 15);
        int padMinZ = Math.max(0, minZ - 15);
        int padMaxX = Math.min(WORLD_X - 1, maxX + 15);
        int padMaxZ = Math.min(WORLD_Z - 1, maxZ + 15);

        // 1. Zeruj w padded region (BFS potrzebuje czystego startu)
        for (int xx = padMinX; xx <= padMaxX; xx++) {
            for (int zz = padMinZ; zz <= padMaxZ; zz++) {
                for (int yy = 0; yy < WORLD_Y; yy++) light[xx][yy][zz] = 0;
            }
        }

        // 2. Sky column: sky=15 wszedzie nad top solid blockiem
        java.util.ArrayDeque<int[]> skyQ = new java.util.ArrayDeque<>();
        for (int xx = padMinX; xx <= padMaxX; xx++) {
            for (int zz = padMinZ; zz <= padMaxZ; zz++) {
                for (int yy = WORLD_Y - 1; yy >= 0; yy--) {
                    int id = world[xx][yy][zz] & 0xff;
                    if (isTransparent(id)) {
                        setSky(xx, yy, zz, 15);
                        skyQ.add(new int[]{xx, yy, zz});
                    } else {
                        break; // ciemno pod solidem
                    }
                }
            }
        }
        propagateSky(skyQ, padMinX, padMinZ, padMaxX, padMaxZ);

        // 3. Block light sources
        java.util.ArrayDeque<int[]> blQ = new java.util.ArrayDeque<>();
        for (int xx = padMinX; xx <= padMaxX; xx++) {
            for (int zz = padMinZ; zz <= padMaxZ; zz++) {
                for (int yy = 0; yy < WORLD_Y; yy++) {
                    int em = emissionOf(world[xx][yy][zz] & 0xff);
                    if (em > 0) {
                        setBlockLight(xx, yy, zz, em);
                        blQ.add(new int[]{xx, yy, zz});
                    }
                }
            }
        }
        propagateBlockLight(blQ, padMinX, padMinZ, padMaxX, padMaxZ);
    }

    private void propagateSky(java.util.ArrayDeque<int[]> q, int minX, int minZ, int maxX, int maxZ) {
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int cx = p[0], cy = p[1], cz = p[2];
            int cur = getSky(cx, cy, cz);
            if (cur <= 1) continue;
            for (int[] dv : dirs) {
                int nx = cx + dv[0], ny = cy + dv[1], nz = cz + dv[2];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ || ny < 0 || ny >= WORLD_Y) continue;
                int nid = world[nx][ny][nz] & 0xff;
                if (!isTransparent(nid)) continue;
                // W dol bez zaniku (sky column), horyzontal -1, w gore -1
                int newLv = (dv[1] == -1 && cur == 15) ? 15 : cur - 1;
                if (nid == WATER) newLv = Math.max(0, newLv - 2);
                else if (nid == LEAVES) newLv = Math.max(0, newLv - 1);
                if (newLv > getSky(nx, ny, nz)) {
                    setSky(nx, ny, nz, newLv);
                    if (newLv > 1) q.add(new int[]{nx, ny, nz});
                }
            }
        }
    }

    private void propagateBlockLight(java.util.ArrayDeque<int[]> q, int minX, int minZ, int maxX, int maxZ) {
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int cx = p[0], cy = p[1], cz = p[2];
            int cur = getBlockLight(cx, cy, cz);
            if (cur <= 1) continue;
            for (int[] dv : dirs) {
                int nx = cx + dv[0], ny = cy + dv[1], nz = cz + dv[2];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ || ny < 0 || ny >= WORLD_Y) continue;
                int nid = world[nx][ny][nz] & 0xff;
                if (!isTransparent(nid)) continue;
                int newLv = cur - 1;
                if (nid == WATER) newLv = Math.max(0, newLv - 1);
                if (newLv > getBlockLight(nx, ny, nz)) {
                    setBlockLight(nx, ny, nz, newLv);
                    if (newLv > 1) q.add(new int[]{nx, ny, nz});
                }
            }
        }
    }

    /** Mnoznik sky light w zaleznosci od pory dnia (0..1). */
    public static float skyDayMultiplier(double dayFraction) {
        double t = ((dayFraction % 1.0) + 1.0) % 1.0;
        float NIGHT = 0.15f;
        if (t < 0.15) return NIGHT;
        if (t < 0.30) return (float)(NIGHT + (t - 0.15) / 0.15 * (1.0 - NIGHT));
        if (t < 0.70) return 1.0f;
        if (t < 0.85) return (float)(1.0 - (t - 0.70) / 0.15 * (1.0 - NIGHT));
        return NIGHT;
    }
}
