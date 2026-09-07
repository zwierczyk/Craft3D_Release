package craft3dmodern.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class OutlineRenderer {
    private final ShaderProgram program;
    private final int vao;
    private final int vbo;

    public OutlineRenderer() {
        String vs =
                "#version 330 core\n" +
                "layout(location=0) in vec3 aPos;\n" +
                "uniform mat4 uMvp;\n" +
                "void main() {\n" +
                "  gl_Position = uMvp * vec4(aPos, 1.0);\n" +
                "}\n";
        String fs =
                "#version 330 core\n" +
                "uniform vec4 uColor;\n" +
                "out vec4 fragColor;\n" +
                "void main() {\n" +
                "  fragColor = uColor;\n" +
                "}\n";
        program = ShaderProgram.create(vs, fs);
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer empty = BufferUtils.createFloatBuffer(72);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, empty, GL15.GL_DYNAMIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, 12, 0L);
        GL30.glBindVertexArray(0);
    }

    public void draw(FloatBuffer mvp, int bx, int by, int bz) {
        float x0 = bx - 0.003f;
        float y0 = by - 0.003f;
        float z0 = bz - 0.003f;
        float x1 = bx + 1.003f;
        float y1 = by + 1.003f;
        float z1 = bz + 1.003f;
        float[] v = {
            x0, y0, z0, x1, y0, z0,
            x0, y1, z0, x1, y1, z0,
            x0, y0, z1, x1, y0, z1,
            x0, y1, z1, x1, y1, z1,
            x0, y0, z0, x0, y1, z0,
            x1, y0, z0, x1, y1, z0,
            x0, y0, z1, x0, y1, z1,
            x1, y0, z1, x1, y1, z1,
            x0, y0, z0, x0, y0, z1,
            x1, y0, z0, x1, y0, z1,
            x0, y1, z0, x0, y1, z1,
            x1, y1, z0, x1, y1, z1,
        };
        program.use();
        program.uniformMat4("uMvp", mvp);
        org.lwjgl.opengl.GL20.glUniform4f(program.uniform("uColor"), 0.1f, 0.1f, 0.1f, 0.75f);
        FloatBuffer buf = BufferUtils.createFloatBuffer(v.length);
        buf.put(v).flip();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        glDrawArrays(GL_LINES, 0, 24);
        GL30.glBindVertexArray(0);
    }

    public void dispose() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        program.dispose();
    }
}
