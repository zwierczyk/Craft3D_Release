package craft3dmodern.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

/**
 * Miniaturowa matematyka macierzy 4x4. Macierze trzymamy w ukladzie
 * wierszowym float[16] (r*4+c) i mnozymy jako M = A*B (wektory kolumnowe).
 * Do GL wysylamy wersje kolumnowa (glUniformMatrix4fv, transpose=false).
 */
public final class Mat4 {
    private Mat4() {}

    public static float[] identity() {
        float[] m = new float[16];
        m[0] = m[5] = m[10] = m[15] = 1f;
        return m;
    }

    public static float[] multiply(float[] a, float[] b) {
        float[] c = new float[16];
        for (int r = 0; r < 4; r++) {
            for (int cc = 0; cc < 4; cc++) {
                float s = 0;
                for (int k = 0; k < 4; k++) s += a[r * 4 + k] * b[k * 4 + cc];
                c[r * 4 + cc] = s;
            }
        }
        return c;
    }

    /** Projekcja perspektywiczna (GL: kamera patrzy w -z, zNear>0). */
    public static float[] perspective(float fovyDeg, float aspect, float near, float far) {
        float f = (float) (1.0 / Math.tan(Math.toRadians(fovyDeg) / 2.0));
        float[] out = new float[16];
        out[0] = f / aspect;
        out[5] = f;
        out[10] = (far + near) / (near - far);
        out[11] = 2f * far * near / (near - far);
        out[14] = -1f;
        return out;
    }

    /** lookAt (prawoskrętne; patrzy w -z w przestrzeni widoku). */
    public static float[] lookAt(float ex, float ey, float ez,
                                 float cx, float cy, float cz,
                                 float ux, float uy, float uz) {
        float fx = cx - ex, fy = cy - ey, fz = cz - ez;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl; fy /= fl; fz /= fl;
        // s = normalize(f x up)
        float sx = fy * uz - fz * uy;
        float sy = fz * ux - fx * uz;
        float sz = fx * uy - fy * ux;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= sl; sy /= sl; sz /= sl;
        // u = s x f
        float uxx = sy * fz - sz * fy;
        float uyy = sz * fx - sx * fz;
        float uzz = sx * fy - sy * fx;

        float[] out = new float[16];
        out[0] = sx;  out[1] = sy;  out[2] = sz;  out[3] = -(sx * ex + sy * ey + sz * ez);
        out[4] = uxx; out[5] = uyy; out[6] = uzz; out[7] = -(uxx * ex + uyy * ey + uzz * ez);
        out[8] = -fx; out[9] = -fy; out[10] = -fz; out[11] = (fx * ex + fy * ey + fz * ez);
        out[15] = 1f;
        return out;
    }

    /** Zamiana wierszowej na kolumnowa (kolejnosc dla glUniformMatrix4fv). */
    public static FloatBuffer columnMajor(float[] rowMajor) {
        FloatBuffer b = BufferUtils.createFloatBuffer(16);
        for (int c = 0; c < 4; c++) {
            for (int r = 0; r < 4; r++) {
                b.put(rowMajor[r * 4 + c]);
            }
        }
        b.flip();
        return b;
    }
}
