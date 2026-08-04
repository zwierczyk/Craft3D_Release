package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/**
 * Post-processing overlay: vignette (winieta), scanlines, itp.
 * Rysowane po wszystkich innych elementach UI ale przed hotbarem/tekstem.
 */
public final class PostProcessing {

    private PostProcessing() {}

    /** Vignette - ciemne rogi ekranu dla immersji. Wymaga GL_SMOOTH dla gradientu. */
    public static void drawVignette(int width, int height, float strength) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // WAZNE: wlaczamy smooth shading zeby quad mial interpolowany kolor.
        // Bez tego (GL_FLAT) wszystkie wierzcholki dostawaly kolor OSTATNIEGO,
        // co powodowalo szare pasy na dole i prawej krawedzi.
        glShadeModel(GL_SMOOTH);

        int size = (int)(Math.min(width, height) * 0.30f);

        // Górny: ciemne u góry, przezroczyste na dole
        glBegin(GL_QUADS);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(0, 0);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(width, 0);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(width, size);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(0, size);
        glEnd();

        // Dolny: przezroczyste u góry, ciemne na dole
        glBegin(GL_QUADS);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(0, height - size);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(width, height - size);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(width, height);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(0, height);
        glEnd();

        // Lewy: ciemne po lewej, przezroczyste po prawej
        glBegin(GL_QUADS);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(0, 0);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(size, 0);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(size, height);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(0, height);
        glEnd();

        // Prawy: przezroczyste po lewej, ciemne po prawej
        glBegin(GL_QUADS);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(width - size, 0);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(width, 0);
        glColor4f(0, 0, 0.03f, strength);
        glVertex2i(width, height);
        glColor4f(0, 0, 0, 0f);
        glVertex2i(width - size, height);
        glEnd();

        // Przywroc flat shading dla reszty renderingu (bloki uzywaja flat)
        glShadeModel(GL_FLAT);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    /** Subtelne scanlines dla feel'u retro/neon. */
    public static void drawScanlines(int width, int height, float alpha) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0, 0, 0, alpha);
        for (int y = 0; y < height; y += 3) {
            UIStyle.quad(0, y, width, 1);
        }
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }
}
