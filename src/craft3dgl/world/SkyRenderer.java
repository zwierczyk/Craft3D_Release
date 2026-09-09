package craft3dgl.world;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;

/** Fixed-function port of Minecraft 1.12 RenderGlobal.renderSky. */
public final class SkyRenderer {
    private static int sunTexture;
    // Fazy ksiezyca 26.2: osobne tekstury (kolejnosc jak w arkuszu 1.12).
    private static final int[] MOON_PHASE_TEXTURES = new int[8];
    private static final String[] MOON_PHASE_FILES = {
        "moon/full_moon.png", "moon/waning_gibbous.png", "moon/third_quarter.png",
        "moon/waning_crescent.png", "moon/new_moon.png", "moon/waxing_crescent.png",
        "moon/first_quarter.png", "moon/waxing_gibbous.png"
    };
    private static int starList;

    private SkyRenderer() {}

    /** Sky RGB followed by fog RGB for the current plains weather. */
    public static float[] getSkyColors(double dayFraction) {
        return getSkyColors(dayFraction, 0f, 0f);
    }

    public static float[] getSkyColors(double dayFraction, float rainStrength, float thunderStrength) {
        float daylight = LightEngine.skyColorMultiplier(dayFraction);

        // Biome.getSkyColorByTemp(0.8): HSV(0.6088889, 0.5266667, 1)
        float skyR = (121.0f / 255.0f) * daylight;
        float skyG = (167.0f / 255.0f) * daylight;
        float skyB = daylight;

        // World.getSkyColor weather desaturation from MCP 9.40.
        float rain = clamp01(rainStrength);
        if (rain > 0f) {
            float grey = (skyR * 0.3f + skyG * 0.59f + skyB * 0.11f) * 0.6f;
            float keep = 1f - rain * 0.75f;
            skyR = skyR * keep + grey * (1f - keep);
            skyG = skyG * keep + grey * (1f - keep);
            skyB = skyB * keep + grey * (1f - keep);
        }
        float thunder = clamp01(thunderStrength);
        if (thunder > 0f) {
            float grey = (skyR * 0.3f + skyG * 0.59f + skyB * 0.11f) * 0.2f;
            float keep = 1f - thunder * 0.75f;
            skyR = skyR * keep + grey * (1f - keep);
            skyG = skyG * keep + grey * (1f - keep);
            skyB = skyB * keep + grey * (1f - keep);
        }

        // WorldProvider.getFogColor, render-distance blend and EntityRenderer weather tint.
        float fogR = 0.7529412f * (daylight * 0.94f + 0.06f);
        float fogG = 0.84705883f * (daylight * 0.94f + 0.06f);
        float fogB = 1.0f * (daylight * 0.91f + 0.09f);
        float distanceBlend = 1.0f - (float)Math.pow(0.25f + 0.75f * 5.0f / 32.0f, 0.25);
        fogR += (skyR - fogR) * distanceBlend;
        fogG += (skyG - fogG) * distanceBlend;
        fogB += (skyB - fogB) * distanceBlend;
        fogR *= 1f - rain * 0.5f;
        fogG *= 1f - rain * 0.5f;
        fogB *= 1f - rain * 0.4f;
        fogR *= 1f - thunder * 0.5f;
        fogG *= 1f - thunder * 0.5f;
        fogB *= 1f - thunder * 0.5f;
        return new float[]{skyR, skyG, skyB, fogR, fogG, fogB};
    }

    /** Compatibility overload used by older callers/tests. */
    public static void drawSky(int screenWidth, int screenHeight, double pitch) {
        drawSky(0.0, pitch, 0.5);
    }

    public static void drawSky(int screenWidth, int screenHeight, double pitch, double dayFraction) {
        drawSky(0.0, pitch, dayFraction);
    }

    /**
     * Draws the vanilla sky planes, sunrise/sunset fan, textured sun/moon and
     * deterministic 1.12 stars. Projection must already be configured.
     */
    public static void drawSky(double yaw, double pitch, double dayFraction) {
        drawSky(yaw, pitch, dayFraction, 0f, 0f);
    }

    public static void drawSky(double yaw, double pitch, double dayFraction,
                               float rainStrength, float thunderStrength) {
        ensureTextures();
        ensureStars();
        float[] colors = getSkyColors(dayFraction, rainStrength, thunderStrength);

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        // Same view orientation as MinecraftGL.lookAt, but without translation.
        glRotated(-Math.toDegrees(pitch), 1.0, 0.0, 0.0);
        glRotated(180.0 - Math.toDegrees(yaw), 0.0, 1.0, 0.0);

        glUseProgramZero();
        glDepthMask(false);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_FOG);
        // Kopula nieba z gradientem: kolor mgly na horyzoncie -> czysty kolor
        // nieba w zenicie (jak wspolczesny vanilla sky dome).
        drawSkyDome(colors[0], colors[1], colors[2], colors[3], colors[4], colors[5]);

