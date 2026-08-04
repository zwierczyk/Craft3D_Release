package craft3dgl.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import craft3dgl.save.AssetFinder;

/**
 * Ladowanie tekstur GUI (creative, inventory, itd) z assets/gui/.
 * Rysowanie jako 9-slice (rogi zachowuja proporcje, srodek streczowany).
 *
 * MC creative UI PNG maja 256x256 ale uzywana jest cezesc 176x166 od (0,0).
 * Rozciagamy na dowolny rozmiar naszego panelu - lekko rozmazane ale wciaz MC-look.
 */
public final class GuiTextures {
    private GuiTextures() {}

    private static final Map<String, int[]> LOADED = new HashMap<>();  // name -> [texId, width, height]

    public static void load() {
        try {
            File dir = AssetFinder.findAssetDir("gui", GuiTextures.class);
            if (dir == null) return;
            loadOne(dir, "tab_items.png");
            loadOne(dir, "tab_inventory.png");
            loadOne(dir, "tab_item_search.png");
            System.out.println("[GuiTextures] loaded " + LOADED.size() + " textures");
        } catch (Throwable t) {
            System.err.println("[GuiTextures] load failed: " + t);
        }
    }

    private static void loadOne(File dir, String filename) {
        File f = new File(dir, filename);
        if (!f.isFile()) return;
        try {
            BufferedImage img = ImageIO.read(f);
            // Konwertuj do RGBA (bo P i LA nie mapuja sie direct)
            BufferedImage rgba = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = rgba.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            int w = rgba.getWidth(), h = rgba.getHeight();
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = rgba.getRGB(x, y);
                    buf.put((byte)((argb >> 16) & 0xFF));
                    buf.put((byte)((argb >> 8) & 0xFF));
                    buf.put((byte)(argb & 0xFF));
                    buf.put((byte)((argb >> 24) & 0xFF));
                }
            }
            buf.flip();
            int tex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            String name = filename.replace(".png", "");
            LOADED.put(name, new int[]{tex, w, h});
        } catch (Exception e) {
            System.err.println("[GuiTextures] " + filename + ": " + e);
        }
    }

    /** Zwraca [texId, width, height] albo null. */
    public static int[] get(String name) { return LOADED.get(name); }

    /**
     * Rysuje 9-slice panel - rogi pozostaja w oryginalnej wielkosci pixela (MC style ostre),
     * srodek streczowany.
     *
     * @param name  nazwa tekstury (bez .png)
     * @param x,y   pozycja na ekranie
     * @param w,h   docelowa wielkosc (dowolna)
     * @param texW,texH  wielkosc uzywanej czesci tekstury (MC: 176x166)
     * @param border    grubosc borderu w pixelach tekstury (MC: 4-7)
     */
    public static void drawNineSlice(String name, int x, int y, int w, int h,
                                      int texW, int texH, int border) {
        int[] info = LOADED.get(name);
        if (info == null) return;
        int tex = info[0];
        int imgW = info[1], imgH = info[2];

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);

        // UV znormalizowane
        float u0 = 0f;
        float u1 = (float) border / imgW;
        float u2 = (float) (texW - border) / imgW;
        float u3 = (float) texW / imgW;
        float v0 = 0f;
        float v1 = (float) border / imgH;
        float v2 = (float) (texH - border) / imgH;
        float v3 = (float) texH / imgH;

        int b = border * 3;                  // scale border 3x na ekranie zeby byl widoczny
        int x0 = x, x1 = x + b, x2 = x + w - b, x3 = x + w;
        int y0 = y, y1 = y + b, y2 = y + h - b, y3 = y + h;

        GL11.glBegin(GL11.GL_QUADS);
        // 9 czesci: 4 rogi (static), 4 boki (stretched 1 osi), 1 srodek (stretched 2 osie)
        drawPart(u0,v0, u1,v1, x0,y0, x1,y1);   // top-left
        drawPart(u1,v0, u2,v1, x1,y0, x2,y1);   // top
        drawPart(u2,v0, u3,v1, x2,y0, x3,y1);   // top-right
        drawPart(u0,v1, u1,v2, x0,y1, x1,y2);   // left
        drawPart(u1,v1, u2,v2, x1,y1, x2,y2);   // center
        drawPart(u2,v1, u3,v2, x2,y1, x3,y2);   // right
        drawPart(u0,v2, u1,v3, x0,y2, x1,y3);   // bottom-left
        drawPart(u1,v2, u2,v3, x1,y2, x2,y3);   // bottom
        drawPart(u2,v2, u3,v3, x2,y2, x3,y3);   // bottom-right
        GL11.glEnd();

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static void drawPart(float u0, float v0, float u1, float v1,
                                  int x0, int y0, int x1, int y1) {
        GL11.glTexCoord2f(u0, v0); GL11.glVertex2i(x0, y0);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex2i(x1, y0);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex2i(x1, y1);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex2i(x0, y1);
    }

    /** Prosta wersja - rozciaga cala teksture na quad (bez 9-slice). */
    public static void drawStretched(String name, int x, int y, int w, int h) {
        int[] info = LOADED.get(name);
        if (info == null) return;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, info[0]);
        // MC creative UI uzywa tylko czesci 176x166 od (0,0)
        float u1 = 176f / info[1];
        float v1 = 166f / info[2];
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);  GL11.glVertex2i(x, y);
        GL11.glTexCoord2f(u1, 0); GL11.glVertex2i(x + w, y);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex2i(x + w, y + h);
        GL11.glTexCoord2f(0, v1); GL11.glVertex2i(x, y + h);
        GL11.glEnd();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
