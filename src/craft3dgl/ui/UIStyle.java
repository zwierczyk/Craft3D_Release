package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/**
 * Dark Neon UI theme - ciemne panele, neon accent (cyjan/fiolet), swiecace outline.
 */
public final class UIStyle {

    // === PALETA KOLOROW (dark neon theme) ===
    public static final float PANEL_BG_R = 0.06f, PANEL_BG_G = 0.08f, PANEL_BG_B = 0.12f;      // ciemne granatowe tlo
    public static final float PANEL_BG_A = 0.92f;
    public static final float PANEL_BORDER_R = 0.10f, PANEL_BORDER_G = 0.85f, PANEL_BORDER_B = 1.00f;  // cyjan neon
    public static final float SLOT_BG_R = 0.04f, SLOT_BG_G = 0.06f, SLOT_BG_B = 0.10f;
    public static final float SLOT_BORDER_R = 0.15f, SLOT_BORDER_G = 0.20f, SLOT_BORDER_B = 0.30f;
    public static final float SLOT_HOVER_R = 0.10f, SLOT_HOVER_G = 0.85f, SLOT_HOVER_B = 1.00f;   // cyjan hover
    public static final float SLOT_SELECTED_R = 0.75f, SLOT_SELECTED_G = 0.30f, SLOT_SELECTED_B = 1.00f;  // fiolet selected

    public static final float TEXT_MAIN = 1.00f;         // biały
    public static final float TEXT_SHADOW = 0.10f;       // ciemny shadow
    public static final float TEXT_ACCENT_R = 0.10f, TEXT_ACCENT_G = 0.85f, TEXT_ACCENT_B = 1.00f;

    private UIStyle() {}

    // ====== PROSTE PRYMITYWY ======

    public static void quad(int x, int y, int w, int h) {
        glBegin(GL_QUADS);
        glVertex2i(x, y); glVertex2i(x + w, y); glVertex2i(x + w, y + h); glVertex2i(x, y + h);
        glEnd();
    }

    public static void lineRect(int x, int y, int w, int h) {
        glBegin(GL_LINE_LOOP);
        glVertex2i(x, y); glVertex2i(x + w, y); glVertex2i(x + w, y + h); glVertex2i(x, y + h);
        glEnd();
    }

    public static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // ====== PANEL DARK NEON ======

    /** Panel z ciemnym półprzezroczystym tłem + świecącą ramką neon. */
    public static void drawPanel(int x, int y, int w, int h) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // === CIEN pod panelem ===
        glColor4f(0f, 0f, 0f, 0.60f);
        quad(x + 8, y + 12, w, h);

        // === TLO panelu - ciemne granatowe z gradientem ===
        glShadeModel(GL_SMOOTH);
        glBegin(GL_QUADS);
        glColor4f(0.08f, 0.10f, 0.16f, PANEL_BG_A);
        glVertex2i(x, y); glVertex2i(x + w, y);
        glColor4f(0.04f, 0.05f, 0.10f, PANEL_BG_A);
        glVertex2i(x + w, y + h); glVertex2i(x, y + h);
        glEnd();
        glShadeModel(GL_FLAT);

        // === ZEWNETRZNA RAMKA CZARNA (2px) ===
        glColor4f(0.02f, 0.02f, 0.04f, 1f);
        quad(x, y, w, 2); quad(x, y, 2, h);
        quad(x, y + h - 2, w, 2); quad(x + w - 2, y, 2, h);

