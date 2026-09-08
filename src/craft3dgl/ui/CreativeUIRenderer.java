package craft3dgl.ui;

import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;

import static org.lwjgl.opengl.GL11.*;

/**
 * Ekran kreatywny 1:1 z vanilla 26.2 (CreativeModeInventoryScreen / 1.21.4).
 *
 * Geometria (GUI px, mnozone przez SCALE):
 *   imageWidth=195, imageHeight=136; tlo tab_items / tab_item_search /
 *   tab_inventory wybrane wg zakladki.
 *   Sloty listy: x=9+j*18, y=18+i*18 (5x9); hotbar: x=9+j*18, y=112.
 *   Suwak: x=175, tor y=18..130, galetka 12x15.
 *   Zakladki: sprite 26x32; gorny rzad y=topPos-28, dolny y=topPos+132
 *   (topPos+imageHeight-4); x=27*columna lub dla alignedRight
 *   x=imageWidth-27*(7-col)+1; ikona w tle na (5, 8+/-1).
 *
 * Typy zakladek jak vanilla: 0..4 = kategorie (dolny rzad, kolumny 0..4),
 * 5 = SEARCH (gorny rzad, kolumna 6, kompas), 6 = INVENTORY (dolny rzad,
 * kolumna 6, skrzynia; tlo tab_inventory z koszem i slotami).
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

    public static final int TAB_SEARCH = 5;
    public static final int TAB_INVENTORY = 6;

    private static final int TAB_W = 26;
    private static final int TAB_H = 32;

    // Opis zakladek (indeks == creativeTab): rzad (true=TOP), kolumna.
    private static final boolean[] TAB_ROW_TOP = {
            false, false, false, false, false, true, false
    };
    private static final int[] TAB_COL = {0, 1, 2, 3, 4, 6, 6};
    // alignedRight (jak vanilla search/inventory) tylko kolumny 6.
    private static final boolean[] TAB_ALIGN_RIGHT = {
            false, false, false, false, false, true, true
    };

    private static final int SEARCH_BG_X = 82;   // EditBox: leftPos+82, topPos+6
    private static final int SEARCH_BG_Y = 6;
    private static final int SEARCH_BG_W = 80;

    private CreativeUIRenderer() {}

    // ======================== POZYCJE PANELU ========================

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }

    // Komorka slotu listy: hit zaczyna sie na (slot.x-1, slot.y-1), pitch 18 px.
    public static int gridX(int screenW) { return panelX(screenW) + 8 * SCALE; }
    public static int gridY(int screenH) { return panelY(screenH) + 17 * SCALE; }
    // Hotbar / main inventory - te same kolumny.
    public static int invX(int screenW) { return gridX(screenW); }
    public static int invY(int screenH) { return panelY(screenH) + 111 * SCALE; }
    public static int hotY(int screenH) { return invY(screenH); }
    // Kosz (destroy slot) - tylko zakladka INVENTORY; x=173, y=112.
    public static int trashX(int screenW) { return panelX(screenW) + 173 * SCALE; }
    public static int trashY(int screenH) { return panelY(screenH) + 112 * SCALE; }

    // ======================== ZAKLADKI ========================

    public static int tabCount() { return 7; }

    private static int tabXRel(int tab) {
        int col = TAB_COL[tab];
        if (TAB_ALIGN_RIGHT[tab]) return TEX_W - 27 * (7 - col) + 1;
        return 27 * col;
    }

    /** Hit-box zakladki (vanilla checkTabClicked: getTabX/getTabY). */
    public static int tabHitX(int screenW, int tab) {
        return panelX(screenW) + tabXRel(tab) * SCALE;
    }

    public static int tabHitY(int screenH, int tab) {
        // getTabY: TOP -> -32, BOTTOM -> +imageHeight
        int rel = TAB_ROW_TOP[tab] ? -32 : TEX_H;
        return panelY(screenH) + rel * SCALE;
    }

    /** Y sprite'a zakladki (renderTabButton: TOP -28, BOTTOM +imageHeight-4). */
    private static int tabSpriteY(int screenH, int tab) {
        int rel = TAB_ROW_TOP[tab] ? -(TAB_H - 4) : TEX_H - 4;
        return panelY(screenH) + rel * SCALE;
    }

    private static boolean isTopRow(int tab) { return TAB_ROW_TOP[tab]; }

    public static int tabWidthSmall() { return TAB_W * SCALE; }
    public static int tabHeightSmall() { return TAB_H * SCALE; }

    private static String bgName(int tab) {
        if (tab == TAB_INVENTORY) return "creative_inventory/tab_inventory";
        if (tab == TAB_SEARCH) return "creative_inventory/tab_item_search";
        return "creative_inventory/tab_items";
    }

    private static String tabSkin(int tab, boolean selected) {
        String side = isTopRow(tab) ? "top" : "bottom";
        String state = selected ? "selected" : "unselected";
        return "tab_" + side + "_" + state + "_" + (TAB_COL[tab] + 1);
    }

    // ================= SLOTY ZAKLADKI INVENTORY (vanilla SlotWrapper) =================
    // Pancerz: 5=glowa(54,6) 6=klata(54,33) 7=nogi(108,6) 8=buty(108,33).
    private static int armorCellX(int idx) { return idx < 2 ? 54 : 108; }
    private static int armorCellY(int idx) { return (idx & 1) == 0 ? 6 : 33; }

    public static int invTabArmorX(int screenW, int idx) {
        return panelX(screenW) + (armorCellX(idx) - 1) * SCALE;
    }
    public static int invTabArmorY(int screenH, int idx) {
        return panelY(screenH) + (armorCellY(idx) - 1) * SCALE;
    }
    // Offhand: k==45 -> (35,20).
    public static int invTabOffX(int screenW) { return panelX(screenW) + 34 * SCALE; }
    public static int invTabOffY(int screenH) { return panelY(screenH) + 19 * SCALE; }
    // Rzedy main inventory: y=54,72,90.
    public static int invTabRowY(int screenH, int row) {
        return panelY(screenH) + (53 + 18 * row) * SCALE;
    }
    public static int invTabColX(int screenW, int col) { return gridX(screenW) + col * SLOT_PITCH; }

    // ======================== RYSOWANIE ========================

    public static void draw(FontRenderer font, IconDrawer iconDrawer, Translations trans,
                            int screenW, int screenH, int mx, int my,
                            int creativeTab, String searchText, float scrollOffs,
                            int[] items,
                            int[] invIds, int[] invCnts, int selectedSlot,
                            int[] eqIds, int[] eqCnts,
                            int cursorId, int cursorCount) {
        UIStyle.drawDimBackground(screenW, screenH, 0.40f);

        int px = panelX(screenW);
        int py = panelY(screenH);

        // ==== Kolejnosc jak vanilla renderBg: nieselektowane zakladki, tlo, tresc,
        //     selektowana zakladka na koncu (nachodzi na panel). ====
        for (int i = 0; i < tabCount(); i++) {
            if (i != creativeTab) drawTabButton(iconDrawer, screenW, screenH, i, false);
        }

        // ==== TLO wg zakladki (komorki narysowane w teksturze) ====
        VanillaGuiTextures.drawRegion(bgName(creativeTab), px, py, 0, 0, TEX_W, TEX_H, SCALE);

        // ==== PASEK WYSZUKIWANIA (tylko zakladka SEARCH) ====
        if (creativeTab == TAB_SEARCH) {
            drawSearchBox(font, screenW, screenH, searchText);
        }

        int gX = gridX(screenW);
        int gY = gridY(screenH);
        int tipId = 0;

        if (creativeTab == TAB_INVENTORY) {
            drawInventoryTab(font, iconDrawer, trans, screenW, screenH, mx, my,
                    px, py, gX, invIds, invCnts, selectedSlot, eqIds, eqCnts);
        } else {
            // ==== LISTA PRZEDMIOTOW 9x5 (scroll) ====
            int n = items == null ? 0 : items.length;
            int scrollRow = 0;
            int rowCount = (n + 8) / 9 - 5;
            if (rowCount > 0) {
                scrollRow = Math.max(0, Math.min(rowCount, (int) (scrollOffs * rowCount + 0.5f)));
            }
            for (int i = 0; i < 45; i++) {
                int col = i % 9, row = i / 9;
                int sx = gX + col * SLOT_PITCH;
                int sy = gY + row * SLOT_PITCH;
                boolean over = mx >= sx && mx < sx + SLOT_PITCH
                        && my >= sy && my < sy + SLOT_PITCH;
                int idx = (row + scrollRow) * 9 + col;
                if (over) {
                    // Vanilla renderSlotHighlight: bialy, polprzezroczysty.
                    glDisable(GL_TEXTURE_2D);
                    glEnable(GL_BLEND);
                    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                    glColor4f(1f, 1f, 1f, 0.32f);
                    UIStyle.quad(sx + 3, sy + 3, SLOT_SIZE, SLOT_SIZE);
                    glEnable(GL_TEXTURE_2D);
                    if (idx < n && items[idx] > 0) tipId = items[idx];
                }
                if (idx < n && items[idx] > 0) {
                    iconDrawer.drawStackIcon(items[idx], 1, sx + 3, sy + 3, SLOT_SIZE);
                }
            }
        }

        // ==== HOTBAR (9 slotow, y=112) - wszystkie typy zakladek ====
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

        // ==== SUWAK (tylko kategorie i SEARCH; vanilla: tylko gdy canScroll()) ====
        if (creativeTab != TAB_INVENTORY) {
            int n = items == null ? 0 : items.length;
            boolean canScrollList = n > 45;
            String scroller = canScrollList ? "scroller" : "scroller_disabled";
            int knobY = py + (18 + (int) ((112 - 15) * scrollOffs)) * SCALE;
            VanillaGuiTextures.draw(scroller, px + 175 * SCALE, knobY, 12, 15, SCALE);
        }

        // ==== NAPIS ZAKLADKI (renderLabels; INVENTORY ma hideTitle) ====
        if (creativeTab != TAB_INVENTORY) {
            String title = tabTitle(trans, creativeTab);
            if (title != null) {
                font.drawVanillaContainerText(title, px + 8 * SCALE, py + 6 * SCALE, SCALE);
            }
        }

        // ==== SELEKTOWANA ZAKLADKA (na koniec - nachodzi na tlo) ====
        drawTabButton(iconDrawer, screenW, screenH, creativeTab, true);

        // ==== TOOLTIPY ====
        int hoverTab = -1;
        for (int i = 0; i < tabCount(); i++) {
            if (tabHovered(i, screenW, screenH, mx, my)) { hoverTab = i; break; }
        }
        String tipText = null;
        if (hoverTab >= 0) tipText = tabTitle(trans, hoverTab);
        else if (tipId > 0) tipText = ItemNames.itemName(tipId, trans.getLanguage());
        else if (hotTip > 0) tipText = ItemNames.itemName(hotTip, trans.getLanguage());
        if (tipText != null) Tooltip.draw(font, tipText, mx, my, screenW, screenH);

        // ==== PRZENOSZONY PRZEDMIOT ====
        if (cursorId > 0 && cursorCount > 0) {
            iconDrawer.drawStackIcon(cursorId, cursorCount, mx - 24, my - 24, 16 * SCALE);
        }
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void drawTabButton(IconDrawer icons,
                                      int screenW, int screenH, int tab, boolean selected) {
        int tx = tabHitX(screenW, tab);
        int ty = tabSpriteY(screenH, tab);
        VanillaGuiTextures.drawRegion(tabSkin(tab, selected), tx, ty, 0, 0, TAB_W, TAB_H, SCALE);
        // Ikona: x+5, y sprite + 8 +/- 1 (gorny rzad +9, dolny +7).
        int ix = tx + 5 * SCALE;
        int iy = ty + (isTopRow(tab) ? 9 : 7) * SCALE;
        if (tab == TAB_SEARCH) {
            VanillaGuiTextures.draw("creative_inventory/tab_icon_search", ix, iy, 16, 16, SCALE);
        } else {
            icons.drawStackIcon(tabIconId(tab), 1, ix, iy, 16 * SCALE);
        }
    }

    private static int tabIconId(int tab) {
        // vanilla: search=kompas, inventory=skrzynia.
        if (tab == TAB_INVENTORY) return craft3dgl.MinecraftGL.CHEST;
        int[] icons = {
            craft3dgl.MinecraftGL.GRASS,
            craft3dgl.MinecraftGL.DIRT,
            craft3dgl.MinecraftGL.ITEM_STONE_AXE,
            craft3dgl.MinecraftGL.ITEM_BREAD,
            craft3dgl.MinecraftGL.ITEM_EMERALD,
            -1 // SEARCH - rysowany z tekstury kompasu
        };
        return icons[tab];
    }

    private static String tabTitle(Translations trans, int tab) {
        String[] keys = {"creative.all", "creative.blocks", "creative.tools",
                "creative.food", "creative.misc", "creative.searchitems", "creative.inventory"};
        return trans.tr(keys[tab]);
    }

    /** Tooltip hovers only nad ikona zakladki (vanilla checkTabHovering: +3,+3 21x27). */
    private static boolean tabHovered(int tab, int screenW, int screenH, int mx, int my) {
        int hx = tabHitX(screenW, tab) + 3 * SCALE;
        int hy = tabHitY(screenH, tab) + 3 * SCALE;
        return mx >= hx && mx < hx + 21 * SCALE && my >= hy && my < hy + 27 * SCALE;
    }

    // ======================== ZAKLADKA INVENTORY ========================

    private static void drawInventoryTab(FontRenderer font, IconDrawer icons, Translations trans,
                                         int screenW, int screenH, int mx, int my,
                                         int px, int py, int gX,
                                         int[] invIds, int[] invCnts, int selectedSlot,
                                         int[] eqIds, int[] eqCnts) {
        // Postac gracza w boxie x=73..105, y=6..49 (renderEntityInInventoryFollowsMouse).
        craft3dgl.entities.SteveRenderer.drawInventoryPlayer(
                screenW, screenH,
                px + 73 * SCALE, py + 6 * SCALE, 32 * SCALE, 43 * SCALE, mx, my);

        int tipId = 0;
        // Pancerz 2x2 (glowa, klata | nogi, buty) + offhand.
        for (int k = 0; k < 4; k++) {
            int ax = invTabArmorX(screenW, k);
            int ay = invTabArmorY(screenH, k);
            if (eqIds[k] > 0 && eqCnts[k] > 0) {
                icons.drawStackIcon(eqIds[k], eqCnts[k], ax + SCALE, ay + SCALE, 16 * SCALE);
            } else {
                ToolTextures.drawEmptyArmorSlot(k, ax + SCALE, ay + SCALE, 16 * SCALE);
            }
            if (slotOver(ax, ay, mx, my)) {
                slotHoverVisual(ax, ay, mx, my);
                if (eqIds[k] > 0) tipId = eqIds[k];
            }
        }
        int ox = invTabOffX(screenW);
        int oy = invTabOffY(screenH);
        if (eqIds[4] > 0 && eqCnts[4] > 0) {
            icons.drawStackIcon(eqIds[4], eqCnts[4], ox + SCALE, oy + SCALE, 16 * SCALE);
        } else {
            ToolTextures.drawEmptyOffhandSlot(ox + SCALE, oy + SCALE, 16 * SCALE);
        }
        if (slotOver(ox, oy, mx, my)) {
            slotHoverVisual(ox, oy, mx, my);
            if (eqIds[4] > 0) tipId = eqIds[4];
        }

        // Main inventory 3x9 (y=54,72,90) - te same kolumny co hotbar.
        for (int row = 0; row < 3; row++) {
            int ry = invTabRowY(screenH, row);
            for (int col = 0; col < 9; col++) {
                int idx = 9 + row * 9 + col;
                int sx = gX + col * SLOT_PITCH;
                icons.drawStackIcon(invIds[idx], invCnts[idx], sx + SCALE, ry + SCALE, 16 * SCALE);
                if (slotOver(sx, ry, mx, my)) {
                    slotHoverVisual(sx, ry, mx, my);
                    if (invIds[idx] > 0) tipId = invIds[idx];
                }
            }
        }

        // Tooltip kosza (inventory.binSlot) nad regionem destroy (173,112).
        int tx = trashX(screenW);
        int ty = trashY(screenH);
        if (mx >= tx && mx < tx + TRASH_W && my >= ty && my < ty + TRASH_W) {
            Tooltip.draw(font, trans.tr("inventory.binSlot"), mx, my, screenW, screenH);
        } else if (tipId > 0) {
            Tooltip.draw(font, ItemNames.itemName(tipId, trans.getLanguage()), mx, my, screenW, screenH);
        }
    }

    private static boolean slotOver(int slotX, int slotY, int mouseX, int mouseY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_PITCH
                && mouseY >= slotY && mouseY < slotY + SLOT_PITCH;
    }

    /** Vanilla renderSlotHighlight: bialy, polprzezroczysty (0.32). */
    private static void slotHoverVisual(int slotX, int slotY, int mouseX, int mouseY) {
        if (!slotOver(slotX, slotY, mouseX, mouseY)) return;
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 0.32f);
        UIStyle.quad(slotX + 3, slotY + 3, SLOT_SIZE, SLOT_SIZE);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
    }

    // ======================== PASEK WYSZUKIWANIA ========================

    private static void drawSearchBox(FontRenderer font, int screenW, int screenH, String text) {
        if (text == null) text = "";
        int px = panelX(screenW);
        int py = panelY(screenH);
        int x = px + SEARCH_BG_X * SCALE;
        int y = py + SEARCH_BG_Y * SCALE;
        int maxW = (SEARCH_BG_W - 2) * SCALE; // margines wewnatrz ramki pola

        // Vanilla przewija tekst tak, by widoczny byl koniec (kursor na koncu).
        String visible = text;
        while (font.mcTextWidth(visible, SCALE) > maxW && visible.length() > 0) {
            visible = visible.substring(1);
        }
        if (!visible.isEmpty()) {
            font.drawVanillaText(visible, x, y, SCALE, 1f);
        }
        int cx = x + font.mcTextWidth(visible, SCALE);
        // Migajacy kursor: vanilla EditBox rysuje '_' co 300 ms (focus od otwarcia).
        boolean blinkOn = (System.currentTimeMillis() / 300L) % 2L == 0L;
        if (blinkOn) {
            font.drawVanillaText("_", cx, y, SCALE, 1f);
        }
    }
}
