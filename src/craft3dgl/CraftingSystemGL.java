package craft3dgl;

public final class CraftingSystemGL {
    public static final Recipe EMPTY = new Recipe(0, 0, new int[9]);

    public static Recipe match(int[] id, int[] count, int size) {
        int nonEmpty = 0;
        for (int i = 0; i < size * size; i++) if (id[i] > 0 && count[i] > 0) nonEmpty++;
        if (nonEmpty == 0) return EMPTY;

        // 1 drewno -> 4 deski
        if (nonEmpty == 1) {
            for (int i = 0; i < size * size; i++) {
                if (id[i] == MinecraftGL.WOOD && count[i] > 0) {
                    int[] consume = new int[9];
                    consume[i] = 1;
                    return new Recipe(MinecraftGL.PLANKS, 4, consume);
                }
            }
        }

        Bounds b = bounds(id, count, size);
        if (b.empty) return EMPTY;

        // 2 deski pionowo -> 4 patyki
        if (b.w == 1 && b.h == 2 && nonEmpty == 2) {
            int a = idx(b.x, b.y, size);
            int c = idx(b.x, b.y + 1, size);
            if (id[a] == MinecraftGL.PLANKS && id[c] == MinecraftGL.PLANKS) {
                int[] consume = new int[9];
                consume[a] = consume[c] = 1;
                return new Recipe(MinecraftGL.ITEM_STICK, 4, consume);
            }
        }

        // 2x2 deski -> crafting table
        if (b.w == 2 && b.h == 2 && nonEmpty == 4) {
            int[] consume = new int[9];
            boolean ok = true;
            for (int y = 0; y < 2; y++) for (int x = 0; x < 2; x++) {
                int k = idx(b.x + x, b.y + y, size);
                if (id[k] != MinecraftGL.PLANKS) ok = false;
                consume[k] = 1;
            }
            if (ok) return new Recipe(MinecraftGL.CRAFTING_TABLE, 1, consume);
        }

        // 3x3 recepty
        if (size >= 3 && b.w == 3 && b.h == 3) {
            if (nonEmpty == 5) {
                Recipe r = matchPickaxe(id, b, size);
                if (!r.empty()) return r;
            }
            if (nonEmpty == 5) {
                Recipe r = matchAxe(id, b, size);
                if (!r.empty()) return r;
            }
            if (nonEmpty == 3) {
                Recipe r = matchShovel(id, b, size);
                if (!r.empty()) return r;
            }
            if (nonEmpty == 3) {
                Recipe r = matchSword(id, b, size);
                if (!r.empty()) return r;
            }
            if (nonEmpty == 8) {
                Recipe r = matchChest(id, b, size);
                if (!r.empty()) return r;
            }
            if (nonEmpty == 6) {
                Recipe r = matchDoor(id, b, size);
                if (!r.empty()) return r;
            }
        }

        // === Bread: 3 wheat horizontally (1x3 lub 3x1) ===
        if (b.w == 3 && b.h == 1 && nonEmpty == 3) {
            int k0 = idx(b.x, b.y, size);
            int k1 = idx(b.x + 1, b.y, size);
            int k2 = idx(b.x + 2, b.y, size);
            if (id[k0] == MinecraftGL.ITEM_WHEAT && id[k1] == MinecraftGL.ITEM_WHEAT && id[k2] == MinecraftGL.ITEM_WHEAT) {
                int[] consume = new int[9];
                consume[k0] = consume[k1] = consume[k2] = 1;
                return new Recipe(MinecraftGL.ITEM_BREAD, 1, consume);
            }
        }
        if (b.w == 1 && b.h == 3 && nonEmpty == 3) {
            int k0 = idx(b.x, b.y, size);
            int k1 = idx(b.x, b.y + 1, size);
            int k2 = idx(b.x, b.y + 2, size);
            if (id[k0] == MinecraftGL.ITEM_WHEAT && id[k1] == MinecraftGL.ITEM_WHEAT && id[k2] == MinecraftGL.ITEM_WHEAT) {
                int[] consume = new int[9];
                consume[k0] = consume[k1] = consume[k2] = 1;
                return new Recipe(MinecraftGL.ITEM_BREAD, 1, consume);
            }
        }

        // === Hoe (motyka) 3x3: jak axe ale tylko 2 materialy (lustrzane wersje) ===
        // P P .         . P P
        // . S .   lub   . S .
        // . S .         . S .
        if (size >= 3 && b.w >= 2 && b.h == 3 && nonEmpty == 4) {
            Recipe r = matchHoe(id, b, size);
            if (!r.empty()) return r;
        }

        return EMPTY;
    }

