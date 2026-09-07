package craft3dgl.ui;

import craft3dgl.input.Translations;
import craft3dgl.items.ItemNames;
import craft3dgl.save.AssetFinder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;

/**
 * Creative Inventory Renderer - MC 1.14.4 style.
 * Layout dokladny z net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen:
 *   - Panel 195x136 (skalowany 3x = 585x408) - ale my uzywamy tylko 176x136 z tekstury
 *   - Grid 9x5 slotow, x=9,y=18, slot 18x18 (skalowany 3x)
 *   - Hotbar y=112, 9 slotow
 *   - Tabs 28x32 na gorze
 *   - Search box x=82,y=6, width=80
 *   - Trash slot x=173,y=112 (ale my mamy 176 wide - dostosowane)
 *   - Bierze background z creative_items.png (256x256 palette-converted-RGBA)
 */
public final class CreativeUIRenderer {

    // MC scale
    private static final int SCALE = 3;
    private static final int TEX_W = 176;         // uzywana czesc PNG
    private static final int TEX_H = 136;
    private static final int TEX_SLOT = 18;

    public static final int PANEL_W = TEX_W * SCALE;       // 528
    public static final int PANEL_H = TEX_H * SCALE;       // 408
    public static final int SLOT_PITCH = TEX_SLOT * SCALE; // 54
    public static final int SLOT_SIZE = 16 * SCALE;        // 48 (jak vanilla)
    public static final int SEARCH_W = 80 * SCALE;
    public static final int SEARCH_X_BTN_W = 12 * SCALE;
    public static final int TRASH_W = 16 * SCALE;          // 48

    // Texture loading (podobnie jak InventoryUIRenderer)
    private static int texCreative = 0;
    private static boolean loaded = false;

