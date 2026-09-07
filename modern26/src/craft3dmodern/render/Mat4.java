package craft3dmodern.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;


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

    public static float[] rotateX(float deg) {
        float a = (float) Math.toRadians(deg);
        float c = (float) Math.cos(a);
        float sn = (float) Math.sin(a);
        float[] m = identity();
        m[5] = c;
        m[6] = -sn;
        m[9] = sn;
        m[10] = c;
        return m;
    }

    public static float[] rotateY(float deg) {
        float a = (float) Math.toRadians(deg);
        float c = (float) Math.cos(a);
        float sn = (float) Math.sin(a);
        float[] m = identity();
        m[0] = c;
        m[2] = sn;
        m[8] = -sn;
        m[10] = c;
        return m;
    }

    public static float[] translate(float x, float y, float z) {
        float[] m = identity();
        m[3] = x;
        m[7] = y;
        m[11] = z;
        return m;
    }

    public static float[] scale(float x, float y, float z) {
        float[] m = identity();
        m[0] = x;
        m[5] = y;
        m[10] = z;
        return m;
    }

    public static float[] orthographic(float left, float right, float bottom, float top,
                                       float near, float far) {
        float[] out = new float[16];
        out[0] = 2f / (right - left);
        out[5] = 2f / (top - bottom);
        out[10] = -2f / (far - near);
        out[3] = -(right + left) / (right - left);
        out[7] = -(top + bottom) / (top - bottom);
        out[11] = -(far + near) / (far - near);
        out[15] = 1f;
        return out;
    }

    
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

    
    public static float[] lookAt(float ex, float ey, float ez,
                                 float cx, float cy, float cz,
                                 float ux, float uy, float uz) {
        float fx = cx - ex, fy = cy - ey, fz = cz - ez;
        float fl = (float) Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= fl; fy /= fl; fz /= fl;
        
        float sx = fy * uz - fz * uy;
        float sy = fz * ux - fx * uz;
        float sz = fx * uy - fy * ux;
        float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= sl; sy /= sl; sz /= sl;
        
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
