package craft3dgl.blaze3d.vertex;

/**
 * MC 1.14.4 Tesselator port 1:1.
 * Singleton owner BufferBuildera + BufferUploadera.
 *
 * Uzycie:
 *   Tesselator tess = Tesselator.getInstance();
 *   BufferBuilder bb = tess.getBuilder();
 *   bb.begin(GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
 *   bb.vertex(x, y, z).color(r, g, b, a).endVertex();
 *   ...
 *   tess.end();   // uploaduje + draw
 */
public class Tesselator {
    private final BufferBuilder builder;
    private final BufferUploader uploader = new BufferUploader();
    private static final Tesselator INSTANCE = new Tesselator(2097152); // 2MB init

    public static Tesselator getInstance() {
        return INSTANCE;
    }

    public Tesselator(int capacity) {
        this.builder = new BufferBuilder(capacity);
    }

    public void end() {
        this.builder.end();
        this.uploader.end(this.builder);
    }

    public BufferBuilder getBuilder() {
        return this.builder;
    }
}
