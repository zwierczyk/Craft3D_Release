package craft3dgl.entities;

/**
 * Czasteczka 3D - kilka rodzajow (kind):
 *  0 = serduszko (heal / love)
 *  1 = obrazenia (dark tick)
 *  2 = okruch bloku (rotowany, kolorowy)
 *  3 = splash wody (jasny, unosi sie do gory potem opada)
 *  4 = dust / pyl (szary, wolno opada)
 *  5 = spark / iskra (zolto-pomaranczowe, jasne addytywne)
 *  6 = smoke / dym (szary, unosi sie do gory, zanika)
 *  7 = leaf / listek (zielony, wiruje w powietrzu)
 */
public final class Particle {
    public double x, y, z;
    public double vx, vy, vz;
    public double age;
    public double maxAge;
    public int kind;
    public float r, g, b; // kolor dla kind=2,3,4,5,6,7
    public double size = 0.06;
    public double gravity = 4.5;   // domyslna grawitacja
    public double drag = 0.96;     // domyslne tlumienie xz
    public boolean additive = false; // additive blending (iskry)

    public Particle(double x, double y, double z,
                    double vx, double vy, double vz,
                    double maxAge, int kind) {
        this.x = x; this.y = y; this.z = z;
        this.vx = vx; this.vy = vy; this.vz = vz;
        this.maxAge = maxAge;
        this.kind = kind;
    }

    public Particle color(float r, float g, float b) {
        this.r = r; this.g = g; this.b = b;
        return this;
    }

    public Particle size(double s) { this.size = s; return this; }
    public Particle gravity(double g) { this.gravity = g; return this; }
    public Particle drag(double d) { this.drag = d; return this; }
    public Particle additive(boolean a) { this.additive = a; return this; }
}
