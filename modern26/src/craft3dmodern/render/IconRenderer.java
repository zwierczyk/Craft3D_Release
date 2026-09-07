package craft3dmodern.render;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import craft3dmodern.model.BlockModels;
import craft3dmodern.world.BlockIds;

public final class IconRenderer {
    private static final float[] SHADE = {0.5f, 1f, 0.8f, 0.8f, 0.6f, 0.6f};
    private static final float[] ROT;

    static {
        float[] rx = Mat4.rotateX(30);
        float[] ry = Mat4.rotateY(-45);
        ROT = Mat4.multiply(rx, ry);
    }

    private final ShaderProgram program;
    private final int vao;
    private final int vbo;
    private int atlasTex;
    private final Map<Integer, float[]> geometry = new HashMap<Integer, float[]>();

    public IconRenderer() {
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
                "  if (c.a < 0.02) discard;\n" +
                "  fragColor = vec4(c.rgb * vColor, c.a);\n" +
                "}\n";
        program = ShaderProgram.create(vs, fs);
        program.use();
        program.uniform1i("uTex", 0);
        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer empty = BufferUtils.createFloatBuffer(48);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, empty, GL15.GL_DYNAMIC_DRAW);
        int stride = 8 * 4;
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 12L);
        GL20.glEnableVertexAttribArray(2);
        GL20.glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 20L);
        GL30.glBindVertexArray(0);
    }

    public void bake(BlockModels bm, TextureAtlas atlas, int atlasTexId) throws IOException {
        this.atlasTex = atlasTexId;
        for (int id = 1; id < BlockIds.count(); id++) {
            String name = BlockIds.name(id);
            String variant = id == BlockIds.OAK_LOG ? "axis=y" : null;
            BlockModels.Model m = bm.modelFor(name, variant);
            geometry.put(id, bake(m, atlas, id));
        }
    }

    private static float[] bake(BlockModels.Model m, TextureAtlas at, int blockId) {
        int tintRgb = BlockIds.tintRgb(blockId);
        java.util.List<float[]> quads = new java.util.ArrayList<float[]>();
        for (BlockModels.Face f : m.faces) {
            TextureAtlas.Entry e = f.tex == null ? null : at.entry(f.tex);
            if (e == null) continue;
            boolean tinted = f.tintIndex >= 0 && e.gray;
            float tr = (tinted ? ((tintRgb >>> 16) & 255) / 255f : 1f);
            float tg = (tinted ? ((tintRgb >>> 8) & 255) / 255f : 1f);
            float tb = (tinted ? (tintRgb & 255) / 255f : 1f);
            float shade = SHADE[f.dir];
            float r = tr * shade;
            float g = tg * shade;
            float b = tb * shade;
            float[] q = new float[4 * 8];
            for (int i = 0; i < 4; i++) {
                float lu = f.u[i] / 16f * e.tileW;
                float lv = f.v[i] / 16f * e.tileW;
                int base = i * 8;
                q[base] = f.x[i] / 16f - 0.5f;
                q[base + 1] = f.y[i] / 16f - 0.5f;
                q[base + 2] = f.z[i] / 16f - 0.5f;
                q[base + 3] = (e.x + lu) / TextureAtlas.PAGE;
                q[base + 4] = (e.y + lv) / TextureAtlas.PAGE;
                q[base + 5] = r;
                q[base + 6] = g;
                q[base + 7] = b;
            }
            quads.add(q);
        }
        float[] out = new float[quads.size() * 6 * 8];
        int p = 0;
        for (float[] q : quads) {
            int[] order = {0, 1, 2, 0, 2, 3};
            for (int oi = 0; oi < order.length; oi++) {
                int base = order[oi] * 8;
                for (int k = 0; k < 8; k++) out[p++] = q[base + k];
            }
        }
        return out;
    }

    public void draw(int blockId, float cxYdown, float cyYdown, float cellPx, int fbw, int fbh) {
        float[] geo = geometry.get(blockId);
        if (geo == null || geo.length == 0 || atlasTex <= 0) return;
        float unitPx = Math.max(1f, cellPx * 0.5f);
        float cyGl = (fbh - cyYdown) + unitPx * 0.1765f;
        float[] m = Mat4.orthographic(0, fbw, 0, fbh, -200, 200);
        m = Mat4.multiply(m, Mat4.translate(cxYdown, cyGl, 0));
        m = Mat4.multiply(m, Mat4.scale(unitPx, unitPx, unitPx));
        m = Mat4.multiply(m, ROT);
        FloatBuffer mvp = Mat4.columnMajor(m);
        program.use();
        program.uniformMat4("uMvp", mvp);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(GL_TEXTURE_2D, atlasTex);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        FloatBuffer buf = BufferUtils.createFloatBuffer(geo.length);
        buf.put(geo).flip();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_DYNAMIC_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, geo.length / 8);
        GL30.glBindVertexArray(0);
    }

    public void dispose() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        program.dispose();
    }
}
