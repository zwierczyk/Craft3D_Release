package craft3dgl.ui;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Przycisk stylizowany na Minecraft - laduje texture button.png i renderuje
 * z 9-slice scaling (rogi/krawedzie stale, srodek rozciagany).
 *
 * Domyslna tekstura: assets/icons/button.png (200x20 grayscale MC-style).
 * Wersje: normal / hover (jasniejsza) / pressed (ciemniejsza).
 *
 * autoFit: dopasowuje szerokosc do tekstu + padding.
 */
public final class MenuButton {
    private MenuButton() {}

    private static int texNormal   = -1;
    private static int texHover    = -1;
    private static int texPressed  = -1;
    private static int texW = 200, texH = 20;
    private static boolean initialized = false;
    private static boolean loadedOK = false;

    /** Szerokosc slice'u brzegu (pixele w oryginalnej teksturze). */
    private static final int SLICE = 3;
    /** Padding wokol tekstu przy autoFit. */
    private static final int PAD_X = 24;
    private static final int MIN_H = 32;

    private static void ensureLoaded() {
        if (initialized) return;
        initialized = true;
        try {
            File dir = AssetFinder.findAssetDir("icons", MenuButton.class);
            texNormal  = loadTexture(new File(dir, "button.png"));
            texHover   = loadTexture(new File(dir, "button_hover.png"));
            texPressed = loadTexture(new File(dir, "button_pressed.png"));
            loadedOK = texNormal > 0;
            if (loadedOK) {
                // odczytaj rozmiar z tekstury normalnej
                try {
                    BufferedImage img = ImageIO.read(new File(dir, "button.png"));
                    if (img != null) { texW = img.getWidth(); texH = img.getHeight(); }
                } catch (Throwable ignored) {}
            }
            System.out.println("[MenuButton] Loaded button textures OK=" + loadedOK + " size=" + texW + "x" + texH);
        } catch (Throwable t) {
            System.err.println("[MenuButton] Blad ladowania: " + t.getMessage());
            loadedOK = false;
        }
    }

