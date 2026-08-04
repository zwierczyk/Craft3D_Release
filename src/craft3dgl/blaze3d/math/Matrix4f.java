package craft3dgl.blaze3d.math;

import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * MC 1.14.4 Matrix4f port 1:1.
 *
 * KOLEJNOSC PAMIECI: column-major (jak OpenGL).
 * values[i + 4*j] = element w kolumnie j, wierszu i.
 */
public final class Matrix4f {
    private final float[] values = new float[16];

    public Matrix4f() {
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || this.getClass() != object.getClass()) return false;
        Matrix4f matrix4f = (Matrix4f) object;
        return Arrays.equals(this.values, matrix4f.values);
    }

    public int hashCode() {
        return Arrays.hashCode(this.values);
    }

    public void load(FloatBuffer floatBuffer) {
        this.load(floatBuffer, false);
    }

    public void load(FloatBuffer floatBuffer, boolean bl) {
        if (bl) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    this.values[i * 4 + j] = floatBuffer.get(j * 4 + i);
                }
            }
        } else {
            floatBuffer.get(this.values);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix4f:\n");
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                sb.append(this.values[i + j * 4]);
                if (j != 3) sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public void store(FloatBuffer floatBuffer) {
        this.store(floatBuffer, false);
    }

    public void store(FloatBuffer floatBuffer, boolean bl) {
        if (bl) {
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    floatBuffer.put(j * 4 + i, this.values[i * 4 + j]);
                }
            }
        } else {
            floatBuffer.put(this.values);
        }
    }

    public void set(int i, int j, float f) {
        this.values[i + 4 * j] = f;
    }

    public float get(int i, int j) {
        return this.values[i + 4 * j];
    }

    /** Perspektywa (fovDeg, aspect, near, far). */
    public static Matrix4f perspective(double d, float f, float g, float h) {
        float i = (float)(1.0 / Math.tan(d * (float)(Math.PI / 180.0) / 2.0));
        Matrix4f m = new Matrix4f();
        m.set(0, 0, i / f);
        m.set(1, 1, i);
        m.set(2, 2, (h + g) / (g - h));
        m.set(3, 2, -1.0F);
        m.set(2, 3, 2.0F * h * g / (g - h));
        return m;
    }

    /** Orto (width, height, near, far). */
    public static Matrix4f orthographic(float f, float g, float h, float i) {
        Matrix4f m = new Matrix4f();
        m.set(0, 0, 2.0F / f);
        m.set(1, 1, 2.0F / g);
        float j = i - h;
        m.set(2, 2, -2.0F / j);
        m.set(3, 3, 1.0F);
        m.set(0, 3, -1.0F);
        m.set(1, 3, -1.0F);
        m.set(2, 3, -(i + h) / j);
        return m;
    }

    /** Identity matrix. */
    public static Matrix4f identity() {
        Matrix4f m = new Matrix4f();
        m.set(0, 0, 1); m.set(1, 1, 1); m.set(2, 2, 1); m.set(3, 3, 1);
        return m;
    }

    /** Zwraca kopie tablicy values (column-major). */
    public float[] toArray() {
        return this.values.clone();
    }
}
