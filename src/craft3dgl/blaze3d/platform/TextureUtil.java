package craft3dgl.blaze3d.platform;

import org.lwjgl.system.MemoryUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 * MC 1.14.4 TextureUtil port - tylko funkcje potrzebne dla Program.compileShader():
 *  - readResource(InputStream): ByteBuffer
 *  - readResourceAsString(InputStream): String
 */
public class TextureUtil {

    private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

    /** MC helper: nowa tekstura OpenGL, zwraca ID. */
    public static int generateTextureId() {
        return GlStateManager.genTexture();
    }

    /** MC helper: zwolnij teksture. */
    public static void releaseTextureId(int id) {
        GlStateManager.deleteTexture(id);
    }


    public static ByteBuffer readResource(InputStream inputStream) throws IOException {
        ByteBuffer buf;
        if (inputStream instanceof FileInputStream) {
            FileInputStream fis = (FileInputStream) inputStream;
            FileChannel ch = fis.getChannel();
            buf = MemoryUtil.memAlloc((int) ch.size() + 1);
            while (ch.read(buf) != -1) { }
        } else {
            buf = MemoryUtil.memAlloc(DEFAULT_IMAGE_BUFFER_SIZE);
            ReadableByteChannel ch = Channels.newChannel(inputStream);
            while (ch.read(buf) != -1) {
                if (buf.remaining() == 0) {
                    buf = MemoryUtil.memRealloc(buf, buf.capacity() * 2);
                }
            }
        }
        return buf;
    }

    public static String readResourceAsString(InputStream inputStream) {
        ByteBuffer buf = null;
        try {
            buf = readResource(inputStream);
            int pos = buf.position();
            buf.rewind();
            return MemoryUtil.memASCII(buf, pos);
        } catch (IOException ignored) {
        } finally {
            if (buf != null) MemoryUtil.memFree(buf);
        }
        return null;
    }
}
