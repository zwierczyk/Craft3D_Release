package craft3dmodern.render;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public final class CloudRenderer {
    public static final double CLOUD_Y = 84.0;
    public static final double HALF = 1024.0;
    public static final double CELL = 512.0;

    private final ShaderProgram program;
    private final int vao;
    private final int vbo;
    private final int tex;

    public CloudRenderer() throws IOException {
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
                "uniform vec3 uTint;\n" +
                "in vec2 vUv;\n" +
                "out vec4 fragColor;\n" +
                "void main() {\n" +
                "  vec4 c = texture(uTex, vUv);\n" +
                "  if (c.a < 0.05) discard;\n" +
                "  fragColor = vec4(c.rgb * uTint, c.a);\n" +
                "}\n";
        program = ShaderProgram.create(vs, fs);
        program.use();
        program.uniform1i("uTex", 0);
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer empty = BufferUtils.createFloatBuffer(30);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, empty, GL15.GL_DYNAMIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, 20, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL_FLOAT, false, 20, 12L);
        GL30.glBindVertexArray(0);

        BufferedImage img = Texture.decode("minecraft/textures/environment/clouds.png");
        int w = img.getWidth();
        int h = img.getHeight();
        ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                pixels.put((byte) ((argb >>> 16) & 255));
                pixels.put((byte) ((argb >>> 8) & 255));
                pixels.put((byte) (argb & 255));
                pixels.put((byte) ((argb >>> 24) & 255));
            }
        }
        pixels.flip();
        tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    }

    public void draw(FloatBuffer mvp, double ex, double ey, double ez,
                     double dayF, long ticks) {
        double drift = (ticks / 20.0 * 0.6) % CELL;
        double du = drift / CELL;
        double u0 = (ex - HALF) / CELL + du;
        double u1 = (ex + HALF) / CELL + du;
        double v0 = (ez - HALF) / CELL;
        double v1 = (ez + HALF) / CELL;
        double c0 = (float) CLOUD_Y - ey;
        float[] verts = {
            (float) (ex - HALF), (float) (ey + c0), (float) (ez - HALF), (float) u0, (float) v0,
            (float) (ex + HALF), (float) (ey + c0), (float) (ez - HALF), (float) u1, (float) v0,
            (float) (ex + HALF), (float) (ey + c0), (float) (ez + HALF), (float) u1, (float) v1,
            (float) (ex - HALF), (float) (ey + c0), (float) (ez - HALF), (float) u0, (float) v0,
            (float) (ex + HALF), (float) (ey + c0), (float) (ez + HALF), (float) u1, (float) v1,
            (float) (ex - HALF), (float) (ey + c0), (float) (ez + HALF), (float) u0, (float) v1,
        };
        float tint = (float) (0.5 + 0.5 * dayF);
        program.use();
        program.uniformMat4("uMvp", mvp);
        org.lwjgl.opengl.GL20.glUniform3f(program.uniform("uTint"), tint, tint, tint);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tex);
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
        org.lwjgl.opengl.GL11.glDeleteTextures(tex);
        program.dispose();
    }
}
