package craft3dgl.entities;

import craft3dgl.AnimalGL;
import craft3dgl.ui.CuboidHelper;

import java.util.List;

import static craft3dgl.ui.CuboidHelper.color;
import static craft3dgl.ui.CuboidHelper.drawCuboid;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer modeli 3D zwierząt: świnia, krowa, owca.
 * Każde zwierzę z piksel-art stylem MC (drobne klocki).
 */
public final class AnimalRenderer {
    private AnimalRenderer() {}

    public static void drawAll(List<AnimalGL> animals) {
        if (animals.isEmpty()) return;
        glDisable(GL_TEXTURE_2D);
        for (AnimalGL a : animals) {
            double drawY = a.displayInit ? a.displayY : a.y;
            glPushMatrix();
            glTranslated(a.x, drawY, a.z);
            glRotated(Math.toDegrees(a.yaw), 0, 1, 0);
            if (a.type == AnimalGL.PIG) drawPig();
            else if (a.type == AnimalGL.COW) drawCow();
            else if (a.type == AnimalGL.SHEEP) drawSheep();
            glPopMatrix();
        }
        glEnable(GL_TEXTURE_2D);
        glColor3f(1, 1, 1);
    }

    private static void drawPig() {
        color(0.96f, 0.53f, 0.62f); drawCuboid(-0.38, 0.20, -0.62, 0.38, 0.70, 0.35);
        color(1.00f, 0.60f, 0.68f); drawCuboid(-0.30, 0.35, 0.30, 0.30, 0.85, 0.82);
        color(0.95f, 0.38f, 0.48f); drawCuboid(-0.18, 0.48, 0.78, 0.18, 0.66, 0.92);
        color(0.02f, 0.02f, 0.02f); drawCuboid(-0.20, 0.66, 0.825, -0.12, 0.74, 0.835);
        color(0.02f, 0.02f, 0.02f); drawCuboid( 0.12, 0.66, 0.825,  0.20, 0.74, 0.835);
        color(0.92f, 0.44f, 0.54f); drawCuboid(-0.32, 0.76, 0.42, -0.18, 1.00, 0.60);
        color(0.92f, 0.44f, 0.54f); drawCuboid( 0.18, 0.76, 0.42,  0.32, 1.00, 0.60);
        color(0.86f, 0.38f, 0.48f); drawCuboid(-0.32, 0.00, -0.50, -0.16, 0.25, -0.34);
        color(0.86f, 0.38f, 0.48f); drawCuboid( 0.16, 0.00, -0.50,  0.32, 0.25, -0.34);
        color(0.86f, 0.38f, 0.48f); drawCuboid(-0.32, 0.00,  0.12, -0.16, 0.25,  0.28);
        color(0.86f, 0.38f, 0.48f); drawCuboid( 0.16, 0.00,  0.12,  0.32, 0.25,  0.28);
    }

    private static void drawCow() {
        color(0.36f, 0.22f, 0.13f); drawCuboid(-0.42, 0.22, -0.68, 0.42, 0.86, 0.36);
        color(0.88f, 0.84f, 0.74f); drawCuboid(-0.22, 0.30, -0.66, 0.18, 0.82, -0.36);
        color(0.33f, 0.20f, 0.12f); drawCuboid(-0.32, 0.48, 0.30, 0.32, 1.02, 0.86);
        color(0.72f, 0.56f, 0.42f); drawCuboid(-0.20, 0.56, 0.80, 0.20, 0.74, 0.96);
        color(0.02f, 0.02f, 0.02f); drawCuboid(-0.22, 0.78, 0.865, -0.14, 0.86, 0.875);
        color(0.02f, 0.02f, 0.02f); drawCuboid( 0.14, 0.78, 0.865,  0.22, 0.86, 0.875);
        color(0.85f, 0.82f, 0.66f); drawCuboid(-0.42, 0.92, 0.38, -0.28, 1.12, 0.58);
        color(0.85f, 0.82f, 0.66f); drawCuboid( 0.28, 0.92, 0.38,  0.42, 1.12, 0.58);
        color(0.20f, 0.12f, 0.08f); drawCuboid(-0.34, 0.00, -0.54, -0.18, 0.28, -0.38);
        color(0.20f, 0.12f, 0.08f); drawCuboid( 0.18, 0.00, -0.54,  0.34, 0.28, -0.38);
        color(0.20f, 0.12f, 0.08f); drawCuboid(-0.34, 0.00,  0.12, -0.18, 0.28,  0.28);
        color(0.20f, 0.12f, 0.08f); drawCuboid( 0.18, 0.00,  0.12,  0.34, 0.28,  0.28);
    }

    private static void drawSheep() {
        color(0.92f, 0.92f, 0.86f); drawCuboid(-0.48, 0.24, -0.66, 0.48, 0.88, 0.34);
        color(0.98f, 0.98f, 0.92f); drawCuboid(-0.55, 0.32, -0.58, 0.55, 0.78, 0.28);
        color(0.18f, 0.16f, 0.14f); drawCuboid(-0.28, 0.42, 0.25, 0.28, 0.88, 0.78);
        color(0.02f, 0.02f, 0.02f); drawCuboid(-0.18, 0.70, 0.785, -0.10, 0.78, 0.795);
        color(0.02f, 0.02f, 0.02f); drawCuboid( 0.10, 0.70, 0.785,  0.18, 0.78, 0.795);
        color(0.12f, 0.10f, 0.09f); drawCuboid(-0.34, 0.00, -0.50, -0.18, 0.26, -0.34);
        color(0.12f, 0.10f, 0.09f); drawCuboid( 0.18, 0.00, -0.50,  0.34, 0.26, -0.34);
        color(0.12f, 0.10f, 0.09f); drawCuboid(-0.34, 0.00,  0.10, -0.18, 0.26,  0.26);
        color(0.12f, 0.10f, 0.09f); drawCuboid( 0.18, 0.00,  0.10,  0.34, 0.26,  0.26);
    }
}
