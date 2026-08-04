package craft3dgl.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import craft3dgl.save.AssetFinder;

import static org.lwjgl.opengl.GL11.*;

/**
 * MC-style hotbar renderer z widgets.png (256x256).
 * Layout z MC:
 *   - Hotbar bg: (0,0) 182x22 pixel (skalowany 3x)
 *   - Selected slot overlay: (0,22) 24x24 pixel (skalowany 3x)
 * Slot MC = 20x20 (18 icon + 1px padding z kazdej strony)
 */
public final class HotbarRenderer {

    private static final int SCALE = 3;
    // MC hotbar wymiary
    private static final int TEX_HOTBAR_W = 182, TEX_HOTBAR_H = 22;
    private static final int TEX_SELECTED_W = 24, TEX_SELECTED_H = 24;
    private static final int TEX_SLOT = 20;   // MC slot 20x20

    public static final int SLOT_SIZE = TEX_SLOT * SCALE;         // 60
    public static final int ICON_SIZE = 16 * SCALE;               // 48 (icon 16px w slocie 20px)
    public static final int HOTBAR_W = TEX_HOTBAR_W * SCALE;      // 546
    public static final int HOTBAR_H = TEX_HOTBAR_H * SCALE;      // 66
    public static final int SELECTED_W = TEX_SELECTED_W * SCALE;  // 72

    private static int texWidgets = 0;
    private static boolean loaded = false;

    private HotbarRenderer() {}

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File dir = AssetFinder.findAssetDir("gui", HotbarRenderer.class);
            File f = new File(dir, "widgets.png");
            if (!f.isFile()) {
                System.err.println("[Hotbar] widgets.png not found");
                return;
            }
            BufferedImage img = ImageIO.read(f);
            int w = img.getWidth(), h = img.getHeight();
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                buf.put((byte)((argb >> 16) & 0xFF));
                buf.put((byte)((argb >> 8) & 0xFF));
                buf.put((byte)(argb & 0xFF));
                buf.put((byte)((argb >> 24) & 0xFF));
            }
            buf.flip();
            texWidgets = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texWidgets);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glBindTexture(GL_TEXTURE_2D, 0);
            System.out.println("[Hotbar] loaded widgets.png: " + texWidgets);
        } catch (Exception e) {
            System.err.println("[Hotbar] load failed: " + e);
        }
    }

    public static int[] drawHotbarFrame(int width, int height, int selectedSlot, int hotbarSize) {
        ensureLoaded();

        int start = width / 2 - HOTBAR_W / 2;
        int by = height - HOTBAR_H - 8;

        if (texWidgets == 0) {
            // Fallback do starego prostego stylu jesli PNG nie zaladowany
            glDisable(GL_TEXTURE_2D);
            glColor4f(0.1f, 0.1f, 0.1f, 0.9f);
            UIStyle.quad(start, by, HOTBAR_W, HOTBAR_H);
            glEnable(GL_TEXTURE_2D);
            return new int[]{start, by};
        }

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 1f);
        glBindTexture(GL_TEXTURE_2D, texWidgets);

        // Hotbar bg - MC widgets.png sprite (0,0) 182x22 na 256x256 texture
        float T = 1.0f / 256f;
        float u0 = 0f, v0 = 0f;
        float u1 = TEX_HOTBAR_W * T;
        float v1 = TEX_HOTBAR_H * T;
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex2i(start, by);
        glTexCoord2f(u1, v0); glVertex2i(start + HOTBAR_W, by);
        glTexCoord2f(u1, v1); glVertex2i(start + HOTBAR_W, by + HOTBAR_H);
        glTexCoord2f(u0, v1); glVertex2i(start, by + HOTBAR_H);
        glEnd();

        // Selected slot overlay - MC sprite (0,22) 24x24
        // Pozycja: srodek wybranego slotu, ale 2px overlap (1px w kazda strone)
        // MC formula: sSel = start - 1 + selectedSlot * 20 (bez skalowania)
        int selectedX = start - 1 * SCALE + selectedSlot * TEX_SLOT * SCALE;
        int selectedY = by - 1 * SCALE;
        float su0 = 0f, sv0 = 22f * T;
        float su1 = TEX_SELECTED_W * T;
        float sv1 = (22f + TEX_SELECTED_H) * T;
        glBegin(GL_QUADS);
        glTexCoord2f(su0, sv0); glVertex2i(selectedX, selectedY);
        glTexCoord2f(su1, sv0); glVertex2i(selectedX + SELECTED_W, selectedY);
        glTexCoord2f(su1, sv1); glVertex2i(selectedX + SELECTED_W, selectedY + SELECTED_W);
        glTexCoord2f(su0, sv1); glVertex2i(selectedX, selectedY + SELECTED_W);
        glEnd();

        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);

        return new int[]{start, by};
    }

    public static int slotX(int width, int hotbarSize, int slotIndex) {
        int start = width / 2 - HOTBAR_W / 2;
        // MC: kazdy slot 20 pixel * scale, pierwszy slot ma padding 1px (3px skalowany)
        return start + 1 * SCALE + slotIndex * TEX_SLOT * SCALE;
    }

    public static int slotY(int height) {
        return height - HOTBAR_H - 8 + 1 * SCALE;
    }

    public static int iconOffset() {
        // Icon 16x16 w slocie 18x18 - offset 1 pixel z kazdej strony (3px skalowany)
        return (SLOT_SIZE - ICON_SIZE) / 2;
    }
}
