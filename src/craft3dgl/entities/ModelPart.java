package craft3dgl.entities;

import static org.lwjgl.opengl.GL11.*;

/**
 * Port MC ModelPart 1:1. Reprezentuje jedna czesc modelu z:
 *  - pozycja (x, y, z) w pixelach skorki
 *  - rotacja (xRot, yRot, zRot) w RADIANACH
 *  - box (offset + rozmiar w pixelach skorki)
 *  - UV origin (u, v) w pixelach tekstury
 *  - mirror flag (dla lewej strony)
 *  - visible flag
 *
 * Renderowanie: uzywa MC-style transform:
 *   translate(x, y, z) * (1/16)
 *   rotate ZYX  (kolejnosc: X, Y, Z w OpenGL czyli glRotate wolane w kolejnosci Z, Y, X)
 *   drawBox()
 */
public class ModelPart {
    public float x, y, z;
    public float xRot, yRot, zRot;
    public boolean mirror = false;
    public boolean visible = true;
    public int texWidth = 64, texHeight = 32;

    private int u, v;
    private float boxX, boxY, boxZ, boxW, boxH, boxD, expand;
    private boolean hasBox = false;

    public ModelPart(int texWidth, int texHeight, int u, int v) {
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.u = u;
        this.v = v;
    }

    public ModelPart addBox(float ox, float oy, float oz, int w, int h, int d, float expand) {
        this.boxX = ox; this.boxY = oy; this.boxZ = oz;
        this.boxW = w; this.boxH = h; this.boxD = d;
        this.expand = expand;
        this.hasBox = true;
        return this;
    }

    public ModelPart addBox(float ox, float oy, float oz, int w, int h, int d) {
        return addBox(ox, oy, oz, w, h, d, 0f);
    }

    public void setPos(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }

    /**
     * Copy rotation from other part (uzywane dla hat=head).
     */
    public void copyFrom(ModelPart other) {
        this.x = other.x; this.y = other.y; this.z = other.z;
        this.xRot = other.xRot; this.yRot = other.yRot; this.zRot = other.zRot;
    }

    /**
     * Render tej czesci. Scale = 0.0625 (1/16) - konwersja pixeli na bloki.
     */
    public void render(float scale) {
        if (!visible) return;
        if (!hasBox) return;

        glPushMatrix();
        // MC ModelRenderer.render() kolejnosc:
        //   translate(x*scale, y*scale, z*scale)
        //   if (zRot != 0) rotate(zRot * 180/PI, 0, 0, 1)
        //   if (yRot != 0) rotate(yRot * 180/PI, 0, 1, 0)
        //   if (xRot != 0) rotate(xRot * 180/PI, 1, 0, 0)
        glTranslatef(x * scale, y * scale, z * scale);
        if (zRot != 0) glRotatef((float)Math.toDegrees(zRot), 0, 0, 1);
        if (yRot != 0) glRotatef((float)Math.toDegrees(yRot), 0, 1, 0);
        if (xRot != 0) glRotatef((float)Math.toDegrees(xRot), 1, 0, 0);

        drawBox(scale);
        glPopMatrix();
    }

    /** Translate to this part's position (dla ItemInHandLayer.translateToHand). */
    public void translateTo(float scale) {
        // MC: translateAndRotate w ModelRenderer
        glTranslatef(x * scale, y * scale, z * scale);
        if (zRot != 0) glRotatef((float)Math.toDegrees(zRot), 0, 0, 1);
        if (yRot != 0) glRotatef((float)Math.toDegrees(yRot), 0, 1, 0);
        if (xRot != 0) glRotatef((float)Math.toDegrees(xRot), 1, 0, 0);
    }

