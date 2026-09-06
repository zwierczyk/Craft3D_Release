package craft3dgl.ui;

import craft3dgl.CraftingSystemGL;
import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;

import static org.lwjgl.opengl.GL11.*;

/**
 * Minecraft 1.12 GuiInventory and GuiCrafting port.
 * Slot positions come directly from ContainerPlayer and ContainerWorkbench in
 * MCP 9.40; the source GUI is 176x166 and is rendered at GUI scale 3.
 */
public final class InventoryUIRenderer {
    public static final int SCALE = 3;
    public static final int PANEL_W = 176 * SCALE;
    public static final int PANEL_H = 166 * SCALE;
    public static final int SLOT_SIZE = 18 * SCALE;

    private InventoryUIRenderer() {}

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }

    // Slot helpers return the 18x18 hit rectangle (one source pixel around the
    // 16x16 item), exactly like GuiContainer#isMouseOverSlot.
    private static int hitX(int screenW, int slotX) { return panelX(screenW) + (slotX - 1) * SCALE; }
    private static int hitY(int screenH, int slotY) { return panelY(screenH) + (slotY - 1) * SCALE; }

    public static int armorX(int screenW) { return hitX(screenW, 8); }
    public static int armorY(int screenH) { return hitY(screenH, 8); }
    public static int armorPitch() { return 18 * SCALE; }

    public static int portraitX(int screenW) { return panelX(screenW) + 25 * SCALE; }
    public static int portraitY(int screenH) { return panelY(screenH) + 7 * SCALE; }
    public static int portraitW() { return 50 * SCALE; }
    public static int portraitH() { return 70 * SCALE; }

    public static int offhandX(int screenW) { return hitX(screenW, 77); }
    public static int offhandY(int screenH) { return hitY(screenH, 62); }

    public static int craftAreaX(int screenW, int craftSize) {
        return hitX(screenW, craftSize == 3 ? 30 : 98);
    }

    public static int craftY(int screenH, int craftSize) {
        return hitY(screenH, craftSize == 3 ? 17 : 18);
    }

    public static int craftPitch() { return 18 * SCALE; }

    public static int outputX(int screenW, int craftSize) {
        return hitX(screenW, craftSize == 3 ? 124 : 154);
    }

    public static int outputY(int screenH, int craftSize) {
        return hitY(screenH, craftSize == 3 ? 35 : 28);
    }

    public static int invX(int screenW) { return hitX(screenW, 8); }
    public static int invY(int screenH) { return hitY(screenH, 84); }
    public static int invPitch() { return 18 * SCALE; }
    public static int hotY(int screenH) { return hitY(screenH, 142); }

    public static void draw(FontRenderer font, IconDrawer icons, Translations trans,
                            int screenW, int screenH, int mouseX, int mouseY,
                            boolean usingCraftingTable,
                            int[] craftIds, int[] craftCounts, CraftingSystemGL.Recipe result,
                            int[] equipmentIds, int[] equipmentCounts,
                            int[] inventoryIds, int[] inventoryCounts, int selectedSlot,
                            int cursorId, int cursorCount) {
        UIStyle.drawDimBackground(screenW, screenH, 0.55f);
        int panelX = panelX(screenW);
        int panelY = panelY(screenH);
        VanillaGuiTextures.draw(usingCraftingTable ? "crafting_table" : "inventory",
                panelX, panelY, 176, 166, SCALE);

        int craftSize = usingCraftingTable ? 3 : 2;

        if (!usingCraftingTable) {
            // GuiInventory.drawEntityOnScreen(i + 51, j + 75, 30, ...).
            drawPortraitCharacter(portraitX(screenW), portraitY(screenH), portraitW(), portraitH(),
                    screenW, screenH, mouseX, mouseY);

            int armorX = armorX(screenW);
            int armorY = armorY(screenH);
            for (int i = 0; i < 4; i++) {
                int slotY = armorY + i * armorPitch();
                if (equipmentIds[i] <= 0 || equipmentCounts[i] <= 0) {
                    ToolTextures.drawEmptyArmorSlot(i, armorX + SCALE, slotY + SCALE, 16 * SCALE);
                }
                drawItem(icons, equipmentIds[i], equipmentCounts[i], armorX, slotY);
                drawSlotHover(armorX, slotY, mouseX, mouseY);
            }

            int offhandX = offhandX(screenW);
            int offhandY = offhandY(screenH);
            if (equipmentIds[4] <= 0 || equipmentCounts[4] <= 0) {
                ToolTextures.drawEmptyOffhandSlot(offhandX + SCALE, offhandY + SCALE, 16 * SCALE);
            }
            drawItem(icons, equipmentIds[4], equipmentCounts[4], offhandX, offhandY);
            drawSlotHover(offhandX, offhandY, mouseX, mouseY);
        }

        int craftX = craftAreaX(screenW, craftSize);
        int craftY = craftY(screenH, craftSize);
        for (int row = 0; row < craftSize; row++) {
            for (int col = 0; col < craftSize; col++) {
                int index = row * craftSize + col;
                int slotX = craftX + col * craftPitch();
                int slotY = craftY + row * craftPitch();
                drawItem(icons, craftIds[index], craftCounts[index], slotX, slotY);
                drawSlotHover(slotX, slotY, mouseX, mouseY);
            }
        }

        int outputX = outputX(screenW, craftSize);
        int outputY = outputY(screenH, craftSize);
        if (!result.empty()) drawItem(icons, result.resultId, result.resultCount, outputX, outputY);
        drawSlotHover(outputX, outputY, mouseX, mouseY);

        int inventoryX = invX(screenW);
        int inventoryY = invY(screenH);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                int slotX = inventoryX + col * invPitch();
                int slotY = inventoryY + row * invPitch();
                drawItem(icons, inventoryIds[index], inventoryCounts[index], slotX, slotY);
                drawSlotHover(slotX, slotY, mouseX, mouseY);
            }
        }

        int hotbarY = hotY(screenH);
        for (int col = 0; col < 9; col++) {
            int slotX = inventoryX + col * invPitch();
            drawItem(icons, inventoryIds[col], inventoryCounts[col], slotX, hotbarY);
            drawSlotHover(slotX, hotbarY, mouseX, mouseY);
        }

        // GuiInventory only labels Crafting. GuiCrafting labels Crafting and Inventory.
        if (usingCraftingTable) {
            font.drawVanillaContainerText(trans.tr("section.craft"),
                    panelX + 28 * SCALE, panelY + 6 * SCALE, SCALE);
            font.drawVanillaContainerText(trans.tr("section.inventory"),
                    panelX + 8 * SCALE, panelY + 72 * SCALE, SCALE);
        } else {
            font.drawVanillaContainerText(trans.tr("section.craft"),
                    panelX + 97 * SCALE, panelY + 8 * SCALE, SCALE);
        }

        int hoverItem = findHoverItem(mouseX, mouseY, screenW, screenH, craftSize,
                craftIds, result, equipmentIds, inventoryIds, usingCraftingTable);
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
        if (!inSlot(mouseX, mouseY, slotX, slotY)) return;
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 0.50f);
        UIStyle.quad(slotX, slotY, SLOT_SIZE, SLOT_SIZE);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void drawPortraitCharacter(int x, int y, int w, int h,
                                               int screenW, int screenH,
                                               int mouseX, int mouseY) {
        craft3dgl.entities.SteveRenderer.drawInventoryPlayer(
                screenW, screenH, x, y, w, h, mouseX, mouseY);
    }

    private static int findHoverItem(int mx, int my, int screenW, int screenH, int craftSize,
                                     int[] craftIds, CraftingSystemGL.Recipe result,
                                     int[] equipmentIds, int[] inventoryIds,
                                     boolean usingCraftingTable) {
        if (!usingCraftingTable) {
            int armorX = armorX(screenW);
            int armorY = armorY(screenH);
            for (int i = 0; i < 4; i++) {
                if (inSlot(mx, my, armorX, armorY + i * armorPitch()) && equipmentIds[i] > 0) {
                    return equipmentIds[i];
                }
            }
            if (inSlot(mx, my, offhandX(screenW), offhandY(screenH)) && equipmentIds[4] > 0) {
                return equipmentIds[4];
            }
        }

        int craftX = craftAreaX(screenW, craftSize);
        int craftY = craftY(screenH, craftSize);
        for (int row = 0; row < craftSize; row++) {
            for (int col = 0; col < craftSize; col++) {
                int index = row * craftSize + col;
                if (inSlot(mx, my, craftX + col * craftPitch(), craftY + row * craftPitch())
                        && craftIds[index] > 0) return craftIds[index];
            }
        }
        if (inSlot(mx, my, outputX(screenW, craftSize), outputY(screenH, craftSize)) && !result.empty()) {
            return result.resultId;
        }

        int inventoryX = invX(screenW);
        int inventoryY = invY(screenH);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = 9 + row * 9 + col;
                if (inSlot(mx, my, inventoryX + col * invPitch(), inventoryY + row * invPitch())
                        && inventoryIds[index] > 0) return inventoryIds[index];
            }
        }
        int hotbarY = hotY(screenH);
        for (int col = 0; col < 9; col++) {
            if (inSlot(mx, my, inventoryX + col * invPitch(), hotbarY) && inventoryIds[col] > 0) {
                return inventoryIds[col];
            }
        }
        return 0;
    }

    private static boolean inSlot(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }
}
