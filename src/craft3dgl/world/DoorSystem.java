package craft3dgl.world;

import java.util.HashMap;

/**
 * System drzwi: trzyma metadata (facing + open state) dla kazdej polowki drzwi w swiecie.
 * Meta encoding:
 *   bity 0-1: facing (0=-Z, 1=+X, 2=+Z, 3=-X)
 *   bit 2:   open flag (1 = otwarte)
 *   bit 3:   openSide (0 = obrocone w prawo, 1 = w lewo)
 */
public final class DoorSystem {
    public final HashMap<Long, Integer> meta = new HashMap<>();

    public static long packKey(int x, int y, int z) {
        return ((long)(x & 0x3FF) << 22) | ((long)(y & 0x3FF) << 11) | (long)(z & 0x3FF);
    }

    public int getMeta(int x, int y, int z) {
        Integer v = meta.get(packKey(x, y, z));
        return v == null ? 0 : v;
    }

    public void setMeta(int x, int y, int z, int m) {
        meta.put(packKey(x, y, z), m);
    }

    public void removeMeta(int x, int y, int z) {
        meta.remove(packKey(x, y, z));
    }

    public void clear() {
        meta.clear();
    }

    /** yaw -> facing (kierunek panelu drzwi tak zeby gracz widzial klamke). */
    public static int yawToFacing(double yaw) {
        double n = yaw;
        while (n < 0) n += 2 * Math.PI;
        while (n >= 2 * Math.PI) n -= 2 * Math.PI;
        int idx = ((int) Math.round(n / (Math.PI / 2))) % 4;
        int[] map = {0, 3, 2, 1};
        return map[idx];
    }

    /** AABB drzwi w lokalnych wspolrzednych komorki (0..1): {x0, z0, x1, z1}. */
    public static double[] doorAabb(int meta) {
        double t = 3.0 / 16.0;
        int facing = meta & 3;
        boolean open = (meta & 4) != 0;
        boolean openSide = (meta & 8) != 0;
        int f;
        if (open) f = openSide ? (facing + 3) % 4 : (facing + 1) % 4;
        else f = facing;
        switch (f) {
            case 0: return new double[]{0, 0, 1, t};
            case 1: return new double[]{1 - t, 0, 1, 1};
            case 2: return new double[]{0, 1 - t, 1, 1};
            case 3: return new double[]{0, 0, t, 1};
        }
        return new double[]{0, 0, 1, 1};
    }

    public static double[] doorAabb(int facing, boolean open) {
        return doorAabb((facing & 3) | (open ? 4 : 0));
    }

    /** Jednostkowy wektor (dx, dz) dla danego facing 0..3. */
    public static double[] dirVec(int facing) {
        switch (facing) {
            case 0: return new double[]{0, -1};
            case 1: return new double[]{1, 0};
            case 2: return new double[]{0, 1};
            case 3: return new double[]{-1, 0};
        }
        return new double[]{0, 0};
    }
}
