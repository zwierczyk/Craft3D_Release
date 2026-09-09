package craft3dgl.world;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/** Minecraft 1.12 fast-cloud renderer using environment/clouds.png. */
public final class CloudRenderer {
    private static int cloudTexture;

    private CloudRenderer() {}

    public static void drawClouds(double playerX, double playerZ) {
        drawClouds(playerX, playerZ, 0.5);
    }

    public static void drawClouds(double playerX, double playerZ, double dayFraction) {
        drawClouds(playerX, playerZ, dayFraction, 0f, 0f);
    }

    public static void drawClouds(double playerX, double playerZ, double dayFraction,
                                  float rainStrength, float thunderStrength) {
        ensureTexture();
        if (cloudTexture == 0) return;

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, cloudTexture);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDepthMask(false);

        float daylight = LightEngine.skyColorMultiplier(dayFraction);
        float rain = Math.max(0f, Math.min(1f, rainStrength));
        float thunder = Math.max(0f, Math.min(1f, thunderStrength));
        float cloud = daylight * 0.9f + 0.1f;
        float grey = cloud * (1f - rain * 0.55f) * (1f - thunder * 0.65f);
        // Vanilla: o wschodzie/zachodzie chmury nabieraja cieplego pomaranczowego
        // tonu od slonca przy horyzoncie (cos kata celestialnego w poblizu 0).
        float cosSun = (float) Math.cos(LightEngine.celestialAngle(dayFraction) * Math.PI * 2.0);
        float warm = 1f - Math.abs(cosSun) / 0.4f;
        warm = Math.max(0f, Math.min(1f, warm)) * 0.75f;
        float r = grey * (1f - warm) + warm * 1f;
        float g = grey * (1f - warm) + warm * 0.55f;
        float b = grey * (1f - warm) + warm * 0.2f;
        glColor4f(r, g, b, 0.8f);

        // RenderGlobal fast clouds: 32-block cells, texture scale 1/2048 and
        // movement 0.03 block per game tick (0.6 block/s).
        double movement = glfwGetTime() * 0.6;
        int originX = (int)Math.floor(playerX / 32.0) * 32;
        int originZ = (int)Math.floor(playerZ / 32.0) * 32;
        double cloudY = 58.33; // scaled to Craft3D's 64-block-tall overworld
        double uvScale = 1.0 / 2048.0;

        glBegin(GL_QUADS);
        for (int ox = -256; ox < 256; ox += 32) {
            for (int oz = -256; oz < 256; oz += 32) {
                double x0 = originX + ox;
                double x1 = x0 + 32;
                double z0 = originZ + oz;
                double z1 = z0 + 32;
                double u0 = (x0 + movement) * uvScale;
                double u1 = (x1 + movement) * uvScale;
                double v0 = z0 * uvScale;
                double v1 = z1 * uvScale;
                glTexCoord2d(u0, v1); glVertex3d(x0, cloudY, z1);
                glTexCoord2d(u1, v1); glVertex3d(x1, cloudY, z1);
                glTexCoord2d(u1, v0); glVertex3d(x1, cloudY, z0);
                glTexCoord2d(u0, v0); glVertex3d(x0, cloudY, z0);
            }
        }
        glEnd();

        glPopAttrib();
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void ensureTexture() {
        if (cloudTexture != 0) return;
        try {
            File dir = AssetFinder.findAssetDir("environment", CloudRenderer.class);
            BufferedImage image = ImageIO.read(new File(dir, "clouds.png"));
            if (image == null) return;
            ByteBuffer pixels = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    pixels.put((byte)(argb >> 16)).put((byte)(argb >> 8));
                    pixels.put((byte)argb).put((byte)(argb >> 24));
                }
            }
            pixels.flip();
            cloudTexture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, cloudTexture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(),
                    0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        } catch (Throwable throwable) {
            System.err.println("[CloudRenderer] Cannot load clouds.png: " + throwable.getMessage());
        }
    }
}
