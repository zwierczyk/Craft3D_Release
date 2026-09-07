package craft3dmodern.world;


public final class World {
    public final int sx, sy, sz;
    private final int[] blocks;

    public World(int sx, int sy, int sz) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.blocks = new int[sx * sy * sz];
    }

    private int index(int x, int y, int z) {
        return (y * sz + z) * sx + x;
    }

    public boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < sx && y >= 0 && y < sy && z >= 0 && z < sz;
    }

    public int get(int x, int y, int z) {
        if (!inBounds(x, y, z)) return BlockIds.AIR;
        return blocks[index(x, y, z)];
    }

    public void set(int x, int y, int z, int id) {
        if (inBounds(x, y, z)) blocks[index(x, y, z)] = id;
    }

    
    public int topSolid(int x, int z) {
        for (int y = sy - 1; y >= 0; y--) {
            if (get(x, y, z) != BlockIds.AIR) return y;
        }
        return -1;
    }
}
