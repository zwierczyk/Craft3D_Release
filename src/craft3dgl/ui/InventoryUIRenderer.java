package craft3dgl.ui;

import craft3dgl.CraftingSystemGL;
import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;
import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer Inventory GUI - uzywa tekstury MC-style (assets/icons/inventory.png)
 * jako tlo, rysuje itemki w slotach.
 *
 * Layout dokladnie jak w oryginalnym MC:
 * - PNG source: 176x166 px, sloty 18x18 (skala S = SLOT_TEX * SCALE)
 * - Skala 3x wyswietlania: panel 528x498, sloty 54x54
 *
 * Pozycje slotow (w PNG @ 176x166, x,y = lewy-gorny pixela slotu):
 *  Armor      : 7,7  7,25  7,43  7,61
 *  Portret box: 25,7 do 76,68  (50x62)
 *  Offhand    : 77,61
 *  Crafting 2x2 (survival): 97,17  115,17  97,35  115,35
 *  Arrow      : ok. 133,26
 *  Output     : 151,25
 *  Backpack   : 7,83 do 151,83 (start row), pitch 18, 3 wiersze
 *  Hotbar     : 7,141 do 151,141
 *
 * Dla craftingu 3x3 (crafting table) uzywamy TEJ SAMEJ tekstury ale rysujemy
 * dodatkowy rzad slotow uzytkownika (bo tekstura ma tylko 2x2 crafting).
 */
public final class InventoryUIRenderer {

    // === Skala UI ===
    private static final int SCALE = 3;

    // === Rozmiary PNG source ===
    private static final int TEX_W = 176;
    private static final int TEX_H = 166;
    private static final int TEX_SLOT = 18;

    // === Skalowane wymiary panelu ===
    public static final int PANEL_W = TEX_W * SCALE;   // 528
    public static final int PANEL_H = TEX_H * SCALE;   // 498
    public static final int SLOT_SIZE = TEX_SLOT * SCALE;  // 54

    // === Pozycje slotow w PNG (piksele w oryginale) ===
    private static final int TEX_ARMOR_X   = 7;
    private static final int TEX_ARMOR_Y0  = 7;
    private static final int TEX_ARMOR_PITCH = 18;

    private static final int TEX_OFFHAND_X = 77;
    private static final int TEX_OFFHAND_Y = 61;

    private static final int TEX_PORTRAIT_X = 25;
    private static final int TEX_PORTRAIT_Y = 7;
    private static final int TEX_PORTRAIT_W = 50;
    // Czarne pole podgladu konczy sie na y=77; model MC stoi przy y=75.
    private static final int TEX_PORTRAIT_H = 70;

    private static final int TEX_CRAFT_X = 97;
    private static final int TEX_CRAFT_Y = 17;
    private static final int TEX_CRAFT_PITCH = 18;

    private static final int TEX_ARROW_X = 133;
    private static final int TEX_ARROW_Y = 26;

    private static final int TEX_OUTPUT_X = 151;
    private static final int TEX_OUTPUT_Y = 25;

    private static final int TEX_BACKPACK_X = 7;
    private static final int TEX_BACKPACK_Y = 83;
    private static final int TEX_HOTBAR_Y   = 141;

    // === Tekstury ===
    private static int texInv = -1;
    private static boolean initialized = false;

    private InventoryUIRenderer() {}

    private static void ensureLoaded() {
        if (initialized) return;
        initialized = true;
        try {
            File dir = AssetFinder.findAssetDir("icons", InventoryUIRenderer.class);
            texInv = loadTexture(new File(dir, "inventory.png"));
            System.out.println("[Inventory] loaded texture: " + texInv);
        } catch (Throwable t) {
            System.err.println("[Inventory] blad ladowania: " + t.getMessage());
        }
    }

