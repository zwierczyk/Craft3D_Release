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

    // ==== ZAKLADKI KREATYWNE (pionowa belka po lewej, sprite'y tab_bottom z 26.2) ====
    private static final int TAB_RAIL_W = 26 * SCALE;      // 78
    private static final int TAB_RAIL_H = 32 * SCALE;      // 96
    private static final int TAB_STEP = 32 * SCALE;        // 96 (sprites same height)

    public static int tabRailX(int screenW) { return gridX(screenW) - TAB_RAIL_W - 4 * SCALE; }
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
        // Ciemne wnetrze jak w vanilla creative - swiat widoczny, ale przygaszony.
        UIStyle.drawDimBackground(screenW, screenH, 0.45f);

        int px = panelX(screenW);
        int py = panelY(screenH);

        // Lekki, polprzezroczysty "obrys" obszaru kontenera (bez starego PNG-panelu).
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.05f, 0.06f, 0.08f, 0.30f);
        UIStyle.quad(px, py, PANEL_W, PANEL_H);

        // ==== ZAKLADKI: pionowa belka kategorii (sprite'y tab_bottom z 26.2) ====
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
            String skin = selected
                    ? "tab_bottom_selected_" + (i + 1)
                    : "tab_bottom_unselected_" + (i + 1);
            // Wybrany tab rysuje sie na wierzchu (laczy sie z obszarem slotow).
            VanillaGuiTextures.drawRegion(skin, tabX, ty, 0, 0, 26, 32, SCALE);
            int iconSize = 16 * SCALE;
            int iconOff = (tabW - iconSize) / 2;
            int iconY = ty + 7 * SCALE;
            glEnable(GL_TEXTURE_2D);
            iconDrawer.drawStackIcon(tabIcons[i], 1, tabX + iconOff, iconY, iconSize);
            if (mx >= tabX && mx < tabX + tabW && my >= ty && my < ty + tabH) {
                hoverTab = i;
            }
        }

        // ==== GRID 9x5 = 45: kazdy slot to osobna komorka slot.png (jak vanilla) ====
        int gX = gridX(screenW);
        int gY = gridY(screenH);
        int tipId = 0;
        for (int i = 0; i < 45; i++) {
            int col = i % 9, row = i / 9;
            int sx = gX + col * SLOT_PITCH, sy = gY + row * SLOT_PITCH;
            VanillaGuiTextures.drawRegion("slot", sx + 3, sy + 3, 1, 1, 16, 16, SCALE);
            boolean over = mx >= sx && mx < sx + SLOT_PITCH && my >= sy && my < sy + SLOT_PITCH;
            if (over) {
                glDisable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(1f, 1f, 1f, 0.30f);
                UIStyle.quad(sx, sy, SLOT_PITCH, SLOT_PITCH);
                glColor4f(1f, 1f, 1f, 0.80f);
                UIStyle.lineRect(sx, sy, SLOT_PITCH, SLOT_PITCH);
                glEnable(GL_TEXTURE_2D);
                if (i < items.length && items[i] > 0) tipId = items[i];
            }
            if (i < items.length && items[i] > 0) {
                // Centruj icon w slocie (48 px w komorce 54 px).
                int off = (SLOT_PITCH - SLOT_SIZE) / 2;
                iconDrawer.drawStackIcon(items[i], 1, sx + off, sy + off, SLOT_SIZE);
            }
        }

        // ==== HOTBAR (9 slotow, wybrany ma jasna ramke) ====
        int hY = hotY(screenH);
        int hotTip = 0;
        for (int col = 0; col < 9; col++) {
            int sx = invX(screenW) + col * SLOT_PITCH;
            VanillaGuiTextures.drawRegion("slot", sx + 3, hY + 3, 1, 1, 16, 16, SCALE);
            if (col == selectedSlot) {
                glDisable(GL_TEXTURE_2D);
                glColor4f(1f, 1f, 1f, 1f);
                UIStyle.lineRect(sx - 1, hY - 1, SLOT_PITCH, SLOT_PITCH);
                glEnable(GL_TEXTURE_2D);
            }
            boolean over = mx >= sx && mx < sx + SLOT_PITCH && my >= hY && my < hY + SLOT_PITCH;
            if (over) {
                glDisable(GL_TEXTURE_2D);
                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glColor4f(1f, 1f, 1f, 0.30f);
                UIStyle.quad(sx, hY, SLOT_PITCH, SLOT_PITCH);
                glEnable(GL_TEXTURE_2D);
                if (invIds[col] > 0) hotTip = invIds[col];
            }
            int hOff = (SLOT_PITCH - SLOT_SIZE) / 2;
            iconDrawer.drawStackIcon(invIds[col], invCnts[col], sx + hOff, hY + hOff, SLOT_SIZE);
        }

        // ==== KOSZ NA PRZEDMIOTY (komorka + czerwony znak) ====
        int tX = trashX(screenW);
        int tY2 = trashY(screenH);
        boolean trashHover = UIStyle.inside(mx, my, tX, tY2, TRASH_W, TRASH_W);
        VanillaGuiTextures.drawRegion("slot", tX, tY2, 1, 1, 16, 16, SCALE);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(trashHover ? 0.85f : 0.60f, 0.10f, 0.10f, 0.85f);
        UIStyle.quad(tX, tY2, TRASH_W, TRASH_W);
        glColor4f(0.95f, 0.95f, 0.95f, 1f);
        int cx = tX + TRASH_W / 2;
        int cy = tY2 + TRASH_W / 2;
        int r = TRASH_W / 3;
        glBegin(GL_QUADS);
        glVertex2i(cx - r, cy - 3); glVertex2i(cx - r, cy + 3); glVertex2i(cx + r, cy + 3); glVertex2i(cx + r, cy - 3);
        glEnd();
        glBegin(GL_QUADS);
        glVertex2i(cx - 3, cy - r); glVertex2i(cx + 3, cy - r); glVertex2i(cx + 3, cy + r); glVertex2i(cx - 3, cy + r);
        glEnd();
        glEnable(GL_TEXTURE_2D);
        if (trashHover) Tooltip.draw(font, trans.tr("creative.trash.short"), mx, my, screenW, screenH);

        // ==== TOOLTIPY: kategoria taba albo nazwa przedmiotu pod kursorem ====
        String tipText = null;
        if (hoverTab >= 0) tipText = tabNames[hoverTab];
        else if (tipId > 0) tipText = craft3dgl.items.ItemNames.itemName(tipId, trans.getLanguage());
        else if (hotTip > 0) tipText = craft3dgl.items.ItemNames.itemName(hotTip, trans.getLanguage());
        if (tipText != null) Tooltip.draw(font, tipText, mx, my, screenW, screenH);

        // ==== CURSOR ITEM (przenoszony przedmiot pod kursorem) ====
        if (cursorId > 0 && cursorCount > 0) {
            iconDrawer.drawStackIcon(cursorId, cursorCount, mx - 24, my - 24, 16 * SCALE);
        }
        glColor4f(1f, 1f, 1f, 1f);
    }
}
