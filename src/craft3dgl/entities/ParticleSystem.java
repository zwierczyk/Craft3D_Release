package craft3dgl.entities;

import craft3dgl.save.AssetFinder;
import craft3dgl.ui.TextureAtlas;
import craft3dgl.world.BlockTextures;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * Minecraft 1.12-style particle manager: simulation at 20 Hz, interpolated
 * camera-facing quads, and separate passes for particles.png and block sprites.
 */
public final class ParticleSystem {
    public interface WorldAccess {
        boolean isSolid(double x, double y, double z);
        boolean isWater(double x, double y, double z);
        default float brightness(double x, double y, double z) { return 1.0f; }
    }

    private static final double TICK_SECONDS = 1.0 / 20.0;
    private static final int MAX_PARTICLES_PER_LAYER = 16384;
    private static final float BASE_QUAD_SIZE = 0.1f;

    public final List<Particle> particles = new ArrayList<Particle>();
    private double tickAccumulator;
    private int particleTexture;
    private boolean textureInitialized;

    public void add(Particle particle) {
        if (particle == null) return;
        boolean blockLayer = particle.usesBlockAtlas();
        int layerSize = 0;
        for (Particle existing : particles) {
            if (existing.usesBlockAtlas() == blockLayer) layerSize++;
        }
        if (layerSize >= MAX_PARTICLES_PER_LAYER) {
            Iterator<Particle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().usesBlockAtlas() == blockLayer) {
                    iterator.remove();
                    break;
                }
            }
        }
        particles.add(particle);
    }

    public void clear() {
        particles.clear();
        tickAccumulator = 0.0;
    }

    /** Advances particles on Minecraft's fixed 20 TPS clock. */
    public void update(double elapsedSeconds, WorldAccess world) {
        tickAccumulator += Math.max(0.0, Math.min(0.25, elapsedSeconds));
        while (tickAccumulator + 1.0E-9 >= TICK_SECONDS) {
            tickAccumulator -= TICK_SECONDS;
            if (tickAccumulator < 0.0) tickAccumulator = 0.0;
            Iterator<Particle> iterator = particles.iterator();
            while (iterator.hasNext()) {
                Particle particle = iterator.next();
                particle.tick(world);
                if (particle.expired) iterator.remove();
            }
        }
    }

    /** Draws MCP layer 0 (particles.png) followed by layer 1 (block atlas). */
    public void draw(int blockTextureAtlas, WorldAccess world) {
        if (particles.isEmpty()) return;
        ensureParticleTexture();

        FloatBuffer matrix = BufferUtils.createFloatBuffer(16);
        glGetFloatv(GL_MODELVIEW_MATRIX, matrix);
        float rightX = matrix.get(0);
        float rightY = matrix.get(4);
        float rightZ = matrix.get(8);
        float upX = matrix.get(1);
        float upY = matrix.get(5);
        float upZ = matrix.get(9);
        float partialTicks = (float)(tickAccumulator / TICK_SECONDS);

        boolean blendWasEnabled = glIsEnabled(GL_BLEND);
        boolean cullWasEnabled = glIsEnabled(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glAlphaFunc(GL_GREATER, 0.003921569f);
        glDepthMask(false);
        glDisable(GL_CULL_FACE);

        glBindTexture(GL_TEXTURE_2D, particleTexture);
        drawLayer(false, partialTicks, rightX, rightY, rightZ, upX, upY, upZ, world);

        glBindTexture(GL_TEXTURE_2D, blockTextureAtlas);
        drawLayer(true, partialTicks, rightX, rightY, rightZ, upX, upY, upZ, world);

        glColor4f(1f, 1f, 1f, 1f);
        glDepthMask(true);
        glAlphaFunc(GL_GREATER, 0.08f);
        if (!blendWasEnabled) glDisable(GL_BLEND);
        if (cullWasEnabled) glEnable(GL_CULL_FACE); else glDisable(GL_CULL_FACE);
    }

    private void drawLayer(boolean blockLayer, float partialTicks,
                           float rightX, float rightY, float rightZ,
                           float upX, float upY, float upZ, WorldAccess world) {
        glBegin(GL_QUADS);
        for (Particle particle : particles) {
            if (particle.usesBlockAtlas() != blockLayer) continue;

            float size = BASE_QUAD_SIZE * particle.renderScale(partialTicks);
            double px = particle.prevX + (particle.x - particle.prevX) * partialTicks;
            double py = particle.prevY + (particle.y - particle.prevY) * partialTicks;
            double pz = particle.prevZ + (particle.z - particle.prevZ) * partialTicks;

            double u0;
            double u1;
            double v0;
            double v1;
            if (blockLayer) {
                int tile = BlockTextures.particleTileFor(particle.blockId);
                double tileU0 = TextureAtlas.atlasU0(tile);
                double tileU1 = TextureAtlas.atlasU1(tile);
                double tileV0 = TextureAtlas.atlasV0();
                double tileV1 = TextureAtlas.atlasV1();
                double uSpan = tileU1 - tileU0;
                double vSpan = tileV1 - tileV0;
                u0 = tileU0 + uSpan * particle.textureJitterX / 4.0;
                u1 = tileU0 + uSpan * (particle.textureJitterX + 1.0) / 4.0;
                v0 = tileV0 + vSpan * particle.textureJitterY / 4.0;
                v1 = tileV0 + vSpan * (particle.textureJitterY + 1.0) / 4.0;
            } else {
                u0 = (particle.textureIndex & 15) / 16.0;
                u1 = u0 + 0.0624375;
                v0 = (particle.textureIndex >> 4) / 16.0;
                v1 = v0 + 0.0624375;
            }

            float light = world == null ? 1.0f : world.brightness(px, py, pz);
            glColor4f(particle.red * light, particle.green * light,
                    particle.blue * light, particle.alpha);
            vertex(px, py, pz, -rightX - upX, -rightY - upY, -rightZ - upZ, size, u0, v1);
            vertex(px, py, pz, -rightX + upX, -rightY + upY, -rightZ + upZ, size, u0, v0);
            vertex(px, py, pz,  rightX + upX,  rightY + upY,  rightZ + upZ, size, u1, v0);
            vertex(px, py, pz,  rightX - upX,  rightY - upY,  rightZ - upZ, size, u1, v1);
        }
        glEnd();
    }

    private static void vertex(double x, double y, double z,
                               float offsetX, float offsetY, float offsetZ, float size,
                               double u, double v) {
        glTexCoord2d(u, v);
        glVertex3d(x + offsetX * size, y + offsetY * size, z + offsetZ * size);
    }

    private void ensureParticleTexture() {
        if (textureInitialized) return;
        textureInitialized = true;
        File root = AssetFinder.findAssetDir("particle", ParticleSystem.class);
        File file = new File(root, "particles.png");
        BufferedImage image = null;
        try {
            if (file.isFile()) image = ImageIO.read(file);
        } catch (Exception exception) {
            System.err.println("[Particles] cannot load " + file + ": " + exception);
        }
        if (image != null && image.getWidth() == 128 && image.getHeight() == 128) {
            particleTexture = uploadTexture(image);
        } else {
            if (file.isFile()) {
                System.err.println("[Particles] expected a Minecraft 1.12 128x128 sheet: " + file);
            } else {
                System.err.println("[Particles] missing assets/particle/particles.png; using procedural fallback sprites");
            }
            particleTexture = uploadFallbackTexture();
        }
    }

    private static int uploadTexture(BufferedImage image) {
        ByteBuffer pixels = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                putArgb(pixels, image.getRGB(x, y));
            }
        }
        pixels.flip();
        return uploadPixels(pixels, image.getWidth(), image.getHeight());
    }

    /** Keeps effects visible until the original Minecraft sheet is supplied. */
    private static int uploadFallbackTexture() {
        int[] argb = new int[128 * 128];
        for (int sprite = 20; sprite <= 23; sprite++) {
            int frame = sprite - 20;
            for (int y = 1 + frame; y < 7; y++) {
                int halfWidth = Math.max(1, (7 - y) / 2);
                for (int x = 4 - halfWidth; x <= 3 + halfWidth; x++) {
                    setSpritePixel(argb, sprite, x, y, 0xBFB0D8FF);
                }
            }
        }
        for (int y = 1; y < 7; y++) {
            for (int x = 1; x < 7; x++) {
                int dx = x * 2 - 7;
                int dy = y * 2 - 7;
                int distance = dx * dx + dy * dy;
                if (distance >= 20 && distance <= 38) setSpritePixel(argb, 32, x, y, 0xD0D8F4FF);
            }
        }
        drawStar(argb, 65, 0xFFE5E5E5);
        drawStar(argb, 67, 0xFFFFF4C0);

        ByteBuffer pixels = BufferUtils.createByteBuffer(128 * 128 * 4);
        for (int color : argb) putArgb(pixels, color);
        pixels.flip();
        return uploadPixels(pixels, 128, 128);
    }

    private static int uploadPixels(ByteBuffer pixels, int width, int height) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
        return texture;
    }

    private static void putArgb(ByteBuffer pixels, int argb) {
        pixels.put((byte)((argb >> 16) & 255));
        pixels.put((byte)((argb >> 8) & 255));
        pixels.put((byte)(argb & 255));
        pixels.put((byte)((argb >> 24) & 255));
    }

    private static void drawStar(int[] image, int sprite, int argb) {
        int[][] points = {{3,0},{4,0},{3,1},{4,1},{2,2},{5,2},{0,3},{1,3},{2,3},{3,3},
                {4,3},{5,3},{6,3},{7,3},{3,4},{4,4},{2,5},{5,5},{1,6},{6,6}};
        for (int[] point : points) setSpritePixel(image, sprite, point[0], point[1], argb);
    }

    private static void setSpritePixel(int[] image, int sprite, int x, int y, int argb) {
        image[((sprite >> 4) * 8 + y) * 128 + (sprite & 15) * 8 + x] = argb;
    }

}