    /** MC ModelBox rendering. Box od (boxX, boxY, boxZ) o rozmiarze (w,h,d) pixeli. */
    private void drawBox(float scale) {
        float e = expand;
        // WAZNE: MC ma Y+ w dol wewnatrz modelu. Ale to jest juz uwzglednione
        // przez LivingEntityRenderer.setupScale() ktory robi glScalef(-1, -1, 1).
        // Wiec tutaj rysujemy w NATYWNYM MC-space: Y+ w dol, X+ w prawo, Z+ do przodu (twarz).
        float x0 = (boxX - e) * scale;
        float y0 = (boxY - e) * scale;
        float z0 = (boxZ - e) * scale;
        float x1 = (boxX + boxW + e) * scale;
        float y1 = (boxY + boxH + e) * scale;
        float z1 = (boxZ + boxD + e) * scale;

        int w = (int) boxW;
        int h = (int) boxH;
        int d = (int) boxD;

        // UV layout MC:
        //   TOP:    (u+d, v)         w x d
        //   BOTTOM: (u+d+w, v)       w x d
        //   RIGHT:  (u, v+d)         d x h    (west, -X face)
        //   FRONT:  (u+d, v+d)       w x h    (north, +Z face w MC = twarz)
        //   LEFT:   (u+d+w, v+d)     d x h    (east, +X face)
        //   BACK:   (u+d+w+d, v+d)   w x h    (south, -Z face)

        float T_u = 1f / texWidth;
        float T_v = 1f / texHeight;

        int uTop   = u + d,       vTop = v;
        int uBot   = u + d + w,   vBot = v;
        int uRight = u,           vSide = v + d;
        int uFront = u + d;
        int uLeft  = u + d + w;
        int uBack  = u + d + w + d;

        // TOP - Y-, patrzac od +Y w dol
        uvQuad(uTop, vTop, w, d, T_u, T_v, mirror,
               x0, y0, z1,  x1, y0, z1,  x1, y0, z0,  x0, y0, z0);
        // BOTTOM - Y+, patrzac od -Y w gore (UV odbite w Y)
        uvQuadFlipV(uBot, vBot, w, d, T_u, T_v, mirror,
               x0, y1, z0,  x1, y1, z0,  x1, y1, z1,  x0, y1, z1);
        // FRONT (+Z) - twarz Steve'a
        uvQuad(uFront, vSide, w, h, T_u, T_v, mirror,
               x0, y0, z1,  x1, y0, z1,  x1, y1, z1,  x0, y1, z1);
        // BACK (-Z)
        uvQuad(uBack, vSide, w, h, T_u, T_v, mirror,
               x1, y0, z0,  x0, y0, z0,  x0, y1, z0,  x1, y1, z0);
        // RIGHT (-X = na lewo Steve'a naszego)
        uvQuad(mirror ? uLeft : uRight, vSide, d, h, T_u, T_v, false,
               x0, y0, z0,  x0, y0, z1,  x0, y1, z1,  x0, y1, z0);
        // LEFT (+X = na prawo Steve'a naszego)
        uvQuad(mirror ? uRight : uLeft, vSide, d, h, T_u, T_v, false,
               x1, y0, z1,  x1, y0, z0,  x1, y1, z0,  x1, y1, z1);
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

    private static void uvQuadFlipV(int u, int v, int tw, int th, float Tu, float Tv, boolean flipU,
                                    float x1, float y1, float z1,
                                    float x2, float y2, float z2,
                                    float x3, float y3, float z3,
                                    float x4, float y4, float z4) {
        float u0 = u * Tu, u1 = (u + tw) * Tu;
        float v0 = v * Tv, v1 = (v + th) * Tv;
        if (flipU) { float t = u0; u0 = u1; u1 = t; }
        glBegin(GL_QUADS);
        glTexCoord2f(u0, v1); glVertex3f(x1, y1, z1);
        glTexCoord2f(u1, v1); glVertex3f(x2, y2, z2);
        glTexCoord2f(u1, v0); glVertex3f(x3, y3, z3);
        glTexCoord2f(u0, v0); glVertex3f(x4, y4, z4);
        glEnd();
    }
}
