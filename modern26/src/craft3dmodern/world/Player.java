package craft3dmodern.world;

public final class Player {
    public static final double WIDTH = 0.6;
    public static final double HEIGHT = 1.8;
    public static final double EYE_HEIGHT = 1.62;
    public static final double WALK_SPEED = 4.317;
    public static final double SPRINT_SPEED = 5.612;
    public static final double SNEAK_SPEED = 1.2951;
    public static final double FLY_SPEED = 10.8;
    public static final double SPRINT_FLY_SPEED = 21.6;
    public static final double GRAVITY = 32.0;
    public static final double JUMP_SPEED = 8.4;

    public double x;
    public double y;
    public double z;
    public double vx;
    public double vy;
    public double vz;
    public boolean onGround;
    public float yaw;
    public float pitch;

    public Player(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double minX() {
        return x - WIDTH / 2;
    }

    public double maxX() {
        return x + WIDTH / 2;
    }

    public double minY() {
        return y;
    }

    public double maxY() {
        return y + HEIGHT;
    }

    public double minZ() {
        return z - WIDTH / 2;
    }

    public double maxZ() {
        return z + WIDTH / 2;
    }

    public double eyeX() {
        return x;
    }

    public double eyeY() {
        return y + EYE_HEIGHT;
    }

    public double eyeZ() {
        return z;
    }

    public double dirX() {
        return -Math.sin(yaw) * Math.cos(pitch);
    }

    public double dirY() {
        return Math.sin(pitch);
    }

    public double dirZ() {
        return -Math.cos(yaw) * Math.cos(pitch);
    }

    public void update(World w, double dt,
                       boolean forward, boolean back, boolean left, boolean right,
                       boolean jump, boolean sprint, boolean sneak, boolean flying) {
        double fx = -Math.sin(yaw);
        double fz = -Math.cos(yaw);
        double rx = -fz;
        double rz = fx;
        double dirx = 0;
        double dirz = 0;
        if (forward) {
            dirx += fx;
            dirz += fz;
        }
        if (back) {
            dirx -= fx;
            dirz -= fz;
        }
        if (right) {
            dirx += rx;
            dirz += rz;
        }
        if (left) {
            dirx -= rx;
            dirz -= rz;
        }
        double len = Math.sqrt(dirx * dirx + dirz * dirz);
        if (len > 1e-5) {
            dirx /= len;
            dirz /= len;
        } else {
            dirx = dirz = 0;
        }
        double speed;
        if (flying) {
            speed = sprint ? SPRINT_FLY_SPEED : FLY_SPEED;
        } else if (sneak) {
            speed = SNEAK_SPEED;
        } else {
            speed = sprint ? SPRINT_SPEED : WALK_SPEED;
        }
        double k = onGround ? 12.0 : (flying ? 8.0 : 2.8);
        double f = Math.min(1.0, k * dt);
        vx += (dirx * speed - vx) * f;
        vz += (dirz * speed - vz) * f;
        if (flying) {
            double up = (jump ? 1 : 0) - (sneak ? 1 : 0);
            double target = up * FLY_SPEED;
            vy += (target - vy) * Math.min(1.0, 8.0 * dt);
        } else {
            vy -= GRAVITY * dt;
            if (vy < -60) vy = -60;
            if (onGround && jump) {
                vy = JUMP_SPEED;
                onGround = false;
            }
        }
        moveCollide(w, dt);
    }

    private void moveCollide(World w, double dt) {
        moveAxis(w, 0, vx * dt);
        moveAxis(w, 2, vz * dt);
        boolean wasFalling = vy < 0;
        boolean collided = moveAxis(w, 1, vy * dt);
        if (wasFalling && collided) {
            onGround = true;
            vy = 0;
        } else if (!wasFalling && collided) {
            vy = 0;
        } else if (!collided) {
            onGround = false;
        }
    }

    private boolean moveAxis(World w, int axis, double d) {
        if (d == 0) return false;
        int n = Math.max(1, (int) Math.ceil(Math.abs(d) / 0.03));
        double step = d / n;
        boolean hit = false;
        for (int i = 0; i < n; i++) {
            double px = x, py = y, pz = z;
            if (axis == 0) x += step;
            else if (axis == 1) y += step;
            else z += step;
            if (w.collidesBox(minX(), minY(), minZ(), maxX(), maxY(), maxZ())) {
                x = px;
                y = py;
                z = pz;
                hit = true;
                break;
            }
        }
        return hit;
    }
}
