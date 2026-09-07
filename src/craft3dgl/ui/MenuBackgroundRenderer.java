package craft3dgl.ui;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/** Minecraft 1.12 title-screen cubemap panorama (the logo is intentionally omitted). */
public final class MenuBackgroundRenderer {
    private static final int[] PANORAMA = {-1, -1, -1, -1, -1, -1};
    private static long animationStart;

    private MenuBackgroundRenderer() {}

    public static void draw(int width, int height) {
        ensureLoaded();
        if (PANORAMA[0] > 0) drawPanorama(width, height);
        else {
            glDisable(GL_TEXTURE_2D);
            glColor3f(0.15f, 0.19f, 0.24f);
            UIStyle.quad(0, 0, width, height);
        }

        // GuiMainMenu applies a dark gradient over its blurred panorama before controls.
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glBegin(GL_QUADS);
        glColor4f(0f, 0f, 0f, 0.20f); glVertex2i(0, 0); glVertex2i(width, 0);
        glColor4f(0f, 0f, 0f, 0.48f); glVertex2i(width, height); glVertex2i(0, height);
        glEnd();
        glDisable(GL_BLEND);
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void drawPanorama(int width, int height) {
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        double near = 0.05;
        double top = near * Math.tan(Math.toRadians(60.0)); // 120 degree GuiMainMenu panorama FOV
        double right = top * width / (double)Math.max(1, height);
        glFrustum(-right, right, -top, top, near, 10.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glRotated(180.0, 1.0, 0.0, 0.0);
        glRotated(90.0, 0.0, 0.0, 1.0);
        // GuiMainMenu.drawPanorama z MCP 9.40: panoramaTimer rosnaca o 1 co tick
        // (20/s), pochylenie = sin(timer/400)*25+20 stopni, obrot Y = -timer*0.1.
        double timer = (System.currentTimeMillis() - animationStart) / 50.0;
        glRotated(20.0 + Math.sin(timer / 400.0) * 25.0, 1.0, 0.0, 0.0);
        glRotated(-timer * 0.1, 0.0, 1.0, 0.0);

        for (int side = 0; side < 6; side++) {
            if (PANORAMA[side] <= 0) continue;
            glPushMatrix();
            if (side == 1) glRotated(90.0, 0.0, 1.0, 0.0);
            if (side == 2) glRotated(180.0, 0.0, 1.0, 0.0);
            if (side == 3) glRotated(-90.0, 0.0, 1.0, 0.0);
            if (side == 4) glRotated(90.0, 1.0, 0.0, 0.0);
            if (side == 5) glRotated(-90.0, 1.0, 0.0, 0.0);
            glBindTexture(GL_TEXTURE_2D, PANORAMA[side]);
            glColor4f(1f, 1f, 1f, 1f);
            glBegin(GL_QUADS);
            glTexCoord2f(0f, 0f); glVertex3f(-1f, -1f, 1f);
            glTexCoord2f(1f, 0f); glVertex3f( 1f, -1f, 1f);
            glTexCoord2f(1f, 1f); glVertex3f( 1f,  1f, 1f);
            glTexCoord2f(0f, 1f); glVertex3f(-1f,  1f, 1f);
            glEnd();
            glPopMatrix();
        }

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glDepthMask(true);
    }

    private static void ensureLoaded() {
        if (PANORAMA[0] != -1) return;
        animationStart = System.currentTimeMillis();
        for (int i = 0; i < PANORAMA.length; i++) PANORAMA[i] = 0;
        try {
            File gui = AssetFinder.findAssetDir("gui", MenuBackgroundRenderer.class);
            if (gui == null) return;
            File directory = new File(new File(gui, "title"), "background");
            for (int i = 0; i < PANORAMA.length; i++) {
                BufferedImage image = ImageIO.read(new File(directory, "panorama_" + i + ".png"));
                if (image != null) PANORAMA[i] = upload(image);
            }
        } catch (Throwable t) {
            System.err.println("[MainMenu] panorama load failed: " + t.getMessage());
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
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        return texture;
    }
}
