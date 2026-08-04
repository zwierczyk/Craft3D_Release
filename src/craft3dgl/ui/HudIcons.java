package craft3dgl.ui;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * HUD ikony - LADUJE prawdziwe PNG z assets/icons/ (heart_full, heart_half,
 * heart_empty, hunger_full, hunger_half, hunger_empty).
 * Fallback: pixel-art bitmap jesli plik nie istnieje.
 */
public final class HudIcons {

    /** Rozmiar ikony w px na ekranie (upscale z 9x9 PNG). */
    public static final int SIZE = 27;

    private static int texHeartFull   = -1;
    private static int texHeartHalf   = -1;
    private static int texHeartEmpty  = -1;
    private static int texHungerFull  = -1;
    private static int texHungerHalf  = -1;
    private static int texHungerEmpty = -1;
    private static int texIcons       = -1;  // MC icons.png (XP bar, itd)
    private static boolean initialized = false;
    private static boolean loadedOK = false;

    private HudIcons() {}

    /** Inicjalizuje tekstury z PNG. Wywolywana lazy przy pierwszym drawHeart/drawHunger. */
    private static void ensureLoaded() {
        if (initialized) return;
        initialized = true;
        try {
            File dir = AssetFinder.findAssetDir("icons", HudIcons.class);
            texHeartFull   = loadTexture(new File(dir, "heart_full.png"));
            texHeartHalf   = loadTexture(new File(dir, "heart_half.png"));
            texHeartEmpty  = loadTexture(new File(dir, "heart_empty.png"));
            texHungerFull  = loadTexture(new File(dir, "hunger_full.png"));
            texHungerHalf  = loadTexture(new File(dir, "hunger_half.png"));
            texHungerEmpty = loadTexture(new File(dir, "hunger_empty.png"));
            // MC icons.png z assets/gui/ (XP bar sprites)
            try {
                java.io.File guiDir = craft3dgl.save.AssetFinder.findAssetDir("gui", HudIcons.class);
                java.io.File iconsFile = new java.io.File(guiDir, "icons.png");
                if (iconsFile.isFile()) texIcons = loadTexture(iconsFile);
            } catch (Exception e) {
                System.err.println("[HudIcons] icons.png load: " + e);
            }
            loadedOK = texHeartFull > 0 && texHeartHalf > 0 && texHungerFull > 0;
            System.out.println("[HudIcons] Loaded icons from: " + dir.getAbsolutePath() + " OK=" + loadedOK);
        } catch (Throwable t) {
            System.err.println("[HudIcons] Nie udalo sie zaladowac ikon: " + t.getMessage());
            loadedOK = false;
        }
    }

