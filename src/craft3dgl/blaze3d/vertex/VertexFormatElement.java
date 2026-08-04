package craft3dgl.blaze3d.vertex;

/**
 * MC 1.14.4 VertexFormatElement port 1:1.
 */
public class VertexFormatElement {
    private final VertexFormatElement.Type type;
    private final VertexFormatElement.Usage usage;
    private final int index;
    private final int count;

    public VertexFormatElement(int i, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int j) {
        if (this.supportsUsage(i, usage)) {
            this.usage = usage;
        } else {
            System.err.println("[VertexFormatElement] Multiple non-UV elements not supported, forcing UV.");
            this.usage = VertexFormatElement.Usage.UV;
        }
        this.type = type;
        this.index = i;
        this.count = j;
    }

    private final boolean supportsUsage(int i, VertexFormatElement.Usage usage) {
        return i == 0 || usage == VertexFormatElement.Usage.UV;
    }

    public final VertexFormatElement.Type getType() { return this.type; }
    public final VertexFormatElement.Usage getUsage() { return this.usage; }
    public final int getCount() { return this.count; }
    public final int getIndex() { return this.index; }

    public String toString() {
        return this.count + "," + this.usage.getName() + "," + this.type.getName();
    }

    public final int getByteSize() {
        return this.type.getSize() * this.count;
    }

    public final boolean isPosition() {
        return this.usage == VertexFormatElement.Usage.POSITION;
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || this.getClass() != object.getClass()) return false;
        VertexFormatElement o = (VertexFormatElement) object;
        if (this.count != o.count) return false;
        if (this.index != o.index) return false;
        return this.type == o.type && this.usage == o.usage;
    }

    public int hashCode() {
        int i = this.type.hashCode();
        i = 31 * i + this.usage.hashCode();
        i = 31 * i + this.index;
        return 31 * i + this.count;
    }

    public static enum Type {
        FLOAT(4, "Float", 5126),
        UBYTE(1, "Unsigned Byte", 5121),
        BYTE(1, "Byte", 5120),
        USHORT(2, "Unsigned Short", 5123),
        SHORT(2, "Short", 5122),
        UINT(4, "Unsigned Int", 5125),
        INT(4, "Int", 5124);

        private final int size;
        private final String name;
        private final int glType;

        private Type(int j, String s, int k) {
            this.size = j; this.name = s; this.glType = k;
        }
        public int getSize() { return this.size; }
        public String getName() { return this.name; }
        public int getGlType() { return this.glType; }
    }

    public static enum Usage {
        POSITION("Position"),
        NORMAL("Normal"),
        COLOR("Vertex Color"),
        UV("UV"),
        MATRIX("Bone Matrix"),
        BLEND_WEIGHT("Blend Weight"),
        PADDING("Padding");

        private final String name;
        private Usage(String s) { this.name = s; }
        public String getName() { return this.name; }
    }
}