        // === NEON GLOW RAMKA (cyjan) ===
        // Wewnetrzna ramka 1px czysty cyjan
        glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, 0.85f);
        quad(x + 2, y + 2, w - 4, 1);
        quad(x + 2, y + 2, 1, h - 4);
        quad(x + 2, y + h - 3, w - 4, 1);
        quad(x + w - 3, y + 2, 1, h - 4);

        // === GLOW BLUR (siły 2, malejacy alpha) ===
        for (int i = 1; i <= 3; i++) {
            float a = 0.20f - i * 0.05f;
            glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, a);
            lineRect(x - i, y - i, w + i * 2, h + i * 2);
        }

        // === NAROZNIKI - jaśniejsze akcenty (jak w cyberpunk UI) ===
        int c = 8;
        glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, 1f);
        // Lewy gora
        quad(x - 1, y - 1, c, 2); quad(x - 1, y - 1, 2, c);
        // Prawy gora
        quad(x + w - c + 1, y - 1, c, 2); quad(x + w - 1, y - 1, 2, c);
        // Lewy dol
        quad(x - 1, y + h - 1, c, 2); quad(x - 1, y + h - c + 1, 2, c);
        // Prawy dol
        quad(x + w - c + 1, y + h - 1, c, 2); quad(x + w - 1, y + h - c + 1, 2, c);

        glEnable(GL_TEXTURE_2D);
    }

    // ====== SLOT DARK NEON ======

    /** Slot z ciemnym wglebionym srodkiem + neon ramka gdy wybrany. */
    public static void drawSlotRect(int x, int y, int s, boolean selected) {
        glDisable(GL_TEXTURE_2D);
        if (selected) {
            // Fioletowa świecąca ramka wokół wybranego slotu
            for (int i = 1; i <= 3; i++) {
                float a = 0.55f - i * 0.13f;
                glColor4f(SLOT_SELECTED_R, SLOT_SELECTED_G, SLOT_SELECTED_B, a);
                lineRect(x - i, y - i, s + i * 2, s + i * 2);
            }
            // Solidna 2px ramka
            glColor4f(SLOT_SELECTED_R, SLOT_SELECTED_G, SLOT_SELECTED_B, 1f);
            quad(x - 2, y - 2, s + 4, 2);
            quad(x - 2, y + s, s + 4, 2);
            quad(x - 2, y - 2, 2, s + 4);
            quad(x + s, y - 2, 2, s + 4);
        }

        // Ciemne tlo slotu z gradientem
        glShadeModel(GL_SMOOTH);
        glBegin(GL_QUADS);
        glColor4f(0.02f, 0.03f, 0.06f, 1f);
        glVertex2i(x, y); glVertex2i(x + s, y);
        glColor4f(0.06f, 0.08f, 0.14f, 1f);
        glVertex2i(x + s, y + s); glVertex2i(x, y + s);
        glEnd();
        glShadeModel(GL_FLAT);

        // Ciemna ramka slotu
        glColor4f(SLOT_BORDER_R, SLOT_BORDER_G, SLOT_BORDER_B, 1f);
        lineRect(x, y, s, s);

        // Wewnetrzna cyan akcent (gora + lewa - dla efektu bevelu)
        glColor4f(0.15f, 0.55f, 0.75f, 0.30f);
        quad(x + 1, y + 1, s - 2, 1);
        quad(x + 1, y + 1, 1, s - 2);

        glEnable(GL_TEXTURE_2D);
    }

    /** Neonowy hover highlight. */
    public static void drawSlotHover(int x, int y, int s, int mx, int my) {
        if (!inside(mx, my, x, y, s, s)) return;
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Cyjan glow zamiast bialego
        glColor4f(SLOT_HOVER_R, SLOT_HOVER_G, SLOT_HOVER_B, 0.25f);
        quad(x + 2, y + 2, s - 4, s - 4);
        // Rama cyjan
        glColor4f(SLOT_HOVER_R, SLOT_HOVER_G, SLOT_HOVER_B, 0.70f);
        lineRect(x + 1, y + 1, s - 2, s - 2);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
    }

    // ====== STRZALKA CRAFTINGU (neon) ======

    public static void drawCraftArrow(int x, int y) {
        glDisable(GL_TEXTURE_2D);
        // Neon shaft
        glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, 0.85f);
        quad(x, y + 14, 32, 8);
        // Neon glow shaft
        glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, 0.30f);
        quad(x, y + 12, 32, 2);
        quad(x, y + 22, 32, 2);
        // Grot
        glBegin(GL_TRIANGLES);
        glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, 1f);
        glVertex2i(x + 30, y + 8); glVertex2i(x + 30, y + 28); glVertex2i(x + 44, y + 18);
        glEnd();
        // Grot glow
        glBegin(GL_TRIANGLES);
        glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, 0.30f);
        glVertex2i(x + 30, y + 6); glVertex2i(x + 30, y + 30); glVertex2i(x + 46, y + 18);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    // ====== SLIDER GLOSNOSCI (neon) ======

    public static void drawVolumeSlider(int x, int y, int w, int h, double value) {
        glDisable(GL_TEXTURE_2D);
        // Ciemne tlo
        glColor4f(0.02f, 0.03f, 0.06f, 0.98f); quad(x, y, w, h);
        glColor4f(SLOT_BORDER_R, SLOT_BORDER_G, SLOT_BORDER_B, 1f); lineRect(x, y, w, h);
        // Neon fill (cyjan)
        int fill = (int)Math.round(w * value);
        glShadeModel(GL_SMOOTH);
        glBegin(GL_QUADS);
        glColor4f(0.05f, 0.55f, 0.75f, 0.95f);
        glVertex2i(x + 2, y + 2);
        glVertex2i(x + Math.max(2, fill - 2), y + 2);
        glColor4f(PANEL_BORDER_R, PANEL_BORDER_G, PANEL_BORDER_B, 0.95f);
        glVertex2i(x + Math.max(2, fill - 2), y + h - 2);
        glVertex2i(x + 2, y + h - 2);
        glEnd();
        glShadeModel(GL_FLAT);
        // Neon knob (fiolet)
        int knobX = x + fill;
        // Glow
        glColor4f(SLOT_SELECTED_R, SLOT_SELECTED_G, SLOT_SELECTED_B, 0.35f);
        quad(knobX - 10, y - 6, 20, h + 12);
        glColor4f(SLOT_SELECTED_R, SLOT_SELECTED_G, SLOT_SELECTED_B, 1f);
        quad(knobX - 7, y - 4, 14, h + 8);
        // Wewnetrzna jasniejsza
        glColor4f(1f, 0.85f, 1f, 1f);
        quad(knobX - 5, y - 2, 10, h + 4);
    }

    // ====== TLO ======

    /** Pół-przezroczyste ciemne tło dla ekranu (zaciemnienie pod UI). */
    public static void drawDimBackground(int width, int height, float alpha) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Delikatny gradient (u góry ciemniejszy, na dole jaśniejszy)
        glShadeModel(GL_SMOOTH);
        glBegin(GL_QUADS);
        glColor4f(0f, 0f, 0.02f, alpha);
        glVertex2i(0, 0); glVertex2i(width, 0);
        glColor4f(0f, 0f, 0f, alpha * 0.85f);
        glVertex2i(width, height); glVertex2i(0, height);
        glEnd();
        glShadeModel(GL_FLAT);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
    }
}
