package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/** Minecraft 1.12 GuiUtils-style hovering text box. */
public final class Tooltip {
    private Tooltip() {}

    public static void draw(FontRenderer font, String text, int mx, int my,
                            int screenW, int screenH) {
        final int scale = 2;
        int textW = FontRenderer.mcTextWidth(text, scale);
        int textH = 8 * scale;
        int x = mx + 24;
        int y = my - 24;
        int boxW = textW + 12;
        int boxH = textH + 12;
        if (x + boxW > screenW) x = mx - boxW - 16;
        if (y + boxH > screenH) y = screenH - boxH - 4;
        if (y < 4) y = 4;

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // GuiUtils.drawGradientRect colours: background 0xF0100010,
        // top border 0x505000FF and bottom border 0x5028007F.
        glColor4f(0.063f, 0f, 0.063f, 0.94f);
        UIStyle.quad(x, y, boxW, boxH);
        glColor4f(0.314f, 0f, 1f, 0.314f);
        UIStyle.quad(x, y, boxW, 2);
        UIStyle.quad(x, y, 2, boxH);
        glColor4f(0.157f, 0f, 0.498f, 0.314f);
        UIStyle.quad(x, y + boxH - 2, boxW, 2);
        UIStyle.quad(x + boxW - 2, y, 2, boxH);

        font.drawVanillaText(text, x + 6, y + 6, scale, 1f);
    }
}
