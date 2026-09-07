package craft3dgl.ui;

import craft3dgl.items.ItemNames;

import static org.lwjgl.opengl.GL11.*;

/** Minecraft 1.12 GuiMerchant layout, based on MCP 9.40. */
public final class TradeUIRenderer {
    private static final int SCALE = 3;
    private static final int GUI_W = 176 * SCALE;
    private static final int GUI_H = 166 * SCALE;
    private static final String TEXTURE = "assets/gui/container/villager.png";

    public static final int PREVIOUS = 100;
    public static final int NEXT = 101;
    public static final int TRADE = 102;

    // input item, input amount, output item, output amount
    private static final int[][] OFFERS = {
            {4, 8, 25, 1},       // wood -> emerald
            {25, 1, 26, 3},      // emerald -> bread
            {28, 20, 25, 1}      // wheat -> emerald
    };

    private TradeUIRenderer() {}

    public static void draw(int screenW, int screenH, FontRenderer font,
                            IconDrawer icons, int[] invIds, int[] invCounts,
                            int selectedOffer, int mouseX, int mouseY, String language) {
        selectedOffer = clampOffer(selectedOffer);
        int x = screenW / 2 - GUI_W / 2;
        int y = screenH / 2 - GUI_H / 2;

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f, 0f, 0f, 0.55f);
        glBegin(GL_QUADS);
        glVertex2i(0, 0); glVertex2i(screenW, 0);
        glVertex2i(screenW, screenH); glVertex2i(0, screenH);
        glEnd();

        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
        VanillaGuiTextures.drawRegion(TEXTURE, x, y, 0, 0, 176, 166, SCALE);

        drawRecipeButton(x, y, true, selectedOffer > 0,
                inside(mouseX, mouseY, x + 17 * SCALE, y + 23 * SCALE, 12 * SCALE, 19 * SCALE));
        drawRecipeButton(x, y, false, selectedOffer < OFFERS.length - 1,
                inside(mouseX, mouseY, x + 147 * SCALE, y + 23 * SCALE, 12 * SCALE, 19 * SCALE));

        int[] offer = OFFERS[selectedOffer];
        icons.drawStackIcon(offer[0], offer[1], x + 36 * SCALE, y + 24 * SCALE, 16 * SCALE);
        icons.drawStackIcon(offer[2], offer[3], x + 120 * SCALE, y + 24 * SCALE, 16 * SCALE);

        // ContainerMerchant: player inventory begins at GUI coordinate (8, 84), hotbar at y=142.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;
                if (slot < invIds.length && invIds[slot] != 0 && invCounts[slot] > 0) {
                    icons.drawStackIcon(invIds[slot], invCounts[slot],
                            x + (8 + col * 18) * SCALE,
                            y + (84 + row * 18) * SCALE, 16 * SCALE);
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            if (col < invIds.length && invIds[col] != 0 && invCounts[col] > 0) {
                icons.drawStackIcon(invIds[col], invCounts[col],
                        x + (8 + col * 18) * SCALE, y + 142 * SCALE, 16 * SCALE);
            }
        }

        boolean english = "en".equals(language);
        String title = english ? "Villager" : "Osadnik";
        String inventory = english ? "Inventory" : "Ekwipunek";
        font.drawVanillaTextColored(title,
                x + GUI_W / 2 - FontRenderer.mcTextWidth(title, SCALE) / 2,
                y + 6 * SCALE, SCALE, 0x404040, 1f, false);
        font.drawVanillaTextColored(inventory, x + 8 * SCALE, y + 72 * SCALE,
                SCALE, 0x404040, 1f, false);

        int hoveredSlot = hoveredInventorySlot(mouseX, mouseY, x, y);
        if (hoveredSlot >= 0 && hoveredSlot < invIds.length && invIds[hoveredSlot] != 0) {
            Tooltip.draw(font, ItemNames.itemName(invIds[hoveredSlot], language), mouseX, mouseY, screenW, screenH);
        } else if (inside(mouseX, mouseY, x + 36 * SCALE, y + 24 * SCALE, 16 * SCALE, 16 * SCALE)) {
            Tooltip.draw(font, ItemNames.itemName(offer[0], language), mouseX, mouseY, screenW, screenH);
        } else if (inside(mouseX, mouseY, x + 120 * SCALE, y + 24 * SCALE, 16 * SCALE, 16 * SCALE)) {
            Tooltip.draw(font, ItemNames.itemName(offer[2], language), mouseX, mouseY, screenW, screenH);
        }
    }

    private static void drawRecipeButton(int x, int y, boolean previous,
                                         boolean enabled, boolean hovered) {
        int drawX = x + (previous ? 17 : 147) * SCALE;
        int drawY = y + 23 * SCALE;
        int sourceX = 176;
        if (!enabled) sourceX += 24;
        else if (hovered) sourceX += 12;
        int sourceY = previous ? 19 : 0;
        glColor4f(1f, 1f, 1f, 1f);
        VanillaGuiTextures.drawRegion(TEXTURE, drawX, drawY,
                sourceX, sourceY, 12, 19, SCALE);
    }

    public static int hitTest(int mx, int my, int screenW, int screenH, int selectedOffer) {
        int x = screenW / 2 - GUI_W / 2;
        int y = screenH / 2 - GUI_H / 2;
        selectedOffer = clampOffer(selectedOffer);
        if (selectedOffer > 0 && inside(mx, my, x + 17 * SCALE, y + 23 * SCALE, 12 * SCALE, 19 * SCALE)) {
            return PREVIOUS;
        }
        if (selectedOffer < OFFERS.length - 1 && inside(mx, my, x + 147 * SCALE, y + 23 * SCALE, 12 * SCALE, 19 * SCALE)) {
            return NEXT;
        }
        if (inside(mx, my, x + 120 * SCALE, y + 24 * SCALE, 16 * SCALE, 16 * SCALE)) {
            return TRADE;
        }
        return -1;
    }

    private static int hoveredInventorySlot(int mx, int my, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                if (inside(mx, my, x + (8 + col * 18) * SCALE,
                        y + (84 + row * 18) * SCALE, 16 * SCALE, 16 * SCALE)) {
                    return 9 + row * 9 + col;
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            if (inside(mx, my, x + (8 + col * 18) * SCALE,
                    y + 142 * SCALE, 16 * SCALE, 16 * SCALE)) return col;
        }
        return -1;
    }

    private static int clampOffer(int offer) {
        return Math.max(0, Math.min(OFFERS.length - 1, offer));
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
