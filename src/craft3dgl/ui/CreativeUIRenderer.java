package craft3dgl.ui;

import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;

import static org.lwjgl.opengl.GL11.*;

/**
 * Creative inventory - layout 1:1 z CreativeModeInventoryScreen (vanilla 26.2):
 *   imageWidth=195, imageHeight=136; tlo = tab_items.png z narysowanymi komorkami.
 *   Slot listy: x=9+j*18, y=18+i*18 (5x9). Hotbar: x=9+j*18, y=112.
 *   Kosz (destroy): x=173, y=112. Zakladki kategorii na dole: y=topPos+132,
 *   x=leftPos+27*col, sprite 26x32, ikona w tabie na (5,7).
 */
public final class CreativeUIRenderer {
    public static final int SCALE = 3;
    public static final int TEX_W = 195;
    public static final int TEX_H = 136;
    public static final int PANEL_W = TEX_W * SCALE;
    public static final int PANEL_H = TEX_H * SCALE;
    public static final int SLOT_PITCH = 18 * SCALE;
    public static final int SLOT_SIZE = 16 * SCALE;
    public static final int TRASH_W = 16 * SCALE;

    private static final int TAB_W = 26 * SCALE;
    private static final int TAB_H = 32 * SCALE;
    private static final int TAB_STEP_X = 27 * SCALE;
    private static final int TAB_Y_OFF = (TEX_H - 4) * SCALE;
    private static final int TAB_ICON_X = 5 * SCALE;
    private static final int TAB_ICON_Y = 7 * SCALE;

