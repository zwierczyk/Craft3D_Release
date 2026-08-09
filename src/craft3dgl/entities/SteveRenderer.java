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

        // Sneak modyfikacje
        double bodyRotX = 0;
        double legY = 0.75;   // Y biodra (w blokach)
        double legZ = 0;
        double headY = 1.5;
        if (sneaking) {
            bodyRotX = 0.5;
            armR += 0.4;
            armL += 0.4;
            legY = 0.5625;  // 9 px w blokach
            legZ = 0.25;    // 4 px w blokach
            headY = 1.5625; // 25 px
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

        if (sneaking) {
            glTranslated(0, -0.125, 0);
        }
        if (bodyRotX != 0) {
            // Pochylenie ciala do przodu (sneak)
            glRotated(Math.toDegrees(bodyRotX), 1, 0, 0);
        }

        // === BODY (12-24 px = 0.75-1.5 blok) ===
        drawBox(16, 16, -0.25f, 0.75f, -0.125f, 8, 12, 4, false);

        // === HEAD ===
        glPushMatrix();
        glTranslated(0, headY, 0);
        // Head yaw (osobno od body)
        if (netHeadYaw != 0) glRotated(Math.toDegrees(netHeadYaw), 0, 1, 0);
        // Head pitch: pitch > 0 (patrzenie w gore) → twarz w gore.
        // Nasza twarz na +Z. glRotate(kat, 1,0,0) obraca (0,0,+1) w kierunku -Y gdy kat>0.
        // Chcemy: pitch > 0 → twarz w +Y (up). Znak ujemny.
        glRotated(-Math.toDegrees(pitch), 1, 0, 0);
        drawBox(0, 0, -0.25f, 0, -0.25f, 8, 8, 8, false);
        // Hat overlay wylaczony
        glPopMatrix();

        // === RIGHT ARM (nasza prawa = +X po bodyYaw rot)
        // Wcielenie MC "right arm" ma pivot na -5 (LEWO w MC), ale my mamy odwrocona X przez brak scale(-1)
        // Wiec prawa reka Steve u nas na +X.
        glPushMatrix();
        glTranslated(0.3125, 1.375, 0);  // pivot ramie (5 px + 22 px)
        glRotated(Math.toDegrees(armR + armR_attack_x), 1, 0, 0);
        if (armR_z != 0 || armR_attack_z != 0) glRotated(Math.toDegrees(armR_z + armR_attack_z), 0, 0, 1);
        drawBox(40, 16, -0.125f, -0.75f, -0.125f, 4, 12, 4, false);
        glPopMatrix();

        // === LEFT ARM
        glPushMatrix();
        glTranslated(-0.3125, 1.375, 0);
        glRotated(Math.toDegrees(armL), 1, 0, 0);
        if (armL_z != 0) glRotated(Math.toDegrees(armL_z), 0, 0, 1);
        drawBox(32, 48, -0.125f, -0.75f, -0.125f, 4, 12, 4, true);
        glPopMatrix();

        // === RIGHT LEG
        glPushMatrix();
        glTranslated(0.11875, legY, legZ);
        glRotated(Math.toDegrees(legR), 1, 0, 0);
        drawBox(0, 16, -0.125f, -0.75f, -0.125f, 4, 12, 4, false);
        glPopMatrix();

        // === LEFT LEG
        glPushMatrix();
        glTranslated(-0.11875, legY, legZ);
        glRotated(Math.toDegrees(legL), 1, 0, 0);
        drawBox(16, 48, -0.125f, -0.75f, -0.125f, 4, 12, 4, true);
        glPopMatrix();

        glPopMatrix();

        // Przywroc stan OpenGL - kluczowe zeby nie popsuc reszty!
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glPopAttrib();
        glColor4f(1, 1, 1, 1);
    }

    /** Rysuje texturowany box wg MC skin unwrap. Y+ w gore u nas. */
    private static void drawBox(int u, int v, float x, float y, float z,
                                int wpx, int hpx, int dpx, boolean mirror) {
        float w = wpx * S, h = hpx * S, d = dpx * S;
        float x0 = x, y0 = y, z0 = z;
        float x1 = x + w, y1 = y + h, z1 = z + d;

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
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.05f);
        glBindTexture(GL_TEXTURE_2D, texSteve);
        glColor4f(1, 1, 1, 1);
        // PlayerModel rightArm: addBox(-3,-2,-2, 4,12,4), rendered at 1/16 scale.
        drawBox(40, 16, -3f / 16f, -2f / 16f, -2f / 16f, 4, 12, 4, false);
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

        glPopMatrix();
        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glPopAttrib();
        glColor4f(1, 1, 1, 1);
    }
}
