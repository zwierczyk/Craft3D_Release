package craft3dgl.blaze3d.vertex;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

/**
 * MC 1.14.4 BufferUploader port.
 *
 * Bierze skonczony BufferBuilder (po .end()) i draw'uje na ekran.
 * Uzywa tymczasowego CPU-side buffer (nie VBO) - dla "immediate" render.
 * Jest to odpowiednik glBegin/glEnd - ale przez VertexFormat.
 *
 * Uzywa fixed-function pipeline (glVertexPointer + glDrawArrays) - zeby dzialal
 * bez shaderow. Jak dodamy shader pipeline, to VertexBuffer + shader zastapi to.
 */
public class BufferUploader {

    public static void end(BufferBuilder builder) {
        if (builder.getVertexCount() > 0) {
            VertexFormat fmt = builder.getVertexFormat();
            ByteBuffer buf = builder.getBuffer();

            // Setup pointerow z CPU buffer (base offset = adres buf)
            int stride = fmt.getVertexSize();
            for (int i = 0; i < fmt.getElementCount(); i++) {
                VertexFormatElement el = fmt.getElement(i);
                int offset = fmt.getOffset(i);
                int glType = el.getType().getGlType();
                int count = el.getCount();
                buf.position(offset);
                java.nio.ByteBuffer view = buf.slice();
                view.order(buf.order());

                switch (el.getUsage()) {
                    case POSITION:
                        glVertexPointer(count, glType, stride, view);
                        glEnableClientState(GL_VERTEX_ARRAY);
                        break;
                    case NORMAL:
                        glNormalPointer(glType, stride, view);
                        glEnableClientState(GL_NORMAL_ARRAY);
                        break;
                    case COLOR:
                        glColorPointer(count, glType, stride, view);
                        glEnableClientState(GL_COLOR_ARRAY);
                        break;
                    case UV:
                        // KRYTYCZNE: glClientActiveTexture dla per-vertex array pointer!
                        org.lwjgl.opengl.GL13.glClientActiveTexture(
                            craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0 + el.getIndex());
                        glTexCoordPointer(count, glType, stride, view);
                        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
                        org.lwjgl.opengl.GL13.glClientActiveTexture(
                            craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0);
                        break;
                    default:
                        break;
                }
                buf.position(0);
            }

            // Draw
            glDrawArrays(builder.getDrawMode(), 0, builder.getVertexCount());

            // Cleanup
            for (int i = 0; i < fmt.getElementCount(); i++) {
                VertexFormatElement el = fmt.getElement(i);
                switch (el.getUsage()) {
                    case POSITION: glDisableClientState(GL_VERTEX_ARRAY); break;
                    case NORMAL: glDisableClientState(GL_NORMAL_ARRAY); break;
                    case COLOR:
                        glDisableClientState(GL_COLOR_ARRAY);
                        glColor4f(1, 1, 1, 1);
                        break;
                    case UV:
                        org.lwjgl.opengl.GL13.glClientActiveTexture(
                            craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0 + el.getIndex());
                        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
                        org.lwjgl.opengl.GL13.glClientActiveTexture(
                            craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0);
                        break;
                    default: break;
                }
            }
        }
        builder.clear();
    }
}
