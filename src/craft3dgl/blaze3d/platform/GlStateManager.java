package craft3dgl.blaze3d.platform;

import craft3dgl.blaze3d.math.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * MC 1.14.4 GlStateManager port - uproszczony.
 * MC wersja robi state tracking (nie wysyla GL call jesli stan sie nie zmienil).
 * My robimy PROSTY pass-through do GL11/GL14 - bez cache, bez trackingu.
 * 
 * WSZYSTKIE metody musza byc SAFE do wywolania nawet gdy uzywamy legacy renderu
 * (glBegin/glEnd) - bo klasy MC (RenderTarget) beda uzywac GlStateManager rownolegle
 * z naszym starym kodem.
 */
public class GlStateManager {

    // === ENUMS ===
    public static enum SourceFactor {
        ZERO(0), ONE(1), SRC_COLOR(768), ONE_MINUS_SRC_COLOR(769),
        DST_COLOR(774), ONE_MINUS_DST_COLOR(775),
        SRC_ALPHA(770), ONE_MINUS_SRC_ALPHA(771),
        DST_ALPHA(772), ONE_MINUS_DST_ALPHA(773),
        CONSTANT_COLOR(32769), ONE_MINUS_CONSTANT_COLOR(32770),
        CONSTANT_ALPHA(32771), ONE_MINUS_CONSTANT_ALPHA(32772),
        SRC_ALPHA_SATURATE(776);
        public final int value;
        SourceFactor(int v) { this.value = v; }
    }

    public static enum DestFactor {
        ZERO(0), ONE(1), SRC_COLOR(768), ONE_MINUS_SRC_COLOR(769),
        DST_COLOR(774), ONE_MINUS_DST_COLOR(775),
        SRC_ALPHA(770), ONE_MINUS_SRC_ALPHA(771),
        DST_ALPHA(772), ONE_MINUS_DST_ALPHA(773),
        CONSTANT_COLOR(32769), ONE_MINUS_CONSTANT_COLOR(32770),
        CONSTANT_ALPHA(32771), ONE_MINUS_CONSTANT_ALPHA(32772);
        public final int value;
        DestFactor(int v) { this.value = v; }
    }

    public static enum FogMode {
        LINEAR(9729), EXP(2048), EXP2(2049);
        public final int value;
        FogMode(int v) { this.value = v; }
    }

    public static enum CullFace {
        FRONT(1028), BACK(1029), FRONT_AND_BACK(1032);
        public final int value;
        CullFace(int v) { this.value = v; }
    }

    public static enum LogicOp {
        AND(5377), OR(5383), XOR(5382), COPY(5379), CLEAR(5376);
        public final int value;
        LogicOp(int v) { this.value = v; }
    }

    public static enum TexGen { S, T, R, Q; }

    public static enum Profile {
        DEFAULT { public void apply() {} public void clean() {} },
        PLAYER_SKIN {
            public void apply() {
                enableBlend();
                blendFuncSeparate(770, 771, 1, 0);
            }
            public void clean() {
                disableBlend();
            }
        },
        TRANSPARENT_MODEL {
            public void apply() {
                color4f(1, 1, 1, 0.15f);
                depthMask(false);
                enableBlend();
                blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
                alphaFunc(516, 0.003921569f);
            }
            public void clean() {
                disableBlend();
                alphaFunc(516, 0.1f);
                depthMask(true);
            }
        };
        public abstract void apply();
        public abstract void clean();
    }

    // === ALPHA ===
    public static void enableAlphaTest() { GL11.glEnable(GL11.GL_ALPHA_TEST); }
    public static void disableAlphaTest() { GL11.glDisable(GL11.GL_ALPHA_TEST); }
    public static void alphaFunc(int i, float f) { GL11.glAlphaFunc(i, f); }

