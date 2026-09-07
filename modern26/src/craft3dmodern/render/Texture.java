package craft3dmodern.render;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;

/**
 * Wczytywanie tekstur vanilla. Decode (czysty Java/ImageIO) dziala tez w
 * testach headless; upload wymaga kontekstu OpenGL.
 */
public final class Texture {
    private Texture() {}

    /** Root assets (domyslnie: katalog assets/ w katalogu roboczym). */
    public static File assetRoot() {
        String custom = System.getProperty("craft3dmodern.assets");
        return new File(custom == null ? "assets" : custom);
    }

    /** Dekoduje plik PNG/JPEG do BufferedImage (bez kontekstu GL). */
    public static BufferedImage decode(String assetPath) throws IOException {
        File file = new File(assetRoot(), assetPath);
        if (!file.isFile()) throw new IOException("asset not found: " + assetPath);
        BufferedImage img = ImageIO.read(file);
        if (img == null) throw new IOException("cannot decode: " + assetPath);
        return img;
    }

    /** Konwertuje ARGB -> RGBA i wgrywa do nowej tekstury GL. */
    public static int upload(BufferedImage image, boolean linear) {
        int w = image.getWidth(), h = image.getHeight();
        ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = image.getRGB(x, y);
                pixels.put((byte) ((argb >>> 16) & 255));
                pixels.put((byte) ((argb >>> 8) & 255));
                pixels.put((byte) (argb & 255));
                pixels.put((byte) ((argb >>> 24) & 255));
            }
        }
        pixels.flip();
        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, linear ? GL_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, linear ? GL_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        return tex;
    }

    /** Wczytaj i wgryj w jednym kroku. */
    public static int load(String assetPath, boolean linear) throws IOException {
        return upload(decode(assetPath), linear);
    }
}
