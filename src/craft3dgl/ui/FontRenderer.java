package craft3dgl.ui;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;

/**
 * Minecraft 1.12 FontRenderer subset.
 *
 * Text is rendered from the original ascii.png with the glyph widths calculated
 * by FontRenderer#readFontTexture. Unicode pages 00 and 01 plus
 * glyph_sizes.bin provide the accented Polish/European characters.
 */
public final class FontRenderer {
    public static final int FONT_CELL = 32; // retained for old layout calculations
    public static final int MC_CELL = 8;

    private static final float[] ASCII_WIDTHS = new float[256];
    private static final byte[] GLYPH_WIDTHS = new byte[65536];
    private static boolean metricsReady;

    static {
        Arrays.fill(ASCII_WIDTHS, 6f);
        ASCII_WIDTHS[' '] = 4f;
    }

    private final int textureAtlasToRestore;
    private int asciiTexture;
    private final int[] unicodeTextures = new int[2];

    public FontRenderer(int textureAtlasIdToRestoreAfterDraw) {
        textureAtlasToRestore = textureAtlasIdToRestoreAfterDraw;
        loadVanillaFont();
    }

    public int getFontTexture() {
        return asciiTexture;
    }

    private void loadVanillaFont() {
        try {
            File gui = AssetFinder.findAssetDir("gui", FontRenderer.class);
            if (gui == null) return;
            BufferedImage ascii = ImageIO.read(new File(gui, "ascii.png"));
            if (ascii != null) {
                calculateAsciiWidths(ascii);
                asciiTexture = upload(ascii);
            }
            File font = new File(gui, "font");
            loadGlyphSizes(new File(font, "glyph_sizes.bin"));
            unicodeTextures[0] = loadTexture(new File(font, "unicode_page_00.png"));
            unicodeTextures[1] = loadTexture(new File(font, "unicode_page_01.png"));
            metricsReady = true;
        } catch (Throwable t) {
            System.err.println("[FontRenderer] vanilla font load failed: " + t.getMessage());
        }
    }