    private static int loadTexture(File file) {
        if (!file.exists()) { System.err.println("[MenuButton] Brak: " + file); return -1; }
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
                    // Jesli tekstura L (grayscale), ImageIO ustawia RGB tak samo, alpha=255 - OK
                    if (a == 0) a = 255; // grayscale bez alpha - wymuszamy pelna nieprzezroczystosc
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
            System.err.println("[MenuButton] Error: " + t.getMessage());
            return -1;
        }
    }

    /** Rysuje 9-slice quad z tekstury na pozycji (x,y) o rozmiarze (w,h). */
    private static void draw9Slice(int tex, int x, int y, int w, int h) {
        if (tex <= 0) return;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBindTexture(GL_TEXTURE_2D, tex);
        glColor4f(1f, 1f, 1f, 1f);

        int sl = SLICE;
        // U/V coords slice'ow
        float u0 = 0f, u1 = (float) sl / texW, u2 = (float)(texW - sl) / texW, u3 = 1f;
        float v0 = 0f, v1 = (float) sl / texH, v2 = (float)(texH - sl) / texH, v3 = 1f;

        // Pozycje ekranowe
        int x0 = x, x1 = x + sl, x2 = x + w - sl, x3 = x + w;
        int y0 = y, y1 = y + sl, y2 = y + h - sl, y3 = y + h;

        glBegin(GL_QUADS);
        // 9 quadow: TL, T, TR, L, C, R, BL, B, BR
        quadUV(x0,y0,x1,y1, u0,v0,u1,v1);
        quadUV(x1,y0,x2,y1, u1,v0,u2,v1);
        quadUV(x2,y0,x3,y1, u2,v0,u3,v1);
        quadUV(x0,y1,x1,y2, u0,v1,u1,v2);
        quadUV(x1,y1,x2,y2, u1,v1,u2,v2);
        quadUV(x2,y1,x3,y2, u2,v1,u3,v2);
        quadUV(x0,y2,x1,y3, u0,v2,u1,v3);
        quadUV(x1,y2,x2,y3, u1,v2,u2,v3);
        quadUV(x2,y2,x3,y3, u2,v2,u3,v3);
        glEnd();
    }

    private static void quadUV(int x0, int y0, int x1, int y1, float u0, float v0, float u1, float v1) {
        glTexCoord2f(u0, v0); glVertex2i(x0, y0);
        glTexCoord2f(u1, v0); glVertex2i(x1, y0);
        glTexCoord2f(u1, v1); glVertex2i(x1, y1);
        glTexCoord2f(u0, v1); glVertex2i(x0, y1);
    }

    // ===== PUBLIC API =====

    /** Rysuje przycisk MC-style o zadanym rozmiarze (bez sprawdzania hover). */
    public static void drawButton(FontRenderer font, int x, int y, int w, int h, String text) {
        drawButtonState(font, x, y, w, h, text, 0);
    }

    /**
     * Rysuje z uwzglednieniem stanu:
     * state=0 normal, 1=hover, 2=pressed.
     */
    public static void drawButtonState(FontRenderer font, int x, int y, int w, int h, String text, int state) {
        ensureLoaded();
        if (loadedOK) {
            int tex = state == 1 ? (texHover > 0 ? texHover : texNormal)
                    : state == 2 ? (texPressed > 0 ? texPressed : texNormal)
                    : texNormal;
            draw9Slice(tex, x, y, w, h);
            // Nieznaczne przesuniecie tekstu przy pressed dla efektu "wcisniecia"
            int offY = (state == 2) ? 1 : 0;
            drawButtonText(font, x, y + offY, w, h, text);
        } else {
            drawButtonFallback(font, x, y, w, h, text);
        }
    }

    /**
     * Autofit: liczy szerokosc z tekstu + padding. Rysuje wysrodkowanego.
     * Zwraca faktyczna szerokosc uzyta.
     */
    public static int drawButtonAuto(FontRenderer font, int centerX, int y, String text, float textScale, int state) {
        int tw = FontRenderer.textWidth(text, textScale);
        int w = Math.max(120, tw + PAD_X * 2);
        int h = Math.max(MIN_H, (int)(FontRenderer.FONT_CELL * textScale) + 14);
        int x = centerX - w / 2;
        ensureLoaded();
        if (loadedOK) {
            int tex = state == 1 ? (texHover > 0 ? texHover : texNormal)
                    : state == 2 ? (texPressed > 0 ? texPressed : texNormal)
                    : texNormal;
            draw9Slice(tex, x, y, w, h);
            int offY = (state == 2) ? 1 : 0;
            font.drawCenteredText(text, x + w / 2, y + h / 2 - (int)(FontRenderer.FONT_CELL * textScale * 0.5) + offY, textScale);
        } else {
            drawButtonFallback(font, x, y, w, h, text);
        }
        return w;
    }

    /**
     * Rysuje na podanym prostokacie z dopasowaniem wysokosci do tekstu (h auto),
     * szerokosc podana. Uzywane w listach.
     */
    public static int drawButtonAutoHeight(FontRenderer font, int x, int y, int w, String text, float textScale, int state) {
        int h = Math.max(MIN_H, (int)(FontRenderer.FONT_CELL * textScale) + 14);
        drawButtonState(font, x, y, w, h, text, state);
        return h;
    }

    private static void drawButtonText(FontRenderer font, int x, int y, int w, int h, String text) {
        // Wysrodkuj tekst - MC domyslnie skala 0.62 dla wysokosci ~36px
        float scale = 0.62f;
        // Skalujemy do wysokosci przycisku
        int desired = Math.max(14, h - 12);
        if (FontRenderer.FONT_CELL * scale > desired) {
            scale = (float) desired / FontRenderer.FONT_CELL;
        }
        font.drawCenteredText(text, x + w / 2, y + h / 2 - (int)(FontRenderer.FONT_CELL * scale * 0.5), scale);
    }

    private static void drawButtonFallback(FontRenderer font, int x, int y, int w, int h, String text) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.35f, 0.35f, 0.35f, 1f);
        UIStyle.quad(x, y, w, h);
        glColor4f(0.6f, 0.6f, 0.6f, 1f);
        UIStyle.quad(x, y, w, 2);
        UIStyle.quad(x, y, 2, h);
        glColor4f(0.15f, 0.15f, 0.15f, 1f);
        UIStyle.quad(x, y + h - 2, w, 2);
        UIStyle.quad(x + w - 2, y, 2, h);
        glEnable(GL_TEXTURE_2D);
        drawButtonText(font, x, y, w, h, text);
    }

    /** Death screen button - ciemny czerwony (bez zmian). */
    public static void drawDeathButton(FontRenderer font, int x, int y, int w, int h, String text) {
        // Uzyj czerwonego tint na texturze
        ensureLoaded();
        if (loadedOK) {
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1.2f, 0.5f, 0.5f, 1f);  // czerwony tint
            draw9Slice(texNormal, x, y, w, h);
            glColor4f(1, 1, 1, 1);
            drawButtonText(font, x, y, w, h, text);
        } else {
            glDisable(GL_TEXTURE_2D);
            glColor4f(0.5f, 0.15f, 0.15f, 1f);
            UIStyle.quad(x, y, w, h);
            glEnable(GL_TEXTURE_2D);
            drawButtonText(font, x, y, w, h, text);
        }
    }
}
