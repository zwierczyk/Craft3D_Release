package craft3dgl;

public final class DroppedItemGL {
    public double x, y, z;
    public double vx, vy, vz;
    public double age;
    public int id;
    public int count;

    public DroppedItemGL(double x, double y, double z, int id, int count) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.id = id;
        this.count = count;
        double a = Math.random() * Math.PI * 2.0;
        double sp = 0.15 + Math.random() * 0.20;
        vx = Math.cos(a) * sp;
        vz = Math.sin(a) * sp;
        vy = 0.40 + Math.random() * 0.25;
    }
}
