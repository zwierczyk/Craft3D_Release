package craft3dgl.entities;

import craft3dgl.AnimalGL;
import craft3dgl.save.AssetFinder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;

import static craft3dgl.ui.CuboidHelper.color;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * Modele swini, krowy i owcy przeniesione z MCP 9.40:
 * ModelPig, ModelCow, ModelSheep2 oraz warstwa ModelSheep1.
 */
public final class AnimalRenderer {
    private AnimalRenderer() {}

    private static final float MODEL_SCALE = 1f / 16f;
    private static final PigModel PIG_MODEL = new PigModel();
    private static final CowModel COW_MODEL = new CowModel();
    private static final SheepModel SHEEP_MODEL = new SheepModel(false);
    private static final SheepModel SHEEP_WOOL_MODEL = new SheepModel(true);

    private static int pigTexture;
    private static int cowTexture;
    private static int sheepTexture;
    private static int sheepWoolTexture;
    private static boolean texturesLoaded;

    public static void drawAll(List<AnimalGL> animals) {
        if (animals.isEmpty()) return;
        glPushAttrib(GL_ENABLE_BIT | GL_TEXTURE_BIT | GL_COLOR_BUFFER_BIT | GL_CURRENT_BIT);
        ensureTexturesLoaded();
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);
        glDisable(GL_BLEND);
        // ModelBox ma rozna kolejnosc wierzcholkow po mirror/ujemnej skali.
        glDisable(GL_CULL_FACE);

        for (AnimalGL animal : animals) {
            double drawY = animal.displayInit ? animal.displayY : animal.y;
            glPushMatrix();
            glTranslated(animal.x, drawY, animal.z);
            // Nasz yaw=0 patrzy na +Z; vanilla model patrzy na -Z.
            glRotated(180.0 + Math.toDegrees(animal.yaw), 0, 1, 0);
            // RenderLivingBase.prepareScale z MCP 9.40.
            glScalef(-1f, -1f, 1f);
            glTranslatef(0f, -1.501f, 0f);

            if (animal.type == AnimalGL.PIG) {
                drawModel(PIG_MODEL, animal, pigTexture, 0.96f, 0.53f, 0.62f);
            } else if (animal.type == AnimalGL.COW) {
                drawModel(COW_MODEL, animal, cowTexture, 0.36f, 0.22f, 0.13f);
            } else if (animal.type == AnimalGL.SHEEP) {
                drawModel(SHEEP_MODEL, animal, sheepTexture, 0.24f, 0.22f, 0.20f);
                // LayerSheepWool: biala welna jest osobnym, nadmuchanym modelem.
                drawModel(SHEEP_WOOL_MODEL, animal, sheepWoolTexture, 0.90f, 0.90f, 0.86f);
            }
            glPopMatrix();
        }

