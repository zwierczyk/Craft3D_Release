package craft3dgl.physics;

/**
 * Pomocnicze obliczenia ruchu gracza/entity.
 */
public final class MovementHelpers {
    private MovementHelpers() {}

    /** Wektor ruchu wzgledem yaw i input WASD.
     *  Zwraca [dx, dz] - znormalizowane na sile motion. */
    public static double[] computeMoveVector(double yaw, double forward, double strafe, double speed, double dt) {
        double len = Math.sqrt(forward * forward + strafe * strafe);
        if (len > 0) { forward /= len; strafe /= len; }
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        double dx = (sin * forward + cos * strafe) * speed * dt;
        double dz = (cos * forward - sin * strafe) * speed * dt;
        return new double[]{dx, dz};
    }

    /** Aktualizuj fazę chodu na podstawie przebytego dystansu. */
    public static double advanceWalkPhase(double currentPhase, double moveDistance, boolean onGround, double dt) {
        if (moveDistance > 0.001 && onGround) {
            double newPhase = currentPhase + moveDistance * 7.0;
            if (newPhase > Math.PI * 4) newPhase -= Math.PI * 4;
            return newPhase;
        } else {
            double target = Math.round(currentPhase / Math.PI) * Math.PI;
            return currentPhase + (target - currentPhase) * Math.min(1.0, dt * 8.0);
        }
    }

    /** Tick swing timera (machniecia reki). */
    public static double updateSwingTimer(double currentSwing, double dt) {
        if (currentSwing <= 0) return 0;
        double next = currentSwing - dt / 0.30;
        return next < 0 ? 0 : next;
    }
}