        float weatherVisibility = 1f - clamp01(rainStrength);
        float[] sunrise = sunriseSunset(dayFraction);
        if (sunrise != null) {
            sunrise[3] *= weatherVisibility;
            drawSunriseFan(sunrise, dayFraction);
        }

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        glColor4f(1f, 1f, 1f, weatherVisibility);
        glPushMatrix();
        glRotated(-90.0, 0.0, 1.0, 0.0);
        glRotated(LightEngine.celestialAngle(dayFraction) * 360.0f, 1.0, 0.0, 0.0);
        if (sunTexture != 0) {
            glBindTexture(GL_TEXTURE_2D, sunTexture);
            texturedCelestialQuad(30.0, 100.0, false, 0.0, 0.0, 1.0, 1.0);
        }
        if (MOON_PHASE_TEXTURES[0] != 0) {
            int phase = ((int)Math.floor(dayFraction) % 8 + 8) % 8;
            glBindTexture(GL_TEXTURE_2D, MOON_PHASE_TEXTURES[phase]);
            texturedCelestialQuad(20.0, -100.0, true, 0.0, 0.0, 1.0, 1.0);
        }

        glDisable(GL_TEXTURE_2D);
        float star = LightEngine.starBrightness(dayFraction) * (1f - clamp01(rainStrength));
        if (star > 0.0f && starList != 0) {
            glColor4f(star, star, star, star);
            glCallList(starList);
        }
        glPopMatrix();

        glPopMatrix();
        glPopAttrib();
        glMatrixMode(GL_MODELVIEW);
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void glUseProgramZero() {
        try { org.lwjgl.opengl.GL20.glUseProgram(0); } catch (Throwable ignored) {}
    }

    private static void drawSkyPlane(double y, boolean reverse) {
        glBegin(GL_QUADS);
        for (int x = -384; x <= 384; x += 64) {
            for (int z = -384; z <= 384; z += 64) {
                double x0 = reverse ? x + 64 : x;
                double x1 = reverse ? x : x + 64;
                glVertex3d(x0, y, z);
                glVertex3d(x1, y, z);
                glVertex3d(x1, y, z + 64);
                glVertex3d(x0, y, z + 64);
            }
        }
        glEnd();
    }

    /**
     * Wspolczesny vanilla rysuje niebo jako kopule, w ktorej kolor plynie od
     * koloru mgly na horyzoncie do pelnego koloru nieba w zenicie (w nocy
     * niemal czarnej). Promien 140 miesci sie w far=180, wiec nic nie jest
     * przycinane; GL_SMOOTH daje gladki gradient miedzy wierzcholkami.
     */
    private static void drawSkyDome(float skyR, float skyG, float skyB,
                                    float fogR, float fogG, float fogB) {
        glShadeModel(GL_SMOOTH);
        double radius = 140.0;
        int latBands = 16;
        int azSteps = 28;
        double pi = Math.PI;
        for (int i = 0; i < latBands; i++) {
            double theta0 = -pi / 2.0 + pi * i / latBands;
            double theta1 = theta0 + pi / latBands;
            glBegin(GL_QUAD_STRIP);
            for (int j = 0; j <= azSteps; j++) {
                double az = 2.0 * pi * j / azSteps;
                skyVertex(theta1, az, radius, skyR, skyG, skyB, fogR, fogG, fogB);
                skyVertex(theta0, az, radius, skyR, skyG, skyB, fogR, fogG, fogB);
            }
            glEnd();
        }
        glShadeModel(GL_FLAT);
    }

    private static void skyVertex(double theta, double az, double radius,
                                  float skyR, float skyG, float skyB,
                                  float fogR, float fogG, float fogB) {
        double cosT = Math.cos(theta);
        double x = Math.cos(az) * cosT * radius;
        double y = Math.sin(theta) * radius;
        double z = Math.sin(az) * cosT * radius;
        // t=0 na horyzoncie (kolor mgly), t=1 w zenicie (kolor nieba);
        // pelny kolor nieba po ~35 stopniach nad horyzontem jak vanilla.
        float t = (float) (theta / 0.62);
        t = Math.max(0f, Math.min(1f, t));
        float r = fogR + (skyR - fogR) * t;
        float g = fogG + (skyG - fogG) * t;
        float b = fogB + (skyB - fogB) * t;
        glColor3f(r, g, b);
        glVertex3d(x, y, z);
    }

    private static float[] sunriseSunset(double dayFraction) {
        float angle = LightEngine.celestialAngle(dayFraction);
        float cosine = (float)Math.cos(angle * Math.PI * 2.0);
        if (cosine < -0.4f || cosine > 0.4f) return null;
        float phase = cosine / 0.4f * 0.5f + 0.5f;
        float alpha = 1.0f - (1.0f - (float)Math.sin(phase * Math.PI)) * 0.99f;
        alpha *= alpha;
        return new float[]{phase * 0.3f + 0.7f, phase * phase * 0.7f + 0.2f, 0.2f, alpha};
    }

