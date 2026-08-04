package craft3dgl.blaze3d.vertex;

import java.util.ArrayList;
import java.util.List;

/**
 * MC 1.14.4 VertexFormat port 1:1.
 * Definiuje uklad wierzchoklow: kolejnosc elementow (position, uv, color, normal).
 */
public class VertexFormat {
    private final List<VertexFormatElement> elements = new ArrayList<>();
    private final List<Integer> offsets = new ArrayList<>();
    private int vertexSize;
    private int colorOffset = -1;
    private final List<Integer> texOffset = new ArrayList<>();
    private int normalOffset = -1;

    public VertexFormat(VertexFormat other) {
        this();
        for (int i = 0; i < other.getElementCount(); i++) {
            this.addElement(other.getElement(i));
        }
        this.vertexSize = other.getVertexSize();
    }

    public VertexFormat() { }

    public void clear() {
        this.elements.clear();
        this.offsets.clear();
        this.colorOffset = -1;
        this.texOffset.clear();
        this.normalOffset = -1;
        this.vertexSize = 0;
    }

    public VertexFormat addElement(VertexFormatElement el) {
        if (el.isPosition() && this.hasPositionElement()) {
            System.err.println("[VertexFormat] Position already exists, ignoring.");
            return this;
        }
        this.elements.add(el);
        this.offsets.add(this.vertexSize);
        switch (el.getUsage()) {
            case NORMAL: this.normalOffset = this.vertexSize; break;
            case COLOR: this.colorOffset = this.vertexSize; break;
            case UV: this.texOffset.add(el.getIndex(), this.vertexSize); break;
        }
        this.vertexSize += el.getByteSize();
        return this;
    }

    public boolean hasNormal() { return this.normalOffset >= 0; }
    public int getNormalOffset() { return this.normalOffset; }
    public boolean hasColor() { return this.colorOffset >= 0; }
    public int getColorOffset() { return this.colorOffset; }
    public boolean hasUv(int i) { return this.texOffset.size() - 1 >= i; }
    public int getUvOffset(int i) { return this.texOffset.get(i); }

    public String toString() {
        StringBuilder sb = new StringBuilder("format: " + this.elements.size() + " elements: ");
        for (int i = 0; i < this.elements.size(); i++) {
            sb.append(this.elements.get(i).toString());
            if (i != this.elements.size() - 1) sb.append(" ");
        }
        return sb.toString();
    }

    private boolean hasPositionElement() {
        for (VertexFormatElement el : this.elements) {
            if (el.isPosition()) return true;
        }
        return false;
    }

    public int getIntegerSize() { return this.getVertexSize() / 4; }
    public int getVertexSize() { return this.vertexSize; }
    public List<VertexFormatElement> getElements() { return this.elements; }
    public int getElementCount() { return this.elements.size(); }
    public VertexFormatElement getElement(int i) { return this.elements.get(i); }
    public int getOffset(int i) { return this.offsets.get(i); }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        VertexFormat vf = (VertexFormat) o;
        if (this.vertexSize != vf.vertexSize) return false;
        if (!this.elements.equals(vf.elements)) return false;
        return this.offsets.equals(vf.offsets);
    }

    public int hashCode() {
        int i = this.elements.hashCode();
        i = 31 * i + this.offsets.hashCode();
        return 31 * i + this.vertexSize;
    }

    /**
     * Ustawia OpenGL vertex arrays (fixed-function pipeline).
     * Wywoluje glVertexPointer/glColorPointer/glTexCoordPointer/glNormalPointer
     * dla kazdego elementu wg jego Usage. Musi byc VBO zbindowane przed.
     */
    public void setupBufferState(long baseOffset) {
        int stride = this.getVertexSize();
        for (int i = 0; i < this.elements.size(); i++) {
            VertexFormatElement el = this.elements.get(i);
            long offset = baseOffset + this.offsets.get(i);
            int glType = el.getType().getGlType();
            int count = el.getCount();
            switch (el.getUsage()) {
                case POSITION:
                    org.lwjgl.opengl.GL11.glVertexPointer(count, glType, stride, offset);
                    org.lwjgl.opengl.GL11.glEnableClientState(org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY);
                    break;
                case NORMAL:
                    org.lwjgl.opengl.GL11.glNormalPointer(glType, stride, offset);
                    org.lwjgl.opengl.GL11.glEnableClientState(org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY);
                    break;
                case COLOR:
                    org.lwjgl.opengl.GL11.glColorPointer(count, glType, stride, offset);
                    org.lwjgl.opengl.GL11.glEnableClientState(org.lwjgl.opengl.GL11.GL_COLOR_ARRAY);
                    break;
                case UV:
                    // Multi-texture: uv0 na TEXTURE0, uv1 na TEXTURE1
                    // KRYTYCZNE: dla per-vertex array pointer NALEZY uzyc glClientActiveTexture
                    // (glActiveTexture = dla binding, glClientActiveTexture = dla vertex array state!)
                    org.lwjgl.opengl.GL13.glClientActiveTexture(craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0 + el.getIndex());
                    org.lwjgl.opengl.GL11.glTexCoordPointer(count, glType, stride, offset);
                    org.lwjgl.opengl.GL11.glEnableClientState(org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY);
                    org.lwjgl.opengl.GL13.glClientActiveTexture(craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0);
                    break;
                case PADDING:
                case MATRIX:
                case BLEND_WEIGHT:
                    // ignore w fixed-function
                    break;
            }
        }
    }

    /** Wylacza vertex arrays ustawione przez setupBufferState. */
    public void clearBufferState() {
        for (int i = 0; i < this.elements.size(); i++) {
            VertexFormatElement el = this.elements.get(i);
            switch (el.getUsage()) {
                case POSITION:
                    org.lwjgl.opengl.GL11.glDisableClientState(org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY);
                    break;
                case NORMAL:
                    org.lwjgl.opengl.GL11.glDisableClientState(org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY);
                    break;
                case COLOR:
                    org.lwjgl.opengl.GL11.glDisableClientState(org.lwjgl.opengl.GL11.GL_COLOR_ARRAY);
                    // Reset koloru (bo COLOR_ARRAY nadpisuje glColor4f)
                    org.lwjgl.opengl.GL11.glColor4f(1, 1, 1, 1);
                    break;
                case UV:
                    org.lwjgl.opengl.GL13.glClientActiveTexture(craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0 + el.getIndex());
                    org.lwjgl.opengl.GL11.glDisableClientState(org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY);
                    org.lwjgl.opengl.GL13.glClientActiveTexture(craft3dgl.blaze3d.platform.GLX.GL_TEXTURE0);
                    break;
                case PADDING:
                case MATRIX:
                case BLEND_WEIGHT:
                    break;
            }
        }
    }
}
