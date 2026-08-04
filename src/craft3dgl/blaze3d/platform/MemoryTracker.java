package craft3dgl.blaze3d.platform;

import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * MC 1.14.4 MemoryTracker port - proste wrappery na LWJGL MemoryUtil.
 * W MC pelna wersja liczy zajete bajty dla debug, my mamy wersje minimalna.
 */
public class MemoryTracker {

    /** Alokuje ByteBuffer z native order (LITTLE_ENDIAN na x86). */
    public static ByteBuffer createByteBuffer(int capacity) {
        return MemoryUtil.memAlloc(capacity).order(ByteOrder.nativeOrder());
    }

    public static FloatBuffer createFloatBuffer(int capacity) {
        return MemoryUtil.memAllocFloat(capacity);
    }

    public static IntBuffer createIntBuffer(int capacity) {
        return MemoryUtil.memAllocInt(capacity);
    }

    public static void free(ByteBuffer buf) {
        MemoryUtil.memFree(buf);
    }
}
