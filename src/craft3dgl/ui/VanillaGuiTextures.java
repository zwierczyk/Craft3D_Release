package craft3dgl.ui;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Minecraft 1.12 container texture loader and blitter.
 *
 * GuiContainer draws source pixels without stretching and lets ScaledResolution
 * scale the complete GUI. Craft3D renders directly in framebuffer coordinates,
 * therefore every source pixel is expanded by the requested integer GUI scale.
 */
public final class VanillaGuiTextures {
    private static final Map<String, Texture> TEXTURES = new HashMap<>();

    private VanillaGuiTextures() {}

    public static void draw(String name, int x, int y, int sourceW, int sourceH, int scale) {
        drawRegion(name, x, y, 0, 0, sourceW, sourceH, scale);
    }

    public static void drawRegion(String name, int x, int y,
                                  int sourceX, int sourceY, int sourceW, int sourceH,
                                  int scale) {
        Texture texture = texture(name);
        if (texture == null) return;

        // Gui rendering in 1.12 is fixed-function. Never inherit the chunk shader,
        // lightmap texture unit, tint, or filtering state from a world render pass.
        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.id);

        float u0 = (float) sourceX / texture.width;
        float v0 = (float) sourceY / texture.height;
        float u1 = (float) (sourceX + sourceW) / texture.width;
        float v1 = (float) (sourceY + sourceH) / texture.height;
        int w = sourceW * scale;
        int h = sourceH * scale;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(u0, v0); GL11.glVertex2i(x, y);
        GL11.glTexCoord2f(u1, v0); GL11.glVertex2i(x + w, y);
        GL11.glTexCoord2f(u1, v1); GL11.glVertex2i(x + w, y + h);
        GL11.glTexCoord2f(u0, v1); GL11.glVertex2i(x, y + h);
        GL11.glEnd();
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private static Texture texture(String name) {
        if (TEXTURES.containsKey(name)) return TEXTURES.get(name);
        Texture loaded = load(name);
        TEXTURES.put(name, loaded);
        return loaded;
    }

    private static Texture load(String name) {
        try {
            File gui = AssetFinder.findAssetDir("gui", VanillaGuiTextures.class);
            if (gui == null) return null;
            File file = new File(new File(gui, "container"), name + ".png");
            if (!file.isFile()) {
                file = new File(new File(gui, "sprites"), name + ".png");
            }
            if (!file.isFile()) {
                file = new File(new File(new File(gui, "sprites"), "container"), name + ".png");
            }
            if (!file.isFile()) {
                file = new File(new File(new File(new File(gui, "sprites"), "container"), "creative_inventory"), name + ".png");
            }
            BufferedImage image = ImageIO.read(file);
            if (image == null) return null;
            int w = image.getWidth();
            int h = image.getHeight();
            ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
            for (int py = 0; py < h; py++) {
                for (int px = 0; px < w; px++) {
                    int argb = image.getRGB(px, py);
                    pixels.put((byte) ((argb >>> 16) & 255));
                    pixels.put((byte) ((argb >>> 8) & 255));
                    pixels.put((byte) (argb & 255));
                    pixels.put((byte) ((argb >>> 24) & 255));
                }
            }
            pixels.flip();
            int id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            System.out.println("[VanillaGui] loaded " + file.getPath());
            return new Texture(id, w, h);
        } catch (Throwable t) {
            System.err.println("[VanillaGui] failed to load " + name + ": " + t.getMessage());
            return null;
        }
    }

    private static final class Texture {
        final int id;
        final int width;
        final int height;

        Texture(int id, int width, int height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }
    }
}