    private CreativeUIRenderer() {}

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }

    // Komorka slotu listy zaczyna sie na (slot.x-1, slot.y-1), pitch 18 px.
    public static int gridX(int screenW) { return panelX(screenW) + 8 * SCALE; }
    public static int gridY(int screenH) { return panelY(screenH) + 17 * SCALE; }
    // Hotbar - te same kolumny, wiersz y=112.
    public static int invX(int screenW) { return gridX(screenW); }
    public static int invY(int screenH) { return panelY(screenH) + 111 * SCALE; }
    public static int hotY(int screenH) { return invY(screenH); }
    // Kosz (destroy slot): x=173, y=112, obszar 16x16.
    public static int trashX(int screenW) { return panelX(screenW) + 173 * SCALE; }
    public static int trashY(int screenH) { return panelY(screenH) + 112 * SCALE; }

    public static int tabCount() { return 5; }
    public static int tabWidthSmall() { return TAB_W; }
    public static int tabHeightSmall() { return TAB_H; }
    public static int tabX(int screenW, int col) { return panelX(screenW) + TAB_STEP_X * col; }
    public static int tabY(int screenH) { return panelY(screenH) + TAB_Y_OFF; }

    public static void draw(FontRenderer font, IconDrawer iconDrawer, Translations trans,
                            int screenW, int screenH, int mx, int my,
                            int[] items, int creativeTab, String searchText,
                            int[] invIds, int[] invCnts, int selectedSlot,
                            int cursorId, int cursorCount) {
        UIStyle.drawDimBackground(screenW, screenH, 0.40f);

        int px = panelX(screenW);
        int py = panelY(screenH);

        // ==== TLO: tab_items.png (195x136, komorki narysowane w teksturze) ====
        VanillaGuiTextures.drawRegion("creative_inventory/tab_items",
                px, py, 0, 0, 195, 136, SCALE);

        // ==== LISTA PRZEDMIOTOW 9x5 ====
        int gX = gridX(screenW);
        int gY = gridY(screenH);
        int tipId = 0;
        for (int i = 0; i < 45; i++) {
            int col = i % 9, row = i / 9;
            int sx = gX + col * SLOT_PITCH;
            int sy = gY + row * SLOT_PITCH;
            boolean over = mx >= sx && mx < sx + SLOT_PITCH
                    && my >= sy && my < sy + SLOT_PITCH;
            if (over) {
                // Vanilla renderSlotHighlight: bialy, polprzezroczysty.
                glDisable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(1f, 1f, 1f, 0.32f);
                UIStyle.quad(sx + 3, sy + 3, SLOT_SIZE, SLOT_SIZE);
                glEnable(GL_TEXTURE_2D);
                if (i < items.length && items[i] > 0) tipId = items[i];
            }
            if (i < items.length && items[i] > 0) {
                // Ikona 16px w komorce (slot.x..slot.x+15).
                iconDrawer.drawStackIcon(items[i], 1, sx + 3, sy + 3, SLOT_SIZE);
            }
        }

        // ==== HOTBAR (9 slotow, y=112) ====
        int hY = hotY(screenH);
        int hotTip = 0;
        for (int col = 0; col < 9; col++) {
            int sx = invX(screenW) + col * SLOT_PITCH;
            boolean over = mx >= sx && mx < sx + SLOT_PITCH
                    && my >= hY && my < hY + SLOT_PITCH;
            if (over) {
                glDisable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(1f, 1f, 1f, 0.32f);
                UIStyle.quad(sx + 3, hY + 3, SLOT_SIZE, SLOT_SIZE);
                glEnable(GL_TEXTURE_2D);
                if (invIds[col] > 0) hotTip = invIds[col];
            }
            iconDrawer.drawStackIcon(invIds[col], invCnts[col], sx + 3, hY + 3, SLOT_SIZE);
        }
        // Wybrany slot hotbara: biala ramka.
        if (selectedSlot >= 0 && selectedSlot < 9) {
            int sx = invX(screenW) + selectedSlot * SLOT_PITCH;
            glDisable(GL_TEXTURE_2D);
            glColor4f(1f, 1f, 1f, 1f);
            UIStyle.lineRect(sx + 1, hY + 1, SLOT_PITCH - 3, SLOT_PITCH - 3);
            glEnable(GL_TEXTURE_2D);
        }

        // ==== KOSZ (destroy slot, x=173, y=112) ====
        int tX = trashX(screenW);
        int tY2 = trashY(screenH);
        boolean trashHover = mx >= tX && mx < tX + TRASH_W
                && my >= tY2 && my < tY2 + TRASH_W;
        if (trashHover) {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1f, 0.2f, 0.2f, 0.25f);
            UIStyle.quad(tX, tY2, TRASH_W, TRASH_W);
            glEnable(GL_TEXTURE_2D);
        }
        // Czerwony X (jak vanilla destroy-item slot).
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.85f, 0.15f, 0.15f, 0.9f);
        int cx0 = tX + 4, cy0 = tY2 + 4;
        glBegin(GL_QUADS);
        glVertex2i(cx0, cy0); glVertex2i(cx0 + 3, cy0); glVertex2i(cx0 + 3, cy0 + 3); glVertex2i(cx0, cy0 + 3);
        glVertex2i(tX + TRASH_W - 7, cy0); glVertex2i(tX + TRASH_W - 4, cy0);
        glVertex2i(tX + TRASH_W - 4, cy0 + 3); glVertex2i(tX + TRASH_W - 7, cy0 + 3);
        glVertex2i(cx0, tY2 + TRASH_W - 7); glVertex2i(cx0 + 3, tY2 + TRASH_W - 7);
        glVertex2i(cx0 + 3, tY2 + TRASH_W - 4); glVertex2i(cx0, tY2 + TRASH_W - 4);
        glVertex2i(tX + TRASH_W - 7, tY2 + TRASH_W - 7); glVertex2i(tX + TRASH_W - 4, tY2 + TRASH_W - 7);
        glVertex2i(tX + TRASH_W - 4, tY2 + TRASH_W - 4); glVertex2i(tX + TRASH_W - 7, tY2 + TRASH_W - 4);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        if (trashHover) Tooltip.draw(font, trans.tr("creative.trash.short"), mx, my, screenW, screenH);

        // ==== ZAKLADKI KATEGORII: dolny rzed (y=topPos+132) ====
        int[] tabIcons = {
            craft3dgl.MinecraftGL.GRASS,           // All
            craft3dgl.MinecraftGL.DIRT,             // Bloki
            craft3dgl.MinecraftGL.ITEM_STONE_AXE,   // Narzedzia
            craft3dgl.MinecraftGL.ITEM_BREAD,        // Jedzenie
            craft3dgl.MinecraftGL.ITEM_EMERALD       // Inne
        };
        String[] tabNames = {
            trans.tr("creative.all"), trans.tr("creative.blocks"),
            trans.tr("creative.tools"), trans.tr("creative.food"),
            trans.tr("creative.misc")
        };
        int tabY2 = tabY(screenH);
        int hoverTab = -1;
        for (int i = 0; i < tabCount(); i++) {
            int tx = tabX(screenW, i);
            boolean selected = (i == creativeTab);
            String skin = selected
                    ? "tab_bottom_selected_" + (i + 1)
                    : "tab_bottom_unselected_" + (i + 1);
            VanillaGuiTextures.drawRegion(skin, tx, tabY2, 0, 0, 26, 32, SCALE);
            iconDrawer.drawStackIcon(tabIcons[i], 1, tx + TAB_ICON_X, tabY2 + TAB_ICON_Y, 16 * SCALE);
            if (mx >= tx && mx < tx + TAB_W && my >= tabY2 && my < tabY2 + TAB_H) {
                hoverTab = i;
            }
        }

        // ==== TOOLTIPY ====
        String tipText = null;
        if (hoverTab >= 0) tipText = tabNames[hoverTab];
        else if (tipId > 0) tipText = craft3dgl.items.ItemNames.itemName(tipId, trans.getLanguage());
        else if (hotTip > 0) tipText = craft3dgl.items.ItemNames.itemName(hotTip, trans.getLanguage());
        if (tipText != null) Tooltip.draw(font, tipText, mx, my, screenW, screenH);

        // ==== PRZENOSZONY PRZEDMIOT ====
        if (cursorId > 0 && cursorCount > 0) {
            iconDrawer.drawStackIcon(cursorId, cursorCount, mx - 24, my - 24, 16 * SCALE);
        }
        glColor4f(1f, 1f, 1f, 1f);
    }
}
