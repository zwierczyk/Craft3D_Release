package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/**
 * Pomocnicze rysowanie 3D cuboidow (prostopadloscianow) - uzywane wszedzie
 * w modelach (zwierzeta, villagerzy, gracz, drzwi).
 *
 * Globalny tint (mnoznik RGB) pozwala przyciemniac cale modele bez ingerencji
 * w kod rendererow - stosowany do dynamicznego swiatla (dzien/noc + jaskinie).
 */
public final class CuboidHelper {
    private CuboidHelper() {}

    // Globalny tint - domyslnie 1,1,1 (bez zmian)
    private static float tintR = 1f, tintG = 1f, tintB = 1f;

    /** Ustaw globalny tint (np. dla ciemnosci nocy / jaskini). */
    public static void setTint(float r, float g, float b) {
        tintR = r; tintG = g; tintB = b;
    }
    public static void clearTint() { tintR = tintG = tintB = 1f; }

    /** Ustaw kolor RGB (mnozony przez globalny tint). */
    public static void color(float r, float g, float b) {
        glColor3f(r * tintR, g * tintG, b * tintB);
    }

    /** Narysuj prostopadloscian o podanych granicach (6 scian quads). */
    public static void drawCuboid(double x0, double y0, double z0, double x1, double y1, double z1) {
        glBegin(GL_QUADS);
        glVertex3d(x0,y0,z1); glVertex3d(x1,y0,z1); glVertex3d(x1,y1,z1); glVertex3d(x0,y1,z1);
        glVertex3d(x1,y0,z0); glVertex3d(x0,y0,z0); glVertex3d(x0,y1,z0); glVertex3d(x1,y1,z0);
        glVertex3d(x1,y0,z1); glVertex3d(x1,y0,z0); glVertex3d(x1,y1,z0); glVertex3d(x1,y1,z1);
        glVertex3d(x0,y0,z0); glVertex3d(x0,y0,z1); glVertex3d(x0,y1,z1); glVertex3d(x0,y1,z0);
        glVertex3d(x0,y1,z1); glVertex3d(x1,y1,z1); glVertex3d(x1,y1,z0); glVertex3d(x0,y1,z0);
        glVertex3d(x0,y0,z0); glVertex3d(x1,y0,z0); glVertex3d(x1,y0,z1); glVertex3d(x0,y0,z1);
        glEnd();
    }
}
