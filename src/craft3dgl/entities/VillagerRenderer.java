package craft3dgl.entities;

import craft3dgl.VillagerGL;
import craft3dgl.save.AssetFinder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;

/** Exact adult ModelVillager/RenderVillager geometry and profession skins from MCP 9.40. */
public final class VillagerRenderer {
    private VillagerRenderer() {}

    private static final int TEXTURE_SIZE = 64;
    private static int texVillager;
    private static int texFarmer;
    private static int texLibrarian;
    private static int texPriest;
    private static int texSmith;
    private static int texButcher;
    private static boolean loaded;

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        File dir = AssetFinder.findAssetDir("entity", VillagerRenderer.class);
        if (dir == null) return;
        texVillager = loadTex(new File(dir, "villager_villager.png"));
        texFarmer = loadTex(new File(dir, "villager_farmer.png"));
        texLibrarian = loadTex(new File(dir, "villager_librarian.png"));
        texPriest = loadTex(new File(dir, "villager_priest.png"));
        texSmith = loadTex(new File(dir, "villager_smith.png"));
        texButcher = loadTex(new File(dir, "villager_butcher.png"));
        System.out.println("[Villager] vanilla profession textures loaded");
    }

    private static int loadTex(File file) {
        if (!file.isFile()) return 0;
        try {
            BufferedImage image = ImageIO.read(file);
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
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            return texture;
        } catch (Exception e) {
            System.err.println("[Villager] cannot load " + file + ": " + e.getMessage());
            return 0;
        }
    }

    public static void drawAll(List<VillagerGL> villagers) {
        if (villagers == null || villagers.isEmpty()) return;
        ensureLoaded();
        if (texVillager <= 0) return;

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);
        glEnable(GL_CULL_FACE);
        glColor4f(1f, 1f, 1f, 1f);
        for (VillagerGL villager : villagers) {
            int texture = villager.profession == 0 ? texFarmer
                    : villager.profession == 1 ? texLibrarian
                    : villager.profession == 2 ? texPriest
                    : villager.profession == 3 ? texSmith
                    : villager.profession == 4 ? texButcher : texVillager;
            if (texture <= 0) texture = texVillager;
            glBindTexture(GL_TEXTURE_2D, texture);
            glPushMatrix();
            double groundY = villager.displayInit ? villager.displayY : villager.y;
            glTranslated(villager.x, groundY, villager.z);
            glRotated(Math.toDegrees(villager.yaw), 0, 1, 0);
            // RenderVillager#preRenderCallback uses 0.9375 for an adult.
            glScaled(0.9375, 0.9375, 0.9375);
            drawModel(villager);
            glPopMatrix();
        }
        glPopAttrib();
        glColor4f(1f, 1f, 1f, 1f);
    }

    private static void drawModel(VillagerGL villager) {
        boolean walking = Math.abs(villager.vx) + Math.abs(villager.vz) > 0.001;
        double limbSwing = villager.age * 6.0;
        double amount = walking ? 1.0 : 0.0;
        double rightLegAngle = Math.cos(limbSwing * 0.6662) * 1.4 * amount * 0.5;
        double leftLegAngle = Math.cos(limbSwing * 0.6662 + Math.PI) * 1.4 * amount * 0.5;

        // ModelVillager.villagerHead and its child nose.
        drawModelBox(0, 0, -4, -10, -4, 8, 10, 8, 0, false);
        drawModelBox(24, 0, -1, -3, -6, 2, 4, 2, 0, false);

        // Body plus the inflated 8x18x6 robe layer from texture offset (0,38).
        drawModelBox(16, 20, -4, 0, -3, 8, 12, 6, 0, false);
        drawModelBox(0, 38, -4, 0, -3, 8, 18, 6, 0.5, false);

        // rightVillagerLeg pivot (-2,12,0), left pivot (2,12,0).
        drawRotatedBox(0, 22, -4, 12, -2, 4, 12, 4,
                -2, 12, 0, rightLegAngle, false);
        drawRotatedBox(0, 22, 0, 12, -2, 4, 12, 4,
                2, 12, 0, leftLegAngle, true);

        // villagerArms pivot is changed by setRotationAngles to (0,3,-1),
        // with rotateAngleX=-0.75. Coordinates below include that pivot.
        glPushMatrix();
        rotateAroundModelPivot(0, 3, -1, -0.75);
        drawModelBox(44, 22, -8, 1, -3, 4, 8, 4, 0, false);
        drawModelBox(44, 22, 4, 1, -3, 4, 8, 4, 0, false);
        drawModelBox(40, 38, -4, 5, -3, 8, 4, 4, 0, false);
        glPopMatrix();
    }

    private static void drawRotatedBox(int u, int v, double modelX, double modelY, double modelZ,
                                       int width, int height, int depth,
                                       double pivotX, double pivotY, double pivotZ,
                                       double angle, boolean mirror) {
        glPushMatrix();
        rotateAroundModelPivot(pivotX, pivotY, pivotZ, angle);
        drawModelBox(u, v, modelX, modelY, modelZ, width, height, depth, 0, mirror);
        glPopMatrix();
    }

    /** Converts Minecraft model pixels (Y down, front -Z) to world coordinates. */
    private static void rotateAroundModelPivot(double x, double y, double z, double angle) {
        double wx = x / 16.0;
        double wy = (24.0 - y) / 16.0;
        double wz = -z / 16.0;
        glTranslated(wx, wy, wz);
        glRotated(Math.toDegrees(angle), 1, 0, 0);
        glTranslated(-wx, -wy, -wz);
    }

    private static void drawModelBox(int u, int v, double modelX, double modelY, double modelZ,
                                     int width, int height, int depth,
                                     double inflate, boolean mirror) {
        double x0 = (modelX - inflate) / 16.0;
        double x1 = (modelX + width + inflate) / 16.0;
        double y0 = (24.0 - (modelY + height + inflate)) / 16.0;
        double y1 = (24.0 - (modelY - inflate)) / 16.0;
        double z0 = -(modelZ + depth + inflate) / 16.0;
        double z1 = -(modelZ - inflate) / 16.0;
        drawTexBox(u, v, width, height, depth, x0, y0, z0, x1, y1, z1, mirror);
    }

    /** ModelBox 64x64 UV unwrap. */
    private static void drawTexBox(int u, int v, int width, int height, int depth,
                                   double x0, double y0, double z0,
                                   double x1, double y1, double z1, boolean mirror) {
        float t = 1f / TEXTURE_SIZE;
        float topU0 = (u + depth) * t, topU1 = (u + depth + width) * t;
        float topV0 = v * t, topV1 = (v + depth) * t;
        float bottomU0 = (u + depth + width) * t;
        float bottomU1 = (u + depth + width + width) * t;
        float sideV0 = (v + depth) * t, sideV1 = (v + depth + height) * t;
        float frontU0 = (u + depth) * t, frontU1 = (u + depth + width) * t;
        float backU0 = (u + depth + width + depth) * t;
        float backU1 = (u + depth + width + depth + width) * t;
        float rightU0 = u * t, rightU1 = (u + depth) * t;
        float leftU0 = (u + depth + width) * t, leftU1 = (u + depth + width + depth) * t;
        if (mirror) {
            float tmp = rightU0; rightU0 = leftU1; leftU1 = tmp;
            tmp = rightU1; rightU1 = leftU0; leftU0 = tmp;
        }

        glBegin(GL_QUADS);
        quad(topU0, topV0, topU1, topV1, x0,y1,z0, x1,y1,z0, x1,y1,z1, x0,y1,z1);
        quad(bottomU0, topV0, bottomU1, topV1, x0,y0,z1, x1,y0,z1, x1,y0,z0, x0,y0,z0);
        quad(frontU0, sideV0, frontU1, sideV1, x0,y1,z1, x1,y1,z1, x1,y0,z1, x0,y0,z1);
        quad(backU0, sideV0, backU1, sideV1, x1,y1,z0, x0,y1,z0, x0,y0,z0, x1,y0,z0);
        quad(rightU0, sideV0, rightU1, sideV1, x0,y1,z0, x0,y1,z1, x0,y0,z1, x0,y0,z0);
        quad(leftU0, sideV0, leftU1, sideV1, x1,y1,z1, x1,y1,z0, x1,y0,z0, x1,y0,z1);
        glEnd();
    }

    private static void quad(float u0, float v0, float u1, float v1,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4) {
        // The coordinate conversion keeps handedness; emit the old helper's
        // vertices in reverse so every ModelBox face has outward CCW winding.
        glTexCoord2f(u0, v1); glVertex3d(x4, y4, z4);
        glTexCoord2f(u1, v1); glVertex3d(x3, y3, z3);
        glTexCoord2f(u1, v0); glVertex3d(x2, y2, z2);
        glTexCoord2f(u0, v0); glVertex3d(x1, y1, z1);
    }
}