    private static void drawSunriseFan(float[] color, double dayFraction) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glShadeModel(GL_SMOOTH);
        glPushMatrix();
        glRotated(90.0, 1.0, 0.0, 0.0);
        float angle = LightEngine.celestialAngle(dayFraction);
        if (Math.sin(angle * Math.PI * 2.0) < 0.0) glRotated(180.0, 0.0, 0.0, 1.0);
        glRotated(90.0, 0.0, 0.0, 1.0);
        glBegin(GL_TRIANGLE_FAN);
        glColor4f(color[0], color[1], color[2], color[3]);
        glVertex3d(0.0, 100.0, 0.0);
        glColor4f(color[0], color[1], color[2], 0.0f);
        for (int i = 0; i <= 16; i++) {
            float a = i * ((float)Math.PI * 2.0f) / 16.0f;
            float sin = (float)Math.sin(a);
            float cos = (float)Math.cos(a);
            glVertex3d(sin * 120.0f, cos * 120.0f, -cos * 40.0f * color[3]);
        }
        glEnd();
        glPopMatrix();
        glShadeModel(GL_FLAT);
    }

    private static void texturedCelestialQuad(double radius, double y, boolean reverse,
                                               double u0, double v0, double u1, double v1) {
        glBegin(GL_QUADS);
        if (!reverse) {
            glTexCoord2d(u0, v0); glVertex3d(-radius, y, -radius);
            glTexCoord2d(u1, v0); glVertex3d(radius, y, -radius);
            glTexCoord2d(u1, v1); glVertex3d(radius, y, radius);
            glTexCoord2d(u0, v1); glVertex3d(-radius, y, radius);
        } else {
            glTexCoord2d(u1, v1); glVertex3d(-radius, y, radius);
            glTexCoord2d(u0, v1); glVertex3d(radius, y, radius);
            glTexCoord2d(u0, v0); glVertex3d(radius, y, -radius);
            glTexCoord2d(u1, v0); glVertex3d(-radius, y, -radius);
        }
        glEnd();
    }

    private static void ensureStars() {
        if (starList != 0) return;
        starList = glGenLists(1);
        glNewList(starList, GL_COMPILE);
        Random random = new Random(10842L);
        glBegin(GL_QUADS);
        for (int i = 0; i < 1500; ++i) {
            double x = random.nextFloat() * 2.0f - 1.0f;
            double y = random.nextFloat() * 2.0f - 1.0f;
            double z = random.nextFloat() * 2.0f - 1.0f;
            double size = 0.15f + random.nextFloat() * 0.1f;
            double length = x * x + y * y + z * z;
            if (length >= 1.0 || length <= 0.01) continue;
            length = 1.0 / Math.sqrt(length);
            x *= length; y *= length; z *= length;
            double cx = x * 100.0, cy = y * 100.0, cz = z * 100.0;
            double azimuth = Math.atan2(x, z);
            double sinA = Math.sin(azimuth), cosA = Math.cos(azimuth);
            double polar = Math.atan2(Math.sqrt(x * x + z * z), y);
            double sinP = Math.sin(polar), cosP = Math.cos(polar);
            double roll = random.nextDouble() * Math.PI * 2.0;
            double sinR = Math.sin(roll), cosR = Math.cos(roll);
            for (int corner = 0; corner < 4; ++corner) {
                double dx = ((corner & 2) - 1) * size;
                double dy = (((corner + 1) & 2) - 1) * size;
                double rx = dx * cosR - dy * sinR;
                double ry = dy * cosR + dx * sinR;
                double py = rx * sinP;
                double pz = -rx * cosP;
                double vx = pz * sinA - ry * cosA;
                double vz = ry * sinA + pz * cosA;
                glVertex3d(cx + vx, cy + py, cz + vz);
            }
        }
        glEnd();
        glEndList();
    }

    private static void ensureTextures() {
        if (sunTexture == 0) sunTexture = loadTexture("sun.png");
        if (MOON_PHASE_TEXTURES[0] == 0) {
            for (int i = 0; i < MOON_PHASE_FILES.length; i++) {
                MOON_PHASE_TEXTURES[i] = loadTexture(MOON_PHASE_FILES[i]);
            }
        }
    }

    private static int loadTexture(String name) {
        try {
            File dir = AssetFinder.findAssetDir("environment", SkyRenderer.class);
            BufferedImage image = ImageIO.read(new File(dir, name));
            if (image == null) return 0;
            ByteBuffer pixels = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getRGB(x, y);
                    pixels.put((byte)(argb >> 16)).put((byte)(argb >> 8));
                    pixels.put((byte)argb).put((byte)(argb >> 24));
                }
            }
            pixels.flip();
            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image.getWidth(), image.getHeight(),
                    0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            return texture;
        } catch (Throwable throwable) {
            System.err.println("[SkyRenderer] Cannot load " + name + ": " + throwable.getMessage());
            return 0;
        }
    }

    public static float[] getFogColor(double dayFraction) {
        return getFogColor(dayFraction, 0f, 0f);
    }

    public static float[] getFogColor(double dayFraction, float rainStrength, float thunderStrength) {
        float[] colors = getSkyColors(dayFraction, rainStrength, thunderStrength);
        return new float[]{colors[3], colors[4], colors[5], 1f};
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
