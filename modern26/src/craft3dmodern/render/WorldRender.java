package craft3dmodern.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Render swiata 3D: shader + VBO z meshem i atlasem tekstur.
 * Oczekuje aktywnego kontekstu GL 3.3 core.
 */
public final class WorldRender {
    private final ShaderProgram program;
    private final int vao;
    private final int vbo;
    private int vertexCount = 0;

    public WorldRender() {
        String vs =
                "#version 330 core\n" +
                "layout(location=0) in vec3 aPos;\n" +
                "layout(location=1) in vec2 aUv;\n" +
                "layout(location=2) in vec3 aColor;\n" +
                "uniform mat4 uMvp;\n" +
                "out vec2 vUv;\n" +
                "out vec3 vColor;\n" +
                "void main() {\n" +
                "  gl_Position = uMvp * vec4(aPos, 1.0);\n" +
                "  vUv = aUv;\n" +
                "  vColor = aColor;\n" +
                "}\n";
        String fs =
                "#version 330 core\n" +
                "uniform sampler2D uTex;\n" +
                "in vec2 vUv;\n" +
                "in vec3 vColor;\n" +
                "out vec4 fragColor;\n" +
                "void main() {\n" +
                "  vec4 c = texture(uTex, vUv);\n" +
                "  if (c.a < 0.15) discard;\n" + // cutout (liscie, overlay trawy)
                "  fragColor = vec4(c.rgb * vColor, 1.0);\n" +
                "}\n";
        program = ShaderProgram.create(vs, fs);
        program.use();
        program.uniform1i("uTex", 0);
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer empty = BufferUtils.createFloatBuffer(32);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, empty, GL15.GL_STATIC_DRAW);
        int stride = MeshVertex.FLOATS * 4;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * 4);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5L * 4);
        GL30.glBindVertexArray(0);
    }

    /** Format wierzcholka meshu swiata (8 floatow). */
    public static final class MeshVertex {
        public static final int FLOATS = 8;
        private MeshVertex() {}
    }

    public void upload(float[] verts) {
        vertexCount = verts.length / MeshVertex.FLOATS;
        FloatBuffer buf = BufferUtils.createFloatBuffer(verts.length);
        buf.put(verts).flip();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL30.glBindVertexArray(0);
    }

    public int vertexCount() {
        return vertexCount;
    }

    /** Rysuje mesh (wymaga uMvp kolumnowego, aktywnej tekstury na unit 0). */
    public void render(java.nio.FloatBuffer mvpColMajor) {
        program.use();
        program.uniformMat4("uMvp", mvpColMajor);
        GL30.glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        GL30.glBindVertexArray(0);
    }

    public void dispose() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        program.dispose();
    }
}
