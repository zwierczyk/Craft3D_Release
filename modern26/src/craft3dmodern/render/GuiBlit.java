package craft3dmodern.render;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Rysowanie teksturowanych czworokatow GUI we wspolrzednych pikselowych.
 * Shader: uklad lewy-gorny (0,0), mnozenie przez uniform uTint (rgba).
 */
public final class GuiBlit {
    private final int vao;
    private final int vbo;
    private final ShaderProgram program;
    private int whiteTexture;
    private int screenW = 1;
    private int screenH = 1;
    private final float[] quad = new float[6 * 4];

    public GuiBlit() {
        String vs =
                "#version 330 core\n" +
                "layout(location=0) in vec2 aPos;\n" +
                "layout(location=1) in vec2 aUv;\n" +
                "uniform vec2 uScreen;\n" +
                "out vec2 vUv;\n" +
                "void main() {\n" +
                "  vUv = aUv;\n" +
                "  vec2 ndc = vec2(aPos.x / uScreen.x * 2.0 - 1.0, 1.0 - aPos.y / uScreen.y * 2.0);\n" +
                "  gl_Position = vec4(ndc, 0.0, 1.0);\n" +
                "}\n";
        String fs =
                "#version 330 core\n" +
                "uniform sampler2D uTex;\n" +
                "uniform vec4 uTint;\n" +
                "in vec2 vUv;\n" +
                "out vec4 fragColor;\n" +
                "void main() { fragColor = texture(uTex, vUv) * uTint; }\n";
        program = ShaderProgram.create(vs, fs);
        program.use();
        program.uniform1i("uTex", 0);
        glUniform4f(program.uniform("uTint"), 1f, 1f, 1f, 1f);
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer empty = BufferUtils.createFloatBuffer(24);
        glBufferData(GL_ARRAY_BUFFER, empty, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8L);
        glBindVertexArray(0);

        BufferedImage white = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        white.setRGB(0, 0, 0xffffffff);
        whiteTexture = Texture.upload(white, false);
    }

    public void setScreenSize(int w, int h) {
        this.screenW = Math.max(1, w);
        this.screenH = Math.max(1, h);
    }

    public void begin() {
        program.use();
        ShaderProgram.uniform2f(program, "uScreen", screenW, screenH);
        // GUI = blending, bez glebi i cullingu
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public int whiteTexture() {
        return whiteTexture;
    }

    private void setTint(float r, float g, float b, float a) {
        glUniform4f(program.uniform("uTint"), r, g, b, a);
    }

    public void draw(int textureId, float x, float y, float w, float h,
                     float u0, float v0, float u1, float v1) {
        drawTinted(textureId, x, y, w, h, u0, v0, u1, v1, 1f, 1f, 1f, 1f);
    }

    /** Kolor prostokata (bez tekstury). */
    public void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        if (w <= 0 || h <= 0) return;
        drawTinted(whiteTexture, x, y, w, h, 0, 0, 1, 1, r, g, b, a);
    }

    public void drawTinted(int textureId, float x, float y, float w, float h,
                           float u0, float v0, float u1, float v1,
                           float r, float g, float b, float a) {
        if (textureId <= 0 || w <= 0 || h <= 0) return;
        setTint(r, g, b, a);
        float x1 = x + w;
        float y1 = y + h;
        int i = 0;
        quad[i++] = x;  quad[i++] = y;  quad[i++] = u0; quad[i++] = v0;
        quad[i++] = x1; quad[i++] = y;  quad[i++] = u1; quad[i++] = v0;
        quad[i++] = x1; quad[i++] = y1; quad[i++] = u1; quad[i++] = v1;
        quad[i++] = x;  quad[i++] = y;  quad[i++] = u0; quad[i++] = v0;
        quad[i++] = x1; quad[i++] = y1; quad[i++] = u1; quad[i++] = v1;
        quad[i++] = x;  quad[i++] = y1; quad[i++] = u0; quad[i++] = v1;

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        FloatBuffer buf = BufferUtils.createFloatBuffer(quad.length);
        buf.put(quad).flip();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
        setTint(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        program.dispose();
    }
}
