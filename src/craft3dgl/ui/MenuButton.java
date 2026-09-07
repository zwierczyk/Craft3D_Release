package craft3dgl.ui;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/** Minecraft 1.12 GuiButton renderer using textures/gui/widgets.png. */
public final class MenuButton {
    private static int widgets = -1;
    private static int textureWidth = 256;
    private static int textureHeight = 256;

    private MenuButton() {}

    private static void ensureLoaded() {
        if (widgets != -1) return;
        widgets = 0;
        try {
            File gui = AssetFinder.findAssetDir("gui", MenuButton.class);
            if (gui == null) return;
            BufferedImage image = ImageIO.read(new File(gui, "widgets.png"));
            if (image == null) return;
            textureWidth = image.getWidth();
            textureHeight = image.getHeight();
            ByteBuffer pixels = BufferUtils.createByteBuffer(textureWidth * textureHeight * 4);
            for (int y = 0; y < textureHeight; y++) for (int x = 0; x < textureWidth; x++) {
                int argb = image.getRGB(x, y);
                pixels.put((byte)((argb >>> 16) & 255));
                pixels.put((byte)((argb >>> 8) & 255));
                pixels.put((byte)(argb & 255));
                pixels.put((byte)((argb >>> 24) & 255));
            }
            pixels.flip();
            widgets = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, widgets);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        } catch (Throwable t) {
            System.err.println("[MenuButton] widgets.png load failed: " + t.getMessage());
        }
    }

    public static void drawButton(FontRenderer font, int x, int y, int w, int h, String text) {
        drawButtonState(font, x, y, w, h, text, 0);
    }

    /** state: 0 normal, 1 hover, 2 pressed/hover, 3 disabled. */
    public static void drawButtonState(FontRenderer font, int x, int y, int w, int h,
                                       String text, int state) {
        ensureLoaded();
        int pixelScale = Math.max(1, Math.round(h / 20f));
        if (widgets > 0) {
            glEnable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glColor4f(1f, 1f, 1f, 1f);
            glBindTexture(GL_TEXTURE_2D, widgets);
            int vanillaState = state == 3 ? 0 : state == 0 ? 1 : 2;
            int sourceY = 46 + vanillaState * 20;
            int left = w / 2;
            int logicalHalf = Math.min(100, Math.max(1,
                    Math.round(left / (float)pixelScale)));
            drawRegion(x, y, left, h, 0, sourceY, logicalHalf, 20);
            drawRegion(x + left, y, w - left, h,
                    200 - logicalHalf, sourceY, logicalHalf, 20);
        } else {
            glDisable(GL_TEXTURE_2D);
            glColor4f(state == 3 ? 0.25f : 0.45f, state == 3 ? 0.25f : 0.45f,
                    state == 3 ? 0.25f : 0.45f, 1f);
            UIStyle.quad(x, y, w, h);
        }
        int textColor = state == 3 ? 0xa0a0a0 : state == 0 ? 0xe0e0e0 : 0xffffa0;
        int textX = x + (w - FontRenderer.mcTextWidth(text, pixelScale)) / 2;
        int textY = y + (h - 8 * pixelScale) / 2;
        font.drawVanillaTextColored(text, textX, textY, pixelScale, textColor, 1f, true);
    }

    private static void drawRegion(int x, int y, int width, int height,
                                   int sourceX, int sourceY, int sourceW, int sourceH) {
        float u0 = (float)sourceX / textureWidth;
        float v0 = (float)sourceY / textureHeight;
        float u1 = (float)(sourceX + sourceW) / textureWidth;
        float v1 = (float)(sourceY + sourceH) / textureHeight;
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex2i(x, y);
        glTexCoord2f(u1, v0); glVertex2i(x + width, y);
        glTexCoord2f(u1, v1); glVertex2i(x + width, y + height);
        glTexCoord2f(u0, v1); glVertex2i(x, y + height);
        glEnd();
    }

    /**
     * Dowolny wycinek widgets.png (np. polowki galki suwaka 4x20) narysowany w
     * rozmiarze destW x destH. Uzywane przez suwaki GuiScreenOptionsSounds.
     */
    public static void drawWidgetSource(int x, int y, int destW, int destH,
                                        int srcX, int srcY, int srcW, int srcH) {
        ensureLoaded();
        if (widgets <= 0) return;
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1f, 1f, 1f, 1f);
        glBindTexture(GL_TEXTURE_2D, widgets);
        drawRegion(x, y, destW, destH, srcX, srcY, srcW, srcH);
    }

    public static int drawButtonAuto(FontRenderer font, int centerX, int y, String text,
                                     float textScale, int state) {
        int width = Math.max(120, FontRenderer.textWidth(text, textScale) + 48);
        int height = Math.max(32, Math.round(32f * textScale + 14f));
        drawButtonState(font, centerX - width / 2, y, width, height, text, state);
        return width;
    }

    public static int drawButtonAutoHeight(FontRenderer font, int x, int y, int width,
                                           String text, float textScale, int state) {
        int height = Math.max(32, Math.round(32f * textScale + 14f));
        drawButtonState(font, x, y, width, height, text, state);
        return height;
    }

    public static void drawDeathButton(FontRenderer font, int x, int y, int w, int h, String text) {
        drawButtonState(font, x, y, w, h, text, 0);
    }
}
