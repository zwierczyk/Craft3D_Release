package craft3dgl.ui;

import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;

import static org.lwjgl.opengl.GL11.*;

/** Minecraft 1.12 GuiChest port for a 27-slot (three-row) chest. */
public final class ChestUIRenderer {
    public static final int SCALE = 3;
    public static final int ROWS = 3;
    public static final int PANEL_W = 176 * SCALE;
    public static final int PANEL_H = (114 + ROWS * 18) * SCALE;
    public static final int SLOT_PITCH = 18 * SCALE;
    public static final int SLOT_SIZE = 18 * SCALE;

    private ChestUIRenderer() {}

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }
    public static int startX(int screenW) { return panelX(screenW) + 7 * SCALE; }
    public static int chestY(int screenH) { return panelY(screenH) + 17 * SCALE; }
    public static int invY(int screenH) { return panelY(screenH) + 84 * SCALE; }
    public static int hotY(int screenH) { return panelY(screenH) + 142 * SCALE; }

    public static void draw(FontRenderer font, IconDrawer icons, Translations trans,
                            int screenW, int screenH, int mouseX, int mouseY,
                            int[] chestIds, int[] chestCounts,
                            int[] inventoryIds, int[] inventoryCounts, int selectedSlot,
                            int cursorId, int cursorCount) {
        UIStyle.drawDimBackground(screenW, screenH, 0.55f);
        int panelX = panelX(screenW);
        int panelY = panelY(screenH);

        // GuiChest uses two regions of generic_54.png. For three rows the upper
        // region is 71 px tall; the fixed 96 px player inventory starts at v=126.
        VanillaGuiTextures.drawRegion("generic_54", panelX, panelY,
                0, 0, 176, ROWS * 18 + 17, SCALE);
        VanillaGuiTextures.drawRegion("generic_54", panelX,
                panelY + (ROWS * 18 + 17) * SCALE,
                0, 126, 176, 96, SCALE);

        int startX = startX(screenW);
        int chestY = chestY(screenH);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                int slotX = startX + col * SLOT_PITCH;
                int slotY = chestY + row * SLOT_PITCH;
                drawItem(icons, chestIds[index], chestCounts[index], slotX, slotY);
                drawSlotHover(slotX, slotY, mouseX, mouseY);
            }
        }

        int inventoryY = invY(screenH);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                int slotX = startX + col * SLOT_PITCH;
                int slotY = inventoryY + row * SLOT_PITCH;
                drawItem(icons, inventoryIds[index], inventoryCounts[index], slotX, slotY);
                drawSlotHover(slotX, slotY, mouseX, mouseY);
            }
        }

        int hotbarY = hotY(screenH);
        for (int col = 0; col < 9; col++) {
            int slotX = startX + col * SLOT_PITCH;
            drawItem(icons, inventoryIds[col], inventoryCounts[col], slotX, hotbarY);
            drawSlotHover(slotX, hotbarY, mouseX, mouseY);
        }

        font.drawVanillaContainerText(trans.tr("chest.title"),
                panelX + 8 * SCALE, panelY + 6 * SCALE, SCALE);
        font.drawVanillaContainerText(trans.tr("section.inventory"),
                panelX + 8 * SCALE, panelY + (PANEL_H / SCALE - 94) * SCALE, SCALE);

        int hoverItem = findHoverItem(mouseX, mouseY, screenW, screenH, chestIds, inventoryIds);
        if (hoverItem > 0 && (cursorId <= 0 || cursorCount <= 0)) {
            Tooltip.draw(font, ItemNames.itemName(hoverItem, trans.getLanguage()),
                    mouseX, mouseY, screenW, screenH);
        }
        if (cursorId > 0 && cursorCount > 0) {
            icons.drawStackIcon(cursorId, cursorCount,
                    mouseX - 8 * SCALE, mouseY - 8 * SCALE, 16 * SCALE);
        }
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void drawItem(IconDrawer icons, int id, int count, int slotX, int slotY) {
        icons.drawStackIcon(id, count, slotX + SCALE, slotY + SCALE, 16 * SCALE);
    }

    private static void drawSlotHover(int slotX, int slotY, int mouseX, int mouseY) {
        if (!UIStyle.inside(mouseX, mouseY, slotX, slotY, SLOT_SIZE, SLOT_SIZE)) return;
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 0.50f);
        UIStyle.quad(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static int findHoverItem(int mouseX, int mouseY, int screenW, int screenH,
                                     int[] chestIds, int[] inventoryIds) {
        int startX = startX(screenW);
        int chestY = chestY(screenH);
        int inventoryY = invY(screenH);
        int hotbarY = hotY(screenH);
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row * 9 + col;
                if (UIStyle.inside(mouseX, mouseY, startX + col * SLOT_PITCH,
                        chestY + row * SLOT_PITCH, SLOT_SIZE, SLOT_SIZE) && chestIds[index] > 0) {
                    return chestIds[index];
                }
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                if (UIStyle.inside(mouseX, mouseY, startX + col * SLOT_PITCH,
                        inventoryY + row * SLOT_PITCH, SLOT_SIZE, SLOT_SIZE) && inventoryIds[index] > 0) {
                    return inventoryIds[index];
                }
            }
        }
        for (int col = 0; col < 9; col++) {
            if (UIStyle.inside(mouseX, mouseY, startX + col * SLOT_PITCH,
                    hotbarY, SLOT_SIZE, SLOT_SIZE) && inventoryIds[col] > 0) return inventoryIds[col];
        }
        return 0;
    }
}
