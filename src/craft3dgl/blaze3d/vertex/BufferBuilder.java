package craft3dgl.blaze3d.vertex;

import craft3dgl.blaze3d.platform.MemoryTracker;
import craft3dgl.blaze3d.vertex.VertexFormatElement.Usage;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * MC 1.14.4 BufferBuilder port 1:1.
 *
 * API do wypelniania wierzchoklow chain-style:
 *   bufferBuilder.begin(GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
 *   bufferBuilder.vertex(x, y, z).color(r, g, b, a).endVertex();
 *   ...
 *   bufferBuilder.end();
 *
 * Kazdy vertex zapisywany do ByteBuffer wg VertexFormat.
 */
public class BufferBuilder {
    private ByteBuffer buffer;
    private IntBuffer intBuffer;
    private ShortBuffer shortBuffer;
    private FloatBuffer floatBuffer;
    private int vertices;
    private VertexFormatElement currentElement;
    private int elementIndex;
    private boolean noColor;
    private int mode;
    private double xo;
    private double yo;
    private double zo;
    private VertexFormat format;
    private boolean building;

    public BufferBuilder(int capacityInInts) {
        this.buffer = MemoryTracker.createByteBuffer(capacityInInts * 4);
        this.intBuffer = this.buffer.asIntBuffer();
        this.shortBuffer = this.buffer.asShortBuffer();
        this.floatBuffer = this.buffer.asFloatBuffer();
    }

    private void ensureCapacity(int i) {
        if (this.vertices * this.format.getVertexSize() + i > this.buffer.capacity()) {
            int oldSize = this.buffer.capacity();
            int newSize = oldSize + roundUp(i);
            int pos = this.intBuffer.position();
            ByteBuffer nb = MemoryTracker.createByteBuffer(newSize);
            this.buffer.position(0);
            nb.put(this.buffer);
            nb.rewind();
            this.buffer = nb;
            this.floatBuffer = this.buffer.asFloatBuffer().asReadOnlyBuffer();
            this.intBuffer = this.buffer.asIntBuffer();
            this.intBuffer.position(pos);
            this.shortBuffer = this.buffer.asShortBuffer();
            this.shortBuffer.position(pos << 1);
        }
    }

    private static int roundUp(int i) {
        int step = 2097152;
        if (i == 0) return step;
        if (i < 0) step *= -1;
        int mod = i % step;
        return mod == 0 ? i : i + step - mod;
    }

    public void sortQuads(float f, float g, float h) {
        int quadCount = this.vertices / 4;
        float[] dists = new float[quadCount];
        for (int j = 0; j < quadCount; j++) {
            dists[j] = getQuadDistanceFromPlayer(
                this.floatBuffer, (float)(f + this.xo), (float)(g + this.yo), (float)(h + this.zo),
                this.format.getIntegerSize(), j * this.format.getVertexSize()
            );
        }
        Integer[] indices = new Integer[quadCount];
        for (int k = 0; k < indices.length; k++) indices[k] = k;
        Arrays.sort(indices, (a, b) -> Float.compare(dists[b], dists[a]));
        BitSet bs = new BitSet();
        int stride = this.format.getVertexSize();
        int[] tmp = new int[stride];
        for (int m = bs.nextClearBit(0); m < indices.length; m = bs.nextClearBit(m + 1)) {
            int n = indices[m];
            if (n != m) {
                this.intBuffer.limit(n * stride + stride);
                this.intBuffer.position(n * stride);
                this.intBuffer.get(tmp);
                int o = n;
                for (int p = indices[n]; o != m; p = indices[p]) {
                    this.intBuffer.limit(p * stride + stride);
                    this.intBuffer.position(p * stride);
                    IntBuffer slice = this.intBuffer.slice();
                    this.intBuffer.limit(o * stride + stride);
                    this.intBuffer.position(o * stride);
                    this.intBuffer.put(slice);
                    bs.set(o);
                    o = p;
                }
                this.intBuffer.limit(m * stride + stride);
                this.intBuffer.position(m * stride);
                this.intBuffer.put(tmp);
            }
            bs.set(m);
        }
    }

    public State getState() {
        this.intBuffer.rewind();
        int i = this.getBufferIndex();
        this.intBuffer.limit(i);
        int[] arr = new int[i];
        this.intBuffer.get(arr);
        this.intBuffer.limit(this.intBuffer.capacity());
        this.intBuffer.position(i);
        return new State(arr, new VertexFormat(this.format));
    }

    private int getBufferIndex() {
        return this.vertices * this.format.getIntegerSize();
    }

    private static float getQuadDistanceFromPlayer(FloatBuffer fb, float f, float g, float h, int i, int j) {
        float k = fb.get(j + i * 0);
        float l = fb.get(j + i * 0 + 1);
        float m = fb.get(j + i * 0 + 2);
        float n = fb.get(j + i);
        float o = fb.get(j + i + 1);
        float p = fb.get(j + i + 2);
        float q = fb.get(j + i * 2);
        float r = fb.get(j + i * 2 + 1);
        float s = fb.get(j + i * 2 + 2);
        float t = fb.get(j + i * 3);
        float u = fb.get(j + i * 3 + 1);
        float v = fb.get(j + i * 3 + 2);
        float w = (k + n + q + t) * 0.25F - f;
        float x = (l + o + r + u) * 0.25F - g;
        float y = (m + p + s + v) * 0.25F - h;
        return w * w + x * x + y * y;
    }

    public void restoreState(State state) {
        this.intBuffer.clear();
        this.ensureCapacity(state.array().length * 4);
        this.intBuffer.put(state.array());
        this.vertices = state.vertices();
        this.format = new VertexFormat(state.getFormat());
    }

    public void clear() {
        this.vertices = 0;
        this.currentElement = null;
        this.elementIndex = 0;
    }

    public boolean isBuilding() { return this.building; }

    public void begin(int mode, VertexFormat format) {
        if (this.building) {
            throw new IllegalStateException("Already building!");
        }
        this.building = true;
        this.clear();
        this.mode = mode;
        this.format = format;
        this.currentElement = format.getElement(this.elementIndex);
        this.noColor = false;
        this.buffer.limit(this.buffer.capacity());
    }

    public BufferBuilder uv(double d, double e) {
        int i = this.vertices * this.format.getVertexSize() + this.format.getOffset(this.elementIndex);
        switch (this.currentElement.getType()) {
            case FLOAT:
                this.buffer.putFloat(i, (float)d);
                this.buffer.putFloat(i + 4, (float)e);
                break;
            case UINT:
            case INT:
                this.buffer.putInt(i, (int)d);
                this.buffer.putInt(i + 4, (int)e);
                break;
            case USHORT:
            case SHORT:
                this.buffer.putShort(i, (short)e);
                this.buffer.putShort(i + 2, (short)d);
                break;
            case UBYTE:
            case BYTE:
                this.buffer.put(i, (byte)e);
                this.buffer.put(i + 1, (byte)d);
                break;
        }
        this.nextElement();
        return this;
    }

    public BufferBuilder uv2(int i, int j) {
        int k = this.vertices * this.format.getVertexSize() + this.format.getOffset(this.elementIndex);
        switch (this.currentElement.getType()) {
            case FLOAT:
                this.buffer.putFloat(k, i);
                this.buffer.putFloat(k + 4, j);
                break;
            case UINT:
            case INT:
                this.buffer.putInt(k, i);
                this.buffer.putInt(k + 4, j);
                break;
            case USHORT:
            case SHORT:
                this.buffer.putShort(k, (short)j);
                this.buffer.putShort(k + 2, (short)i);
                break;
            case UBYTE:
            case BYTE:
                this.buffer.put(k, (byte)j);
                this.buffer.put(k + 1, (byte)i);
                break;
        }
        this.nextElement();
        return this;
    }

    public void faceTex2(int i, int j, int k, int l) {
        int m = (this.vertices - 4) * this.format.getIntegerSize() + this.format.getUvOffset(1) / 4;
        int n = this.format.getVertexSize() >> 2;
        this.intBuffer.put(m, i);
        this.intBuffer.put(m + n, j);
        this.intBuffer.put(m + n * 2, k);
        this.intBuffer.put(m + n * 3, l);
    }

    public void postProcessFacePosition(double d, double e, double f) {
        int i = this.format.getIntegerSize();
        int j = (this.vertices - 4) * i;
        for (int k = 0; k < 4; k++) {
            int l = j + k * i;
            int m = l + 1;
            int n = m + 1;
            this.intBuffer.put(l, Float.floatToRawIntBits((float)(d + this.xo) + Float.intBitsToFloat(this.intBuffer.get(l))));
            this.intBuffer.put(m, Float.floatToRawIntBits((float)(e + this.yo) + Float.intBitsToFloat(this.intBuffer.get(m))));
            this.intBuffer.put(n, Float.floatToRawIntBits((float)(f + this.zo) + Float.intBitsToFloat(this.intBuffer.get(n))));
        }
    }

    private int getStartingColorIndex(int i) {
        return ((this.vertices - i) * this.format.getVertexSize() + this.format.getColorOffset()) / 4;
    }

    public void faceTint(float f, float g, float h, int i) {
        int j = this.getStartingColorIndex(i);
        int k = -1;
        if (!this.noColor) {
            k = this.intBuffer.get(j);
            if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                int rr = (int)((k & 0xFF) * f);
                int gg = (int)((k >> 8 & 0xFF) * g);
                int bb = (int)((k >> 16 & 0xFF) * h);
                k &= -16777216;
                k |= bb << 16 | gg << 8 | rr;
            } else {
                int rr = (int)((k >> 24 & 0xFF) * f);
                int gg = (int)((k >> 16 & 0xFF) * g);
                int bb = (int)((k >> 8 & 0xFF) * h);
                k &= 255;
                k |= rr << 24 | gg << 16 | bb << 8;
            }
        }
        this.intBuffer.put(j, k);
    }

    private void fixupVertexColor(int rgb, int j) {
        int k = this.getStartingColorIndex(j);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        this.putColor(k, r, g, b);
    }

    public void fixupVertexColor(float f, float g, float h, int i) {
        int j = this.getStartingColorIndex(i);
        int r = clamp((int)(f * 255.0F), 0, 255);
        int gg = clamp((int)(g * 255.0F), 0, 255);
        int b = clamp((int)(h * 255.0F), 0, 255);
        this.putColor(j, r, gg, b);
    }

    private static int clamp(int i, int j, int k) {
        if (i < j) return j;
        return i > k ? k : i;
    }

    private void putColor(int i, int r, int g, int b) {
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            this.intBuffer.put(i, 0xFF000000 | b << 16 | g << 8 | r);
        } else {
            this.intBuffer.put(i, r << 24 | g << 16 | b << 8 | 0xFF);
        }
    }

    public void noColor() {
        this.noColor = true;
    }

    public BufferBuilder color(float f, float g, float h, float i) {
        return this.color((int)(f * 255.0F), (int)(g * 255.0F), (int)(h * 255.0F), (int)(i * 255.0F));
    }

    public BufferBuilder color(int r, int g, int b, int a) {
        if (this.noColor) return this;
        int m = this.vertices * this.format.getVertexSize() + this.format.getOffset(this.elementIndex);
        switch (this.currentElement.getType()) {
            case FLOAT:
                this.buffer.putFloat(m, r / 255.0F);
                this.buffer.putFloat(m + 4, g / 255.0F);
                this.buffer.putFloat(m + 8, b / 255.0F);
                this.buffer.putFloat(m + 12, a / 255.0F);
                break;
            case UINT:
            case INT:
                this.buffer.putFloat(m, r);
                this.buffer.putFloat(m + 4, g);
                this.buffer.putFloat(m + 8, b);
                this.buffer.putFloat(m + 12, a);
                break;
            case USHORT:
            case SHORT:
                this.buffer.putShort(m, (short)r);
                this.buffer.putShort(m + 2, (short)g);
                this.buffer.putShort(m + 4, (short)b);
                this.buffer.putShort(m + 6, (short)a);
                break;
            case UBYTE:
            case BYTE:
                if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                    this.buffer.put(m, (byte)r);
                    this.buffer.put(m + 1, (byte)g);
                    this.buffer.put(m + 2, (byte)b);
                    this.buffer.put(m + 3, (byte)a);
                } else {
                    this.buffer.put(m, (byte)a);
                    this.buffer.put(m + 1, (byte)b);
                    this.buffer.put(m + 2, (byte)g);
                    this.buffer.put(m + 3, (byte)r);
                }
                break;
        }
        this.nextElement();
        return this;
    }

    public void putBulkData(int[] is) {
        this.ensureCapacity(is.length * 4 + this.format.getVertexSize());
        this.intBuffer.position(this.getBufferIndex());
        this.intBuffer.put(is);
        this.vertices += is.length / this.format.getIntegerSize();
    }

    public void endVertex() {
        this.vertices++;
        this.ensureCapacity(this.format.getVertexSize());
    }

    public BufferBuilder vertex(double d, double e, double f) {
        int i = this.vertices * this.format.getVertexSize() + this.format.getOffset(this.elementIndex);
        switch (this.currentElement.getType()) {
            case FLOAT:
                this.buffer.putFloat(i, (float)(d + this.xo));
                this.buffer.putFloat(i + 4, (float)(e + this.yo));
                this.buffer.putFloat(i + 8, (float)(f + this.zo));
                break;
            case UINT:
            case INT:
                this.buffer.putInt(i, Float.floatToRawIntBits((float)(d + this.xo)));
                this.buffer.putInt(i + 4, Float.floatToRawIntBits((float)(e + this.yo)));
                this.buffer.putInt(i + 8, Float.floatToRawIntBits((float)(f + this.zo)));
                break;
            case USHORT:
            case SHORT:
                this.buffer.putShort(i, (short)(d + this.xo));
                this.buffer.putShort(i + 2, (short)(e + this.yo));
                this.buffer.putShort(i + 4, (short)(f + this.zo));
                break;
            case UBYTE:
            case BYTE:
                this.buffer.put(i, (byte)(d + this.xo));
                this.buffer.put(i + 1, (byte)(e + this.yo));
                this.buffer.put(i + 2, (byte)(f + this.zo));
                break;
        }
        this.nextElement();
        return this;
    }

    public void postNormal(float f, float g, float h) {
        int i = (byte)(f * 127.0F) & 255;
        int j = (byte)(g * 127.0F) & 255;
        int k = (byte)(h * 127.0F) & 255;
        int packed = i | j << 8 | k << 16;
        int stride = this.format.getVertexSize() >> 2;
        int idx = (this.vertices - 4) * stride + this.format.getNormalOffset() / 4;
        this.intBuffer.put(idx, packed);
        this.intBuffer.put(idx + stride, packed);
        this.intBuffer.put(idx + stride * 2, packed);
        this.intBuffer.put(idx + stride * 3, packed);
    }

    private void nextElement() {
        this.elementIndex++;
        this.elementIndex = this.elementIndex % this.format.getElementCount();
        this.currentElement = this.format.getElement(this.elementIndex);
        if (this.currentElement.getUsage() == Usage.PADDING) {
            this.nextElement();
        }
    }

    public BufferBuilder normal(float f, float g, float h) {
        int i = this.vertices * this.format.getVertexSize() + this.format.getOffset(this.elementIndex);
        switch (this.currentElement.getType()) {
            case FLOAT:
                this.buffer.putFloat(i, f);
                this.buffer.putFloat(i + 4, g);
                this.buffer.putFloat(i + 8, h);
                break;
            case UINT:
            case INT:
                this.buffer.putInt(i, (int)f);
                this.buffer.putInt(i + 4, (int)g);
                this.buffer.putInt(i + 8, (int)h);
                break;
            case USHORT:
            case SHORT:
                this.buffer.putShort(i, (short)((int)f * 32767 & 65535));
                this.buffer.putShort(i + 2, (short)((int)g * 32767 & 65535));
                this.buffer.putShort(i + 4, (short)((int)h * 32767 & 65535));
                break;
            case UBYTE:
            case BYTE:
                this.buffer.put(i, (byte)((int)f * 127 & 0xFF));
                this.buffer.put(i + 1, (byte)((int)g * 127 & 0xFF));
                this.buffer.put(i + 2, (byte)((int)h * 127 & 0xFF));
                break;
        }
        this.nextElement();
        return this;
    }

    public void offset(double d, double e, double f) {
        this.xo = d;
        this.yo = e;
        this.zo = f;
    }

    public void end() {
        if (!this.building) {
            throw new IllegalStateException("Not building!");
        }
        this.building = false;
        this.buffer.position(0);
        this.buffer.limit(this.getBufferIndex() * 4);
    }

    public ByteBuffer getBuffer() { return this.buffer; }
    public VertexFormat getVertexFormat() { return this.format; }
    public int getVertexCount() { return this.vertices; }
    public int getDrawMode() { return this.mode; }

    public void fixupQuadColor(int rgb) {
        for (int j = 0; j < 4; j++) this.fixupVertexColor(rgb, j + 1);
    }

    public void fixupQuadColor(float f, float g, float h) {
        for (int i = 0; i < 4; i++) this.fixupVertexColor(f, g, h, i + 1);
    }

    public class State {
        private final int[] array;
        private final VertexFormat format;

        public State(int[] arr, VertexFormat fmt) {
            this.array = arr;
            this.format = fmt;
        }

        public int[] array() { return this.array; }
        public int vertices() { return this.array.length / this.format.getIntegerSize(); }
        public VertexFormat getFormat() { return this.format; }
    }
}
