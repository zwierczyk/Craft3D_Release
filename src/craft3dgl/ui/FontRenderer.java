package craft3dgl.ui;

import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer fontu: tworzy texture atlas znakow 16x16 i rysuje tekst kafelek po kafelku.
 * Dodatkowo laduje MC ascii.png (8x8 pixel font) dla drawXpLevel.
 */
public final class FontRenderer {
    public static final int FONT_CELL = 32;
    public static final int MC_CELL = 8;   // MC ascii.png: 128x128 z 16x16 znakow 8x8

    private final int fontTexture;
    private final int mcFontTexture;
    private final int textureAtlasToRestore;

    public FontRenderer(int textureAtlasIdToRestoreAfterDraw) {
        this.textureAtlasToRestore = textureAtlasIdToRestoreAfterDraw;
        this.fontTexture = createFontTexture();
        this.mcFontTexture = loadMcFontTexture();
    }

    public int getFontTexture() {
        return fontTexture;
    }

    private static int glyphFor(char ch) {
        if (ch >= 32 && ch <= 126) return ch;
        return '?';
    }

    private static char charForGlyph(int glyph) {
        if (glyph >= 32 && glyph <= 126) return (char) glyph;
        return 0;
    }

    public static int textWidth(String text, float scale) {
        if (text == null) return 0;
        return (int)(text.length() * FONT_CELL * scale * 0.66f);
    }

    private int createFontTexture() {
        int cols = 16, rows = 16;
        BufferedImage img = new BufferedImage(cols * FONT_CELL, rows * FONT_CELL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setFont(new Font("Dialog", Font.BOLD, 25));
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        FontMetrics fm = g.getFontMetrics();
        for (int c = 0; c < 256; c++) {
            char ch = charForGlyph(c);
            if (ch == 0) continue;
            int x = (c & 15) * FONT_CELL;
            int y = (c >> 4) * FONT_CELL;
            String str = String.valueOf(ch);
            int gx = x + Math.max(1, (FONT_CELL - fm.stringWidth(str)) / 2);
            int gy = y + Math.max(fm.getAscent(), (FONT_CELL - fm.getHeight()) / 2 + fm.getAscent());
            g.drawString(str, gx, gy);
        }
        g.dispose();
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        ByteBuffer buf = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
        for (int yy = 0; yy < img.getHeight(); yy++)
            for (int xx = 0; xx < img.getWidth(); xx++) {
                int argb = img.getRGB(xx, yy);
                buf.put((byte)255).put((byte)255).put((byte)255).put((byte)((argb >>> 24) & 255));
            }
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        return tex;
    }

    /** Laduje MC ascii.png (128x128 pixel font, 16x16 grid 8x8 glyphs). */
    private int loadMcFontTexture() {
        BufferedImage img;
        try {
            img = ImageIO.read(new File("assets/gui/ascii.png"));
            if (img == null) {
                System.err.println("[FontRenderer] ascii.png nie zaladowany, MC font niedostepny");
                return 0;
            }
        } catch (IOException e) {
            System.err.println("[FontRenderer] blad ladowania ascii.png: " + e.getMessage());
            return 0;
        }
        int w = img.getWidth(), h = img.getHeight();
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int yy = 0; yy < h; yy++)
            for (int xx = 0; xx < w; xx++) {
                int argb = img.getRGB(xx, yy);
                int a = (argb >>> 24) & 255;
                int r = (argb >>> 16) & 255;
                int gv = (argb >>> 8) & 255;
                int b = argb & 255;
                buf.put((byte)r).put((byte)gv).put((byte)b).put((byte)a);
            }
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        return tex;
    }