    private static Recipe matchHoe(int[] id, Bounds b, int size) {
        // Sprobuj wersje lewa (top: lx=0,1 z mat; pionowo lx=1 z 2 kijow)
        if (b.w == 2) {
            int t0 = idx(b.x, b.y, size);
            int t1 = idx(b.x + 1, b.y, size);
            int m1 = idx(b.x + 1, b.y + 1, size);
            int bo = idx(b.x + 1, b.y + 2, size);
            int mat = id[t0];
            if (isMatPickaxeAxe(mat) && id[t1] == mat
                    && id[m1] == MinecraftGL.ITEM_STICK
                    && id[bo] == MinecraftGL.ITEM_STICK) {
                if (!onlyTheseInBounds(id, b, size, new int[]{t0, t1, m1, bo})) return EMPTY;
                int[] consume = new int[9];
                consume[t0] = consume[t1] = consume[m1] = consume[bo] = 1;
                int result = mat == MinecraftGL.STONE ? MinecraftGL.ITEM_STONE_HOE : MinecraftGL.ITEM_WOOD_HOE;
                return new Recipe(result, 1, consume);
            }
            // Wersja lustrzana: top mat, mid kij na lewo (lx=0)
            int m0 = idx(b.x, b.y + 1, size);
            int bo0 = idx(b.x, b.y + 2, size);
            if (isMatPickaxeAxe(mat) && id[t1] == mat
                    && id[m0] == MinecraftGL.ITEM_STICK
                    && id[bo0] == MinecraftGL.ITEM_STICK) {
                if (!onlyTheseInBounds(id, b, size, new int[]{t0, t1, m0, bo0})) return EMPTY;
                int[] consume = new int[9];
                consume[t0] = consume[t1] = consume[m0] = consume[bo0] = 1;
                int result = mat == MinecraftGL.STONE ? MinecraftGL.ITEM_STONE_HOE : MinecraftGL.ITEM_WOOD_HOE;
                return new Recipe(result, 1, consume);
            }
        }
        return EMPTY;
    }

    private static Recipe matchPickaxe(int[] id, Bounds b, int size) {
        int t0 = idx(b.x,     b.y,     size);
        int t1 = idx(b.x + 1, b.y,     size);
        int t2 = idx(b.x + 2, b.y,     size);
        int m  = idx(b.x + 1, b.y + 1, size);
        int bo = idx(b.x + 1, b.y + 2, size);
        int mat = id[t0];
        if (!isMatPickaxeAxe(mat)) return EMPTY;
        if (id[t1] != mat || id[t2] != mat) return EMPTY;
        if (id[m] != MinecraftGL.ITEM_STICK || id[bo] != MinecraftGL.ITEM_STICK) return EMPTY;
        if (!onlyTheseInBounds(id, b, size, new int[]{t0, t1, t2, m, bo})) return EMPTY;
        int[] consume = new int[9];
        consume[t0] = consume[t1] = consume[t2] = 1;
        consume[m] = consume[bo] = 1;
        int result = mat == MinecraftGL.STONE ? MinecraftGL.ITEM_STONE_PICKAXE : MinecraftGL.ITEM_WOOD_PICKAXE;
        return new Recipe(result, 1, consume);
    }

