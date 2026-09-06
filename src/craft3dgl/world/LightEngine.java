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

    /** Block.getLightOpacity values relevant to the current 1.12 block set. */
    public static int lightOpacity(int id) {
        if (id == AIR || id == TALL_GRASS
                || id == WHEAT_0 || id == WHEAT_1 || id == WHEAT_2 || id == WHEAT_3
                || id == DOOR_BOTTOM || id == DOOR_TOP || id == CHEST) return 0;
        if (id == LEAVES) return 1;
        if (id == WATER) return 3;
        return 15;
    }

    /** Blok pozwala przepuscic swiatlo. */
    public static boolean isTransparent(int id) {
        return lightOpacity(id) < 15;
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

        // 2. Chunk.generateSkylightMap: direct sky remains 15 through air,
        // while leaves/water apply their 1.12 light-opacity values.
        IntQueue skyQ = new IntQueue();
        for (int xx = padMinX; xx <= padMaxX; xx++) {
            for (int zz = padMinZ; zz <= padMaxZ; zz++) {
                int level = 15;
                for (int yy = WORLD_Y - 1; yy >= 0 && level > 0; yy--) {
                    int id = world[xx][yy][zz] & 0xff;
                    int opacity = lightOpacity(id);
                    if (opacity >= 15) break;
                    if (opacity == 0 && level != 15) opacity = 1;
                    level = Math.max(0, level - opacity);
                    if (level > 0) {
                        setSky(xx, yy, zz, level);
                        skyQ.add(pack(xx, yy, zz));
                    }
                }
            }
        }
        propagateSky(skyQ, padMinX, padMinZ, padMaxX, padMaxZ);

        // 3. Block light sources
        IntQueue blQ = new IntQueue();
        for (int xx = padMinX; xx <= padMaxX; xx++) {
            for (int zz = padMinZ; zz <= padMaxZ; zz++) {
                for (int yy = 0; yy < WORLD_Y; yy++) {
                    int em = emissionOf(world[xx][yy][zz] & 0xff);
                    if (em > 0) {
                        setBlockLight(xx, yy, zz, em);
                        blQ.add(pack(xx, yy, zz));
                    }
                }
            }
        }
        propagateBlockLight(blQ, padMinX, padMinZ, padMaxX, padMaxZ);
    }

    private void propagateSky(IntQueue q, int minX, int minZ, int maxX, int maxZ) {
        final int[] dx = {1, -1, 0, 0, 0, 0};
        final int[] dy = {0, 0, 1, -1, 0, 0};
        final int[] dz = {0, 0, 0, 0, 1, -1};
        while (!q.isEmpty()) {
            int packed = q.poll();
            int cx = unpackX(packed), cy = unpackY(packed), cz = unpackZ(packed);
            int cur = getSky(cx, cy, cz);
            if (cur <= 1) continue;
            for (int i = 0; i < 6; i++) {
                int nx = cx + dx[i], ny = cy + dy[i], nz = cz + dz[i];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ || ny < 0 || ny >= WORLD_Y) continue;
                int nid = world[nx][ny][nz] & 0xff;
                int opacity = lightOpacity(nid);
                if (opacity >= 15) continue;
                // Direct level-15 skylight does not decay down through air.
                int attenuation = Math.max(1, opacity);
                int newLv = (dy[i] == -1 && cur == 15 && opacity == 0)
                        ? 15 : Math.max(0, cur - attenuation);
                if (newLv > getSky(nx, ny, nz)) {
                    setSky(nx, ny, nz, newLv);
                    if (newLv > 1) q.add(pack(nx, ny, nz));
                }
            }
        }
    }

    private void propagateBlockLight(IntQueue q, int minX, int minZ, int maxX, int maxZ) {
        final int[] dx = {1, -1, 0, 0, 0, 0};
        final int[] dy = {0, 0, 1, -1, 0, 0};
        final int[] dz = {0, 0, 0, 0, 1, -1};
        while (!q.isEmpty()) {
            int packed = q.poll();
            int cx = unpackX(packed), cy = unpackY(packed), cz = unpackZ(packed);
            int cur = getBlockLight(cx, cy, cz);
            if (cur <= 1) continue;
            for (int i = 0; i < 6; i++) {
                int nx = cx + dx[i], ny = cy + dy[i], nz = cz + dz[i];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ || ny < 0 || ny >= WORLD_Y) continue;
                int nid = world[nx][ny][nz] & 0xff;
                int opacity = lightOpacity(nid);
                if (opacity >= 15) continue;
                int newLv = Math.max(0, cur - Math.max(1, opacity));
                if (newLv > getBlockLight(nx, ny, nz)) {
                    setBlockLight(nx, ny, nz, newLv);
                    if (newLv > 1) q.add(pack(nx, ny, nz));
                }
            }
        }
    }

    // WORLD_X/Z are 1024 and WORLD_Y is 64, so one position fits in 26 bits.
    private static int pack(int x, int y, int z) { return (x << 16) | (y << 10) | z; }
    private static int unpackX(int p) { return (p >>> 16) & 1023; }
    private static int unpackY(int p) { return (p >>> 10) & 63; }
    private static int unpackZ(int p) { return p & 1023; }

    /** Allocation-free primitive FIFO used by light propagation. */
    private static final class IntQueue {
        private int[] values = new int[65536];
        private int head;
        private int size;

        boolean isEmpty() { return size == 0; }

        void add(int value) {
            if (size == values.length) grow();
            values[(head + size) & (values.length - 1)] = value;
            size++;
        }

        int poll() {
            int value = values[head];
            head = (head + 1) & (values.length - 1);
            size--;
            return value;
        }

        private void grow() {
            int[] larger = new int[values.length << 1];
            int first = Math.min(size, values.length - head);
            System.arraycopy(values, head, larger, 0, first);
            System.arraycopy(values, 0, larger, first, size - first);
            values = larger;
            head = 0;
        }
    }

    /**
     * WorldProvider.calculateCelestialAngle from MCP 9.40. Craft3D's public
     * clock uses 0=midnight, .25=sunrise, .5=noon and .75=sunset, while
     * Minecraft's world time starts at sunrise.
     */
    public static float celestialAngle(double dayFraction) {
        double worldTime = dayFraction + 0.75;
        worldTime = worldTime - Math.floor(worldTime);
        float angle = (float)worldTime - 0.25f;
        if (angle < 0.0f) angle += 1.0f;
        if (angle > 1.0f) angle -= 1.0f;
        float eased = 1.0f - ((float)Math.cos(angle * Math.PI) + 1.0f) / 2.0f;
        return angle + (eased - angle) / 3.0f;
    }

    /** World.getSunBrightness from Minecraft 1.12, including weather dimming. */
    public static float skyDayMultiplier(double dayFraction) {
        return skyDayMultiplier(dayFraction, 0f, 0f);
    }

    public static float skyDayMultiplier(double dayFraction, float rainStrength, float thunderStrength) {
        float angle = celestialAngle(dayFraction);
        float darkness = 1.0f - ((float)Math.cos(angle * Math.PI * 2.0) * 2.0f + 0.2f);
        darkness = Math.max(0.0f, Math.min(1.0f, darkness));
        float sun = 1.0f - darkness;
        sun *= 1.0f - clamp01(rainStrength) * 5.0f / 16.0f;
        sun *= 1.0f - clamp01(thunderStrength) * 5.0f / 16.0f;
        return sun * 0.8f + 0.2f;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /** Multiplier used by World.getSkyColor and WorldProvider.getFogColor. */
    public static float skyColorMultiplier(double dayFraction) {
        float angle = celestialAngle(dayFraction);
        float value = (float)Math.cos(angle * Math.PI * 2.0) * 2.0f + 0.5f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    /** World.getStarBrightness from MCP 9.40. */
    public static float starBrightness(double dayFraction) {
        float angle = celestialAngle(dayFraction);
        float value = 1.0f - ((float)Math.cos(angle * Math.PI * 2.0) * 2.0f + 0.25f);
        value = Math.max(0.0f, Math.min(1.0f, value));
        return value * value * 0.5f;
    }
}
