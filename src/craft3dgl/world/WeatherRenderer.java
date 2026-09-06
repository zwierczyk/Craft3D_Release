package craft3dgl.world;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static craft3dgl.world.WorldConstants.*;

/** Fixed-function port of EntityRenderer.renderRainSnow from MCP 9.40. */
public final class WeatherRenderer {
    private static int rainTexture;
    private static int snowTexture;

    private WeatherRenderer() {}

    public static void draw(byte[][][] world, long seed,
                            double playerX, double playerY, double playerZ,
                            double elapsedSeconds, float rainStrength) {
        if (world == null || rainStrength <= 0.001f) return;
        ensureTextures();
        if (rainTexture == 0 && snowTexture == 0) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);

        int px = floor(playerX);
        int py = floor(playerY);
        int pz = floor(playerZ);
        renderPass(world, seed, px, py, pz, elapsedSeconds, rainStrength, false);
        renderPass(world, seed, px, py, pz, elapsedSeconds, rainStrength, true);

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopAttrib();
    }

    private static void renderPass(byte[][][] world, long seed, int px, int py, int pz,
                                   double elapsedSeconds, float strength, boolean snow) {
        int texture = snow ? snowTexture : rainTexture;
        if (texture == 0) return;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        int radius = 10; // fancy rain distance in EntityRenderer
        double tick = elapsedSeconds * 20.0;
        GL11.glBegin(GL11.GL_QUADS);
        for (int z = pz - radius; z <= pz + radius; z++) {
            if (z < 0 || z >= world[0][0].length) continue;
            for (int x = px - radius; x <= px + radius; x++) {
                if (x < 0 || x >= world.length) continue;
                int biome = BiomeGenerator.biomeAt(x, z, seed);
                if (biome == BIOME_DESERT) continue;
                boolean columnSnow = biome == BIOME_MOUNTAINS;
                if (columnSnow != snow) continue;

                int precipitationY = precipitationHeight(world, x, z);
                int bottom = Math.max(py - radius, precipitationY);
                int top = py + radius;
                if (top < precipitationY) top = precipitationY;
                top = Math.min(top, world[x].length);
                bottom = Math.max(0, Math.min(bottom, top));
                if (bottom == top) continue;

                double rx = x + 0.5 - (px + 0.5);
                double rz = z + 0.5 - (pz + 0.5);
                double distance = Math.sqrt(rx * rx + rz * rz);
                double sideX;
                double sideZ;
                if (distance < 0.001) {
                    sideX = 0.5;
                    sideZ = 0.0;
                } else {
                    sideX = -rz / distance * 0.5;
                    sideZ = rx / distance * 0.5;
                }
                float radial = (float)(distance / radius);
                float alpha = ((1f - radial * radial) * (snow ? 0.3f : 0.5f) + 0.5f) * strength;
                if (alpha <= 0.01f) continue;

                long hash = x * (long)x * 3121L + x * 45238971L
                        ^ z * (long)z * 418711L + z * 13761L;
                double scroll;
                double uJitter = 0.0;
                if (snow) {
                    scroll = -(tick % 512.0) / 512.0 + hashNoise(hash) * 0.01;
                    uJitter = hashNoise(hash ^ 0x6a09e667L);
                } else {
                    scroll = -(((hash & 31L) + tick) / 32.0) * (3.0 + hashNoise(hash ^ seed));
                }

                GL11.glColor4f(1f, 1f, 1f, alpha);
                double u0 = uJitter;
                double u1 = 1.0 + uJitter;
                double vBottom = bottom * 0.25 + scroll;
                double vTop = top * 0.25 + scroll;
                GL11.glTexCoord2d(u0, vBottom); GL11.glVertex3d(x - sideX + 0.5, bottom, z - sideZ + 0.5);
                GL11.glTexCoord2d(u1, vBottom); GL11.glVertex3d(x + sideX + 0.5, bottom, z + sideZ + 0.5);
                GL11.glTexCoord2d(u1, vTop);    GL11.glVertex3d(x + sideX + 0.5, top,    z + sideZ + 0.5);
                GL11.glTexCoord2d(u0, vTop);    GL11.glVertex3d(x - sideX + 0.5, top,    z - sideZ + 0.5);
            }
        }
        GL11.glEnd();
    }

    private static int precipitationHeight(byte[][][] world, int x, int z) {
        for (int y = world[x].length - 1; y >= 0; y--) {
            int block = world[x][y][z] & 255;
            if (block != AIR && block != WATER && block != TALL_GRASS
                    && block != WHEAT_0 && block != WHEAT_1
                    && block != WHEAT_2 && block != WHEAT_3) return y + 1;
        }
        return 0;
    }

    private static int floor(double value) {
        int i = (int)value;
        return value < i ? i - 1 : i;
    }

    private static double hashNoise(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        value ^= value >>> 33;
        return (value & 0xffffffL) / (double)0x1000000L;
    }

    private static void ensureTextures() {
        if (rainTexture == 0) rainTexture = load("rain.png");
        if (snowTexture == 0) snowTexture = load("snow.png");
    }

    private static int load(String name) {
        try {
            File dir = AssetFinder.findAssetDir("environment", WeatherRenderer.class);
            if (dir == null) return 0;
            BufferedImage image = ImageIO.read(new File(dir, name));
            if (image == null) return 0;
            ByteBuffer pixels = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    pixels.put((byte)(argb >>> 16)).put((byte)(argb >>> 8));
                    pixels.put((byte)argb).put((byte)(argb >>> 24));
                }
            }
            pixels.flip();
            int result = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, result);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, image.getWidth(), image.getHeight(),
                    0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            return result;
        } catch (Throwable throwable) {
            System.err.println("[WeatherRenderer] Cannot load " + name + ": " + throwable.getMessage());
            return 0;
        }
    }
}
