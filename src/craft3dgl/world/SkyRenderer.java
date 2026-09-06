package craft3dgl.world;

import static org.lwjgl.opengl.GL11.*;

/**
 * Minecraft 1.12-style overworld sky/fog colours drawn behind the chunk passes.
 * dayFraction: 0..1 (0=polnoc, 0.25=wschod, 0.5=poludnie, 0.75=zachod).
 */
public final class SkyRenderer {
    private SkyRenderer() {}

    /** Vanilla-overworld sky and fog colours, without a full-screen cinematic sunset tint. */
    public static float[] getSkyColors(double dayFraction) {
        float raw = LightEngine.skyDayMultiplier(dayFraction);
        float daylight = clamp((raw - 0.15f) / 0.85f);
        // World.getSkyColor and WorldProvider.getFogColor constants from MCP 9.40.
        float skyFactor = daylight;
        float topR = 0.50f * skyFactor;
        float topG = 0.66275f * skyFactor;
        float topB = 1.00f * skyFactor;
        float horizonR = 0.7529412f * (daylight * 0.94f + 0.06f);
        float horizonG = 0.84705883f * (daylight * 0.94f + 0.06f);
        float horizonB = 1.0f * (daylight * 0.91f + 0.09f);
        return new float[]{topR, topG, topB, horizonR, horizonG, horizonB};
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
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
