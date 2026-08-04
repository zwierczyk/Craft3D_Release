package craft3dgl.blaze3d.shadow;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import craft3dgl.save.AssetFinder;

/**
 * MC-style animated water texture.
 *   water_still.png = 16x512 (32 klatki pionowo po 16x16)
 *   Klatka zmieniana co ~120ms (jak w MC).
 *   Kolor tekstury jest szary/bialy - tint niebieski nakladany przez shader/RenderSystem.
 */
public class WaterTexture {
    public static final int FRAMES = 32;
    public static final int TILE = 16;
    private int textureId;
    private ByteBuffer[] frames;
    private long lastFrameTime = 0;
    private int currentFrame = 0;
    private static final int FRAME_DURATION_MS = 120;

    public WaterTexture() {
        loadFrames();
        // Utworz textureId + upload klatki 0
        textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
            TILE, TILE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frames[0]);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        System.out.println("[WaterTexture] loaded 32 frames, texId=" + textureId);
    }

    private void loadFrames() {
        frames = new ByteBuffer[FRAMES];
        try {
            File dir = AssetFinder.findAssetDir("water", WaterTexture.class);
            File f = new File(dir, "water_still.png");
            BufferedImage img = ImageIO.read(f);
            // 32 klatki pionowo po 16x16
            for (int frame = 0; frame < FRAMES; frame++) {
                ByteBuffer buf = BufferUtils.createByteBuffer(TILE * TILE * 4);
                for (int y = 0; y < TILE; y++) {
                    for (int x = 0; x < TILE; x++) {
                        int argb = img.getRGB(x, frame * TILE + y);
                        buf.put((byte)((argb >> 16) & 0xFF));
                        buf.put((byte)((argb >> 8) & 0xFF));
                        buf.put((byte)(argb & 0xFF));
                        buf.put((byte)((argb >> 24) & 0xFF));
                    }
                }
                buf.flip();
                frames[frame] = buf;
            }
        } catch (Exception e) {
            System.err.println("[WaterTexture] load failed: " + e);
            // Fallback - biala tekstura
            for (int i = 0; i < FRAMES; i++) {
                ByteBuffer buf = BufferUtils.createByteBuffer(TILE * TILE * 4);
                for (int j = 0; j < TILE * TILE; j++) {
                    buf.put((byte)0xC0).put((byte)0xE0).put((byte)0xFF).put((byte)0xFF);
                }
                buf.flip();
                frames[i] = buf;
            }
        }
    }

    /** Update tekstury - wywolaj co klatke, uploaduje kolejna klatke animacji. */
    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastFrameTime > FRAME_DURATION_MS) {
            lastFrameTime = now;
            currentFrame = (currentFrame + 1) % FRAMES;
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, TILE, TILE,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frames[currentFrame]);
        }
    }

    public int getTextureId() { return textureId; }

    public void cleanup() {
        GL11.glDeleteTextures(textureId);
    }
}
