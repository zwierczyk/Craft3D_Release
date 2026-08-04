package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/**
 * Tooltip dark neon - czarne tło + cyjan neon ramka.
 */
public final class Tooltip {

    private Tooltip() {}

    public static void draw(FontRenderer font, String text, int mx, int my, int screenW, int screenH) {
        int w = FontRenderer.textWidth(text, 0.55f) + 20;
        int h = 26;
        int x = mx + 14;
        int y = my + 10;
        if (x + w > screenW) x = screenW - w - 4;
        if (y + h > screenH) y = screenH - h - 4;

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Neon glow (blur)
        for (int i = 1; i <= 3; i++) {
            float a = 0.35f - i * 0.10f;
            glColor4f(UIStyle.PANEL_BORDER_R, UIStyle.PANEL_BORDER_G, UIStyle.PANEL_BORDER_B, a);
            UIStyle.lineRect(x - i, y - i, w + i * 2, h + i * 2);
        }

        // Czarne tlo
        glColor4f(0.02f, 0.03f, 0.06f, 0.97f);
        UIStyle.quad(x, y, w, h);

        // Cyjan ramka
        glColor4f(UIStyle.PANEL_BORDER_R, UIStyle.PANEL_BORDER_G, UIStyle.PANEL_BORDER_B, 1f);
        UIStyle.lineRect(x, y, w, h);

        // Corner akcenty
        int c = 5;
        UIStyle.quad(x, y, c, 1); UIStyle.quad(x, y, 1, c);
        UIStyle.quad(x + w - c, y, c, 1); UIStyle.quad(x + w - 1, y, 1, c);
        UIStyle.quad(x, y + h - 1, c, 1); UIStyle.quad(x, y + h - c, 1, c);
        UIStyle.quad(x + w - c, y + h - 1, c, 1); UIStyle.quad(x + w - 1, y + h - c, 1, c);

        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        font.drawText(text, x + 8, y + 5, 0.55f);
    }
}