        glPopAttrib();
    }

    private static void drawModel(QuadrupedModel model, AnimalGL animal, int texture,
                                  float fallbackR, float fallbackG, float fallbackB) {
        model.setupAnimation(animal);
        if (texture > 0) {
            glEnable(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, texture);
            color(1f, 1f, 1f);
        } else {
            glDisable(GL_TEXTURE_2D);
            color(fallbackR, fallbackG, fallbackB);
        }
        model.render();
    }

    private static void ensureTexturesLoaded() {
        if (texturesLoaded) return;
        texturesLoaded = true;
        File root = AssetFinder.findAssetDir("entity", AnimalRenderer.class);
        if (root == null) return;
        pigTexture = loadTexture(findTexture(root, "pig/pig.png", "pig.png"));
        cowTexture = loadTexture(findTexture(root, "cow/cow.png", "cow.png"));
        sheepTexture = loadTexture(findTexture(root, "sheep/sheep.png", "sheep.png"));
        sheepWoolTexture = loadTexture(findTexture(root, "sheep/sheep_fur.png", "sheep_fur.png"));
        System.out.println("[Animals] textures pig=" + pigTexture + " cow=" + cowTexture
                + " sheep=" + sheepTexture + " wool=" + sheepWoolTexture);
    }

    private static File findTexture(File root, String minecraftPath, String flatName) {
        File nested = new File(root, minecraftPath);
        return nested.isFile() ? nested : new File(root, flatName);
    }

    private static int loadTexture(File file) {
        if (file == null || !file.isFile()) return 0;
        try {
            BufferedImage image = ImageIO.read(file);
            if (image == null || image.getWidth() != 64 || image.getHeight() != 32) {
                System.err.println("[Animals] expected 64x32 texture: " + file);
                return 0;
            }
            ByteBuffer pixels = BufferUtils.createByteBuffer(64 * 32 * 4);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    int argb = image.getRGB(x, y);
                    pixels.put((byte)((argb >> 16) & 255));
                    pixels.put((byte)((argb >> 8) & 255));
                    pixels.put((byte)(argb & 255));
                    pixels.put((byte)((argb >> 24) & 255));
                }
            }
            pixels.flip();
            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, 64, 32, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            return texture;
        } catch (Exception exception) {
            glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            System.err.println("[Animals] cannot load " + file + ": " + exception);
            return 0;
        }
    }

    private abstract static class QuadrupedModel {
        ModelPart head;
        ModelPart body;
        ModelPart leg1;
        ModelPart leg2;
        ModelPart leg3;
        ModelPart leg4;

        void buildQuadruped(int legHeight, float inflate) {
            head = part(0, 0).addBox(-4f, -4f, -8f, 8, 8, 8, inflate);
            head.setPos(0f, 18f - legHeight, -6f);
            body = part(28, 8).addBox(-5f, -10f, -7f, 10, 16, 8, inflate);
            body.setPos(0f, 17f - legHeight, 2f);
            leg1 = part(0, 16).addBox(-2f, 0f, -2f, 4, legHeight, 4, inflate);
            leg1.setPos(-3f, 24f - legHeight, 7f);
            leg2 = part(0, 16).addBox(-2f, 0f, -2f, 4, legHeight, 4, inflate);
            leg2.setPos(3f, 24f - legHeight, 7f);
            leg3 = part(0, 16).addBox(-2f, 0f, -2f, 4, legHeight, 4, inflate);
            leg3.setPos(-3f, 24f - legHeight, -5f);
            leg4 = part(0, 16).addBox(-2f, 0f, -2f, 4, legHeight, 4, inflate);
            leg4.setPos(3f, 24f - legHeight, -5f);
        }

        ModelPart part(int u, int v) {
            return new ModelPart(64, 32, u, v);
        }

        void setupAnimation(AnimalGL animal) {
            // ModelQuadruped.setRotationAngles z MCP 9.40.
            head.xRot = (float)animal.headPitch;
            head.yRot = (float)animal.headYaw;
            head.zRot = 0f;
            body.xRot = (float)Math.PI / 2f;
            float phase = (float)animal.limbSwing * 0.6662f;
            float amount = animal.limbSwingAmount;
            leg1.xRot = (float)Math.cos(phase) * 1.4f * amount;
            leg2.xRot = (float)Math.cos(phase + Math.PI) * 1.4f * amount;
            leg3.xRot = (float)Math.cos(phase + Math.PI) * 1.4f * amount;
            leg4.xRot = (float)Math.cos(phase) * 1.4f * amount;
        }

        void render() {
            head.render(MODEL_SCALE);
            body.render(MODEL_SCALE);
            leg1.render(MODEL_SCALE);
            leg2.render(MODEL_SCALE);
            leg3.render(MODEL_SCALE);
            leg4.render(MODEL_SCALE);
        }
    }

    /** ModelPig(super(6)): dodatkowy box ryja ma UV (16,16). */
    private static final class PigModel extends QuadrupedModel {
        PigModel() {
            buildQuadruped(6, 0f);
            head.setTextureOffset(16, 16).addBox(-2f, 0f, -9f, 4, 3, 1, 0f);
        }
    }

    /** ModelCow: szerszy korpus, rogi i wymie dokladnie jak w MCP 9.40. */
    private static final class CowModel extends QuadrupedModel {
        CowModel() {
            buildQuadruped(12, 0f);
            head = part(0, 0).addBox(-4f, -4f, -6f, 8, 8, 6, 0f);
            head.setPos(0f, 4f, -8f);
            head.setTextureOffset(22, 0).addBox(-5f, -5f, -4f, 1, 3, 1, 0f);
            head.setTextureOffset(22, 0).addBox(4f, -5f, -4f, 1, 3, 1, 0f);
            body = part(18, 4).addBox(-6f, -10f, -7f, 12, 18, 10, 0f);
            body.setPos(0f, 5f, 2f);
            body.setTextureOffset(52, 0).addBox(-2f, 2f, -8f, 4, 6, 1, 0f);
            leg1.x -= 1f;
            leg2.x += 1f;
            leg3.x -= 1f;
            leg4.x += 1f;
            leg3.z -= 1f;
            leg4.z -= 1f;
        }
    }

    /** ModelSheep2 (cialo) albo ModelSheep1 (zewnetrzna warstwa welny). */
    private static final class SheepModel extends QuadrupedModel {
        SheepModel(boolean wool) {
            buildQuadruped(12, 0f);
            if (!wool) {
                head = part(0, 0).addBox(-3f, -4f, -6f, 6, 6, 8, 0f);
                head.setPos(0f, 6f, -8f);
                body = part(28, 8).addBox(-4f, -10f, -7f, 8, 16, 6, 0f);
                body.setPos(0f, 5f, 2f);
            } else {
                head = part(0, 0).addBox(-3f, -4f, -4f, 6, 6, 6, 0.6f);
                head.setPos(0f, 6f, -8f);
                body = part(28, 8).addBox(-4f, -10f, -7f, 8, 16, 6, 1.75f);
                body.setPos(0f, 5f, 2f);
                leg1 = part(0, 16).addBox(-2f, 0f, -2f, 4, 6, 4, 0.5f);
                leg1.setPos(-3f, 12f, 7f);
                leg2 = part(0, 16).addBox(-2f, 0f, -2f, 4, 6, 4, 0.5f);
                leg2.setPos(3f, 12f, 7f);
                leg3 = part(0, 16).addBox(-2f, 0f, -2f, 4, 6, 4, 0.5f);
                leg3.setPos(-3f, 12f, -5f);
                leg4 = part(0, 16).addBox(-2f, 0f, -2f, 4, 6, 4, 0.5f);
                leg4.setPos(3f, 12f, -5f);
            }
        }
    }
}
