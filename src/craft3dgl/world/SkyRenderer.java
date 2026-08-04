package craft3dgl.world;

import static org.lwjgl.opengl.GL11.*;

/**
 * Gradientowe niebo z day/night cycle. Rysowane jako 2D quad w tle PRZED chunkami.
 * dayFraction: 0..1 (0=polnoc, 0.25=wschod, 0.5=poludnie, 0.75=zachod).
 */
public final class SkyRenderer {
    private SkyRenderer() {}

    /** Zwraca kolor top/bottom nieba dla dayFraction. */
    public static float[] getSkyColors(double dayFraction) {
        // Kilka keyframes:
        // 0.0 (noc)    top: 0.03,0.05,0.13   horizon: 0.10,0.13,0.25
        // 0.20 (przed wschodem) top: 0.20,0.20,0.42 horiz: 0.85,0.55,0.45
        // 0.30 (dzien) top: 0.28,0.55,0.88 horiz: 0.70,0.85,0.98
        // 0.7  (dzien) top: 0.28,0.55,0.88 horiz: 0.70,0.85,0.98
        // 0.80 (zachod) top: 0.30,0.20,0.45 horiz: 1.00,0.55,0.35
        // 0.9 (noc)  ...
        float[][] keys = {
            {0.00f,  0.03f, 0.05f, 0.13f,   0.10f, 0.13f, 0.25f}, // noc
            {0.20f,  0.20f, 0.20f, 0.42f,   0.85f, 0.55f, 0.45f}, // wschod
            {0.30f,  0.28f, 0.55f, 0.88f,   0.70f, 0.85f, 0.98f}, // dzien
            {0.70f,  0.28f, 0.55f, 0.88f,   0.70f, 0.85f, 0.98f}, // dzien
            {0.80f,  0.30f, 0.20f, 0.45f,   1.00f, 0.55f, 0.35f}, // zachod
            {0.95f,  0.05f, 0.06f, 0.16f,   0.15f, 0.15f, 0.30f}, // noc
            {1.00f,  0.03f, 0.05f, 0.13f,   0.10f, 0.13f, 0.25f}, // noc (loop)
        };
        double t = dayFraction % 1.0;
        for (int i = 0; i < keys.length - 1; i++) {
            if (t >= keys[i][0] && t <= keys[i+1][0]) {
                double range = keys[i+1][0] - keys[i][0];
                double f = range > 0 ? (t - keys[i][0]) / range : 0;
                float[] out = new float[6];
                for (int j = 0; j < 6; j++) {
                    out[j] = (float)(keys[i][1+j] * (1-f) + keys[i+1][1+j] * f);
                }
                return out;
            }
        }
        return new float[]{0.28f, 0.55f, 0.88f, 0.70f, 0.85f, 0.98f};
    }

    /** Rysuje gradientowe niebo. Wywolywac PRZED setupCamera(). */
    public static void drawSky(int screenWidth, int screenHeight, double pitch) {
        drawSky(screenWidth, screenHeight, pitch, 0.5);
    }

    public static void drawSky(int screenWidth, int screenHeight, double pitch, double dayFraction) {
        float[] c = getSkyColors(dayFraction);
        float topR = c[0], topG = c[1], topB = c[2];
        float horR = c[3], horG = c[4], horB = c[5];

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, screenWidth, screenHeight, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_FOG);
        glDisable(GL_TEXTURE_2D);
        glShadeModel(GL_SMOOTH);

        double horizonOffset = pitch * screenHeight * 0.35;
        int horizonY = screenHeight / 2 + (int) horizonOffset;

        // Ground pas dolny - zawsze troche ciemniejszy
        float groundR = horR * 0.75f;
        float groundG = horG * 0.80f;
        float groundB = horB * 0.85f;

        glBegin(GL_QUADS);
        glColor3f(topR, topG, topB);
        glVertex2i(0, 0);
        glColor3f(topR, topG, topB);
        glVertex2i(screenWidth, 0);
        glColor3f(horR, horG, horB);
        glVertex2i(screenWidth, horizonY);
        glColor3f(horR, horG, horB);
        glVertex2i(0, horizonY);
        glEnd();

        glBegin(GL_QUADS);
        glColor3f(horR, horG, horB);
        glVertex2i(0, horizonY);
        glColor3f(horR, horG, horB);
        glVertex2i(screenWidth, horizonY);
        glColor3f(groundR, groundG, groundB);
        glVertex2i(screenWidth, screenHeight);
        glColor3f(groundR, groundG, groundB);
        glVertex2i(0, screenHeight);
        glEnd();

        glShadeModel(GL_FLAT);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_FOG);
        glEnable(GL_DEPTH_TEST);
        glColor3f(1, 1, 1);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    /** Zwraca aktualny kolor mgly zsynchronizowany z niebem (kolor horyzontu). */
    public static float[] getFogColor(double dayFraction) {
        float[] c = getSkyColors(dayFraction);
        return new float[]{c[3], c[4], c[5], 1f};
    }
}