    /** Tekst bialy z ciemnym cieniem + subtelny neon glow (na HUD/panele ciemne). */
    public void drawText(String text, int x, int y, float scale) {
        if (text == null || text.isEmpty()) return;
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Cien - 2px offset
        glColor4f(0f, 0f, 0f, 0.85f);
        drawTextRaw(text, x + 2, y + 2, scale);
        // Delikatny cyjan glow za tekstem
        glColor4f(0.05f, 0.60f, 0.85f, 0.30f);
        drawTextRaw(text, x + 1, y, scale);
        drawTextRaw(text, x - 1, y, scale);
        drawTextRaw(text, x, y + 1, scale);
        drawTextRaw(text, x, y - 1, scale);
        // Glowny tekst
        glColor4f(1f, 1f, 1f, 1f);
        drawTextRaw(text, x, y, scale);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, textureAtlasToRestore);
    }

    /** Tekst cyjan/neon (do title barow, section labeli na ciemnych panelach). */
    public void drawTextDark(String text, int x, int y, float scale) {
        if (text == null || text.isEmpty()) return;
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Czarny cien
        glColor4f(0f, 0f, 0f, 0.90f);
        drawTextRaw(text, x + 2, y + 2, scale);
        // Neon glow (cyjan halo)
        glColor4f(0.10f, 0.85f, 1.00f, 0.40f);
        drawTextRaw(text, x + 1, y, scale);
        drawTextRaw(text, x - 1, y, scale);
        drawTextRaw(text, x, y + 1, scale);
        drawTextRaw(text, x, y - 1, scale);
        // Glowny - jasny cyjan
        glColor4f(0.70f, 0.95f, 1.00f, 1f);
        drawTextRaw(text, x, y, scale);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, textureAtlasToRestore);
    }

    public void drawCenteredText(String text, int centerX, int y, float scale) {
        drawText(text, centerX - textWidth(text, scale) / 2, y, scale);
    }

    public void drawCenteredTextDark(String text, int centerX, int y, float scale) {
        drawTextDark(text, centerX - textWidth(text, scale) / 2, y, scale);
    }

    /**
     * Vanilla container label: Minecraft's ascii font with no neon glow or custom
     * drop shadow. GuiInventory/GuiCrafting/GuiChest use colour 4210752 (#404040).
     */
    public void drawVanillaContainerText(String text, int x, int y, int pixelScale) {
        if (text == null || text.isEmpty()) return;
        if (mcFontTexture == 0) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, fontTexture);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(0.25f, 0.25f, 0.25f, 1f);
            drawTextRaw(text, x, y, pixelScale * 0.25f);
        } else {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, mcFontTexture);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(0.25f, 0.25f, 0.25f, 1f);
            drawMcTextRaw(text, x, y, pixelScale);
        }
        glDisable(GL_BLEND);
        glColor4f(1f, 1f, 1f, 1f);
        glBindTexture(GL_TEXTURE_2D, textureAtlasToRestore);
    }

    /** GuiNewChat/GuiTextField white pixel font with vanilla one-pixel shadow. */
    public void drawVanillaText(String text, int x, int y, int pixelScale, float alpha) {
        if (text == null || text.isEmpty()) return;
        alpha = Math.max(0f, Math.min(1f, alpha));
        if (mcFontTexture == 0) {
            drawText(text, x, y, pixelScale * 0.25f);
            return;
        }
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, mcFontTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0f, 0f, 0f, 0.65f * alpha);
        drawMcTextRaw(text, x + pixelScale, y + pixelScale, pixelScale);
        glColor4f(1f, 1f, 1f, alpha);
        drawMcTextRaw(text, x, y, pixelScale);
        glDisable(GL_BLEND);
        glColor4f(1f, 1f, 1f, 1f);
        glBindTexture(GL_TEXTURE_2D, textureAtlasToRestore);
    }

    /** Vanilla ItemRenderer stack count: white pixel font with one-pixel shadow. */
    public void drawVanillaStackCount(String text, int x, int y, int pixelScale) {
        if (text == null || text.isEmpty()) return;
        if (mcFontTexture == 0) {
            drawText(text, x, y, pixelScale * 0.25f);
            return;
        }
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, mcFontTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(0.25f, 0.25f, 0.25f, 1f);
        drawMcTextRaw(text, x + pixelScale, y + pixelScale, pixelScale);
        glColor4f(1f, 1f, 1f, 1f);
        drawMcTextRaw(text, x, y, pixelScale);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, textureAtlasToRestore);
    }

    /** Szerokosc tekstu w MC pixel font. MC glyph = 5px szerokosc + 1px spacing = 6px * pixelScale. */
    public static int mcTextWidth(String text, int pixelScale) {
        if (text == null) return 0;
        return text.length() * 6 * pixelScale;
    }

    /** MC 1.14 xp level: pixel font 8x8, skalowany, 4x czarny obrys + zielony #80FF20.
     *  pixelScale = 3 daje MC-native GUI scale 3 look (glyph 24px wysokosci). */
    public void drawXpLevel(String text, int centerX, int y, int pixelScale) {
        if (text == null || text.isEmpty()) return;
        if (mcFontTexture == 0) {
            // fallback do starego renderera
            drawXpLevelFallback(text, centerX, y, pixelScale * 0.3f);
            return;
        }
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, mcFontTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        int tw = mcTextWidth(text, pixelScale);
        int x = centerX - tw / 2;
        int off = pixelScale;  // MC: 1 pixel offset w skali GUI
        // 4x czarny obrys (MC Notch trick)
        glColor4f(0f, 0f, 0f, 1f);
        drawMcTextRaw(text, x - off, y, pixelScale);
        drawMcTextRaw(text, x + off, y, pixelScale);
        drawMcTextRaw(text, x, y - off, pixelScale);
        drawMcTextRaw(text, x, y + off, pixelScale);
        // Zielony MC XP color #80FF20 = decimal 8453920
        glColor4f(0x80/255f, 0xFF/255f, 0x20/255f, 1f);
        drawMcTextRaw(text, x, y, pixelScale);
        glDisable(GL_BLEND);
        glColor4f(1f, 1f, 1f, 1f);
        glBindTexture(GL_TEXTURE_2D, textureAtlasToRestore);
    }

    /** Fallback: uzywa starego generowanego fontu jesli MC ascii.png sie nie zaladowal. */
    private void drawXpLevelFallback(String text, int centerX, int y, float scale) {
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, fontTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        int tw = textWidth(text, scale);
        int x = centerX - tw / 2;
        int off = Math.max(2, Math.round(scale * 3f));
        glColor4f(0f, 0f, 0f, 1f);
        drawTextRaw(text, x - off, y, scale);
        drawTextRaw(text, x + off, y, scale);
        drawTextRaw(text, x, y - off, scale);
        drawTextRaw(text, x, y + off, scale);
        glColor4f(0x80/255f, 0xFF/255f, 0x20/255f, 1f);
        drawTextRaw(text, x, y, scale);
        glDisable(GL_BLEND);
        glColor4f(1f, 1f, 1f, 1f);
    }

    /** Rysuje tekst uzywajac MC ascii.png (128x128, 16x16 grid, 8x8 glyphs).
     *  pixelScale = ile pixeli ekranu na 1 pixel fontu. */
    private void drawMcTextRaw(String text, int x, int y, int pixelScale) {
        float glyphPx = MC_CELL * pixelScale;   // rozmiar wyrenderowanego glifu (8*scale)
        float advance = 6 * pixelScale;         // MC standard: 5px glyph + 1px spacing
        glBegin(GL_QUADS);
        for (int i = 0; i < text.length(); i++) {
            int c = glyphFor(text.charAt(i));
            int tx = c & 15;
            int ty = c >> 4;
            float u0 = tx / 16f;
            float v0 = ty / 16f;
            float u1 = (tx + 1) / 16f;
            float v1 = (ty + 1) / 16f;
            float px = x + i * advance;
            float py = y;
            glTexCoord2f(u0, v0); glVertex2f(px, py);
            glTexCoord2f(u1, v0); glVertex2f(px + glyphPx, py);
            glTexCoord2f(u1, v1); glVertex2f(px + glyphPx, py + glyphPx);
            glTexCoord2f(u0, v1); glVertex2f(px, py + glyphPx);
        }
        glEnd();
    }

    private void drawTextRaw(String text, int x, int y, float scale) {
        float size = FONT_CELL * scale;
        glBegin(GL_QUADS);
        for (int i = 0; i < text.length(); i++) {
            int c = glyphFor(text.charAt(i));
            int tx = c & 15;
            int ty = c >> 4;
            float u0 = tx / 16f;
            float v0 = ty / 16f;
            float u1 = (tx + 1) / 16f;
            float v1 = (ty + 1) / 16f;
            float px = x + i * size * 0.66f;
            float py = y;
            glTexCoord2f(u0, v0); glVertex2f(px, py);
            glTexCoord2f(u1, v0); glVertex2f(px + size, py);
            glTexCoord2f(u1, v1); glVertex2f(px + size, py + size);
            glTexCoord2f(u0, v1); glVertex2f(px, py + size);
        }
        glEnd();
    }
}