    private static Recipe matchAxe(int[] id, Bounds b, int size) {
        if (tryAxeLeft(id, b, size)) {
            int t0 = idx(b.x, b.y, size);
            int t1 = idx(b.x + 1, b.y, size);
            int m1 = idx(b.x, b.y + 1, size);
            int m2 = idx(b.x + 1, b.y + 1, size);
            int bo = idx(b.x + 1, b.y + 2, size);
            int mat = id[t0];
            if (!onlyTheseInBounds(id, b, size, new int[]{t0, t1, m1, m2, bo})) return EMPTY;
            int[] consume = new int[9];
            consume[t0] = consume[t1] = consume[m1] = 1;
            consume[m2] = consume[bo] = 1;
            int result = mat == MinecraftGL.STONE ? MinecraftGL.ITEM_STONE_AXE : MinecraftGL.ITEM_WOOD_AXE;
            return new Recipe(result, 1, consume);
        }
        if (tryAxeRight(id, b, size)) {
            int t1 = idx(b.x + 1, b.y, size);
            int t2 = idx(b.x + 2, b.y, size);
            int m1 = idx(b.x + 1, b.y + 1, size);
            int m2 = idx(b.x + 2, b.y + 1, size);
            int bo = idx(b.x + 1, b.y + 2, size);
            int mat = id[t1];
            if (!onlyTheseInBounds(id, b, size, new int[]{t1, t2, m1, m2, bo})) return EMPTY;
            int[] consume = new int[9];
            consume[t1] = consume[t2] = consume[m1] = 1;
            consume[m2] = consume[bo] = 1;
            int result = mat == MinecraftGL.STONE ? MinecraftGL.ITEM_STONE_AXE : MinecraftGL.ITEM_WOOD_AXE;
            return new Recipe(result, 1, consume);
        }
        return EMPTY;
    }

    private static boolean tryAxeLeft(int[] id, Bounds b, int size) {
        int t0 = idx(b.x, b.y, size);
        int t1 = idx(b.x + 1, b.y, size);
        int m1 = idx(b.x, b.y + 1, size);
        int m2 = idx(b.x + 1, b.y + 1, size);
        int bo = idx(b.x + 1, b.y + 2, size);
        int mat = id[t0];
        if (!isMatPickaxeAxe(mat)) return false;
        return id[t1] == mat && id[m1] == mat
                && id[m2] == MinecraftGL.ITEM_STICK
                && id[bo] == MinecraftGL.ITEM_STICK;
    }

    private static boolean tryAxeRight(int[] id, Bounds b, int size) {
        int t1 = idx(b.x + 1, b.y, size);
        int t2 = idx(b.x + 2, b.y, size);
        int m1 = idx(b.x + 1, b.y + 1, size);
        int m2 = idx(b.x + 2, b.y + 1, size);
        int bo = idx(b.x + 1, b.y + 2, size);
        int mat = id[t1];
        if (!isMatPickaxeAxe(mat)) return false;
        return id[t2] == mat && id[m2] == mat
                && id[m1] == MinecraftGL.ITEM_STICK
                && id[bo] == MinecraftGL.ITEM_STICK;
    }

    private static Recipe matchShovel(int[] id, Bounds b, int size) {
        if (b.w != 1 || b.h != 3) return EMPTY;
        int t = idx(b.x, b.y,     size);
        int m = idx(b.x, b.y + 1, size);
        int o = idx(b.x, b.y + 2, size);
        int mat = id[t];
        if (!isMatPickaxeAxe(mat)) return EMPTY;
        if (id[m] != MinecraftGL.ITEM_STICK || id[o] != MinecraftGL.ITEM_STICK) return EMPTY;
        if (!onlyTheseInBounds(id, b, size, new int[]{t, m, o})) return EMPTY;
        int[] consume = new int[9];
        consume[t] = consume[m] = consume[o] = 1;
        int result = mat == MinecraftGL.STONE ? MinecraftGL.ITEM_STONE_SHOVEL : MinecraftGL.ITEM_WOOD_SHOVEL;
        return new Recipe(result, 1, consume);
    }