    /** Port of MCP 9.40 FontRenderer#readFontTexture for a 128x128 atlas. */
    private static void calculateAsciiWidths(BufferedImage image) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int charWidth = imageWidth / 16;
        int charHeight = imageHeight / 16;
        float scale = imageWidth / 128f;
        for (int character = 0; character < 256; character++) {
            int cellX = character % 16;
            int cellY = character / 16;
            int right;
            for (right = charWidth - 1; right >= 0; right--) {
                boolean empty = true;
                int pixelX = cellX * charWidth + right;
                for (int row = 0; row < charHeight && empty; row++) {
                    int alpha = image.getRGB(pixelX, cellY * charHeight + row) >>> 24;
                    if (alpha > 16) empty = false;
                }
                if (!empty) break;
            }
            if (character == ' ') right = charWidth <= 8 ? (int)(2f * scale) : (int)(1.5f * scale);
            ASCII_WIDTHS[character] = (right + 1) / scale + 1f;
        }
    }

    private static void loadGlyphSizes(File file) {
        if (!file.isFile()) return;
        try (FileInputStream input = new FileInputStream(file)) {
            int offset = 0;
            while (offset < GLYPH_WIDTHS.length) {
                int read = input.read(GLYPH_WIDTHS, offset, GLYPH_WIDTHS.length - offset);
                if (read < 0) break;
                offset += read;
            }
        } catch (Exception e) {
            System.err.println("[FontRenderer] glyph_sizes.bin: " + e.getMessage());
        }
    }

    private static int loadTexture(File file) {
        try {
            BufferedImage image = ImageIO.read(file);
            return image == null ? 0 : upload(image);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int upload(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int argb = image.getRGB(x, y);
            pixels.put((byte)((argb >>> 16) & 255));
            pixels.put((byte)((argb >>> 8) & 255));
            pixels.put((byte)(argb & 255));
            pixels.put((byte)((argb >>> 24) & 255));
        }
        pixels.flip();
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        return texture;
    }

    private static boolean isAscii(char character) {
        return character >= 32 && character <= 126;
    }

    private static float vanillaCharWidth(char character) {
        if (character == '\u00a7') return 0f;
        if (isAscii(character)) return ASCII_WIDTHS[character];
        int packed = GLYPH_WIDTHS[character] & 255;
        if (packed != 0) {
            int start = packed >>> 4;
            int end = packed & 15;
            return (end + 1 - start) / 2f + 1f;
        }
        return ASCII_WIDTHS['?'];
    }

    private static float vanillaWidth(String text) {
        if (text == null) return 0f;
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) { i++; continue; }
            width += vanillaCharWidth(c);
        }
        return width;
    }

    /** Old callers use a 32-pixel generated-font scale; 4 maps that to 8-pixel vanilla glyphs. */
    public static int textWidth(String text, float scale) {
        return Math.round(vanillaWidth(text) * scale * 4f);
    }

    public static int mcTextWidth(String text, int pixelScale) {
        return Math.round(vanillaWidth(text) * pixelScale);
    }

    public void drawText(String text, int x, int y, float scale) {
        drawVanilla(text, x, y, scale * 4f, 1f, 1f, 1f, 1f, true);
    }

    public void drawTextDark(String text, int x, int y, float scale) {
        drawVanilla(text, x, y, scale * 4f, 1f, 1f, 1f, 1f, true);
    }

    public void drawCenteredText(String text, int centerX, int y, float scale) {
        drawText(text, centerX - textWidth(text, scale) / 2, y, scale);
    }

    public void drawCenteredTextDark(String text, int centerX, int y, float scale) {
        drawTextDark(text, centerX - textWidth(text, scale) / 2, y, scale);
    }

    public void drawVanillaContainerText(String text, int x, int y, int pixelScale) {
        drawVanilla(text, x, y, pixelScale, 0.25f, 0.25f, 0.25f, 1f, false);
    }

    public void drawVanillaText(String text, int x, int y, int pixelScale, float alpha) {
        drawVanilla(text, x, y, pixelScale, 1f, 1f, 1f, alpha, true);
    }

    public void drawVanillaTextColored(String text, int x, int y, int pixelScale,
                                       int rgb, float alpha, boolean shadow) {
        drawTextColorScale(text, x, y, pixelScale, rgb, alpha, shadow);
    }

    public void drawTextColored(String text, int x, int y, float legacyScale,
                                int rgb, float alpha, boolean shadow) {
        drawTextColorScale(text, x, y, legacyScale * 4f, rgb, alpha, shadow);
    }

    private void drawTextColorScale(String text, int x, int y, float scale,
                                    int rgb, float alpha, boolean shadow) {
        float r = ((rgb >>> 16) & 255) / 255f;
        float g = ((rgb >>> 8) & 255) / 255f;
        float b = (rgb & 255) / 255f;
        drawVanilla(text, x, y, scale, r, g, b, alpha, shadow);
    }

    public void drawVanillaStackCount(String text, int x, int y, int pixelScale) {
        drawVanilla(text, x, y, pixelScale, 1f, 1f, 1f, 1f, true);
    }

    public void drawXpLevel(String text, int centerX, int y, int pixelScale) {
        int x = centerX - mcTextWidth(text, pixelScale) / 2;
        int offset = pixelScale;
        drawVanilla(text, x - offset, y, pixelScale, 0f, 0f, 0f, 1f, false);
        drawVanilla(text, x + offset, y, pixelScale, 0f, 0f, 0f, 1f, false);
        drawVanilla(text, x, y - offset, pixelScale, 0f, 0f, 0f, 1f, false);
        drawVanilla(text, x, y + offset, pixelScale, 0f, 0f, 0f, 1f, false);
        drawVanilla(text, x, y, pixelScale, 0x80 / 255f, 1f, 0x20 / 255f, 1f, false);
    }

    private void drawVanilla(String text, float x, float y, float scale,
                             float r, float g, float b, float alpha, boolean shadow) {
        if (text == null || text.isEmpty() || asciiTexture <= 0) return;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if (shadow) {
            glColor4f(r * 0.25f, g * 0.25f, b * 0.25f, alpha);
            drawRaw(text, x + scale, y + scale, scale);
        }
        glColor4f(r, g, b, alpha);
        drawRaw(text, x, y, scale);
        glColor4f(1f, 1f, 1f, 1f);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, textureAtlasToRestore);
    }

    private void drawRaw(String text, float x, float y, float scale) {
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\u00a7' && i + 1 < text.length()) { i++; continue; }
            if (character == ' ') {
                cursor += vanillaCharWidth(character) * scale;
                continue;
            }
            if (isAscii(character)) {
                glBindTexture(GL_TEXTURE_2D, asciiTexture);
                drawAsciiGlyph(character, cursor, y, scale);
            } else {
                int page = character >>> 8;
                int packed = GLYPH_WIDTHS[character] & 255;
                if (page < unicodeTextures.length && unicodeTextures[page] > 0 && packed != 0) {
                    glBindTexture(GL_TEXTURE_2D, unicodeTextures[page]);
                    drawUnicodeGlyph(character, packed, cursor, y, scale);
                } else {
                    glBindTexture(GL_TEXTURE_2D, asciiTexture);
                    drawAsciiGlyph('?', cursor, y, scale);
                }
            }
            cursor += vanillaCharWidth(character) * scale;
        }
    }

    /** MCP renderDefaultChar: 7.99 high and a seven-pixel-wide sampled quad. */
    private static void drawAsciiGlyph(char character, float x, float y, float scale) {
        int cellX = character & 15;
        int cellY = character >>> 4;
        float u0 = cellX * 8f / 128f;
        float v0 = cellY * 8f / 128f;
        float u1 = (cellX * 8f + 7f) / 128f;
        float v1 = (cellY * 8f + 7.99f) / 128f;
        float width = 7f * scale;
        float height = 7.99f * scale;
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex2f(x, y);
        glTexCoord2f(u1, v0); glVertex2f(x + width, y);
        glTexCoord2f(u1, v1); glVertex2f(x + width, y + height);
        glTexCoord2f(u0, v1); glVertex2f(x, y + height);
        glEnd();
    }

    /** MCP renderUnicodeChar: a 16-pixel source cell displayed at half width. */
    private static void drawUnicodeGlyph(char character, int packed, float x, float y, float scale) {
        int start = packed >>> 4;
        int end = packed & 15;
        float sourceX = (character & 15) * 16f + start;
        float sourceY = ((character & 255) >>> 4) * 16f;
        float sourceWidth = end + 1f - start - 0.02f;
        float u0 = sourceX / 256f, v0 = sourceY / 256f;
        float u1 = (sourceX + sourceWidth) / 256f, v1 = (sourceY + 15.98f) / 256f;
        float width = sourceWidth * 0.5f * scale;
        float height = 7.99f * scale;
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex2f(x, y);
        glTexCoord2f(u1, v0); glVertex2f(x + width, y);
        glTexCoord2f(u1, v1); glVertex2f(x + width, y + height);
        glTexCoord2f(u0, v1); glVertex2f(x, y + height);
        glEnd();
    }
}
