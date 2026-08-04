package craft3dgl.blaze3d.vertex;

import craft3dgl.blaze3d.platform.GLX;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * MC 1.14.4 VertexBuffer port 1:1.
 *
 * Wrapper na VBO (Vertex Buffer Object).
 * Upload once, draw many times.
 *
 * Uzycie:
 *   VertexBuffer vbo = new VertexBuffer(DefaultVertexFormat.BLOCK);
 *   // build BufferBuilder z wierzchol.kami, end()
 *   vbo.upload(bufferBuilder.getBuffer());
 *   // potem per klatka:
 *   vbo.bind();
 *   format.setupBufferState(0);
 *   vbo.draw(GL_QUADS);
 *   format.clearBufferState();
 *   VertexBuffer.unbind();
 */
public class VertexBuffer {
    private int id;
    private final VertexFormat format;
    private int vertexCount;

    public VertexBuffer(VertexFormat format) {
        this.format = format;
        this.id = GLX.glGenBuffers();
    }

    public void bind() {
        GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, this.id);
    }

    public void upload(ByteBuffer byteBuffer) {
        this.bind();
        GLX.glBufferData(GLX.GL_ARRAY_BUFFER, byteBuffer, GLX.GL_STATIC_DRAW);
        unbind();
        this.vertexCount = byteBuffer.limit() / this.format.getVertexSize();
    }

    public void draw(int mode) {
        glDrawArrays(mode, 0, this.vertexCount);
    }

    public static void unbind() {
        GLX.glBindBuffer(GLX.GL_ARRAY_BUFFER, 0);
    }

    public void delete() {
        if (this.id >= 0) {
            GLX.glDeleteBuffers(this.id);
            this.id = -1;
        }
    }

    public VertexFormat getFormat() { return this.format; }
    public int getVertexCount() { return this.vertexCount; }
    public int getId() { return this.id; }
}