    private static Recipe matchSword(int[] id, Bounds b, int size) {
        if (b.w != 1 || b.h != 3) return EMPTY;
        int t = idx(b.x, b.y,     size);
        int m = idx(b.x, b.y + 1, size);
        int o = idx(b.x, b.y + 2, size);
        int mat = id[t];
        if (!isMatPickaxeAxe(mat)) return EMPTY;
        if (id[m] != mat || id[o] != MinecraftGL.ITEM_STICK) return EMPTY;
        if (!onlyTheseInBounds(id, b, size, new int[]{t, m, o})) return EMPTY;
        int[] consume = new int[9];
        consume[t] = consume[m] = consume[o] = 1;
        int result = mat == MinecraftGL.STONE ? MinecraftGL.ITEM_STONE_SWORD : MinecraftGL.ITEM_WOOD_SWORD;
        return new Recipe(result, 1, consume);
    }

    private static Recipe matchChest(int[] id, Bounds b, int size) {
        if (b.w != 3 || b.h != 3) return EMPTY;
        int[] used = new int[8];
        int k = 0;
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) {
            if (x == 1 && y == 1) continue;
            int i = idx(b.x + x, b.y + y, size);
            if (id[i] != MinecraftGL.PLANKS) return EMPTY;
            used[k++] = i;
        }
        int center = idx(b.x + 1, b.y + 1, size);
        if (id[center] != 0) return EMPTY;
        int[] consume = new int[9];
        for (int u : used) consume[u] = 1;
        return new Recipe(MinecraftGL.CHEST, 1, consume);
    }

    private static Recipe matchDoor(int[] id, Bounds b, int size) {
        if (b.w != 2 || b.h != 3) return EMPTY;
        int[] used = new int[6];
        int k = 0;
        for (int y = 0; y < 3; y++) for (int x = 0; x < 2; x++) {
            int i = idx(b.x + x, b.y + y, size);
            if (id[i] != MinecraftGL.PLANKS) return EMPTY;
            used[k++] = i;
        }
        if (!onlyTheseInBounds(id, b, size, used)) return EMPTY;
        int[] consume = new int[9];
        for (int u : used) consume[u] = 1;
        return new Recipe(MinecraftGL.DOOR_BOTTOM, 1, consume);
    }

    private static boolean isMatPickaxeAxe(int mat) {
        return mat == MinecraftGL.PLANKS || mat == MinecraftGL.STONE;
    }

    private static boolean onlyTheseInBounds(int[] id, Bounds b, int size, int[] allowed) {
        for (int y = 0; y < b.h; y++) for (int x = 0; x < b.w; x++) {
            int k = idx(b.x + x, b.y + y, size);
            if (id[k] == 0) continue;
            boolean ok = false;
            for (int a : allowed) if (a == k) { ok = true; break; }
            if (!ok) return false;
        }
        return true;
    }

    private static int idx(int x, int y, int size) { return y * size + x; }

    private static Bounds bounds(int[] id, int[] count, int size) {
        Bounds b = new Bounds();
        b.x = size;
        b.y = size;
        int maxX = -1, maxY = -1;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int i = idx(x, y, size);
            if (id[i] > 0 && count[i] > 0) {
                if (x < b.x) b.x = x;
                if (y < b.y) b.y = y;
                if (x > maxX) maxX = x;
                if (y > maxY) maxY = y;
            }
        }
        b.empty = maxX < 0;
        b.w = b.empty ? 0 : maxX - b.x + 1;
        b.h = b.empty ? 0 : maxY - b.y + 1;
        return b;
    }

    public static final class Recipe {
        public final int resultId;
        public final int resultCount;
        public final int[] consume;
        public Recipe(int resultId, int resultCount, int[] consume) {
            this.resultId = resultId;
            this.resultCount = resultCount;
            this.consume = consume;
        }
        public boolean empty() { return resultId <= 0 || resultCount <= 0; }
    }

    static final class Bounds { int x, y, w, h; boolean empty; }
}
