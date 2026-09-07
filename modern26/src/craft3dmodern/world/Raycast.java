package craft3dmodern.world;

public final class Raycast {
    public static final class Hit {
        public final int x;
        public final int y;
        public final int z;
        public final int face;
        public final double dist;

        Hit(int x, int y, int z, int face, double dist) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.face = face;
            this.dist = dist;
        }
    }

    private Raycast() {}

    public static Hit cast(World w, double ox, double oy, double oz,
                           double dx, double dy, double dz, double maxDist) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) return null;
        dx /= len;
        dy /= len;
        dz /= len;
        int x = (int) Math.floor(ox);
        int y = (int) Math.floor(oy);
        int z = (int) Math.floor(oz);
        int sx = dx > 0 ? 1 : (dx < 0 ? -1 : 0);
        int sy = dy > 0 ? 1 : (dy < 0 ? -1 : 0);
        int sz = dz > 0 ? 1 : (dz < 0 ? -1 : 0);
        double tdx = sx != 0 ? Math.abs(1.0 / dx) : Double.POSITIVE_INFINITY;
        double tdy = sy != 0 ? Math.abs(1.0 / dy) : Double.POSITIVE_INFINITY;
        double tdz = sz != 0 ? Math.abs(1.0 / dz) : Double.POSITIVE_INFINITY;
        double tmx = sx != 0 ? ((sx > 0 ? x + 1 - ox : ox - x)) * Math.abs(1.0 / dx)
                : Double.POSITIVE_INFINITY;
        double tmy = sy != 0 ? ((sy > 0 ? y + 1 - oy : oy - y)) * Math.abs(1.0 / dy)
                : Double.POSITIVE_INFINITY;
        double tmz = sz != 0 ? ((sz > 0 ? z + 1 - oz : oz - z)) * Math.abs(1.0 / dz)
                : Double.POSITIVE_INFINITY;
        int face = -1;
        double t = 0;
        int guard = 0;
        while (t <= maxDist && guard++ < 512) {
            if (w.get(x, y, z) != BlockIds.AIR) {
                return new Hit(x, y, z, face, t);
            }
            if (tmx < tmy && tmx < tmz) {
                x += sx;
                t = tmx;
                tmx += tdx;
                face = sx > 0 ? 4 : 5;
            } else if (tmy < tmz) {
                y += sy;
                t = tmy;
                tmy += tdy;
                face = sy > 0 ? 0 : 1;
            } else {
                z += sz;
                t = tmz;
                tmz += tdz;
                face = sz > 0 ? 2 : 3;
            }
        }
        return null;
    }
}
