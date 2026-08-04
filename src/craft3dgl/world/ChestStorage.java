package craft3dgl.world;

import java.util.HashMap;

/**
 * Magazyn skrzyni: trzyma id/count itemow dla kazdej skrzyni w swiecie
 * indeksowane przez upakowane wspolrzedne XYZ.
 */
public final class ChestStorage {
    public static final int CHEST_SIZE = 27;

    public final HashMap<Long, int[]> ids = new HashMap<>();
    public final HashMap<Long, int[]> counts = new HashMap<>();

    public static long packKey(int x, int y, int z) {
        return ((long)(x & 0x3FF) << 22) | ((long)(y & 0x3FF) << 11) | (long)(z & 0x3FF);
    }

    public int[] idsAt(int x, int y, int z) {
        long k = packKey(x, y, z);
        int[] arr = ids.get(k);
        if (arr == null) { arr = new int[CHEST_SIZE]; ids.put(k, arr); }
        return arr;
    }

    public int[] countsAt(int x, int y, int z) {
        long k = packKey(x, y, z);
        int[] arr = counts.get(k);
        if (arr == null) { arr = new int[CHEST_SIZE]; counts.put(k, arr); }
        return arr;
    }

    public void remove(int x, int y, int z) {
        long k = packKey(x, y, z);
        ids.remove(k);
        counts.remove(k);
    }

    public void clear() {
        ids.clear();
        counts.clear();
    }
}
