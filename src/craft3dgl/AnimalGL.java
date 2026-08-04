package craft3dgl;

public final class AnimalGL {
    public static final int PIG = 1;
    public static final int COW = 2;
    public static final int SHEEP = 3;

    public int type;
    public double x, y, z;
    public double vx, vz;
    public double vy;
    public boolean onGround;
    public double yaw;
    public double targetYaw;
    public double walkTimer;
    public int health;
    public double age;
    public double displayY;
    public boolean displayInit;

    // Panika
    public double panicTimer;
    public double panicDirX;
    public double panicDirZ;
    public double panicRecalc;
    public double panicRedirect;

    public AnimalGL(int type, double x, double y, double z) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.displayY = y;
        this.displayInit = true;
        this.health = type == COW ? 12 : type == SHEEP ? 8 : 10;
        this.yaw = Math.random() * Math.PI * 2;
        this.targetYaw = this.yaw;
        this.walkTimer = Math.random() * 3;
    }

    public boolean isPanicking() { return panicTimer > 0; }
}
