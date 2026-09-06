package craft3dgl.entities;

import craft3dgl.DroppedItemGL;
import craft3dgl.items.ItemRegistry;
import craft3dgl.world.ChestRenderer;

import java.util.List;

import static craft3dgl.world.WorldConstants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer itemow w świecie - bloki rysowane jako miniaturki, narzędzia jako 2D lines.
 */
public final class DroppedItemRenderer {
    private DroppedItemRenderer() {}

    /** Callback do rysowania pojedynczej sciany bloku (uzywa textury atlas + MC face). */
    public interface BlockFaceDrawer {
        void drawFace(int x, int y, int z, int id, int dir);
    }

    public interface ItemTextureResolver {
        int textureFor(int itemId);
    }

    public static void drawAll(List<DroppedItemGL> drops, int textureAtlas,
                               BlockFaceDrawer faceDrawer) {
        drawAll(drops, textureAtlas, faceDrawer, null);
    }

    public static void drawAll(List<DroppedItemGL> drops, int textureAtlas,
                               BlockFaceDrawer faceDrawer, ItemTextureResolver textureResolver) {
        if (drops.isEmpty()) return;
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureAtlas);
        for (DroppedItemGL d : drops) {
            glPushMatrix();
            // Bounce animation - lekkie unoszenie sie na fali
            double bounce = Math.sin(d.age * 3.0) * 0.05;
            glTranslated(d.x, d.y + 0.16 + bounce, d.z);
            glRotated(d.age * 50.0, 0, 1, 0);
            if (ItemRegistry.isBlockItem(d.id)) {
                glScaled(0.32, 0.32, 0.32);
                glTranslated(-0.5, -0.5, -0.5);
                if (d.id == CHEST && ChestRenderer.drawItemModel()) {
                    // The tile-entity texture/model replaces the old synthetic cube.
                } else {
                    glEnable(GL_TEXTURE_2D);
                    glBindTexture(GL_TEXTURE_2D, textureAtlas);
                    glBegin(GL_QUADS);
                    for (int dir = 0; dir < 6; dir++) faceDrawer.drawFace(0, 0, 0, d.id, dir);
                    glEnd();
                }
            } else {
                int itemTexture = textureResolver == null ? -1 : textureResolver.textureFor(d.id);
                if (itemTexture > 0) drawTexturedGeneratedItem(itemTexture);
                else drawItemAs2D(d.id);
                glBindTexture(GL_TEXTURE_2D, textureAtlas);
            }
            glPopMatrix();
        }
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    /** Minecraft item/generated sprite: the real item PNG, with alpha on both sides. */
    static void drawTexturedGeneratedItem(int texture) {
        org.lwjgl.opengl.GL20.glUseProgram(0);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);
        glDisable(GL_BLEND);
        glDisable(GL_CULL_FACE);
        glBindTexture(GL_TEXTURE_2D, texture);
        glColor4f(1f, 1f, 1f, 1f);
        double size = 0.28;
        double depth = 0.012;
        glBegin(GL_QUADS);
        glTexCoord2d(0, 1); glVertex3d(-size, -size, depth);
        glTexCoord2d(1, 1); glVertex3d( size, -size, depth);
        glTexCoord2d(1, 0); glVertex3d( size,  size, depth);
        glTexCoord2d(0, 0); glVertex3d(-size,  size, depth);
        glTexCoord2d(0, 1); glVertex3d(-size, -size, -depth);
        glTexCoord2d(0, 0); glVertex3d(-size,  size, -depth);
        glTexCoord2d(1, 0); glVertex3d( size,  size, -depth);
        glTexCoord2d(1, 1); glVertex3d( size, -size, -depth);
        glEnd();
    }

    private static void drawItemAs2D(int id) {
        glDisable(GL_TEXTURE_2D);
        if (id == ITEM_STICK) glColor3f(0.55f, 0.32f, 0.15f);
        else if (id == ITEM_WOOD_PICKAXE) glColor3f(0.75f, 0.45f, 0.20f);
        else if (id == ITEM_PORK) glColor3f(0.95f, 0.38f, 0.42f);
        else if (id == ITEM_BEEF) glColor3f(0.58f, 0.12f, 0.10f);
        else if (id == ITEM_MUTTON) glColor3f(0.82f, 0.30f, 0.32f);
        else glColor3f(0.68f, 0.68f, 0.75f);
        glLineWidth(4f);
        glBegin(GL_LINES);
        glVertex3d(-0.18, -0.18, 0); glVertex3d(0.18, 0.18, 0);
        if (id != ITEM_STICK) {
            glVertex3d(-0.22, 0.16, 0); glVertex3d(0.22, 0.16, 0);
        }
        glEnd();
        glLineWidth(1f);
    }
}
