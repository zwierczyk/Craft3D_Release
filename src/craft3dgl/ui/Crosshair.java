package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/**
 * Crosshair MC-style. Z pulsujacym "hitmark" gdy trafisz oraz rozszerzeniem podczas kopania.
 */
public final class Crosshair {
    private Crosshair() {}

    private static double hitFlashTime = 0.0;   // 0..0.3 - anim po hicie
    private static double lastTime = -1;

    /** Wywolaj kiedy trafiles blok/entity - crosshair blyknie. */
    public static void triggerHit() { hitFlashTime = 0.35; }

    /**
     * Draw crosshair.
     * miningProgress 0..1 (0 = nie kopie, 1 = prawie skoncze) - rozszerza crosshair
     * currentTime - do animacji (glfwGetTime)
     */
    public static void draw(int width, int height, double miningProgress, double currentTime) {
        if (lastTime < 0) lastTime = currentTime;
        double dt = currentTime - lastTime;
        lastTime = currentTime;
        if (hitFlashTime > 0) hitFlashTime = Math.max(0, hitFlashTime - dt);

        int cx = width / 2;
        int cy = height / 2;
        // Rozszerzenie podczas kopania
        int expand = (int) (miningProgress * 6);
        int len = 10;
        int gap = 4 + expand;

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // 1. Dark outline
        glColor4f(0f, 0f, 0f, 0.85f);
        UIStyle.quad(cx - gap - len - 1, cy - 2, len + 2, 4);
        UIStyle.quad(cx + gap - 1, cy - 2, len + 2, 4);
        UIStyle.quad(cx - 2, cy - gap - len - 1, 4, len + 2);
        UIStyle.quad(cx - 2, cy + gap - 1, 4, len + 2);
        UIStyle.quad(cx - 2, cy - 2, 4, 4);

        // 2. White inner
        glColor4f(1f, 1f, 1f, 1f);
        UIStyle.quad(cx - gap - len, cy - 1, len, 2);
        UIStyle.quad(cx + gap, cy - 1, len, 2);
        UIStyle.quad(cx - 1, cy - gap - len, 2, len);
        UIStyle.quad(cx - 1, cy + gap, 2, len);
        UIStyle.quad(cx - 1, cy - 1, 2, 2);

        // 3. Hit flash - jasny cyjan pierscien
        if (hitFlashTime > 0) {
            float a = (float) (hitFlashTime / 0.35);
            int r = (int) (12 + (1 - a) * 10);
            glColor4f(0.10f, 0.85f, 1.00f, a * 0.85f);
            glBegin(GL_LINE_LOOP);
            int segs = 24;
            for (int i = 0; i < segs; i++) {
                double ang = i * Math.PI * 2 / segs;
                glVertex2d(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r);
            }
            glEnd();
            // Drugi pierscien w srodku
            glColor4f(1f, 1f, 1f, a * 0.5f);
            glBegin(GL_LINE_LOOP);
            int r2 = r - 3;
            for (int i = 0; i < segs; i++) {
                double ang = i * Math.PI * 2 / segs;
                glVertex2d(cx + Math.cos(ang) * r2, cy + Math.sin(ang) * r2);
            }
            glEnd();
        }

        // 4. Mining progress circle
        if (miningProgress > 0.02) {
            glColor4f(1.0f, 0.5f, 0.1f, 0.9f);
            int r = 18;
            int segs = 32;
            int upTo = (int) (miningProgress * segs);
            glBegin(GL_LINE_STRIP);
            for (int i = 0; i <= upTo; i++) {
                double ang = -Math.PI * 0.5 + i * Math.PI * 2 / segs;
                glVertex2d(cx + Math.cos(ang) * r, cy + Math.sin(ang) * r);
            }
            glEnd();
        }

        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    // Backward compat
    public static void draw(int width, int height) {
        draw(width, height, 0.0, System.nanoTime() * 1e-9);
    }
}
