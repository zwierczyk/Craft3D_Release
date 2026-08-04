package craft3dgl.ui;

import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer GUI skrzyni: 27 slotow skrzyni + 27 slotow plecaka + 9 hotbar.
 */
public final class ChestUIRenderer {

    public static final int PANEL_W = 490;
    public static final int PANEL_H = 510;
    public static final int SLOT_PITCH = 46;
    public static final int SLOT_SIZE = 40;

    private ChestUIRenderer() {}

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }

    /** Pozycje slotow obliczane przez te metody (zsynchronizowane z draw). */
    public static int startX(int screenW) { return panelX(screenW) + 32; }
    public static int chestY(int screenH) { return panelY(screenH) + 70; }
    public static int invY(int screenH) { return panelY(screenH) + PANEL_H - 200; }
    public static int hotY(int screenH) { return panelY(screenH) + PANEL_H - 56; }

    public static void draw(FontRenderer font, IconDrawer iconDrawer, Translations trans,
                            int screenW, int screenH, int mx, int my,
                            int[] chestIds, int[] chestCnts,
                            int[] invIds, int[] invCnts, int selectedSlot,
                            int cursorId, int cursorCount) {
        UIStyle.drawDimBackground(screenW, screenH, 0.45f);
        int px = panelX(screenW);
        int py = panelY(screenH);
        UIStyle.drawPanel(px, py, PANEL_W, PANEL_H);
        font.drawCenteredTextDark(trans.tr("chest.title"), px + PANEL_W / 2, py + 12, 0.85f);

        int sx0 = startX(screenW);
        int cY = chestY(screenH);
        int iY = invY(screenH);
        int hY = hotY(screenH);

        // Skrzynia 3x9
        font.drawTextDark(trans.tr("section.chest"), sx0, cY - 20, 0.60f);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = row * 9 + col;
            int sx = sx0 + col * SLOT_PITCH, sy = cY + row * SLOT_PITCH;
            UIStyle.drawSlotRect(sx, sy, SLOT_SIZE, false);
            iconDrawer.drawStackIcon(chestIds[idx], chestCnts[idx], sx + 4, sy + 4, 32);
            UIStyle.drawSlotHover(sx, sy, SLOT_SIZE, mx, my);
        }
        // Separator
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.40f, 0.38f, 0.36f, 1f);
        UIStyle.quad(sx0, cY + 3 * SLOT_PITCH + 18, 9 * SLOT_PITCH - 4, 2);
        glColor4f(0.95f, 0.93f, 0.90f, 1f);
        UIStyle.quad(sx0, cY + 3 * SLOT_PITCH + 20, 9 * SLOT_PITCH - 4, 1);
        glEnable(GL_TEXTURE_2D);

        // Backpack 3x9
        font.drawTextDark(trans.tr("section.inventory"), sx0, iY - 20, 0.60f);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = 9 + row * 9 + col;
            int sx = sx0 + col * SLOT_PITCH, sy = iY + row * SLOT_PITCH;
            UIStyle.drawSlotRect(sx, sy, SLOT_SIZE, false);
            iconDrawer.drawStackIcon(invIds[idx], invCnts[idx], sx + 4, sy + 4, 32);
            UIStyle.drawSlotHover(sx, sy, SLOT_SIZE, mx, my);
        }

        // Hotbar
        font.drawTextDark(trans.tr("section.hotbar"), sx0, hY - 20, 0.60f);
        for (int col = 0; col < 9; col++) {
            int sx = sx0 + col * SLOT_PITCH;
            UIStyle.drawSlotRect(sx, hY, SLOT_SIZE, col == selectedSlot);
            iconDrawer.drawStackIcon(invIds[col], invCnts[col], sx + 4, hY + 4, 32);
            UIStyle.drawSlotHover(sx, hY, SLOT_SIZE, mx, my);
        }

        // Hover tooltip
        int hoverItem = findHoverItem(mx, my, screenW, screenH, chestIds, invIds);
        if (hoverItem > 0 && (cursorId == 0 || cursorCount == 0)) {
            Tooltip.draw(font, ItemNames.itemName(hoverItem, trans.getLanguage()), mx, my, screenW, screenH);
        }
        if (cursorId > 0 && cursorCount > 0) {
            iconDrawer.drawStackIcon(cursorId, cursorCount, mx - 16, my - 16, 32);
        }
    }

    private static int findHoverItem(int mx, int my, int screenW, int screenH,
                                     int[] chestIds, int[] invIds) {
        int sx0 = startX(screenW);
        int cY = chestY(screenH);
        int iY = invY(screenH);
        int hY = hotY(screenH);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = row * 9 + col;
            int sx = sx0 + col * SLOT_PITCH, sy = cY + row * SLOT_PITCH;
            if (UIStyle.inside(mx, my, sx, sy, SLOT_SIZE, SLOT_SIZE) && chestIds[idx] > 0) return chestIds[idx];
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = 9 + row * 9 + col;
            int sx = sx0 + col * SLOT_PITCH, sy = iY + row * SLOT_PITCH;
            if (UIStyle.inside(mx, my, sx, sy, SLOT_SIZE, SLOT_SIZE) && invIds[idx] > 0) return invIds[idx];
        }
        for (int col = 0; col < 9; col++) {
            int sx = sx0 + col * SLOT_PITCH;
            if (UIStyle.inside(mx, my, sx, hY, SLOT_SIZE, SLOT_SIZE) && invIds[col] > 0) return invIds[col];
        }
        return 0;
    }
}
