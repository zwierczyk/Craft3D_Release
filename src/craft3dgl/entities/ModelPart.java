package craft3dgl.entities;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Lekki port ModelRenderer/ModelBox z Minecraft 1.12 (MCP 9.40).
 * Pozycje, rozmiary i punkty obrotu sa podawane w pikselach modelu, zas
 * rotacje w radianach. Jedna czesc moze zawierac wiele boxow z roznymi UV
 * (np. glowa i ryj swini albo glowa i rogi krowy).
 */
public class ModelPart {
    public float x, y, z;
    public float xRot, yRot, zRot;
    public boolean mirror = false;
    public boolean visible = true;
    public final int texWidth, texHeight;

    private int textureU, textureV;
    private final List<Box> boxes = new ArrayList<>();

    private static final class Box {
        final int u, v;
        final float x, y, z;
        final int w, h, d;
        final float inflate;
        final boolean mirror;

        Box(int u, int v, float x, float y, float z,
            int w, int h, int d, float inflate, boolean mirror) {
            this.u = u;
            this.v = v;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.d = d;
            this.inflate = inflate;
            this.mirror = mirror;
        }
    }

    public ModelPart(int texWidth, int texHeight, int u, int v) {
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.textureU = u;
        this.textureV = v;
    }

    /** Odpowiednik ModelRenderer.setTextureOffset. */
    public ModelPart setTextureOffset(int u, int v) {
        this.textureU = u;
        this.textureV = v;
        return this;
    }

    /** Odpowiednik ModelRenderer.addBox; nie usuwa wczesniej dodanych boxow. */
    public ModelPart addBox(float ox, float oy, float oz, int w, int h, int d, float inflate) {
        boxes.add(new Box(textureU, textureV, ox, oy, oz, w, h, d, inflate, mirror));
        return this;
    }

    public ModelPart addBox(float ox, float oy, float oz, int w, int h, int d) {
        return addBox(ox, oy, oz, w, h, d, 0f);
    }

    public void setPos(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void copyFrom(ModelPart other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.xRot = other.xRot;
        this.yRot = other.yRot;
        this.zRot = other.zRot;
    }

    /** Odpowiednik ModelRenderer.render(scale). */
    public void render(float scale) {
        if (!visible || boxes.isEmpty()) return;
        glPushMatrix();
        translateTo(scale);
        for (Box box : boxes) drawBox(box, scale);
        glPopMatrix();
    }

    /** Odpowiednik ModelRenderer.postRender/translateAndRotate. */
    public void translateTo(float scale) {
        glTranslatef(x * scale, y * scale, z * scale);
        if (zRot != 0) glRotatef((float)Math.toDegrees(zRot), 0, 0, 1);
        if (yRot != 0) glRotatef((float)Math.toDegrees(yRot), 0, 1, 0);
        if (xRot != 0) glRotatef((float)Math.toDegrees(xRot), 1, 0, 0);
    }

    /** Dokladny uklad wierzcholkow i UV konstruktora ModelBox z MCP 9.40. */
    private void drawBox(Box box, float scale) {
        float x0 = box.x - box.inflate;
        float y0 = box.y - box.inflate;
        float z0 = box.z - box.inflate;
        float x1 = box.x + box.w + box.inflate;
        float y1 = box.y + box.h + box.inflate;
        float z1 = box.z + box.d + box.inflate;
        if (box.mirror) {
            float swap = x1;
            x1 = x0;
            x0 = swap;
        }

        float[] p0 = point(x0, y0, z0, scale);
        float[] p1 = point(x1, y0, z0, scale);
        float[] p2 = point(x1, y1, z0, scale);
        float[] p3 = point(x0, y1, z0, scale);
        float[] p4 = point(x0, y0, z1, scale);
        float[] p5 = point(x1, y0, z1, scale);
        float[] p6 = point(x1, y1, z1, scale);
        float[] p7 = point(x0, y1, z1, scale);

        int u = box.u, v = box.v;
        int w = box.w, h = box.h, d = box.d;
        // Kolejnosc odpowiada ModelBox.quadList[0..5].
        mcQuad(u + d + w, v + d, u + d + w + d, v + d + h, p5, p1, p2, p6);
        mcQuad(u,         v + d, u + d,         v + d + h, p0, p4, p7, p3);
        mcQuad(u + d,     v,     u + d + w,     v + d,     p5, p4, p0, p1);
        mcQuad(u + d + w, v + d, u + d + w + w, v,         p2, p3, p7, p6);
        mcQuad(u + d,     v + d, u + d + w,     v + d + h, p1, p0, p3, p2);
        mcQuad(u + d + w + d, v + d, u + d + w + d + w, v + d + h,
                p4, p5, p6, p7);
    }

    private static float[] point(float x, float y, float z, float scale) {
        return new float[]{x * scale, y * scale, z * scale};
    }

    /** TexturedQuad: p0=(u2,v1), p1=(u1,v1), p2=(u1,v2), p3=(u2,v2). */
    private void mcQuad(int u1, int v1, int u2, int v2,
                        float[] p0, float[] p1, float[] p2, float[] p3) {
        float fu1 = u1 / (float)texWidth;
        float fv1 = v1 / (float)texHeight;
        float fu2 = u2 / (float)texWidth;
        float fv2 = v2 / (float)texHeight;
        glBegin(GL_QUADS);
        vertex(p0, fu2, fv1);
        vertex(p1, fu1, fv1);
        vertex(p2, fu1, fv2);
        vertex(p3, fu2, fv2);
        glEnd();
    }

    private static void vertex(float[] p, float u, float v) {
        glTexCoord2f(u, v);
        glVertex3f(p[0], p[1], p[2]);
    }
}