    private static int loadTexture(File file) {
        if (!file.exists()) return -1;
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return -1;
            int w = img.getWidth(), h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = pixels[y * w + x];
                    int a = (p >> 24) & 0xff;
                    if (a == 0) a = 255;
                    buf.put((byte)((p >> 16) & 0xff));
                    buf.put((byte)((p >> 8) & 0xff));
                    buf.put((byte)(p & 0xff));
                    buf.put((byte) a);
                }
            }
            buf.flip();
            int tex = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, tex);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            return tex;
        } catch (Throwable t) {
            return -1;
        }
    }

    // === Public position helpers (screen coords) ===

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }

    public static int armorX(int screenW) { return panelX(screenW) + TEX_ARMOR_X * SCALE; }
    public static int armorY(int screenH) { return panelY(screenH) + TEX_ARMOR_Y0 * SCALE; }
    public static int armorPitch() { return TEX_ARMOR_PITCH * SCALE; }

    public static int portraitX(int screenW) { return panelX(screenW) + TEX_PORTRAIT_X * SCALE; }
    public static int portraitY(int screenH) { return panelY(screenH) + TEX_PORTRAIT_Y * SCALE; }
    public static int portraitW() { return TEX_PORTRAIT_W * SCALE; }
    public static int portraitH() { return TEX_PORTRAIT_H * SCALE; }

    public static int offhandX(int screenW) { return panelX(screenW) + TEX_OFFHAND_X * SCALE; }
    public static int offhandY(int screenH) { return panelY(screenH) + TEX_OFFHAND_Y * SCALE; }

    public static int craftAreaX(int screenW, int craftSize) {
        // Dla 2x2 uzyj pozycji z PNG. Dla 3x3 przesun w lewo o 1 slot (aby zmiescic dodatkowy rzad).
        int offset = craftSize == 3 ? -TEX_CRAFT_PITCH : 0;
        return panelX(screenW) + (TEX_CRAFT_X + offset) * SCALE;
    }
    public static int craftY(int screenH) {
        return panelY(screenH) + TEX_CRAFT_Y * SCALE;
    }
    public static int craftPitch() { return TEX_CRAFT_PITCH * SCALE; }

    public static int outputX(int screenW, int craftSize) {
        return panelX(screenW) + TEX_OUTPUT_X * SCALE;
    }
    public static int outputY(int screenH, int craftSize) {
        return panelY(screenH) + TEX_OUTPUT_Y * SCALE;
    }

    public static int invX(int screenW) { return panelX(screenW) + TEX_BACKPACK_X * SCALE; }
    public static int invY(int screenH) { return panelY(screenH) + TEX_BACKPACK_Y * SCALE; }
    public static int invPitch() { return TEX_CRAFT_PITCH * SCALE; }

    public static int hotY(int screenH) { return panelY(screenH) + TEX_HOTBAR_Y * SCALE; }

    // === Main draw ===

    public static void draw(FontRenderer font, IconDrawer iconDrawer, Translations trans,
                            int screenW, int screenH, int mx, int my,
                            boolean usingCraftingTable,
                            int[] craftIds, int[] craftCnts, CraftingSystemGL.Recipe craftResult,
                            int[] equipIds, int[] equipCnts,
                            int[] invIds, int[] invCnts, int selectedSlot,
                            int cursorId, int cursorCount) {
        ensureLoaded();
        UIStyle.drawDimBackground(screenW, screenH, 0.55f);

        int px = panelX(screenW);
        int py = panelY(screenH);

        // Rysuj tlo z tekstury (upscale 3x, pixel-perfect)
        if (texInv > 0) {
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glBindTexture(GL_TEXTURE_2D, texInv);
            glColor4f(1f, 1f, 1f, 1f);
            // Uwaga: tylko fragment 176x166 z tekstury 256x256!
            float u1 = (float) TEX_W / 256f;
            float v1 = (float) TEX_H / 256f;
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);   glVertex2i(px, py);
            glTexCoord2f(u1, 0);  glVertex2i(px + PANEL_W, py);
            glTexCoord2f(u1, v1); glVertex2i(px + PANEL_W, py + PANEL_H);
            glTexCoord2f(0, v1);  glVertex2i(px, py + PANEL_H);
            glEnd();
        }

        int craftSize = usingCraftingTable ? 3 : 2;

        // ARMOR 4 pionowo
        int aX = armorX(screenW);
        int aY = armorY(screenH);
        int aPitch = armorPitch();
        for (int i = 0; i < 4; i++) {
            int sx = aX, sy = aY + i * aPitch;
            iconDrawer.drawStackIcon(equipIds[i], equipCnts[i], sx + 3, sy + 3, SLOT_SIZE - 6);
            if (equipIds[i] == 0) drawArmorSilhouette(sx, sy, i);
            drawSlotHover(sx, sy, mx, my);
        }

        // Pelny model 3D patrzy w kierunku kursora jak w GuiInventory MC 1.12.
        drawPortraitCharacter(portraitX(screenW), portraitY(screenH), portraitW(), portraitH(),
                screenW, screenH, mx, my);

        // OFFHAND
        int oX = offhandX(screenW);
        int oY = offhandY(screenH);
        iconDrawer.drawStackIcon(equipIds[4], equipCnts[4], oX + 3, oY + 3, SLOT_SIZE - 6);
        if (equipIds[4] == 0) {
            glDisable(GL_TEXTURE_2D);
            glColor4f(0.30f, 0.30f, 0.30f, 0.45f);
            UIStyle.quad(oX + 18, oY + 12, 18, 24);
            UIStyle.quad(oX + 22, oY + 36, 10, 6);
            glEnable(GL_TEXTURE_2D);
        }
        drawSlotHover(oX, oY, mx, my);

        // CRAFTING
        int cx = craftAreaX(screenW, craftSize);
        int cy = craftY(screenH);
        int cPitch = craftPitch();
        // Jesli 3x3 (crafting table), musimy narysowac dodatkowe sloty (tekstura tylko 2x2 pokazuje)
        if (craftSize == 3) {
            // Dodatkowa kolumna z lewej i dodatkowy rzad na dole - narysowac slot backgroundy
            for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
                if (row < 2 && col > 0) continue; // ten obszar jest juz na teksturze
                int sx = cx + col * cPitch;
                int sy = cy + row * cPitch;
                drawSlotBackground(sx, sy);
            }
        }
        for (int row = 0; row < craftSize; row++) for (int col = 0; col < craftSize; col++) {
            int idx = row * craftSize + col;
            int sx = cx + col * cPitch;
            int sy = cy + row * cPitch;
            iconDrawer.drawStackIcon(craftIds[idx], craftCnts[idx], sx + 3, sy + 3, SLOT_SIZE - 6);
            drawSlotHover(sx, sy, mx, my);
        }
        // Output
        int outX = outputX(screenW, craftSize);
        int outY = outputY(screenH, craftSize);
        if (!craftResult.empty()) {
            iconDrawer.drawStackIcon(craftResult.resultId, craftResult.resultCount, outX + 3, outY + 3, SLOT_SIZE - 6);
        }
        drawSlotHover(outX, outY, mx, my);

        // Tytul (nad crafting area, na jasnoszarym pasku)
        String title = usingCraftingTable ? trans.tr("crafting.title") : trans.tr("section.craft");
        font.drawTextDark(title, cx, py + 6, 0.55f);

        // INV 3x9
        int iX = invX(screenW);
        int iY = invY(screenH);
        int iPitch = invPitch();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = 9 + row * 9 + col;
            int sx = iX + col * iPitch;
            int sy = iY + row * iPitch;
            iconDrawer.drawStackIcon(invIds[idx], invCnts[idx], sx + 3, sy + 3, SLOT_SIZE - 6);
            drawSlotHover(sx, sy, mx, my);
        }

        // HOTBAR
        int hY = hotY(screenH);
        for (int col = 0; col < 9; col++) {
            int sx = iX + col * iPitch;
            iconDrawer.drawStackIcon(invIds[col], invCnts[col], sx + 3, hY + 3, SLOT_SIZE - 6);
            if (col == selectedSlot) {
                drawSlotSelected(sx, hY);
            }
            drawSlotHover(sx, hY, mx, my);
        }

        // Tooltip
        int hoverItem = findHoverItem(mx, my, screenW, screenH, craftSize,
                                       craftIds, craftResult, equipIds, invIds);
        if (hoverItem > 0 && (cursorId == 0 || cursorCount == 0)) {
            Tooltip.draw(font, ItemNames.itemName(hoverItem, trans.getLanguage()), mx, my, screenW, screenH);
        }
        if (cursorId > 0 && cursorCount > 0) {
            iconDrawer.drawStackIcon(cursorId, cursorCount, mx - SLOT_SIZE / 2, my - SLOT_SIZE / 2, SLOT_SIZE - 6);
        }
    }

    // === Helpers ===

    private static void drawSlotHover(int sx, int sy, int mx, int my) {
        if (mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE) {
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1f, 1f, 1f, 0.35f);
            UIStyle.quad(sx, sy, SLOT_SIZE, SLOT_SIZE);
            glEnable(GL_TEXTURE_2D);
        }
    }

    private static void drawSlotSelected(int sx, int sy) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glColor4f(1f, 1f, 1f, 0.85f);
        UIStyle.lineRect(sx - 1, sy - 1, SLOT_SIZE + 2, SLOT_SIZE + 2);
        UIStyle.lineRect(sx, sy, SLOT_SIZE, SLOT_SIZE);
        glEnable(GL_TEXTURE_2D);
    }

    private static void drawSlotBackground(int sx, int sy) {
        // Slot na jasnoszarym tle z ciemniejszym wnetrzem (dla dodatkowych slotow craftingu 3x3)
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.545f, 0.545f, 0.545f, 1f); // #8b8b8b - jasnoszary MC
        UIStyle.quad(sx - 3, sy - 3, SLOT_SIZE + 6, SLOT_SIZE + 6);
        glColor4f(0.216f, 0.216f, 0.216f, 1f); // ciemna ramka
        UIStyle.quad(sx, sy, SLOT_SIZE, 3);
        UIStyle.quad(sx, sy, 3, SLOT_SIZE);
        glColor4f(1f, 1f, 1f, 1f);
        UIStyle.quad(sx + SLOT_SIZE - 3, sy, 3, SLOT_SIZE);
        UIStyle.quad(sx, sy + SLOT_SIZE - 3, SLOT_SIZE, 3);
        glColor4f(0.545f, 0.545f, 0.545f, 1f);
        UIStyle.quad(sx + 3, sy + 3, SLOT_SIZE - 6, SLOT_SIZE - 6);
        glEnable(GL_TEXTURE_2D);
    }

    private static void drawArmorSilhouette(int sx, int sy, int armorIdx) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(0.35f, 0.35f, 0.35f, 0.55f);
        int cxs = sx + SLOT_SIZE / 2;
        int cys = sy + SLOT_SIZE / 2;
        if (armorIdx == 0) {         // helm
            UIStyle.quad(cxs - 15, cys - 18, 30, 6);
            UIStyle.quad(cxs - 18, cys - 12, 36, 18);
            UIStyle.quad(cxs - 8, cys + 6, 16, 6);
        } else if (armorIdx == 1) {  // chest
            UIStyle.quad(cxs - 18, cys - 18, 36, 8);
            UIStyle.quad(cxs - 21, cys - 10, 42, 22);
            UIStyle.quad(cxs - 18, cys + 12, 36, 6);
        } else if (armorIdx == 2) {  // legs
            UIStyle.quad(cxs - 15, cys - 18, 30, 8);
            UIStyle.quad(cxs - 15, cys - 10, 12, 26);
            UIStyle.quad(cxs + 3, cys - 10, 12, 26);
        } else {                     // boots
            UIStyle.quad(cxs - 15, cys - 12, 12, 24);
            UIStyle.quad(cxs + 3, cys - 12, 12, 24);
            UIStyle.quad(cxs - 18, cys + 12, 15, 6);
            UIStyle.quad(cxs + 3, cys + 12, 15, 6);
        }
        glEnable(GL_TEXTURE_2D);
    }

    private static void drawPortraitCharacter(int x, int y, int w, int h,
                                              int screenW, int screenH,
                                              int mouseX, int mouseY) {
        if (!craft3dgl.entities.SteveRenderer.drawInventoryPlayer(
                screenW, screenH, x, y, w, h, mouseX, mouseY)) {
            drawPortraitFallback(x, y, w, h);
        }
    }

    private static void drawPortraitFallback(int x, int y, int w, int h) {
        glDisable(GL_TEXTURE_2D);
        int cx2 = x + w / 2;
        int hY = y + h / 5;
        glColor3f(0.92f, 0.72f, 0.52f);
        UIStyle.quad(cx2 - 18, hY, 36, 33);
        glColor3f(0.78f, 0.18f, 0.20f);
        UIStyle.quad(cx2 - 22, hY + 36, 44, 57);
        glEnable(GL_TEXTURE_2D);
    }

    private static int findHoverItem(int mx, int my, int screenW, int screenH, int craftSize,
                                     int[] craftIds, CraftingSystemGL.Recipe craftResult,
                                     int[] equipIds, int[] invIds) {
        int aX = armorX(screenW);
        int aY = armorY(screenH);
        int aPitch = armorPitch();
        for (int i = 0; i < 4; i++) {
            if (inSlot(mx, my, aX, aY + i * aPitch) && equipIds[i] > 0) return equipIds[i];
        }
        int oX = offhandX(screenW), oY = offhandY(screenH);
        if (inSlot(mx, my, oX, oY) && equipIds[4] > 0) return equipIds[4];
        int cx = craftAreaX(screenW, craftSize);
        int cy = craftY(screenH);
        int cP = craftPitch();
        for (int row = 0; row < craftSize; row++) for (int col = 0; col < craftSize; col++) {
            int idx = row * craftSize + col;
            int sx = cx + col * cP, sy = cy + row * cP;
            if (inSlot(mx, my, sx, sy) && craftIds[idx] > 0) return craftIds[idx];
        }
        int outX = outputX(screenW, craftSize), outY = outputY(screenH, craftSize);
        if (inSlot(mx, my, outX, outY) && !craftResult.empty()) return craftResult.resultId;
        int iX = invX(screenW), iY = invY(screenH);
        int iP = invPitch();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            int idx = 9 + row * 9 + col;
            int sx = iX + col * iP, sy = iY + row * iP;
            if (inSlot(mx, my, sx, sy) && invIds[idx] > 0) return invIds[idx];
        }
        int hY = hotY(screenH);
        for (int col = 0; col < 9; col++) {
            int sx = iX + col * iP;
            if (inSlot(mx, my, sx, hY) && invIds[col] > 0) return invIds[col];
        }
        return 0;
    }

    private static boolean inSlot(int mx, int my, int sx, int sy) {
        return mx >= sx && mx < sx + SLOT_SIZE && my >= sy && my < sy + SLOT_SIZE;
    }
}
