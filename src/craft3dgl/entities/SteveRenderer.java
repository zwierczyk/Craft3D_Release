package craft3dgl.entities;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * Prosty Steve renderer - NASZA konwencja (Y+ w gore, yaw=0 patrzy w +Z).
 * BEZ scale(-1,-1,1) zeby nie psuc reszty renderu (kolejnosci quadow itd).
 *
 * Wszystkie boxy rysowane bezposrednio w blokach z pivot points MC.
 */
public final class SteveRenderer {
    private static final float S = 1f / 16f;
    // RenderPlayer.doRender + ModelBiped.render/setRotationAngles, MCP 9.40.
    public static final double SNEAK_RENDER_OFFSET_Y = -0.325;
    public static final double SNEAK_ARM_ROTATION_X = 0.4;

    private static int texSteve = -1;
    private static boolean initialized = false;

    private SteveRenderer() {}

    public static void ensureLoaded() {
        if (initialized) return;
        initialized = true;
        try {
            File dir = AssetFinder.findAssetDir("icons", SteveRenderer.class);
            File file = new File(dir, "steve.png");
            if (!file.exists()) return;
            BufferedImage img = ImageIO.read(file);
            int w = img.getWidth(), h = img.getHeight();
            int[] pixels = new int[w * h];
            img.getRGB(0, 0, w, h, pixels, 0, w);
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int p = pixels[y * w + x];
                    buf.put((byte)((p >> 16) & 0xff));
                    buf.put((byte)((p >> 8) & 0xff));
                    buf.put((byte)(p & 0xff));
                    buf.put((byte)((p >> 24) & 0xff));
                }
            }
            buf.flip();
            int tex = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, tex);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            texSteve = tex;
            System.out.println("[Steve] loaded " + w + "x" + h);
        } catch (Throwable t) { System.err.println("[Steve] " + t); }
    }

    public static boolean isLoaded() { ensureLoaded(); return texSteve > 0; }
    public static int textureId() { ensureLoaded(); return texSteve; }

    /**
     * Rysuje gracza. (px,py,pz) = stopy, bodyYaw/headYaw/pitch w RADIANACH.
     * limbSwing/Amount jak w MC, attackTime 0..1.
     *
     * KAZDY glPushMatrix ma odpowiadajacy glPopMatrix - GWARANTUJE ZE nie zaburzy globalnego stanu.
     * NIE UZYWA glScale (-1) zeby nie zepsuc pozostalych renderow.
     */
    public static void drawPlayer(double px, double py, double pz,
                                  double bodyYaw, double headYaw, double pitch,
                                  float limbSwing, float limbSwingAmount,
                                  float ageInTicks, float attackTime,
                                  boolean sneaking) {
        ensureLoaded();
        if (texSteve <= 0) return;

        // Zachowaj stan OpenGL zeby nic nie popsuc
        glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.05f);
        glBindTexture(GL_TEXTURE_2D, texSteve);
        glColor4f(1, 1, 1, 1);

        // MC HumanoidModel.setupAnim - wartosci per klatka
        double armR = Math.cos(limbSwing * 0.6662 + Math.PI) * 2.0 * limbSwingAmount * 0.5;
        double armL = Math.cos(limbSwing * 0.6662) * 2.0 * limbSwingAmount * 0.5;
        double legR = Math.cos(limbSwing * 0.6662) * 1.4 * limbSwingAmount;
        double legL = Math.cos(limbSwing * 0.6662 + Math.PI) * 1.4 * limbSwingAmount;
        // Idle bujanie ramion (MC)
        double armR_z = Math.cos(ageInTicks * 0.09) * 0.05 + 0.05;
        double armL_z = -(Math.cos(ageInTicks * 0.09) * 0.05 + 0.05);
        armR += Math.sin(ageInTicks * 0.067) * 0.05;
        armL -= Math.sin(ageInTicks * 0.067) * 0.05;

        // Attack anim (MC)
        double bodyYawExtra = 0;
        double armR_attack_x = 0;
        double armR_attack_z = 0;
        if (attackTime > 0) {
            bodyYawExtra = Math.sin(Math.sqrt(attackTime) * Math.PI * 2) * 0.2;
            double f1 = 1.0 - attackTime;
            f1 = f1 * f1; f1 = f1 * f1; f1 = 1.0 - f1;
            double n = Math.sin(f1 * Math.PI);
            armR_attack_x = -(n * 1.2);
            armR_attack_z = Math.sin(attackTime * Math.PI) * -0.4;
        }

        // ModelBiped.setRotationAngles + RenderPlayer.doRender z MCP 9.40.
        // Os Y modelu Minecrafta jest skierowana w dol, dlatego pivot nog przy
        // skradaniu (9 px zamiast 12 px) przesuwa sie u nas W GORE.
        double modelYOffset = 0.0;
        double bodyRotX = 0.0;
        double legY = 0.75;       // rotationPointY = 12 px
        double legZ = -0.00625;   // rotationPointZ = 0.1 px, za graczem
        double headY = 1.5;       // rotationPointY = 0 px
        if (sneaking) {
            // RenderPlayer: -0.125; ModelBiped.render: translate(0, 0.2, 0).
            modelYOffset = SNEAK_RENDER_OFFSET_Y;
            bodyRotX = 0.5;
            armR += SNEAK_ARM_ROTATION_X;
            armL += SNEAK_ARM_ROTATION_X;
            legY = 0.9375;        // rotationPointY = 9 px
            legZ = -0.25;         // rotationPointZ = 4 px, za graczem
            headY = 1.4375;       // rotationPointY = 1 px
        }

        // netHeadYaw dla glowy
        double netHeadYaw = headYaw - bodyYaw;
        while (netHeadYaw > Math.PI) netHeadYaw -= Math.PI * 2;
        while (netHeadYaw < -Math.PI) netHeadYaw += Math.PI * 2;

        // === RENDER ===
        glPushMatrix();
        glTranslated(px, py, pz);
        // Nasza konwencja: yaw=0 patrzy w +Z. glRotated(yaw, 0, 1, 0) obraca CCW z +Y.
        // Sprawdzenie: yaw=0 => Steve twarza w +Z. yaw=+90 => twarz obrocona o +90 CCW z +Y = +X.
        // A wektor ruchu naszego dla yaw=+90 = (sin(90), 0, cos(90)) = (1, 0, 0) = +X. OK, zgadza sie.
        glRotated(Math.toDegrees(bodyYaw + bodyYawExtra), 0, 1, 0);

        if (modelYOffset != 0) {
            glTranslated(0, modelYOffset, 0);
        }

        // === BODY (12-24 px = 0.75-1.5 blok) ===
        // W MCP tylko tulow ma rotateAngleX=0.5. Obracanie calego modelu
        // powodowalo zapadanie nog i nienaturalne odchylenie glowy.
        glPushMatrix();
        glTranslated(0, 1.5, 0);
        if (bodyRotX != 0) glRotated(Math.toDegrees(bodyRotX), 1, 0, 0);
        drawBox(16, 16, -0.25f, -0.75f, -0.125f, 8, 12, 4, false);
        // Jacket, druga warstwa skina (PlayerModel.bodyWear, inflate 0.25 px).
        drawBox(16, 32, -0.25f, -0.75f, -0.125f, 8, 12, 4, false, 0.25f * S);
        glPopMatrix();

        // === HEAD ===
        glPushMatrix();
        // Pivot jest na szyi, tak jak PlayerModel.head.setPos(0, 0, 0), a nie
        // w srodku glowy. Dzieki temu pitch nie odrywa glowy od tulowia.
        glTranslated(0, headY, 0);
        if (netHeadYaw != 0) glRotated(Math.toDegrees(netHeadYaw), 0, 1, 0);
        // Craft3D: dodatni pitch oznacza patrzenie w gore.
        glRotated(-Math.toDegrees(pitch), 1, 0, 0);
        drawBox(0, 0, -0.25f, 0.0f, -0.25f, 8, 8, 8, false);
        // Hat/headwear, druga warstwa skina (inflate 0.5 px).
        drawBox(32, 0, -0.25f, 0.0f, -0.25f, 8, 8, 8, false, 0.5f * S);
        glPopMatrix();

        // === RIGHT ARM (nasza prawa = +X po bodyYaw rot)
        // Wcielenie MC "right arm" ma pivot na -5 (LEWO w MC), ale my mamy odwrocona X przez brak scale(-1)
        // Wiec prawa reka Steve u nas na +X.
        glPushMatrix();
        // PlayerModel: pivot (5, 2), box (-1, -2)..(3, 10), po
        // przeliczeniu osi modelu MC na Y+ w gore.
        glTranslated(0.3125, 1.375, 0);
        glRotated(Math.toDegrees(armR + armR_attack_x), 1, 0, 0);
        if (armR_z != 0 || armR_attack_z != 0) glRotated(Math.toDegrees(armR_z + armR_attack_z), 0, 0, 1);
        drawBox(40, 16, -0.0625f, -0.625f, -0.125f, 4, 12, 4, false);
        drawBox(40, 32, -0.0625f, -0.625f, -0.125f, 4, 12, 4, false, 0.25f * S);
        glPopMatrix();

        // === LEFT ARM
        glPushMatrix();
        glTranslated(-0.3125, 1.375, 0);
        glRotated(Math.toDegrees(armL), 1, 0, 0);
        if (armL_z != 0) glRotated(Math.toDegrees(armL_z), 0, 0, 1);
        drawBox(32, 48, -0.1875f, -0.625f, -0.125f, 4, 12, 4, true);
        drawBox(48, 48, -0.1875f, -0.625f, -0.125f, 4, 12, 4, true, 0.25f * S);
        glPopMatrix();

        // === RIGHT LEG
        glPushMatrix();
        glTranslated(0.11875, legY, legZ);
        glRotated(Math.toDegrees(legR), 1, 0, 0);
        drawBox(0, 16, -0.125f, -0.75f, -0.125f, 4, 12, 4, false);
        drawBox(0, 32, -0.125f, -0.75f, -0.125f, 4, 12, 4, false, 0.25f * S);
        glPopMatrix();

        // === LEFT LEG
        glPushMatrix();
        glTranslated(-0.11875, legY, legZ);
        glRotated(Math.toDegrees(legL), 1, 0, 0);
        drawBox(16, 48, -0.125f, -0.75f, -0.125f, 4, 12, 4, true);
        drawBox(0, 48, -0.125f, -0.75f, -0.125f, 4, 12, 4, true, 0.25f * S);
        glPopMatrix();

        glPopMatrix();

        // Przywroc stan OpenGL - kluczowe zeby nie popsuc reszty!
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glPopAttrib();
        glColor4f(1, 1, 1, 1);
    }

    /**
     * Doklada do aktualnej macierzy dokladnie te same transformacje prawego
     * ramienia co drawPlayer, a nastepnie przechodzi do dloni. Wywolujacy
     * ustawia wczesniej pozycje gracza i bodyYaw.
     */
    public static void applyThirdPersonRightHandTransform(float limbSwing,
                                                           float limbSwingAmount,
                                                           float ageInTicks,
                                                           float attackTime,
                                                           boolean sneaking) {
        double armX = Math.cos(limbSwing * 0.6662 + Math.PI)
                * 2.0 * limbSwingAmount * 0.5;
        double armZ = Math.cos(ageInTicks * 0.09) * 0.05 + 0.05;
        armX += Math.sin(ageInTicks * 0.067) * 0.05;

        double bodyYawExtra = 0.0;
        if (attackTime > 0) {
            bodyYawExtra = Math.sin(Math.sqrt(attackTime) * Math.PI * 2) * 0.2;
            double f1 = 1.0 - attackTime;
            f1 = f1 * f1;
            f1 = f1 * f1;
            f1 = 1.0 - f1;
            armX -= Math.sin(f1 * Math.PI) * 1.2;
            armZ += Math.sin(attackTime * Math.PI) * -0.4;
        }
        if (sneaking) armX += SNEAK_ARM_ROTATION_X;

        if (bodyYawExtra != 0) {
            glRotated(Math.toDegrees(bodyYawExtra), 0, 1, 0);
        }
        if (sneaking) {
            glTranslated(0, SNEAK_RENDER_OFFSET_Y, 0);
        }
        glTranslated(0.3125, 1.375, 0);
        glRotated(Math.toDegrees(armX), 1, 0, 0);
        if (armZ != 0) glRotated(Math.toDegrees(armZ), 0, 0, 1);
        glTranslated(0, -0.7, 0.1);
    }

    /**
     * Podglad gracza w ekwipunku zgodny z GuiInventory.drawEntityOnScreen.
     * Cialo i glowa obracaja sie w strone aktualnej pozycji kursora.
     */
    public static boolean drawInventoryPlayer(int screenWidth, int screenHeight,
                                              int portraitX, int portraitY,
                                              int portraitWidth, int portraitHeight,
                                              int mouseX, int mouseY) {
        ensureLoaded();
        if (texSteve <= 0) return false;

        float guiScale = portraitWidth / 50.0f;
        float renderX = portraitX + 26.0f * guiScale;
        float renderY = portraitY + 68.0f * guiScale;
        float lookY = portraitY + 18.0f * guiScale;
        float mouseScale = 40.0f * guiScale;
        float horizontal = mouseX - renderX;
        float vertical = lookY - mouseY;

        double bodyYaw = Math.toRadians(Math.atan(horizontal / mouseScale) * 20.0);
        double headYaw = Math.toRadians(Math.atan(horizontal / mouseScale) * 40.0);
        double headPitch = Math.toRadians(Math.atan(vertical / mouseScale) * 20.0);
        float modelScale = 30.0f * guiScale;
        float ageInTicks = (float)(System.nanoTime() / 50_000_000.0);

        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glEnable(GL_SCISSOR_TEST);
        glScissor(portraitX, screenHeight - portraitY - portraitHeight,
                portraitWidth, portraitHeight);
        glDepthMask(true);
        glClear(GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        glDisable(GL_CULL_FACE);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, screenWidth, screenHeight, 0, -1000, 1000);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glTranslated(renderX, renderY, 0);
        // Model ma Y+ do gory, a GUI ma Y+ w dol. GuiInventory pochyla
        // dodatkowo caly model w pionie, niezaleznie od pitchu glowy.
        glScaled(modelScale, -modelScale, modelScale);
        glRotated(-Math.toDegrees(headPitch), 1, 0, 0);
        drawPlayer(0, 0, 0, bodyYaw, headYaw, headPitch,
                0, 0, ageInTicks, 0, false);
        glPopMatrix();

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopAttrib();
        return true;
    }

    /** Rysuje texturowany box wg MC skin unwrap. Y+ w gore u nas. */
    private static void drawBox(int u, int v, float x, float y, float z,
                                int wpx, int hpx, int dpx, boolean mirror) {
        drawBox(u, v, x, y, z, wpx, hpx, dpx, mirror, 0.0f);
    }

    /** Box z opcjonalnym minecraftowym "inflate" dla drugiej warstwy skina. */
    private static void drawBox(int u, int v, float x, float y, float z,
                                int wpx, int hpx, int dpx, boolean mirror,
                                float inflate) {
        float w = wpx * S, h = hpx * S, d = dpx * S;
        float x0 = x - inflate, y0 = y - inflate, z0 = z - inflate;
        float x1 = x + w + inflate, y1 = y + h + inflate, z1 = z + d + inflate;

        float T_u = 1f / 64f;
        float T_v = 1f / 64f;

        // MC unwrap:
        //   TOP:    (u+d, v)         w x d
        //   BOT:    (u+d+w, v)       w x d
        //   RIGHT:  (u, v+d)         d x h    (-X face)
        //   FRONT:  (u+d, v+d)       w x h    (+Z face, twarz)
        //   LEFT:   (u+d+w, v+d)     d x h    (+X face)
        //   BACK:   (u+d+w+d, v+d)   w x h    (-Z face)

        int uTop = u + dpx, vTop = v;
        int uBot = u + dpx + wpx, vBot = v;
        int uRight = u, vSide = v + dpx;
        int uFront = u + dpx;
        int uLeft = u + dpx + wpx;
        int uBack = u + dpx + wpx + dpx;

        // TOP (Y+ w gore, patrzysz z gory w dol na box)
        uvQuad(uTop, vTop, wpx, dpx, T_u, T_v, mirror,
               x0, y1, z1,  x1, y1, z1,  x1, y1, z0,  x0, y1, z0);
        // BOTTOM (Y-)
        uvQuad(uBot, vBot, wpx, dpx, T_u, T_v, mirror,
               x0, y0, z0,  x1, y0, z0,  x1, y0, z1,  x0, y0, z1);
        // FRONT (+Z twarz)
        uvQuad(uFront, vSide, wpx, hpx, T_u, T_v, mirror,
               x0, y1, z1,  x1, y1, z1,  x1, y0, z1,  x0, y0, z1);
        // BACK (-Z)
        uvQuad(uBack, vSide, wpx, hpx, T_u, T_v, mirror,
               x1, y1, z0,  x0, y1, z0,  x0, y0, z0,  x1, y0, z0);
        // RIGHT (-X)
        uvQuad(mirror ? uLeft : uRight, vSide, dpx, hpx, T_u, T_v, false,
               x0, y1, z0,  x0, y1, z1,  x0, y0, z1,  x0, y0, z0);
        // LEFT (+X)
        uvQuad(mirror ? uRight : uLeft, vSide, dpx, hpx, T_u, T_v, false,
               x1, y1, z1,  x1, y1, z0,  x1, y0, z0,  x1, y0, z1);
    }

    private static void uvQuad(int u, int v, int tw, int th, float Tu, float Tv, boolean flipU,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               float x4, float y4, float z4) {
        float u0 = u * Tu, u1 = (u + tw) * Tu;
        float v0 = v * Tv, v1 = (v + th) * Tv;
        if (flipU) { float t = u0; u0 = u1; u1 = t; }
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v0); glVertex3f(x1, y1, z1);
        glTexCoord2f(u1, v0); glVertex3f(x2, y2, z2);
        glTexCoord2f(u1, v1); glVertex3f(x3, y3, z3);
        glTexCoord2f(u0, v1); glVertex3f(x4, y4, z4);
        glEnd();
    }

    /** Raw right arm matching PlayerModel.rightArm, for ItemInHandRenderer transforms. */
    public static void drawMinecraftFirstPersonArm() {
        ensureLoaded();
        if (texSteve <= 0) return;
        glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT);
        // ItemRenderer/RenderPlayer w MCP 9.40 wylacza culling dla reki.
        glDisable(GL_CULL_FACE);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.05f);
        glBindTexture(GL_TEXTURE_2D, texSteve);
        glColor4f(1, 1, 1, 1);
        // PlayerModel rightArm: addBox(-3,-2,-2, 4,12,4), rendered at 1/16 scale.
        drawBox(40, 16, -3f / 16f, -2f / 16f, -2f / 16f, 4, 12, 4, false);
        drawBox(40, 32, -3f / 16f, -2f / 16f, -2f / 16f,
                4, 12, 4, false, 0.25f * S);
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glPopAttrib();
        glColor4f(1, 1, 1, 1);
    }

    /**
     * Prawa reka pierwszoosobowa z ItemRenderer.renderArmFirstPerson
     * i RenderPlayer.renderRightArm, Minecraft 1.12 / MCP 9.40.
     */
    public static void drawMinecraftFirstPersonArm(float attackProgress, float equipProgress) {
        ensureLoaded();
        if (texSteve <= 0) return;
        float attack = Math.max(0f, Math.min(1f, attackProgress));
        float equip = Math.max(0f, Math.min(1f, equipProgress));
        float root = (float)Math.sqrt(attack);
        float swingX = -0.3f * (float)Math.sin(root * Math.PI);
        float swingY =  0.4f * (float)Math.sin(root * Math.PI * 2.0);
        float swingZ = -0.4f * (float)Math.sin(attack * Math.PI);
        float body = (float)Math.sin(attack * attack * Math.PI);
        float arm = (float)Math.sin(root * Math.PI);

        glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT);
        // ItemRenderer.renderArmFirstPerson robi to przed RenderPlayer.renderRightArm.
        glDisable(GL_CULL_FACE);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.05f);
        glBindTexture(GL_TEXTURE_2D, texSteve);
        glColor4f(1, 1, 1, 1);
        glPushMatrix();
        // ItemInHandRenderer.renderPlayerArm, right arm.
        glTranslatef(swingX + 0.64000005f, swingY - 0.6f - equip * 0.6f, swingZ - 0.71999997f);
        glRotatef(45f, 0, 1, 0);
        glRotatef(arm * 70f, 0, 1, 0);
        glRotatef(body * -20f, 0, 0, 1);
        glTranslatef(-1f, 3.6f, 3.5f);
        glRotatef(120f, 0, 0, 1);
        glRotatef(200f, 1, 0, 0);
        glRotatef(-135f, 0, 1, 0);
        glTranslatef(5.6f, 0, 0);
        // RenderPlayer.renderRightArm: pivot i box sa juz przeliczone przez 1/16.
        // Nie dodajemy drugiego offsetu ani skali — wczesniej spychaly reke poza ekran.
        // PlayerModel rightArm: setPos(-5,2,0), addBox(-3,-2,-2, 4,12,4), render(1/16).
        glTranslatef(-5f / 16f, 2f / 16f, 0);
        drawBox(40, 16, -3f / 16f, -2f / 16f, -2f / 16f, 4, 12, 4, false);
        drawBox(40, 32, -3f / 16f, -2f / 16f, -2f / 16f,
                4, 12, 4, false, 0.25f * S);
        glPopMatrix();
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glPopAttrib();
        glColor4f(1, 1, 1, 1);
    }

    /** 1st person ręka. */
    public static void drawFirstPersonArm(float attackTime, float ageInTicks) {
        ensureLoaded();
        if (texSteve <= 0) return;
        glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.05f);
        glBindTexture(GL_TEXTURE_2D, texSteve);
        glColor4f(1, 1, 1, 1);

        glPushMatrix();
        glTranslatef(0.56f, -0.55f, -0.72f);

        if (attackTime > 0) {
            float sq = (float)Math.sqrt(attackTime);
            float f  = (float)Math.sin(attackTime * attackTime * Math.PI);
            float f2 = (float)Math.sin(sq * Math.PI);
            glTranslatef(-f2 * 0.3f, f * 0.15f, -f2 * 0.15f);
            glRotatef(f2 * -20f, 0, 1, 0);
            glRotatef(f * -60f, 1, 0, 0);
        }

        glRotatef(-75, 1, 0, 0);
        glRotatef(-20, 0, 1, 0);
        // The old 0.4 scale made the first-person arm noticeably smaller than Minecraft.
        glScalef(0.65f, 0.65f, 0.65f);

        drawBox(40, 16, -0.125f, -0.75f, -0.125f, 4, 12, 4, false);
        drawBox(40, 32, -0.125f, -0.75f, -0.125f,
                4, 12, 4, false, 0.25f * S);

        glPopMatrix();
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glPopAttrib();
        glColor4f(1, 1, 1, 1);
    }
}
