package craft3dgl.blaze3d.shadow;

import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Kalkulacje macierzy transformacji z pozycji slonca.
 *
 * Uzywamy orthographic projection (nie perspective!) bo slonce jest daleko,
 * jego promienie sa rownolegle. Ortho box obejmuje aktualny \"shadow area\" wokol gracza.
 */
public class SunLightMatrix {

    /** Rozmiar boxa ortho wokol kamery (w blokach) - jak daleko idzie shadow. */
    private static final float SHADOW_DISTANCE = 64.0f;

    /** Kat elewacji slonca (0 = horyzont, 90 = zenit). */
    private static float sunElevationDeg = 55.0f;
    /** Azimuth slonca (kompas: 0=N, 90=E, 180=S, 270=W). */
    private static float sunAzimuthDeg = 135.0f;

    /**
     * Ladowanie macierzy view+proj z pozycji slonca do glMatrixMode(GL_PROJECTION/MODELVIEW).
     * Trzeba wywolac PRZED rysowaniem sceny do shadow map.
     * Kamera slonca patrzy na graczera.
     */
    public static void setupSunView(double playerX, double playerY, double playerZ) {
        // Kierunek promieni slonca (jednostkowy wektor OD slonca DO ziemi)
        double elevRad = Math.toRadians(sunElevationDeg);
        double azRad = Math.toRadians(sunAzimuthDeg);
        double dirX = -Math.cos(elevRad) * Math.sin(azRad);
        double dirY = -Math.sin(elevRad);
        double dirZ = -Math.cos(elevRad) * Math.cos(azRad);

        // Pozycja slonca: daleko OD gracza w kierunku PRZECIWNYM do promieni
        double distance = SHADOW_DISTANCE * 2.0;
        double sunX = playerX - dirX * distance;
        double sunY = playerY - dirY * distance;
        double sunZ = playerZ - dirZ * distance;

        // Ortho projection - box wokol gracza
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        float s = SHADOW_DISTANCE;
        GL11.glOrtho(-s, s, -s, s, 0.1, distance * 3.0);

        // View: kamera slonca patrzy na gracza
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        gluLookAt(sunX, sunY, sunZ,      // eye
                  playerX, playerY, playerZ,  // center
                  0, 1, 0);              // up
    }

    /** Przywroc default projection/modelview po shadow pass. */
    public static void restoreView() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }

    /**
     * Zwraca macierz LightViewProjection ktora idzie do glowny shader jako uniform.
     * Uwaga: musi byc wywolane W CZASIE setupSunView (kiedy projection+modelview sa ustawione na slonce).
     */
    public static float[] getLightSpaceMatrix() {
        // proj * view = LightViewProjection
        FloatBuffer proj = BufferUtils.createFloatBuffer(16);
        FloatBuffer view = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, proj);
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, view);

        float[] p = new float[16];
        float[] v = new float[16];
        proj.get(p);
        view.get(v);

        // Wynik = P * V (column-major mnozenie)
        float[] r = new float[16];
        multiplyMatrices(p, v, r);
        return r;
    }

    private static void multiplyMatrices(float[] a, float[] b, float[] out) {
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) sum += a[k * 4 + row] * b[col * 4 + k];
                out[col * 4 + row] = sum;
            }
        }
    }

    /** LookAt bez GLU (bo GLU moze byc niedostepne). */
    private static void gluLookAt(double ex, double ey, double ez,
                                   double cx, double cy, double cz,
                                   double ux, double uy, double uz) {
        double fx = cx - ex, fy = cy - ey, fz = cz - ez;
        double flen = Math.sqrt(fx*fx + fy*fy + fz*fz);
        fx /= flen; fy /= flen; fz /= flen;
        // s = f cross up
        double sx = fy*uz - fz*uy;
        double sy = fz*ux - fx*uz;
        double sz = fx*uy - fy*ux;
        double slen = Math.sqrt(sx*sx + sy*sy + sz*sz);
        sx /= slen; sy /= slen; sz /= slen;
        // u2 = s cross f
        double u2x = sy*fz - sz*fy;
        double u2y = sz*fx - sx*fz;
        double u2z = sx*fy - sy*fx;
        FloatBuffer m = BufferUtils.createFloatBuffer(16);
        m.put(new float[]{
            (float)sx, (float)u2x, (float)-fx, 0,
            (float)sy, (float)u2y, (float)-fy, 0,
            (float)sz, (float)u2z, (float)-fz, 0,
            0, 0, 0, 1
        });
        m.flip();
        GL11.glMultMatrixf(m);
        GL11.glTranslated(-ex, -ey, -ez);
    }

    public static void setSunAngle(float elevation, float azimuth) {
        sunElevationDeg = elevation;
        sunAzimuthDeg = azimuth;
    }
    public static float getSunElevation() { return sunElevationDeg; }
    public static float getSunAzimuth() { return sunAzimuthDeg; }
}