    // === LIGHTING ===
    public static void enableLighting() { GL11.glEnable(GL11.GL_LIGHTING); }
    public static void disableLighting() { GL11.glDisable(GL11.GL_LIGHTING); }
    public static void enableLight(int i) { GL11.glEnable(16384 + i); }
    public static void disableLight(int i) { GL11.glDisable(16384 + i); }
    public static void enableColorMaterial() { GL11.glEnable(GL11.GL_COLOR_MATERIAL); }
    public static void disableColorMaterial() { GL11.glDisable(GL11.GL_COLOR_MATERIAL); }
    public static void colorMaterial(int face, int mode) { GL11.glColorMaterial(face, mode); }
    public static void light(int i, int j, FloatBuffer b) { GL11.glLightfv(i, j, b); }
    public static void lightModel(int i, FloatBuffer b) { GL11.glLightModelfv(i, b); }
    public static void normal3f(float f, float g, float h) { GL11.glNormal3f(f, g, h); }

    // === DEPTH ===
    public static void enableDepthTest() { GL11.glEnable(GL11.GL_DEPTH_TEST); }
    public static void disableDepthTest() { GL11.glDisable(GL11.GL_DEPTH_TEST); }
    public static void depthFunc(int i) { GL11.glDepthFunc(i); }
    public static void depthMask(boolean b) { GL11.glDepthMask(b); }

    // === BLEND ===
    public static void enableBlend() { GL11.glEnable(GL11.GL_BLEND); }
    public static void disableBlend() { GL11.glDisable(GL11.GL_BLEND); }
    public static void blendFunc(SourceFactor sf, DestFactor df) { GL11.glBlendFunc(sf.value, df.value); }
    public static void blendFunc(int sf, int df) { GL11.glBlendFunc(sf, df); }
    public static void blendFuncSeparate(SourceFactor sf, DestFactor df, SourceFactor sf2, DestFactor df2) {
        GLX.glBlendFuncSeparate(sf.value, df.value, sf2.value, df2.value);
    }
    public static void blendFuncSeparate(int i, int j, int k, int l) {
        GLX.glBlendFuncSeparate(i, j, k, l);
    }
    public static void blendEquation(int i) { GL14.glBlendEquation(i); }

    // === FOG ===
    public static void enableFog() { GL11.glEnable(GL11.GL_FOG); }
    public static void disableFog() { GL11.glDisable(GL11.GL_FOG); }
    public static void fogMode(FogMode m) { GL11.glFogi(2917, m.value); }
    public static void fogDensity(float f) { GL11.glFogf(2914, f); }
    public static void fogStart(float f) { GL11.glFogf(2915, f); }
    public static void fogEnd(float f) { GL11.glFogf(2916, f); }
    public static void fog(int i, FloatBuffer b) { GL11.glFogfv(i, b); }
    public static void fogi(int i, int j) { GL11.glFogi(i, j); }

    // === CULL ===
    public static void enableCull() { GL11.glEnable(GL11.GL_CULL_FACE); }
    public static void disableCull() { GL11.glDisable(GL11.GL_CULL_FACE); }
    public static void cullFace(CullFace c) { GL11.glCullFace(c.value); }
    public static void polygonMode(int i, int j) { GL11.glPolygonMode(i, j); }

