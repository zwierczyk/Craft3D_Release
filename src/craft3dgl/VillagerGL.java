package craft3dgl;

public final class VillagerGL {
    public double x, y, z;
    public double vx, vz, vy;
    public boolean onGround;
    public double yaw, targetYaw;
    public double walkTimer;
    public double age;
    public double displayY;
    public boolean displayInit;
    public int profession; // 0 farmer, 1 librarian, 2 toolsmith
    public int health = 20;

    // Pole spawnu - wioska do ktorej villager nalezy. Uzywamy do trzymania ich blisko.
    public double homeX, homeZ;
    public boolean hasHome;

    public VillagerGL(double x, double y, double z, int profession) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.displayY = y;
        this.displayInit = true;
        this.profession = profession;
        this.yaw = Math.random() * Math.PI * 2.0;
        this.targetYaw = yaw;
        this.walkTimer = Math.random() * 3.0;
        this.homeX = x;
        this.homeZ = z;
        this.hasHome = true;
    }
}