    private CreativeUIRenderer() {}

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File dir = AssetFinder.findAssetDir("icons", CreativeUIRenderer.class);
            File f = new File(dir, "creative_items.png");
            if (!f.isFile()) {
                System.err.println("[Creative] creative_items.png not found");
                return;
            }
            BufferedImage img = ImageIO.read(f);
            int w = img.getWidth(), h = img.getHeight();
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    buf.put((byte)((argb >> 16) & 0xFF));
                    buf.put((byte)((argb >> 8) & 0xFF));
                    buf.put((byte)(argb & 0xFF));
                    buf.put((byte)((argb >> 24) & 0xFF));
                }
            }
            buf.flip();
            texCreative = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texCreative);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glBindTexture(GL_TEXTURE_2D, 0);
            System.out.println("[Creative] loaded texture: " + texCreative);
        } catch (Exception e) {
            System.err.println("[Creative] load failed: " + e);
        }
    }

    public static int panelX(int screenW) { return screenW / 2 - PANEL_W / 2; }
    public static int panelY(int screenH) { return screenH / 2 - PANEL_H / 2; }

    // ==== ZAKLADKI KREATYWNE (pionowa belka po lewej, jak w vanilla MC) ====
    private static final int TAB_RAIL_W = 22 * SCALE;      // 66
    private static final int TAB_RAIL_H = 24 * SCALE;      // 72
    private static final int TAB_STEP = TAB_RAIL_H + 2;    // 74 (2 px odstepu)
    private static final int TAB_OVERLAP = 6 * SCALE;      // 18 px wchodzi na panel

    public static int tabRailX(int screenW) { return panelX(screenW) - TAB_RAIL_W + TAB_OVERLAP; }
    public static int tabRailY(int screenH, int tab) { return panelY(screenH) + 4 * SCALE + tab * TAB_STEP; }
    public static int tabCount() { return 5; }
    public static int tabHeightSmall() { return TAB_RAIL_H; }
    public static int tabWidthSmall() { return TAB_RAIL_W; }
    // Grid slotow MC: x=9, y=18
    public static int gridX(int screenW) { return panelX(screenW) + 9 * SCALE; }
    public static int gridY(int screenH) { return panelY(screenH) + 18 * SCALE; }
    // Hotbar MC: x=9, y=112
    public static int invX(int screenW) { return panelX(screenW) + 9 * SCALE; }
    public static int invY(int screenH) { return panelY(screenH) + 112 * SCALE; }
    public static int hotY(int screenH) { return invY(screenH); }
    // Trash MC: x=173, y=112 - ale my mamy 176 wide, wiec przesuwamy do wewnatrz
    // Kosz - poza panelem po prawej stronie, na wysokosci grid
    public static int trashX(int screenW) { return panelX(screenW) + PANEL_W + 15; }
    public static int trashY(int screenH) { return panelY(screenH) + 30 * SCALE; }
    // Search box (dla search tab) MC: x=82, y=6
    public static int searchX(int screenW) { return panelX(screenW) + 82 * SCALE; }
    public static int searchY(int screenH) { return panelY(screenH) + 6 * SCALE; }
    public static int searchClearX(int screenW) { return searchX(screenW) + SEARCH_W + 2; }

    public static void draw(FontRenderer font, IconDrawer iconDrawer, Translations trans,
                            int screenW, int screenH, int mx, int my,
                            int[] items, int creativeTab, String searchText,
                            int[] invIds, int[] invCnts, int selectedSlot,
                            int cursorId, int cursorCount) {
        ensureLoaded();
        UIStyle.drawDimBackground(screenW, screenH, 0.55f);

        int px = panelX(screenW);
        int py = panelY(screenH);

        // ==== PANEL BACKGROUND (creative_items.png, fragment 176x136) ====
        if (texCreative > 0) {
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glBindTexture(GL_TEXTURE_2D, texCreative);
            glColor4f(1f, 1f, 1f, 1f);
            float u1 = (float) TEX_W / 256f;
            float v1 = (float) TEX_H / 256f;
            glBegin(GL_QUADS);
            glTexCoord2f(0, 0);   glVertex2i(px, py);
            glTexCoord2f(u1, 0);  glVertex2i(px + PANEL_W, py);
            glTexCoord2f(u1, v1); glVertex2i(px + PANEL_W, py + PANEL_H);
            glTexCoord2f(0, v1);  glVertex2i(px, py + PANEL_H);
            glEnd();
        } else {
            // Fallback
            UIStyle.drawPanel(px, py, PANEL_W, PANEL_H);
        }

        // Tytul (MC: x=8, y=6, kolor 4210752 = 0x404040)
        font.drawTextDark(trans.tr("creative.title"), px + 8 * SCALE, py + 6 * SCALE, 0.55f);

        // ==== ZAKLADKI: pionowa belka kategorii po lewej (jak vanilla MC) ====
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
        int tabCount = craft3dgl.ui.CreativeUIRenderer.tabCount();
        int tabX = craft3dgl.ui.CreativeUIRenderer.tabRailX(screenW);
        int tabW = craft3dgl.ui.CreativeUIRenderer.tabWidthSmall();
        int tabH = craft3dgl.ui.CreativeUIRenderer.tabHeightSmall();
        int hoverTab = -1;
        for (int i = 0; i < tabCount; i++) {
            int ty = craft3dgl.ui.CreativeUIRenderer.tabRailY(screenH, i);
            boolean selected = (i == creativeTab);
            // Wybrany tab nachodzi na panel (OVERLAP), reszta wystaje w lewo.
            glDisable(GL_TEXTURE_2D);
            if (selected) {
                glColor4f(0.87f, 0.87f, 0.87f, 1f);
                UIStyle.quad(tabX, ty, tabW, tabH);
                glColor4f(0.13f, 0.13f, 0.13f, 1f);
                UIStyle.quad(tabX + tabW, ty, 2 * SCALE, tabH);
            } else {
                glColor4f(0.47f, 0.47f, 0.47f, 1f);
                UIStyle.quad(tabX, ty, tabW, tabH);
                glColor4f(0.12f, 0.12f, 0.12f, 1f);
                UIStyle.lineRect(tabX, ty, tabW, tabH);
            }
            glEnable(GL_TEXTURE_2D);
            int iconSize = 14 * SCALE;
            int iconOff = (tabW - iconSize) / 2;
            int iconY = ty + (tabH - iconSize) / 2 - 1;
            iconDrawer.drawStackIcon(tabIcons[i], 1, tabX + iconOff, iconY, iconSize);
            if (mx >= tabX && mx < tabX + tabW && my >= ty && my < ty + tabH) {
                hoverTab = i;
            }
        }

        // ==== GRID SLOTOW 9x5 = 45 (icons wrisujemy na wierzchu PNG slotow) ====
        int gX = gridX(screenW);
        int gY = gridY(screenH);
        int tipId = 0;
        for (int i = 0; i < 45; i++) {
            int col = i % 9, row = i / 9;
            int sx = gX + col * SLOT_PITCH, sy = gY + row * SLOT_PITCH;
            if (i < items.length && items[i] > 0) {
                // Centruj icon w slocie
                int off = (SLOT_PITCH - SLOT_SIZE) / 2;
                iconDrawer.drawStackIcon(items[i], 1, sx + off, sy + off, SLOT_SIZE);
            }
            // Hover highlight (caly slot, nie tylko mniejszy prostokat)
            if (mx >= sx && mx < sx + SLOT_PITCH && my >= sy && my < sy + SLOT_PITCH) {
                glDisable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glColor4f(1f, 1f, 1f, 0.35f);
                UIStyle.quad(sx, sy, SLOT_PITCH, SLOT_PITCH);
                glEnable(GL_TEXTURE_2D);
                if (i < items.length && items[i] > 0) tipId = items[i];
            }
        }

        // ==== HOTBAR (9 slotow na dole) ====
        int hY = hotY(screenH);
        int hotTip = 0;
        for (int col = 0; col < 9; col++) {
            int sx = invX(screenW) + col * SLOT_PITCH;
            if (col == selectedSlot) {
                // Highlight selected slot (jasna obwodka)
                glDisable(GL_TEXTURE_2D);
                glColor4f(1f, 1f, 1f, 1f);
                UIStyle.lineRect(sx - 1, hY - 1, SLOT_PITCH, SLOT_PITCH);
                UIStyle.lineRect(sx, hY, SLOT_PITCH - 2, SLOT_PITCH - 2);
                glEnable(GL_TEXTURE_2D);
            }
            int hOff = (SLOT_PITCH - SLOT_SIZE) / 2;
            iconDrawer.drawStackIcon(invIds[col], invCnts[col], sx + hOff, hY + hOff, SLOT_SIZE);
            // Hover na caly slot
            if (mx >= sx && mx < sx + SLOT_PITCH && my >= hY && my < hY + SLOT_PITCH) {
                glDisable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glColor4f(1f, 1f, 1f, 0.35f);
                UIStyle.quad(sx, hY, SLOT_PITCH, SLOT_PITCH);
                glEnable(GL_TEXTURE_2D);
                if (invIds[col] > 0) hotTip = invIds[col];
            }
        }

        // ==== TRASH SLOT ====
        int tX = trashX(screenW);
        int tY2 = trashY(screenH);
        boolean trashHover = UIStyle.inside(mx, my, tX, tY2, TRASH_W, TRASH_W);
        glDisable(GL_TEXTURE_2D);
        glColor4f(trashHover ? 0.80f : 0.60f, 0.15f, 0.15f, 1f);
        UIStyle.quad(tX, tY2, TRASH_W, TRASH_W);
        glColor4f(0.20f, 0.20f, 0.20f, 1f);
        UIStyle.lineRect(tX, tY2, TRASH_W, TRASH_W);
        // Ikona kubelka smieci (proceduralna, biale kreski)
        glColor4f(0.95f, 0.95f, 0.95f, 1f);
        UIStyle.quad(tX + 8, tY2 + 10, 32, 4);              // pokrywa
        UIStyle.quad(tX + 12, tY2 + 8, 24, 4);              // uchwyt na gorze
        UIStyle.lineRect(tX + 10, tY2 + 14, 28, 26);        // kubelek
        UIStyle.quad(tX + 16, tY2 + 18, 3, 18);             // paski
        UIStyle.quad(tX + 22, tY2 + 18, 3, 18);
        UIStyle.quad(tX + 28, tY2 + 18, 3, 18);
        glEnable(GL_TEXTURE_2D);
        if (trashHover) Tooltip.draw(font, trans.tr("creative.trash.short"), mx, my, screenW, screenH);

        // ==== TOOLTIPY: kategoria taba albo nazwa przedmiotu pod kursorem ====
        String tipText = null;
        if (hoverTab >= 0) tipText = tabNames[hoverTab];
        else if (tipId > 0) tipText = craft3dgl.items.ItemNames.itemName(tipId, trans.getLanguage());
        else if (hotTip > 0) tipText = craft3dgl.items.ItemNames.itemName(hotTip, trans.getLanguage());
        if (tipText != null) Tooltip.draw(font, tipText, mx, my, screenW, screenH);

        // ==== CURSOR ITEM (item na kursorze przy przenoszeniu) ====
        if (cursorId > 0 && cursorCount > 0) {
            iconDrawer.drawStackIcon(cursorId, cursorCount, mx - 16, my - 16, 32);
        }
    }
}
