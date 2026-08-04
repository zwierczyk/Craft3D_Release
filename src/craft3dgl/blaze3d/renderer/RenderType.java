package craft3dgl.blaze3d.renderer;

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import craft3dgl.blaze3d.vertex.VertexFormat;
import craft3dgl.blaze3d.vertex.BufferBuilder;

/**
 * MC 1.14.4-style RenderType - kombinacja VertexFormat + tryb rysowania (GL_QUADS itd)
 * + lista RenderStateShardow ktore ustawia/czysci OpenGL state.
 *
 * Uzycie:
 *   RenderType type = RenderTypes.SOLID;
 *   type.setupRenderState();
 *   BufferBuilder bb = ...;
 *   bb.begin(type.mode(), type.format());
 *   // ... rysujemy
 *   bb.end();
 *   type.clearRenderState();
 */
public class RenderType {
    private final String name;
    private final VertexFormat format;
    private final int mode;                // GL11.GL_QUADS / GL_TRIANGLES / GL_LINES
    private final int bufferSize;          // sugerowana pojemnosc BufferBuildera
    private final boolean affectsCrumbling;// czy layer ma dostawac shard breaking
    private final boolean sortOnUpload;    // sortowanie back-to-front dla translucent
    private final List<RenderStateShard> shards;

    public RenderType(String name, VertexFormat format, int mode, int bufferSize,
                      boolean affectsCrumbling, boolean sortOnUpload,
                      List<RenderStateShard> shards) {
        this.name = name;
        this.format = format;
        this.mode = mode;
        this.bufferSize = bufferSize;
        this.affectsCrumbling = affectsCrumbling;
        this.sortOnUpload = sortOnUpload;
        this.shards = shards;
    }

    public String name() { return name; }
    public VertexFormat format() { return format; }
    public int mode() { return mode; }
    public int bufferSize() { return bufferSize; }
    public boolean affectsCrumbling() { return affectsCrumbling; }
    public boolean sortOnUpload() { return sortOnUpload; }
    public List<RenderStateShard> shards() { return shards; }

    public void setupRenderState() {
        for (RenderStateShard s : shards) s.setupRenderState();
    }

    public void clearRenderState() {
        // czyscimy w odwrotnej kolejnosci - jak stos
        for (int i = shards.size() - 1; i >= 0; i--) shards.get(i).clearRenderState();
    }

    /** Rysuje buforek w tym RenderType (setup -> draw -> clear). */
    public void draw(BufferBuilder builder) {
        setupRenderState();
        builder.end();
        // BufferUploader tu by mial byc dla VBO, na razie legacy immediate mode
        craft3dgl.blaze3d.vertex.BufferUploader.end(builder);
        clearRenderState();
    }

    @Override public String toString() { return "RenderType[" + name + "]"; }

    // ================ BUILDER ================
    public static Builder builder(String name, VertexFormat format, int mode, int bufferSize) {
        return new Builder(name, format, mode, bufferSize);
    }

    public static class Builder {
        private final String name;
        private final VertexFormat format;
        private final int mode;
        private final int bufferSize;
        private boolean affectsCrumbling = false;
        private boolean sortOnUpload = false;
        private final List<RenderStateShard> shards = new ArrayList<>();

        public Builder(String name, VertexFormat format, int mode, int bufferSize) {
            this.name = name; this.format = format; this.mode = mode; this.bufferSize = bufferSize;
        }
        public Builder shard(RenderStateShard s) { this.shards.add(s); return this; }
        public Builder crumbling(boolean b) { this.affectsCrumbling = b; return this; }
        public Builder sort(boolean b) { this.sortOnUpload = b; return this; }
        public RenderType build() {
            return new RenderType(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, shards);
        }
    }
}
