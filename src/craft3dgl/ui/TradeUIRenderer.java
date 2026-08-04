package craft3dgl.ui;

import craft3dgl.items.ItemNames;

import static craft3dgl.world.WorldConstants.*;

/**
 * Renderer GUI handlu z villagerem - panel z 3 trade'ami + przycisk "Gotowe".
 */
public final class TradeUIRenderer {

    public static final int PANEL_W = 430;
    public static final int PANEL_H = 310;

    public static final int BTN1_OFFSET_Y = 76;
    public static final int BTN2_OFFSET_Y = 128;
    public static final int BTN3_OFFSET_Y = 180;
    public static final int DONE_OFFSET_Y = 250;
    public static final int TRADE_BTN_X = 60;
    public static final int TRADE_BTN_W = 310;
    public static final int TRADE_BTN_H = 42;
    public static final int DONE_BTN_X = 120;
    public static final int DONE_BTN_W = 190;
    public static final int DONE_BTN_H = 36;

    private TradeUIRenderer() {}

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }

    public static void draw(FontRenderer font, int screenW, int screenH, String language) {
        UIStyle.drawDimBackground(screenW, screenH, 0.45f);
        int px = panelX(screenW);
        int py = panelY(screenH);
        UIStyle.drawPanel(px, py, PANEL_W, PANEL_H);
        String title = "en".equals(language) ? "Villager" : "Wiesniak";
        font.drawCenteredTextDark(title, px + PANEL_W / 2, py + 12, 0.85f);

        MenuButton.drawButton(font, px + TRADE_BTN_X, py + BTN1_OFFSET_Y, TRADE_BTN_W, TRADE_BTN_H,
                "8 " + ItemNames.itemName(WOOD, language) + " -> 1 " + ItemNames.itemName(ITEM_EMERALD, language));
        MenuButton.drawButton(font, px + TRADE_BTN_X, py + BTN2_OFFSET_Y, TRADE_BTN_W, TRADE_BTN_H,
                "1 " + ItemNames.itemName(ITEM_EMERALD, language) + " -> 3 " + ItemNames.itemName(ITEM_BREAD, language));
        MenuButton.drawButton(font, px + TRADE_BTN_X, py + BTN3_OFFSET_Y, TRADE_BTN_W, TRADE_BTN_H,
                "20 " + ItemNames.itemName(ITEM_WHEAT, language) + " -> 1 " + ItemNames.itemName(ITEM_EMERALD, language));
        MenuButton.drawButton(font, px + DONE_BTN_X, py + DONE_OFFSET_Y, DONE_BTN_W, DONE_BTN_H,
                "en".equals(language) ? "Done" : "Gotowe");
    }

    /** Wykrywa ktore trade kliknieto. Zwraca: -1 = nic, 0/1/2 = trade index, 3 = Done. */
    public static int hitTest(int mx, int my, int screenW, int screenH) {
        int px = panelX(screenW);
        int py = panelY(screenH);
        if (UIStyle.inside(mx, my, px + TRADE_BTN_X, py + BTN1_OFFSET_Y, TRADE_BTN_W, TRADE_BTN_H)) return 0;
        if (UIStyle.inside(mx, my, px + TRADE_BTN_X, py + BTN2_OFFSET_Y, TRADE_BTN_W, TRADE_BTN_H)) return 1;
        if (UIStyle.inside(mx, my, px + TRADE_BTN_X, py + BTN3_OFFSET_Y, TRADE_BTN_W, TRADE_BTN_H)) return 2;
        if (UIStyle.inside(mx, my, px + DONE_BTN_X, py + DONE_OFFSET_Y, DONE_BTN_W, DONE_BTN_H)) return 3;
        return -1;
    }
}
