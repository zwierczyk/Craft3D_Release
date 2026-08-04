package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/**
 * Czerwony blysk na ekranie gdy gracz dostaje damage (jak w MC).
 * Rysowany po wszystkich elementach 3D ale przed HUD.
 */
public final class DamageOverlay {
    private DamageOverlay() {}

    /** Rysuje czerwony overlay z alpha = intensity (0-1). */
    public static void draw(int width, int height, double intensity) {
        if (intensity < 0.01) return;
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glShadeModel(GL_SMOOTH);

        // Vignette-style czerwony: mocniejszy w rogach, slabszy w srodku
        float alpha = (float) Math.min(0.55, intensity);
        int size = (int)(Math.min(width, height) * 0.55f);

        // Górny pas
        glBegin(GL_QUADS);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(0, 0);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(width, 0);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(width, size);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(0, size);
        glEnd();

        // Dolny pas
        glBegin(GL_QUADS);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(0, height - size);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(width, height - size);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(width, height);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(0, height);
        glEnd();

        // Lewy
        glBegin(GL_QUADS);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(0, 0);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(size, 0);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(size, height);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(0, height);
        glEnd();

        // Prawy
        glBegin(GL_QUADS);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(width - size, 0);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(width, 0);
        glColor4f(0.8f, 0.05f, 0.05f, alpha);
        glVertex2i(width, height);
        glColor4f(0.8f, 0.05f, 0.05f, 0f);
        glVertex2i(width - size, height);
        glEnd();

        glShadeModel(GL_FLAT);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }
}
