package craft3dgl.entities;

/**
 * XP orb - unosi sie w powietrzu, przyciaga do gracza (magnes) gdy blisko.
 */
public final class ExperienceOrb {
    public double x, y, z;
    public double vx, vy, vz;
    public int xpValue;               // ile XP daje przy zebraniu
    public double age;                // wiek w sekundach (do animacji bobbing)
    public double lifetime;           // od kiedy istnieje (max 300s = 5min)
    public boolean magneted;          // czy zostalo przyciagniete do gracza
    public double bobOffset;          // random phase dla bobbing

    public ExperienceOrb(double x, double y, double z, int xpValue) {
        this.x = x; this.y = y; this.z = z;
        this.xpValue = xpValue;
        this.age = 0;
        this.lifetime = 0;
        // Random initial velocity - orbs sie rozpraszaja
        double angle = Math.random() * Math.PI * 2;
        double speed = 1.5 + Math.random() * 1.0;
        this.vx = Math.cos(angle) * speed;
        this.vz = Math.sin(angle) * speed;
        this.vy = 2.5 + Math.random() * 1.5;
        this.bobOffset = Math.random() * Math.PI * 2;
    }
}
