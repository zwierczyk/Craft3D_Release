package craft3dgl.combat;

/**
 * Wynik raycastu: trafiony blok + normal (na której ścianie) + dystans.
 */
public final class Hit {
    public final boolean hit;
    public final int x, y, z;
    public final int nx, ny, nz;  // normal vector (która ściana)
    public final int block;
    public final double dist;

    public Hit(boolean hit, int x, int y, int z, int nx, int ny, int nz, int block, double dist) {
        this.hit = hit;
        this.x = x; this.y = y; this.z = z;
        this.nx = nx; this.ny = ny; this.nz = nz;
        this.block = block;
        this.dist = dist;
    }

    public static final Hit MISS = new Hit(false, 0, 0, 0, 0, 0, 0, 0, 0);
}
