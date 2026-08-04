package craft3dgl.blaze3d.shaders;

import craft3dgl.blaze3d.math.Matrix4f;
import craft3dgl.blaze3d.platform.GLX;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * MC 1.14.4 Uniform port 1:1.
 * Reprezentuje uniform w shaderze (int/float/vec2/vec3/vec4/mat2/mat3/mat4).
 */
public class Uniform extends AbstractUniform implements AutoCloseable {
    private int location;
    private final int count;
    private final int type;
    private final IntBuffer intValues;
    private final FloatBuffer floatValues;
    private final String name;
    private boolean dirty;
    private final Effect parent;

    public Uniform(String name, int type, int count, Effect parent) {
        this.name = name;
        this.count = count;
        this.type = type;
        this.parent = parent;
        if (type <= 3) {
            this.intValues = MemoryUtil.memAllocInt(count);
            this.floatValues = null;
        } else {
            this.intValues = null;
            this.floatValues = MemoryUtil.memAllocFloat(count);
        }
        this.location = -1;
        this.markDirty();
    }

    @Override
    public void close() {
        if (this.intValues != null) MemoryUtil.memFree(this.intValues);
        if (this.floatValues != null) MemoryUtil.memFree(this.floatValues);
    }

    private void markDirty() {
        this.dirty = true;
        if (this.parent != null) this.parent.markDirty();
    }

    /**
     * Konwertuje nazwe typu z JSON na int.
     * "int"->0, "float"->4, "matrix4x4"->10, itd.
     */
    public static int getTypeFromString(String s) {
        int i = -1;
        if ("int".equals(s)) i = 0;
        else if ("float".equals(s)) i = 4;
        else if (s.startsWith("matrix")) {
            if (s.endsWith("2x2")) i = 8;
            else if (s.endsWith("3x3")) i = 9;
            else if (s.endsWith("4x4")) i = 10;
        }
        return i;
    }

    public void setLocation(int i) { this.location = i; }
    public String getName() { return this.name; }

    @Override
    public void set(float f) {
        this.floatValues.position(0);
        this.floatValues.put(0, f);
        this.markDirty();
    }

    @Override
    public void set(float f, float g) {
        this.floatValues.position(0);
        this.floatValues.put(0, f);
        this.floatValues.put(1, g);
        this.markDirty();
    }

    @Override
    public void set(float f, float g, float h) {
        this.floatValues.position(0);
        this.floatValues.put(0, f);
        this.floatValues.put(1, g);
        this.floatValues.put(2, h);
        this.markDirty();
    }

    @Override
    public void set(float f, float g, float h, float i) {
        this.floatValues.position(0);
        this.floatValues.put(f);
        this.floatValues.put(g);
        this.floatValues.put(h);
        this.floatValues.put(i);
        this.floatValues.flip();
        this.markDirty();
    }

    @Override
    public void setSafe(float f, float g, float h, float i) {
        this.floatValues.position(0);
        if (this.type >= 4) this.floatValues.put(0, f);
        if (this.type >= 5) this.floatValues.put(1, g);
        if (this.type >= 6) this.floatValues.put(2, h);
        if (this.type >= 7) this.floatValues.put(3, i);
        this.markDirty();
    }

    @Override
    public void setSafe(int i, int j, int k, int l) {
        this.intValues.position(0);
        if (this.type >= 0) this.intValues.put(0, i);
        if (this.type >= 1) this.intValues.put(1, j);
        if (this.type >= 2) this.intValues.put(2, k);
        if (this.type >= 3) this.intValues.put(3, l);
        this.markDirty();
    }

    @Override
    public void set(float[] fs) {
        if (fs.length < this.count) {
            System.err.println("[Uniform] set called with too-small array (expected " + this.count + ", got " + fs.length + ")");
        } else {
            this.floatValues.position(0);
            this.floatValues.put(fs);
            this.floatValues.position(0);
            this.markDirty();
        }
    }

    @Override
    public void set(Matrix4f m) {
        this.floatValues.position(0);
        m.store(this.floatValues);
        this.markDirty();
    }

    public void upload() {
        if (!this.dirty) {
            // NOTE: MC ma pusty if - upload ZAWSZE (chyba bug w MC, ale zachowujemy 1:1)
        }
        this.dirty = false;
        if (this.type <= 3) {
            this.uploadAsInteger();
        } else if (this.type <= 7) {
            this.uploadAsFloat();
        } else {
            if (this.type > 10) {
                System.err.println("[Uniform] upload: invalid type " + this.type);
                return;
            }
            this.uploadAsMatrix();
        }
    }

    private void uploadAsInteger() {
        if (this.floatValues != null) this.floatValues.clear();
        switch (this.type) {
            case 0: GLX.glUniform1(this.location, this.intValues); break;
            case 1: GLX.glUniform2(this.location, this.intValues); break;
            case 2: GLX.glUniform3(this.location, this.intValues); break;
            case 3: GLX.glUniform4(this.location, this.intValues); break;
            default: System.err.println("[Uniform] int upload: bad count " + this.count);
        }
    }

    private void uploadAsFloat() {
        this.floatValues.clear();
        switch (this.type) {
            case 4: GLX.glUniform1(this.location, this.floatValues); break;
            case 5: GLX.glUniform2(this.location, this.floatValues); break;
            case 6: GLX.glUniform3(this.location, this.floatValues); break;
            case 7: GLX.glUniform4(this.location, this.floatValues); break;
            default: System.err.println("[Uniform] float upload: bad count " + this.count);
        }
    }

    private void uploadAsMatrix() {
        this.floatValues.clear();
        switch (this.type) {
            case 8: GLX.glUniformMatrix2(this.location, false, this.floatValues); break;
            case 9: GLX.glUniformMatrix3(this.location, false, this.floatValues); break;
            case 10: GLX.glUniformMatrix4(this.location, false, this.floatValues); break;
        }
    }
}
