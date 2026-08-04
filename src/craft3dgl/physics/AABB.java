package craft3dgl.physics;

/**
 * Axis-Aligned Bounding Box - prostokątne pudełko kolizyjne.
 */
public final class AABB {
    public double x0, y0, z0, x1, y1, z1;

    public AABB(double x0, double y0, double z0, double x1, double y1, double z1) {
        this.x0 = x0; this.y0 = y0; this.z0 = z0;
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
    }

    /** Czy dwa AABB się przecinają. */
    public static boolean intersects(double ax0, double ay0, double az0,
                                     double ax1, double ay1, double az1,
                                     double bx0, double by0, double bz0,
                                     double bx1, double by1, double bz1) {
        return ax1 > bx0 && ax0 < bx1
            && ay1 > by0 && ay0 < by1
            && az1 > bz0 && az0 < bz1;
    }

    public boolean intersects(AABB other) {
        return intersects(x0, y0, z0, x1, y1, z1,
                         other.x0, other.y0, other.z0, other.x1, other.y1, other.z1);
    }

    /** Czy punkt jest wewnątrz AABB. */
    public boolean contains(double x, double y, double z) {
        return x >= x0 && x < x1 && y >= y0 && y < y1 && z >= z0 && z < z1;
    }
}