    private static int loadTexture(File file) {
        if (!file.exists()) {
            System.err.println("[HudIcons] Brak pliku: " + file.getAbsolutePath());
            return -1;
        }
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) return -1;
            int w = img.getWidth();
            int h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = pixels[y * w + x];
                    buf.put((byte)((p >> 16) & 0xff)); // R
                    buf.put((byte)((p >> 8)  & 0xff)); // G
                    buf.put((byte)(p & 0xff));         // B
                    buf.put((byte)((p >> 24) & 0xff)); // A
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
            System.err.println("[HudIcons] Blad ladowania " + file + ": " + t.getMessage());
            return -1;
        }
    }

    /** Rysuje sprite 9x9 pixel z icons.png (256x256) skalowany do SIZE. */
    private static void drawIconSprite(int spriteX, int spriteY, int x, int y) {
        if (texIcons <= 0) return;
        float T = 1.0f / 256f;
        float u0 = spriteX * T, u1 = (spriteX + 9) * T;
        float v0 = spriteY * T, v1 = (spriteY + 9) * T;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 1f);
        glBindTexture(GL_TEXTURE_2D, texIcons);
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex2i(x, y);
        glTexCoord2f(u1, v0); glVertex2i(x + SIZE, y);
        glTexCoord2f(u1, v1); glVertex2i(x + SIZE, y + SIZE);
        glTexCoord2f(u0, v1); glVertex2i(x, y + SIZE);
        glEnd();
        glBindTexture(GL_TEXTURE_2D, 0);
        glDisable(GL_BLEND);
    }

    /** Rysuje tekstury quad na pozycji (x,y) o rozmiarze SIZE. */
    private static void drawTexQuad(int tex, int x, int y) {
        if (tex <= 0) return;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, tex);
        glColor4f(1f, 1f, 1f, 1f);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0); glVertex2i(x, y);
        glTexCoord2f(1, 0); glVertex2i(x + SIZE, y);
        glTexCoord2f(1, 1); glVertex2i(x + SIZE, y + SIZE);
        glTexCoord2f(0, 1); glVertex2i(x, y + SIZE);
        glEnd();
    }

    // ===== PUBLIC API =====

    public static void drawHeart(int x, int y, boolean full, boolean half) {
        ensureLoaded();
        // Preferujemy MC icons.png (sprite atlas) - jesli zaladowany
        if (texIcons > 0) {
            drawIconSprite(16, 0, x, y);          // empty heart bg
            if (full) drawIconSprite(52, 0, x, y);
            else if (half) drawIconSprite(61, 0, x, y);
            return;
        }
        // Fallback do osobnych PNG heart_*.png
        if (loadedOK) {
            if (texHeartEmpty > 0) drawTexQuad(texHeartEmpty, x, y);
            if (full) drawTexQuad(texHeartFull, x, y);
            else if (half) drawTexQuad(texHeartHalf, x, y);
        } else {
            drawHeartFallback(x, y, full, half);
        }
    }

    public static void drawHunger(int x, int y, boolean full, boolean half) {
        ensureLoaded();
        if (texIcons > 0) {
            drawIconSprite(16, 27, x, y);          // empty hunger bg
            if (full) drawIconSprite(52, 27, x, y);
            else if (half) drawIconSprite(61, 27, x, y);
            return;
        }
        if (loadedOK) {
            if (texHungerEmpty > 0) drawTexQuad(texHungerEmpty, x, y);
            if (full) drawTexQuad(texHungerFull, x, y);
            else if (half) drawTexQuad(texHungerHalf, x, y);
        } else {
            drawHungerFallback(x, y, full, half);
        }
    }

    // ===== FALLBACK (pixel-art gdy PNG nie ma) =====

    private static void drawHeartFallback(int x, int y, boolean full, boolean half) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glColor4f(0.15f, 0.05f, 0.05f, 0.9f);
        UIStyle.quad(x + 2, y + 3, 14, 12);
        if (full || half) {
            glColor4f(0.92f, 0.15f, 0.20f, 1f);
            UIStyle.quad(x + 2, y + 3, half ? 7 : 14, 12);
            glColor4f(1f, 0.75f, 0.75f, 1f);
            UIStyle.quad(x + 3, y + 4, 2, 2);
        }
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    private static void drawHungerFallback(int x, int y, boolean full, boolean half) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glColor4f(0.12f, 0.06f, 0.03f, 0.9f);
        UIStyle.quad(x + 2, y + 3, 14, 12);
        if (full || half) {
            glColor4f(0.78f, 0.42f, 0.20f, 1f);
            UIStyle.quad(x + 2, y + 3, half ? 7 : 14, 12);
            glColor4f(0.96f, 0.94f, 0.82f, 1f);
            UIStyle.quad(x + 3, y + 12, 3, 3);
        }
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    // ===== ARMOR / XP (bez zmian) =====

    public static void drawArmor(int x, int y, boolean full, boolean half) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.10f, 0.10f, 0.12f, 0.9f);
        UIStyle.quad(x + 3, y + 2, 12, 14);
        if (full || half) {
            glColor4f(0.72f, 0.75f, 0.85f, 1f);
            UIStyle.quad(x + 4, y + 3, half ? 5 : 10, 12);
            glColor4f(0.90f, 0.92f, 0.98f, 1f);
            UIStyle.quad(x + 5, y + 4, 2, 2);
        }
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    public static void drawXpBar(int x, int y, int w, int h, double fillD) {
        ensureLoaded();
        // MC icons.png: XP bar empty (0,64,182,5), XP bar full (0,69,182,5) - skalowane 3x
        if (texIcons > 0) {
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1f, 1f, 1f, 1f);
            glBindTexture(GL_TEXTURE_2D, texIcons);
            float T = 1.0f / 256f;
            // Bar wysokosc: 5 pixels x scale. Nasze h = 8 - dopasuj do MC (uzyj MC standard 15px = 5*3)
            int barH = 15;   // MC 5*3
            // Empty bar
            float u0 = 0, v0 = 64 * T;
            float u1 = 182 * T, v1 = (64+5) * T;
            glBegin(GL_QUADS);
            glTexCoord2f(u0, v0); glVertex2i(x, y);
            glTexCoord2f(u1, v0); glVertex2i(x + w, y);
            glTexCoord2f(u1, v1); glVertex2i(x + w, y + barH);
            glTexCoord2f(u0, v1); glVertex2i(x, y + barH);
            glEnd();
            // Full bar (green fill) - rysowany do width * fill
            float fill = (float) Math.max(0, Math.min(1, fillD));
            int filled = (int)(w * fill);
            if (filled > 0) {
                float fillU1 = (182 * fill) * T;
                float fv0 = 69 * T, fv1 = (69+5) * T;
                glBegin(GL_QUADS);
                glTexCoord2f(0, fv0);     glVertex2i(x, y);
                glTexCoord2f(fillU1, fv0); glVertex2i(x + filled, y);
                glTexCoord2f(fillU1, fv1); glVertex2i(x + filled, y + barH);
                glTexCoord2f(0, fv1);     glVertex2i(x, y + barH);
                glEnd();
            }
            glBindTexture(GL_TEXTURE_2D, 0);
            glDisable(GL_BLEND);
            glColor4f(1, 1, 1, 1);
            return;
        }
        // Fallback proceduralny (gdy PNG nie zaladowany)
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.10f, 0.10f, 0.14f, 0.95f);
        UIStyle.quad(x, y, w, h);
        float fill = (float) fillD;
        int filled = (int)(w * Math.max(0, Math.min(1, fill)));
        glColor4f(0.30f, 0.90f, 0.30f, 1f);
        UIStyle.quad(x, y, filled, h);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }
}
