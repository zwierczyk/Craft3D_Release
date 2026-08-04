package craft3dgl.ui;

/**
 * Projekcja punktu swiata na wspolrzedne ekranu (2D orto) dla popupow HUD.
 * Bazuje na wzorze kamery gry: forward = (sin(yaw)*cos(pitch), sin(pitch), cos(yaw)*cos(pitch)).
 * Right = (cos(yaw), 0, -sin(yaw)). Up = right x forward.
 */
public final class WorldToScreen {
    public double screenX, screenY;
    public boolean visible;
    public double depth;

    public void project(double wx, double wy, double wz,
                        double camX, double camY, double camZ,
                        double yaw, double pitch,
                        double fovDeg, double aspect,
                        int screenW, int screenH) {
        double dx = wx - camX;
        double dy = wy - camY;
        double dz = wz - camZ;

        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);

        // Forward
        double fx = sy * cp, fy = sp, fz = cy * cp;
        // Right = (cos(yaw), 0, -sin(yaw))
        double rx = cy, ry = 0, rz = -sy;
        // Up = right x forward
        double ux = ry * fz - rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy - ry * fx;

        // Wspolrzedne w ukladzie kamery (right, up, forward)
        double camRight   = dx * rx + dy * ry + dz * rz;
        double camUp      = dx * ux + dy * uy + dz * uz;
        double camForward = dx * fx + dy * fy + dz * fz;

        if (camForward <= 0.15) { visible = false; return; }

        double fovRad = Math.toRadians(fovDeg);
        double f = 1.0 / Math.tan(fovRad * 0.5);
        double ndcX = (camRight * f / aspect) / camForward;
        double ndcY = (camUp * f) / camForward;
        screenX = (ndcX * 0.5 + 0.5) * screenW;
        screenY = (0.5 - ndcY * 0.5) * screenH;
        depth = camForward;
        visible = screenX >= -100 && screenX < screenW + 100 && screenY >= -100 && screenY < screenH + 100;
    }
}
