package craft3dgl.entities;

import craft3dgl.items.ItemRegistry;

import static craft3dgl.ui.CuboidHelper.color;
import static craft3dgl.ui.CuboidHelper.drawCuboid;
import static craft3dgl.world.WorldConstants.*;
import static org.lwjgl.opengl.GL11.*;

public final class PlayerRenderer {
    private PlayerRenderer() {}

    public interface BlockFaceDrawer {
        void drawFace(int x, int y, int z, int id, int dir);
    }

    public static void drawPlayerModel(double x, double y, double z, double yaw, double pitch,
            double walkPhase, double swingTimer,
            int held, int heldCount,
            int textureAtlas, BlockFaceDrawer faceDrawer) {
        glDisable(GL_TEXTURE_2D);
        glPushMatrix();
        glTranslated(x, y, z);
        glRotated(Math.toDegrees(yaw), 0, 1, 0);
        double legSwing = Math.sin(walkPhase) * 35.0;
        double armSwing = Math.sin(walkPhase) * 22.0;
        double swing = swingTimer;
        glPushMatrix();
        glTranslated(-0.135, 0.78, 0);
        glRotated(-legSwing, 1, 0, 0);
        glTranslated(0.135, -0.78, 0);
        color(0.18f, 0.26f, 0.52f); drawCuboid(-0.235, 0.10, -0.14, -0.045, 0.78, 0.14);
        color(0.12f, 0.18f, 0.40f); drawCuboid(-0.225, 0.42, 0.141, -0.075, 0.62, 0.146);
        color(0.06f, 0.06f, 0.08f); drawCuboid(-0.245, 0.00, -0.20, -0.035, 0.12, 0.18);
        color(0.85f, 0.85f, 0.85f); drawCuboid(-0.20, 0.06, 0.181, -0.08, 0.08, 0.182);
        glPopMatrix();
        glPushMatrix();
        glTranslated(0.135, 0.78, 0);
        glRotated(legSwing, 1, 0, 0);
        glTranslated(-0.135, -0.78, 0);
        color(0.18f, 0.26f, 0.52f); drawCuboid(0.045, 0.10, -0.14, 0.235, 0.78, 0.14);
        color(0.12f, 0.18f, 0.40f); drawCuboid(0.075, 0.42, 0.141, 0.225, 0.62, 0.146);
        color(0.06f, 0.06f, 0.08f); drawCuboid(0.035, 0.00, -0.20, 0.245, 0.12, 0.18);
        color(0.85f, 0.85f, 0.85f); drawCuboid(0.08, 0.06, 0.181, 0.20, 0.08, 0.182);
        glPopMatrix();
        color(0.78f, 0.18f, 0.20f); drawCuboid(-0.34, 0.78, -0.18, 0.34, 1.45, 0.18);
        color(0.92f, 0.92f, 0.94f);
        drawCuboid(-0.341, 1.05, -0.181, 0.341, 1.10, 0.181);
        drawCuboid(-0.341, 1.20, -0.181, 0.341, 1.25, 0.181);
        drawCuboid(-0.341, 1.35, -0.181, 0.341, 1.40, 0.181);
        color(0.95f, 0.95f, 0.20f);
        drawCuboid(-0.025, 1.30, 0.181, 0.025, 1.34, 0.183);
        drawCuboid(-0.025, 1.15, 0.181, 0.025, 1.19, 0.183);
        color(0.20f, 0.10f, 0.05f); drawCuboid(-0.345, 0.74, -0.185, 0.345, 0.82, 0.185);
        color(0.85f, 0.75f, 0.20f); drawCuboid(-0.04, 0.755, 0.186, 0.04, 0.805, 0.187);
        glPushMatrix();
        glTranslated(-0.46, 1.42, 0);
        glRotated(armSwing, 1, 0, 0);
        glTranslated(0.46, -1.42, 0);
        color(0.78f, 0.18f, 0.20f); drawCuboid(-0.56, 1.20, -0.14, -0.36, 1.45, 0.14);
        color(0.62f, 0.10f, 0.12f); drawCuboid(-0.561, 1.20, -0.141, -0.359, 1.22, 0.141);
        color(0.92f, 0.72f, 0.52f); drawCuboid(-0.56, 0.78, -0.14, -0.36, 1.20, 0.14);
        color(0.86f, 0.65f, 0.45f); drawCuboid(-0.56, 0.66, -0.14, -0.36, 0.80, 0.14);
        glPopMatrix();
        glPushMatrix();
        glTranslated(0.46, 1.42, 0);
        glRotated(-armSwing, 1, 0, 0);
        if (swing > 0) {
            double t = 1.0 - swing;
            double swingAng = Math.sin(t * Math.PI) * 80.0;
            glRotated(-swingAng, 1, 0, 0);
            glRotated(Math.sin(t * Math.PI) * 15.0, 0, 0, 1);
        }
        glTranslated(-0.46, -1.42, 0);
        color(0.78f, 0.18f, 0.20f); drawCuboid(0.36, 1.20, -0.14, 0.56, 1.45, 0.14);
        color(0.62f, 0.10f, 0.12f); drawCuboid(0.359, 1.20, -0.141, 0.561, 1.22, 0.141);
        color(0.92f, 0.72f, 0.52f); drawCuboid(0.36, 0.78, -0.14, 0.56, 1.20, 0.14);
        color(0.86f, 0.65f, 0.45f); drawCuboid(0.36, 0.66, -0.14, 0.56, 0.80, 0.14);

        if (held > 0 && heldCount > 0) {
            glPushMatrix();
            glTranslated(0.46, 0.73, 0.05);
            if (ItemRegistry.isBlockItem(held)) {
                glRotated(20, 1, 0, 0);
                glRotated(15, 0, 1, 0);
                glEnable(GL_TEXTURE_2D);
                glBindTexture(GL_TEXTURE_2D, textureAtlas);
                glScaled(0.25, 0.25, 0.25);
                glTranslated(-0.5, 0, -0.5);
                glBegin(GL_QUADS);
                for (int dir = 0; dir < 6; dir++) faceDrawer.drawFace(0, 0, 0, held, dir);
                glEnd();
                glDisable(GL_TEXTURE_2D);
            } else if (ItemRegistry.toolCategory(held) > 0) {
                glRotated(-15, 1, 0, 0);
                glRotated(50, 0, 0, 1);
                glScaled(0.55, 0.55, 0.55);
                drawToolModel3D(held);
            } else if (held == ITEM_STICK) {
                glRotated(-15, 1, 0, 0);
                glRotated(45, 0, 0, 1);
                color(0.55f, 0.32f, 0.15f);
                drawCuboid(-0.018, -0.05, -0.025, 0.018, 0.30, 0.025);
            } else if (held == ITEM_PORK || held == ITEM_BEEF || held == ITEM_MUTTON) {
                if (held == ITEM_BEEF) color(0.58f, 0.12f, 0.10f);
                else if (held == ITEM_MUTTON) color(0.82f, 0.30f, 0.32f);
                else color(0.95f, 0.38f, 0.42f);
                drawCuboid(-0.08, -0.04, -0.05, 0.08, 0.10, 0.07);
            } else if (held == ITEM_BREAD) {
                color(0.76f, 0.48f, 0.18f);
                drawCuboid(-0.08, -0.04, -0.05, 0.08, 0.08, 0.07);
                color(0.95f, 0.70f, 0.32f);
                drawCuboid(-0.06, 0.00, -0.051, 0.06, 0.06, -0.049);
            } else if (held == ITEM_SEEDS) {
                color(0.5f, 0.7f, 0.2f);
                drawCuboid(-0.05, -0.04, -0.04, 0.05, 0.0, 0.04);
            } else if (held == ITEM_WHEAT) {
                color(0.85f, 0.65f, 0.20f);
                drawCuboid(-0.04, -0.04, -0.02, -0.02, 0.16, 0.0);
                drawCuboid(0.0, -0.04, -0.02, 0.02, 0.16, 0.0);
                drawCuboid(0.02, -0.04, -0.02, 0.04, 0.16, 0.0);
                color(0.95f, 0.78f, 0.30f);
                drawCuboid(-0.05, 0.14, -0.025, 0.05, 0.20, 0.005);
            } else if (held == ITEM_EMERALD) {
                color(0.10f, 0.85f, 0.45f);
                drawCuboid(-0.04, -0.02, -0.03, 0.04, 0.10, 0.03);
                color(0.55f, 1.00f, 0.72f);
                drawCuboid(-0.02, 0.02, 0.031, 0.02, 0.07, 0.033);
            }
            glPopMatrix();
        }
        glPopMatrix();
        glPushMatrix();
        glTranslated(0, 1.65, 0);
        glRotated(-Math.toDegrees(pitch), 1, 0, 0);
        glTranslated(0, -1.65, 0);
        color(0.92f, 0.72f, 0.52f); drawCuboid(-0.30, 1.45, -0.28, 0.30, 2.05, 0.28);
        color(0.45f, 0.28f, 0.10f); drawCuboid(-0.31, 1.88, -0.29, 0.31, 2.07, 0.29);
        color(0.40f, 0.24f, 0.08f); drawCuboid(-0.31, 1.75, -0.29, -0.18, 1.92, 0.29);
        color(0.40f, 0.24f, 0.08f); drawCuboid(0.18, 1.75, -0.29, 0.31, 1.92, 0.29);
        color(0.45f, 0.28f, 0.10f); drawCuboid(-0.31, 1.45, -0.285, 0.31, 1.95, -0.27);
        color(0.95f, 0.95f, 0.95f); drawCuboid(-0.20, 1.70, 0.281, -0.08, 1.80, 0.282);
        color(0.95f, 0.95f, 0.95f); drawCuboid( 0.08, 1.70, 0.281,  0.20, 1.80, 0.282);
        color(0.10f, 0.30f, 0.65f); drawCuboid(-0.17, 1.72, 0.283, -0.11, 1.78, 0.284);
        color(0.10f, 0.30f, 0.65f); drawCuboid( 0.11, 1.72, 0.283,  0.17, 1.78, 0.284);
        color(0.30f, 0.18f, 0.05f); drawCuboid(-0.21, 1.81, 0.282, -0.07, 1.84, 0.283);
        color(0.30f, 0.18f, 0.05f); drawCuboid( 0.07, 1.81, 0.282,  0.21, 1.84, 0.283);
        color(0.85f, 0.62f, 0.42f); drawCuboid(-0.04, 1.60, 0.281, 0.04, 1.70, 0.295);
        color(0.55f, 0.20f, 0.18f); drawCuboid(-0.09, 1.52, 0.282, 0.09, 1.55, 0.283);
        color(0.86f, 0.65f, 0.45f); drawCuboid(-0.32, 1.62, -0.10, -0.28, 1.78, 0.10);
        color(0.86f, 0.65f, 0.45f); drawCuboid( 0.28, 1.62, -0.10,  0.32, 1.78, 0.10);
        glPopMatrix();
        glPopMatrix();
        glEnable(GL_TEXTURE_2D);
        glColor3f(1,1,1);
    }