    // === POLYGON OFFSET ===
    public static void enablePolygonOffset() { GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL); }
    public static void disablePolygonOffset() { GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL); }
    public static void enableLineOffset() { GL11.glEnable(10754); }
    public static void disableLineOffset() { GL11.glDisable(10754); }
    public static void polygonOffset(float f, float g) { GL11.glPolygonOffset(f, g); }

    // === COLOR LOGIC ===
    public static void enableColorLogicOp() { GL11.glEnable(3058); }
    public static void disableColorLogicOp() { GL11.glDisable(3058); }
    public static void logicOp(LogicOp o) { GL11.glLogicOp(o.value); }
    public static void logicOp(int i) { GL11.glLogicOp(i); }

    // === TEXTURES ===
    public static void activeTexture(int i) { GLX.glActiveTexture(i); }
    public static void enableTexture() { GL11.glEnable(GL11.GL_TEXTURE_2D); }
    public static void disableTexture() { GL11.glDisable(GL11.GL_TEXTURE_2D); }
    public static void texEnv(int i, int j, FloatBuffer b) { GL11.glTexEnvfv(i, j, b); }
    public static void texEnv(int i, int j, int k) { GL11.glTexEnvi(i, j, k); }
    public static void texEnv(int i, int j, float f) { GL11.glTexEnvf(i, j, f); }
    public static void texParameter(int i, int j, float f) { GL11.glTexParameterf(i, j, f); }
    public static void texParameter(int i, int j, int k) { GL11.glTexParameteri(i, j, k); }
    public static int getTexLevelParameter(int i, int j, int k) { return GL11.glGetTexLevelParameteri(i, j, k); }
    public static int genTexture() { return GL11.glGenTextures(); }
    public static void deleteTexture(int i) { GL11.glDeleteTextures(i); }
    public static void bindTexture(int i) { GL11.glBindTexture(GL11.GL_TEXTURE_2D, i); }
    public static void texImage2D(int i, int j, int k, int l, int m, int n, int o, int p, IntBuffer b) {
        GL11.glTexImage2D(i, j, k, l, m, n, o, p, b);
    }
    public static void texSubImage2D(int i, int j, int k, int l, int m, int n, int o, int p, long q) {
        GL11.glTexSubImage2D(i, j, k, l, m, n, o, p, q);
    }
    public static void copyTexSubImage2D(int i, int j, int k, int l, int m, int n, int o, int p) {
        GL11.glCopyTexSubImage2D(i, j, k, l, m, n, o, p);
    }

    // === NORMALIZE / SHADE ===
    public static void enableNormalize() { GL11.glEnable(2977); }
    public static void disableNormalize() { GL11.glDisable(2977); }
    public static void shadeModel(int i) { GL11.glShadeModel(i); }
    public static void enableRescaleNormal() { GL11.glEnable(32826); }
    public static void disableRescaleNormal() { GL11.glDisable(32826); }

    // === VIEWPORT ===
    public static void viewport(int x, int y, int w, int h) { GL11.glViewport(x, y, w, h); }

    // === COLOR MASK ===
    public static void colorMask(boolean r, boolean g, boolean b, boolean a) { GL11.glColorMask(r, g, b, a); }

    // === STENCIL ===
    public static void stencilFunc(int i, int j, int k) { GL11.glStencilFunc(i, j, k); }
    public static void stencilMask(int i) { GL11.glStencilMask(i); }
    public static void stencilOp(int i, int j, int k) { GL11.glStencilOp(i, j, k); }

    // === CLEAR ===
    public static void clearDepth(double d) { GL11.glClearDepth(d); }
    public static void clearColor(float f, float g, float h, float i) { GL11.glClearColor(f, g, h, i); }
    public static void clearStencil(int i) { GL11.glClearStencil(i); }
    public static void clear(int mask, boolean errorCheck) {
        GL11.glClear(mask);
        if (errorCheck) GL11.glGetError();
    }

    // === MATRIX ===
    public static void matrixMode(int i) { GL11.glMatrixMode(i); }
    public static void loadIdentity() { GL11.glLoadIdentity(); }
    public static void pushMatrix() { GL11.glPushMatrix(); }
    public static void popMatrix() { GL11.glPopMatrix(); }
    public static void getMatrix(int i, FloatBuffer b) { GL11.glGetFloatv(i, b); }
    public static void ortho(double a, double b, double c, double d, double e, double f) {
        GL11.glOrtho(a, b, c, d, e, f);
    }
    public static void rotatef(float f, float g, float h, float i) { GL11.glRotatef(f, g, h, i); }
    public static void rotated(double f, double g, double h, double i) { GL11.glRotated(f, g, h, i); }
    public static void scalef(float f, float g, float h) { GL11.glScalef(f, g, h); }
    public static void scaled(double f, double g, double h) { GL11.glScaled(f, g, h); }
    public static void translatef(float f, float g, float h) { GL11.glTranslatef(f, g, h); }
    public static void translated(double f, double g, double h) { GL11.glTranslated(f, g, h); }
    public static void multMatrix(FloatBuffer b) { GL11.glMultMatrixf(b); }

    // === COLOR ===
    public static void color4f(float f, float g, float h, float i) { GL11.glColor4f(f, g, h, i); }
    public static void color3f(float f, float g, float h) { GL11.glColor3f(f, g, h); }
    public static void texCoord2f(float f, float g) { GL11.glTexCoord2f(f, g); }
    public static void vertex3f(float f, float g, float h) { GL11.glVertex3f(f, g, h); }
    public static void clearCurrentColor() { GL11.glColor4f(1, 1, 1, 1); }

    // === VERTEX POINTERS ===
    public static void vertexPointer(int i, int j, int k, int l) { GL11.glVertexPointer(i, j, k, l); }
    public static void vertexPointer(int i, int j, int k, ByteBuffer b) { GL11.glVertexPointer(i, j, k, b); }
    public static void colorPointer(int i, int j, int k, int l) { GL11.glColorPointer(i, j, k, l); }
    public static void colorPointer(int i, int j, int k, ByteBuffer b) { GL11.glColorPointer(i, j, k, b); }
    public static void normalPointer(int i, int j, int k) { GL11.glNormalPointer(i, j, k); }
    public static void normalPointer(int i, int j, ByteBuffer b) { GL11.glNormalPointer(i, j, b); }
    public static void texCoordPointer(int i, int j, int k, int l) { GL11.glTexCoordPointer(i, j, k, l); }
    public static void texCoordPointer(int i, int j, int k, ByteBuffer b) { GL11.glTexCoordPointer(i, j, k, b); }
    public static void enableClientState(int i) { GL11.glEnableClientState(i); }
    public static void disableClientState(int i) { GL11.glDisableClientState(i); }

    // === IMMEDIATE MODE (compat) ===
    public static void begin(int i) { GL11.glBegin(i); }
    public static void end() { GL11.glEnd(); }
    public static void drawArrays(int i, int j, int k) { GL11.glDrawArrays(i, j, k); }
    public static void lineWidth(float f) { GL11.glLineWidth(f); }

    // === DISPLAY LISTS ===
    public static int genLists(int i) { return GL11.glGenLists(i); }
    public static void newList(int i, int j) { GL11.glNewList(i, j); }
    public static void endList() { GL11.glEndList(); }
    public static void callList(int i) { GL11.glCallList(i); }
    public static void deleteLists(int i, int j) { GL11.glDeleteLists(i, j); }

    // === PIXEL ===
    public static void pixelStore(int i, int j) { GL11.glPixelStorei(i, j); }
    public static void readPixels(int i, int j, int k, int l, int m, int n, ByteBuffer b) {
        GL11.glReadPixels(i, j, k, l, m, n, b);
    }

    // === ERROR / STRING ===
    public static int getError() { return GL11.glGetError(); }
    public static String getString(int i) { return GL11.glGetString(i); }
    public static int getInteger(int i) { return GL11.glGetInteger(i); }
    public static void getInteger(int i, IntBuffer b) { GL11.glGetIntegerv(i, b); }

    // === PROFILES ===
    public static void setProfile(Profile p) { p.apply(); }
    public static void unsetProfile(Profile p) { p.clean(); }

    // === MATRIX4F ===
    private static final FloatBuffer MATRIX_BUFFER = MemoryTracker.createFloatBuffer(16);
    public static Matrix4f getMatrix4f(int i) {
        GL11.glGetFloatv(i, MATRIX_BUFFER);
        MATRIX_BUFFER.rewind();
        Matrix4f m = new Matrix4f();
        m.load(MATRIX_BUFFER);
        MATRIX_BUFFER.rewind();
        return m;
    }
    public static void multMatrix(Matrix4f m) {
        m.store(MATRIX_BUFFER);
        MATRIX_BUFFER.rewind();
        GL11.glMultMatrixf(MATRIX_BUFFER);
    }

    // === ATTRIB PUSH/POP ===
    public static void pushLightingAttributes() { GL11.glPushAttrib(8256); }
    public static void pushTextureAttributes() { GL11.glPushAttrib(270336); }
    public static void popAttributes() { GL11.glPopAttrib(); }
}
