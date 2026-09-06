package craft3dgl.ui;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/** GuiIngame crosshair and crosshair-mode attack indicator from Minecraft 1.12. */
public final class Crosshair {
    private static int iconsTexture = -1;
    private static int textureWidth = 256;
    private static int textureHeight = 256;

    private Crosshair() {}

    /** Kept for old call sites; vanilla 1.12 does not draw a custom hit ring. */
    public static void triggerHit() {}

    public static void draw(int width, int height, double miningProgress,
                            double currentTime, float attackStrength) {
        ensureTexture();
        if (iconsTexture <= 0) return;
        int cx = width / 2;
        int cy = height / 2;

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glEnable(GL_ALPHA_TEST);
        glColor4f(1f, 1f, 1f, 1f);
        glBindTexture(GL_TEXTURE_2D, iconsTexture);

        // GuiIngame#renderAttackIndicator uses the inverted destination blend
        // so the 16x16 icon remains visible on every world background.
        GL14.glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);
        blit(cx - 7, cy - 7, 0, 0, 16, 16);

        float strength = Math.max(0f, Math.min(1f, attackStrength));
        if (strength < 1f) {
            int indicatorX = cx - 8;
            int indicatorY = cy + 9;
            int fill = (int)(strength * 17f);
            blit(indicatorX, indicatorY, 36, 94, 16, 4);
            if (fill > 0) blit(indicatorX, indicatorY, 52, 94, fill, 4);
        }
        glPopAttrib();
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void blit(int x, int y, int u, int v, int width, int height) {
        float u0 = (float)u / textureWidth;
        float v0 = (float)v / textureHeight;
        float u1 = (float)(u + width) / textureWidth;
        float v1 = (float)(v + height) / textureHeight;
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex2i(x, y);
        glTexCoord2f(u1, v0); glVertex2i(x + width, y);
        glTexCoord2f(u1, v1); glVertex2i(x + width, y + height);
        glTexCoord2f(u0, v1); glVertex2i(x, y + height);
        glEnd();
    }

    private static void ensureTexture() {
        if (iconsTexture != -1) return;
        iconsTexture = 0;
        try {
            File gui = AssetFinder.findAssetDir("gui", Crosshair.class);
            if (gui == null) return;
            BufferedImage image = ImageIO.read(new File(gui, "icons.png"));
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
            iconsTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, iconsTexture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, textureWidth, textureHeight, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        } catch (Throwable t) {
            System.err.println("[Crosshair] icons.png load failed: " + t.getMessage());
            iconsTexture = 0;
        }
    }

    public static void draw(int width, int height, double miningProgress, double currentTime) {
        draw(width, height, miningProgress, currentTime, 1.0f);
    }

    public static void draw(int width, int height) {
        draw(width, height, 0.0, System.nanoTime() * 1e-9, 1.0f);
    }
}
