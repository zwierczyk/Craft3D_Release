package craft3dgl.entities;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import craft3dgl.save.AssetFinder;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer XP orb - texture experience_orb.png 64x64 (8x8 sprite grid, 4 klatki animacji).
 * Rysuje jako billboard (zawsze patrzy w kamere), z animacja klatek + bobbing.
 */
public final class ExperienceOrbRenderer {
    private ExperienceOrbRenderer() {}

    private static int texOrb = 0;
    private static boolean loaded = false;
    private static final int ATLAS_ROWS = 4, ATLAS_COLS = 4;   // 16 klatek 8x8 na 64x64
    private static final int FRAME_MS = 100;

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            File dir = AssetFinder.findAssetDir("entity", ExperienceOrbRenderer.class);
            File f = new File(dir, "experience_orb.png");
            if (!f.isFile()) return;
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
            texOrb = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texOrb);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            System.out.println("[XPOrb] loaded texture: " + texOrb);
        } catch (Exception e) {
            System.err.println("[XPOrb] load failed: " + e);
        }
    }

    public static void drawAll(List<ExperienceOrb> orbs, double camX, double camY, double camZ,
                               double yaw, double pitch) {
        if (orbs.isEmpty()) return;
        ensureLoaded();
        if (texOrb == 0) return;

        // STATE GUARD - zapisujemy wszystko zeby nie zepsuc HUD
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(true);
        // ZIELONY TINT - MC style, pulsujacy jasny zielony
        long ms = System.currentTimeMillis();
        float pulse = 0.75f + 0.25f * (float)Math.sin(ms / 200.0);
        glColor4f(0.3f * pulse, 1.0f * pulse, 0.3f * pulse, 1f);
        glBindTexture(GL_TEXTURE_2D, texOrb);

        // Aktualna klatka animacji (0..15)
        int frame = (int)((System.currentTimeMillis() / FRAME_MS) % (ATLAS_ROWS * ATLAS_COLS));
        int fcol = frame % ATLAS_COLS;
        int frow = frame / ATLAS_COLS;
        float T = 1.0f / ATLAS_COLS;   // 4x4 grid, kazda klatka 0.25 szerokosci
        float u0 = fcol * T, u1 = u0 + T;
        float v0 = frow * T, v1 = v0 + T;

        double size = 0.15;   // wielkosc orb w blokach (MC-native)
        // Billboard vectors (przeciwne do kamery)
        double cosY = Math.cos(yaw);
        double sinY = Math.sin(yaw);
        // Right vector = perpendicular to view yaw
        double rx = cosY, rz = -sinY;
        // Up vector = po prostu Y
        double ux = 0, uy = 1, uz = 0;

        for (ExperienceOrb orb : orbs) {
            // Bobbing (kolysanie w gore/dol)
            double bob = Math.sin(orb.age * 3.0 + orb.bobOffset) * 0.08;
            double ox = orb.x, oy = orb.y + bob, oz = orb.z;
            // 4 rogi billboardu
            double p1x = ox - rx * size,  p1y = oy - size, p1z = oz - rz * size;
            double p2x = ox + rx * size,  p2y = oy - size, p2z = oz + rz * size;
            double p3x = ox + rx * size,  p3y = oy + size, p3z = oz + rz * size;
            double p4x = ox - rx * size,  p4y = oy + size, p4z = oz - rz * size;
            glBegin(GL_QUADS);
            glTexCoord2f(u0, v1); glVertex3d(p1x, p1y, p1z);
            glTexCoord2f(u1, v1); glVertex3d(p2x, p2y, p2z);
            glTexCoord2f(u1, v0); glVertex3d(p3x, p3y, p3z);
            glTexCoord2f(u0, v0); glVertex3d(p4x, p4y, p4z);
            glEnd();
        }

        glColor4f(1, 1, 1, 1);
        glBindTexture(GL_TEXTURE_2D, 0);
        // STATE RESTORE - przywroc wszystko sprzed pushAttrib
        glPopAttrib();
    }
}
