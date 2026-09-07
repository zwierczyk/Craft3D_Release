package craft3dmodern.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Rysowanie teksturowanych czworokatow GUI we wspolrzednych pikselowych
 * (poczatek ukladu: lewy-gorny rog okna) - fundament pod GuiGraphics.
 */
public final class GuiBlit {
    private final int vao;
    private final int vbo;
    private final ShaderProgram program;
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
                "in vec2 vUv;\n" +
                "out vec4 fragColor;\n" +
                "void main() { fragColor = texture(uTex, vUv); }\n";
        program = ShaderProgram.create(vs, fs);
        program.use();
        program.uniform1i("uTex", 0);
        vao = glGenVertexArrays();
        vbo = org.lwjgl.opengl.GL15.glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer empty = BufferUtils.createFloatBuffer(24);
        glBufferData(GL_ARRAY_BUFFER, empty, GL_DYNAMIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8L);
        glBindVertexArray(0);
    }

    public void setScreenSize(int w, int h) {
        this.screenW = Math.max(1, w);
        this.screenH = Math.max(1, h);
    }

    public void begin() {
        program.use();
        ShaderProgram.uniform2f(program, "uScreen", screenW, screenH);
    }

    /** Rysuje teksture w pikselach; uv w jednostkach tekstury (0..1). */
    public void draw(int textureId, float x, float y, float w, float h,
                     float u0, float v0, float u1, float v1) {
        if (textureId <= 0 || w <= 0 || h <= 0) return;
        float x1 = x + w;
        float y1 = y + h;
        int i = 0;
        quad[i++] = x;  quad[i++] = y;  quad[i++] = u0; quad[i++] = v0;
        quad[i++] = x1; quad[i++] = y;  quad[i++] = u1; quad[i++] = v0;
        quad[i++] = x1; quad[i++] = y1; quad[i++] = u1; quad[i++] = v1;
        quad[i++] = x;  quad[i++] = y;  quad[i++] = u0; quad[i++] = v0;
        quad[i++] = x1; quad[i++] = y1; quad[i++] = u1; quad[i++] = v1;
        quad[i++] = x;  quad[i++] = y1; quad[i++] = u0; quad[i++] = v1;

        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, textureId);
        FloatBuffer buf = BufferUtils.createFloatBuffer(quad.length);
        buf.put(quad).flip();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_DYNAMIC_DRAW);
        org.lwjgl.opengl.GL11.glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }

    public void dispose() {
        program.dispose();
    }
}
