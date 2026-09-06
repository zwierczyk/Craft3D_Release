package craft3dgl.entities;

import craft3dgl.VillagerGL;
import craft3dgl.save.AssetFinder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;

import static craft3dgl.ui.CuboidHelper.color;
import static org.lwjgl.opengl.GL11.*;

/**
 * VillagerRenderer z texturami PNG (villager.png + farmer.png overlay).
 * Model uzywa tej samej struktury co stary proceduralny (dziala poprawnie):
 *   - nogi od y=0 do y=0.55
 *   - tulow od y=0.55 do y=1.30
 *   - glowa od y=1.30 do y=1.85 (przez glTranslated 1.30 + head 0..0.55)
 *   - Y+ w gore, Z+ do przodu (standard OpenGL)
 * Texture UV z MC villager.png 64x64 unwrap.
 */
public final class VillagerRenderer {
    private VillagerRenderer() {}

    private static int texVillager = 0;
    private static int texFarmer = 0;
    private static boolean loaded = false;

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File dir = AssetFinder.findAssetDir("entity", VillagerRenderer.class);
            texVillager = loadTex(new File(dir, "villager.png"));
            texFarmer = loadTex(new File(dir, "villager_profession_farmer.png"));
            System.out.println("[Villager] villager=" + texVillager + " farmer=" + texFarmer);
        } catch (Exception e) {
            System.err.println("[Villager] load failed: " + e);
        }
    }

    private static int loadTex(File f) {
        if (!f.isFile()) return 0;
        try {
            BufferedImage img = ImageIO.read(f);
            int w = img.getWidth(), h = img.getHeight();
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                buf.put((byte)((argb >> 16) & 0xFF));
                buf.put((byte)((argb >> 8) & 0xFF));
                buf.put((byte)(argb & 0xFF));
                buf.put((byte)((argb >> 24) & 0xFF));
            }
            buf.flip();
            int tex = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, tex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            return tex;
        } catch (Exception e) {
            return 0;
        }
    }

    public static void drawAll(List<VillagerGL> villagers) {
        if (villagers.isEmpty()) return;
        ensureLoaded();
        if (texVillager == 0) return;

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);

        for (VillagerGL v : villagers) {
            color(1f, 1f, 1f);
            glPushMatrix();
            double gy = v.displayInit ? v.displayY : v.y;
            glTranslated(v.x, gy, v.z);
            glRotated(Math.toDegrees(v.yaw), 0, 1, 0);
            double bob = Math.sin(v.age * 4.0) * 0.02;
            glTranslated(0, bob, 0);
            drawModel(v);
            glPopMatrix();
        }

        glDisable(GL_ALPHA_TEST);
        glColor4f(1, 1, 1, 1);
    }

    private static void drawModel(VillagerGL v) {
        // Pass 1: base texture villager.png (glowa, tulow, nogi)
        drawModelPass(v, texVillager);
        // Pass 2: farmer overlay (kapelusz + fartuch) tylko dla profession=0
        if (v.profession == 0 && texFarmer > 0) {
            glPushMatrix();
            // Delikatnie wieksze zeby uniknac z-fighting z bazowa skorka
            glScaled(1.01, 1.01, 1.01);
            drawModelPass(v, texFarmer);
            glPopMatrix();
        }
    }

    private static void drawModelPass(VillagerGL v, int tex) {
        glBindTexture(GL_TEXTURE_2D, tex);

        // Animacja chodu
        double walkAng = 0;
        if (Math.abs(v.vx) + Math.abs(v.vz) > 0.001) {
            walkAng = Math.sin(v.age * 6.0) * 25.0;
        }

        // === LEGS (od y=0 do y=0.55, obie 0.16 szerokie, 0.20 glebokie) ===
        // MC leg tex: (0, 22), size 4x12x4
        // Lewa noga (widok z przodu - po LEWEJ ekranu = po prawej stronie villagera, +X)
        glPushMatrix();
        glTranslated(0.10, 0.55, 0);
        glRotated(walkAng, 1, 0, 0);
        glTranslated(-0.10, -0.55, 0);
        // Noga box: od -0.08 do 0.08 w X, 0 do 0.55 w Y, -0.08 do 0.08 w Z
        drawTexBox(0, 22, 4, 12, 4,
            0.02, 0.00, -0.08,     // x0,y0,z0 (przesuniete o +0.10 na lokalny)
            0.18, 0.55, 0.08,      // x1,y1,z1
            false);
        glPopMatrix();

        // Prawa noga (po prawej ekranu = -X od villagera)
        glPushMatrix();
        glTranslated(-0.10, 0.55, 0);
        glRotated(-walkAng, 1, 0, 0);
        glTranslated(0.10, -0.55, 0);
        drawTexBox(0, 22, 4, 12, 4,
            -0.18, 0.00, -0.08,
            -0.02, 0.55, 0.08,
            true);   // mirror
        glPopMatrix();

        // === BODY (od y=0.55 do y=1.30, szerokosc 0.52, glebokosc 0.28) ===
        // MC body tex: (16, 20), size 8x12x6
        drawTexBox(16, 20, 8, 12, 6,
            -0.26, 0.55, -0.16,
             0.26, 1.30,  0.16, false);

        // === RECE (od y=0.80 do y=1.30, przy tulowiu) ===
        // MC arms tex: 44,22 - 4x8x4 - lewy i prawy ramie, skrzyzowane do przodu
        // Lewa reka (patrzac z przodu = lewa strona ekranu = +X)
        drawTexBox(44, 22, 4, 8, 4,
            0.26, 0.80, -0.08,
            0.42, 1.28,  0.08, false);
        // Prawa reka (-X)
        drawTexBox(44, 22, 4, 8, 4,
            -0.42, 0.80, -0.08,
            -0.26, 1.28,  0.08, true);

        // === GLOWA (od y=1.30 do y=1.85, sześcian 0.52) ===
        // MC head tex: (0, 0), size 8x10x8
        // Twarz na +Z, oczy z tekstury MC villager.png (na frontowej scianie)
        glPushMatrix();
        glTranslated(0, 1.30, 0);
        drawTexBox(0, 0, 8, 10, 8,
            -0.26, 0.00, -0.26,
             0.26, 0.65,  0.26, false);
        // Nos - MC (24, 0), size 2x4x2 - wystaje do PRZODU (+Z)
        drawTexBox(24, 0, 2, 4, 2,
            -0.06, 0.11, 0.26,
             0.06, 0.27, 0.40, false);
        glPopMatrix();
    }

    /**
     * Rysuje texturowany box wg MC skin unwrap.
     * Uwaga: nasz Y+ w GORE, MC skin unwrap zaklada Y+ w GORE (w skince MC top = wyzej).
     * u,v w pixelach na 64x64 texturze.
     * wPx,hPx,dPx w pixelach = wielkosc MC (do UV unwrap - NIE do naszych world coord)
     * x0..z1 = world coordinates (nasze, w blokach)
     * mirror = odbicie lustrzane (dla mirror = true, uzywa "leftrgt" strony)
     */
    private static void drawTexBox(int u, int v, int wPx, int hPx, int dPx,
                                    double x0, double y0, double z0,
                                    double x1, double y1, double z1,
                                    boolean mirror) {
        float T = 1.0f / 64f;

        // MC skin unwrap:
        //   TOP:    (u+d, v)         wPx x dPx
        //   BOT:    (u+d+w, v)       wPx x dPx
        //   RIGHT:  (u, v+d)         dPx x hPx    (-X face)
        //   FRONT:  (u+d, v+d)       wPx x hPx    (+Z face)
        //   LEFT:   (u+d+w, v+d)     dPx x hPx    (+X face)
        //   BACK:   (u+d+w+d, v+d)   wPx x hPx    (-Z face)

        // UV coords
        float uTop0 = (u + dPx) * T,       uTop1 = (u + dPx + wPx) * T;
        float vTop0 = v * T,               vTop1 = (v + dPx) * T;
        float uBot0 = (u + dPx + wPx) * T, uBot1 = (u + dPx + wPx + wPx) * T;
        float vBot0 = v * T,               vBot1 = (v + dPx) * T;
        // FRONT (+Z twarz)
        float uFront0 = (u + dPx) * T,     uFront1 = (u + dPx + wPx) * T;
        float vSide0 = (v + dPx) * T,      vSide1 = (v + dPx + hPx) * T;
        // BACK (-Z)
        float uBack0 = (u + dPx + wPx + dPx) * T, uBack1 = (u + dPx + wPx + dPx + wPx) * T;
        // RIGHT (-X, po prawej strony patrzac na twarz z przodu)
        float uRight0 = u * T, uRight1 = (u + dPx) * T;
        // LEFT (+X)
        float uLeft0 = (u + dPx + wPx) * T, uLeft1 = (u + dPx + wPx + dPx) * T;

        // Dla mirror: zamien LEFT <-> RIGHT UV
        if (mirror) {
            float t;
            t = uRight0; uRight0 = uLeft1; uLeft1 = t;
            t = uRight1; uRight1 = uLeft0; uLeft0 = t;
        }

        glBegin(GL_QUADS);
        // TOP (Y=y1, patrzac z gory w dol)
        glTexCoord2f(uTop0, vTop0); glVertex3d(x0, y1, z0);
        glTexCoord2f(uTop1, vTop0); glVertex3d(x1, y1, z0);
        glTexCoord2f(uTop1, vTop1); glVertex3d(x1, y1, z1);
        glTexCoord2f(uTop0, vTop1); glVertex3d(x0, y1, z1);
        // BOTTOM (Y=y0)
        glTexCoord2f(uBot0, vBot0); glVertex3d(x0, y0, z1);
        glTexCoord2f(uBot1, vBot0); glVertex3d(x1, y0, z1);
        glTexCoord2f(uBot1, vBot1); glVertex3d(x1, y0, z0);
        glTexCoord2f(uBot0, vBot1); glVertex3d(x0, y0, z0);
        // FRONT (+Z, twarz, oczy tutaj)
        glTexCoord2f(uFront0, vSide0); glVertex3d(x0, y1, z1);
        glTexCoord2f(uFront1, vSide0); glVertex3d(x1, y1, z1);
        glTexCoord2f(uFront1, vSide1); glVertex3d(x1, y0, z1);
        glTexCoord2f(uFront0, vSide1); glVertex3d(x0, y0, z1);
        // BACK (-Z)
        glTexCoord2f(uBack0, vSide0); glVertex3d(x1, y1, z0);
        glTexCoord2f(uBack1, vSide0); glVertex3d(x0, y1, z0);
        glTexCoord2f(uBack1, vSide1); glVertex3d(x0, y0, z0);
        glTexCoord2f(uBack0, vSide1); glVertex3d(x1, y0, z0);
        // RIGHT (-X, gdy patrzysz z przodu to po Twojej lewej)
        glTexCoord2f(uRight0, vSide0); glVertex3d(x0, y1, z0);
        glTexCoord2f(uRight1, vSide0); glVertex3d(x0, y1, z1);
        glTexCoord2f(uRight1, vSide1); glVertex3d(x0, y0, z1);
        glTexCoord2f(uRight0, vSide1); glVertex3d(x0, y0, z0);
        // LEFT (+X, gdy patrzysz z przodu to po Twojej prawej)
        glTexCoord2f(uLeft0, vSide0); glVertex3d(x1, y1, z1);
        glTexCoord2f(uLeft1, vSide0); glVertex3d(x1, y1, z0);
        glTexCoord2f(uLeft1, vSide1); glVertex3d(x1, y0, z0);
        glTexCoord2f(uLeft0, vSide1); glVertex3d(x1, y0, z1);
        glEnd();
    }
}
