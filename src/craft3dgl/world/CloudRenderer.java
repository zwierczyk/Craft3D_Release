package craft3dgl.world;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer chmur 3D w stylu Minecraft - prostopadlosciany z animacja.
 */
public final class CloudRenderer {

    private CloudRenderer() {}

    /** Hash do pseudo-losowych pozycji chmur. */
    private static int hash(int a, int b) {
        int h = a * 73428767 ^ b * 9122719;
        h ^= h >>> 13;
        h *= 1274126177;
        return h;
    }

    /**
     * Glowne wywolanie - rysuje wszystkie chmury widoczne wokol gracza.
     * @param playerX X gracza
     * @param playerZ Z gracza
     */
    public static void drawClouds(double playerX, double playerZ) {
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_FOG);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        double t = glfwGetTime() * 1.15;
        int baseX = (int) Math.floor((playerX + t) / 24) * 24;
        int baseZ = (int) Math.floor(playerZ / 24) * 24;
        double cy = 58.0;

        for (int cx = baseX - 144; cx <= baseX + 144; cx += 24) {
            for (int cz = baseZ - 144; cz <= baseZ + 144; cz += 24) {
                int h = hash(cx / 24, cz / 24);
                if ((h & 3) == 0) {
                    double ox = (h & 15) - 8;
                    double oz = ((h >> 4) & 15) - 8;
                    double px = cx + ox - t;
                    double pz = cz + oz;
                    drawCloudCluster(px, cy + ((h >> 8) & 3) * 0.35, pz, h);
                }
            }
        }

        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_FOG);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    private static void drawCloudCluster(double x, double y, double z, int seed) {
        drawCloudBox(x, y, z, 14, 2.2, 8, 0.78f);
        drawCloudBox(x + 8, y + 0.5, z - 2, 18, 2.5, 10, 0.82f);
        drawCloudBox(x + 20, y, z + 1, 16, 2.1, 8, 0.76f);
        if ((seed & 8) != 0) drawCloudBox(x + 34, y + 0.2, z - 1, 12, 2.0, 7, 0.68f);
        if ((seed & 16) != 0) drawCloudBox(x - 10, y + 0.1, z + 2, 12, 2.0, 7, 0.66f);
    }

    private static void drawCloudBox(double x, double y, double z, double w, double h, double d, float alpha) {
        double x1 = x + w, y1 = y + h, z1 = z + d;
        glBegin(GL_QUADS);
        glColor4f(1.00f, 1.00f, 1.00f, alpha);
        glVertex3d(x, y1, z); glVertex3d(x1, y1, z); glVertex3d(x1, y1, z1); glVertex3d(x, y1, z1);
        glColor4f(0.86f, 0.90f, 0.96f, alpha * 0.88f);
        glVertex3d(x, y, z1); glVertex3d(x1, y, z1); glVertex3d(x1, y, z); glVertex3d(x, y, z);
        glColor4f(0.92f, 0.95f, 1.00f, alpha * 0.94f);
        glVertex3d(x, y, z1); glVertex3d(x, y1, z1); glVertex3d(x, y1, z); glVertex3d(x, y, z);
        glVertex3d(x1, y, z); glVertex3d(x1, y1, z); glVertex3d(x1, y1, z1); glVertex3d(x1, y, z1);
        glColor4f(0.96f, 0.98f, 1.00f, alpha * 0.96f);
        glVertex3d(x, y, z); glVertex3d(x1, y, z); glVertex3d(x1, y1, z); glVertex3d(x, y1, z);
        glVertex3d(x1, y, z1); glVertex3d(x, y, z1); glVertex3d(x, y1, z1); glVertex3d(x1, y1, z1);
        glEnd();
    }
}
