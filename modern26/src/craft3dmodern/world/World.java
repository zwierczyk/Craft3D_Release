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

    public boolean solidAt(double x, double y, double z) {
        return get((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)) != BlockIds.AIR;
    }

    public boolean collidesBox(double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ) {
        int x0 = (int) Math.floor(minX);
        int y0 = (int) Math.floor(minY);
        int z0 = (int) Math.floor(minZ);
        int x1 = (int) Math.floor(maxX - 1e-9);
        int y1 = (int) Math.floor(maxY - 1e-9);
        int z1 = (int) Math.floor(maxZ - 1e-9);
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    if (get(x, y, z) != BlockIds.AIR) return true;
                }
            }
        }
        return false;
    }
}
