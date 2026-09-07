package craft3dmodern.render;

import java.io.IOException;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class CelestialRenderer {
    private static final String[] MOON_NAMES = {
        "full_moon", "waning_gibbous", "third_quarter", "waning_crescent",
        "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"
    };

    private final ShaderProgram program;
    private final int vao;
    private final int vbo;
    public int sunTex;
    public final int[] moonTex = new int[MOON_NAMES.length];

    public CelestialRenderer() {
        String vs =
                "#version 330 core\n" +
                "layout(location=0) in vec3 aPos;\n" +
                "layout(location=1) in vec2 aUv;\n" +
                "uniform mat4 uMvp;\n" +
                "out vec2 vUv;\n" +
                "void main() {\n" +
                "  gl_Position = uMvp * vec4(aPos, 1.0);\n" +
                "  vUv = aUv;\n" +
                "}\n";
        String fs =
                "#version 330 core\n" +
                "uniform sampler2D uTex;\n" +
                "in vec2 vUv;\n" +
                "out vec4 fragColor;\n" +
                "void main() {\n" +
                "  fragColor = texture(uTex, vUv);\n" +
                "}\n";
        program = ShaderProgram.create(vs, fs);
        program.use();
        program.uniform1i("uTex", 0);
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer empty = BufferUtils.createFloatBuffer(36);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, empty, GL15.GL_DYNAMIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL_FLOAT, false, 20, 12L);
        GL30.glBindVertexArray(0);
    }

    public void loadTextures() throws IOException {
        sunTex = Texture.load("minecraft/textures/environment/celestial/sun.png", false);
        for (int i = 0; i < MOON_NAMES.length; i++) {
            moonTex[i] = Texture.load("minecraft/textures/environment/celestial/moon/"
                    + MOON_NAMES[i] + ".png", false);
        }
    }

    public int moonPhase(int day) {
        return ((day % 8) + 8) % 8;
    }

    public void draw(FloatBuffer mvp, int tex, float cx, float cy, float cz,
                     float rx, float ry, float rz, float halfW,
                     float ux, float uy, float uz, float halfH) {
        program.use();
        program.uniformMat4("uMvp", mvp);
        float[] verts = new float[30];
        float[] c = {cx, cy, cz};
        float[] r = {rx * halfW, ry * halfW, rz * halfW};
        float[] u = {ux * halfH, uy * halfH, uz * halfH};
        float[][] corners = {
            {-1, -1}, {1, -1}, {1, 1}, {-1, 1}
        };
        float[][] uv = {
            {0, 0}, {1, 0}, {1, 1}, {0, 1}
        };
        int[] tri = {0, 1, 2, 0, 2, 3};
        int p = 0;
        for (int t = 0; t < tri.length; t++) {
            float[] q = corners[tri[t]];
            verts[p++] = c[0] + r[0] * q[0] + u[0] * q[1];
            verts[p++] = c[1] + r[1] * q[0] + u[1] * q[1];
            verts[p++] = c[2] + r[2] * q[0] + u[2] * q[1];
            verts[p++] = uv[tri[t]][0];
            verts[p++] = uv[tri[t]][1];
        }
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, tex);
        FloatBuffer buf = BufferUtils.createFloatBuffer(verts.length);
        buf.put(verts).flip();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    public void dispose() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        program.dispose();
        Texture.dispose(sunTex);
        for (int t : moonTex) Texture.dispose(t);
    }
}