    public static void drawHandArmModel() {
        glDisable(GL_TEXTURE_2D);
        glColor3f(0.78f, 0.18f, 0.20f);
        drawCuboid(-0.15, 0.0, -0.07, 0.15, 0.32, 0.32);
        glColor3f(0.62f, 0.10f, 0.12f);
        drawCuboid(-0.151, 0.30, -0.071, 0.151, 0.32, 0.321);
        glColor3f(0.92f, 0.72f, 0.52f);
        drawCuboid(-0.13, -0.25, -0.06, 0.13, 0.0, 0.30);
        glColor3f(0.86f, 0.65f, 0.45f);
        drawCuboid(-0.13, -0.36, -0.06, 0.13, -0.25, 0.30);
        glColor3f(0.65f, 0.45f, 0.30f);
        drawCuboid(-0.06, -0.36, 0.301, -0.04, -0.27, 0.302);
        drawCuboid(-0.02, -0.36, 0.301, 0.00, -0.27, 0.302);
        drawCuboid( 0.04, -0.36, 0.301,  0.06, -0.27, 0.302);
        drawCuboid( 0.08, -0.36, 0.301,  0.10, -0.27, 0.302);
    }

    public static void drawToolModel3D(int item) {
        int tCat = ItemRegistry.toolCategory(item);
        int tier = ItemRegistry.toolTier(item);
        if (tCat == 0 || tier == 0) return;
        glDisable(GL_TEXTURE_2D);
        glColor3f(0.55f, 0.32f, 0.15f);
        drawCuboid(-0.025, -0.05, -0.025, 0.025, 0.50, 0.025);
        glColor3f(0.30f, 0.18f, 0.05f);
        drawCuboid(-0.026, 0.30, -0.026, 0.026, 0.36, 0.026);
        if (tier == 2) glColor3f(0.65f, 0.65f, 0.70f);
        else glColor3f(0.78f, 0.55f, 0.28f);
        if (tCat == 1) {
            drawCuboid(-0.22, 0.50, -0.04, 0.22, 0.58, 0.04);
            drawCuboid(-0.10, 0.46, -0.05, 0.10, 0.62, 0.05);
            if (tier == 2) { glColor3f(0.85f, 0.85f, 0.88f); drawCuboid(-0.21, 0.501, -0.039, 0.21, 0.52, 0.039); }
            else { glColor3f(0.92f, 0.70f, 0.40f); drawCuboid(-0.21, 0.501, -0.039, 0.21, 0.52, 0.039); }
        } else if (tCat == 2) {
            drawCuboid(-0.10, 0.45, -0.05, 0.10, 0.66, 0.05);
            if (tier == 2) glColor3f(0.85f, 0.85f, 0.88f); else glColor3f(0.92f, 0.70f, 0.40f);
            drawCuboid(-0.099, 0.65, -0.051, 0.099, 0.66, 0.051);
        } else if (tCat == 3) {
            drawCuboid(0.04, 0.46, -0.05, 0.22, 0.60, 0.05);
            drawCuboid(0.04, 0.48, -0.06, 0.18, 0.58, 0.06);
            if (tier == 2) glColor3f(0.85f, 0.85f, 0.88f); else glColor3f(0.92f, 0.70f, 0.40f);
            drawCuboid(0.20, 0.461, -0.04, 0.225, 0.599, 0.04);
        } else if (tCat == 4) {
            drawCuboid(-0.025, 0.45, -0.025, 0.025, 0.85, 0.025);
            if (tier == 2) glColor3f(0.85f, 0.85f, 0.88f); else glColor3f(0.92f, 0.70f, 0.40f);
            drawCuboid(-0.024, 0.451, -0.024, 0.024, 0.849, 0.024);
            glColor3f(0.40f, 0.25f, 0.10f);
            drawCuboid(-0.12, 0.42, -0.03, 0.12, 0.46, 0.03);
        } else if (tCat == 5) {
            // Motyka 3D - poziomy kafelek + nasada
            drawCuboid(-0.18, 0.50, -0.04, 0.18, 0.58, 0.04);
            drawCuboid(0.06, 0.50, -0.05, 0.18, 0.62, 0.05);
            if (tier == 2) glColor3f(0.85f, 0.85f, 0.88f); else glColor3f(0.92f, 0.70f, 0.40f);
            drawCuboid(-0.17, 0.501, -0.039, 0.17, 0.51, 0.039);
        }
    }
}
