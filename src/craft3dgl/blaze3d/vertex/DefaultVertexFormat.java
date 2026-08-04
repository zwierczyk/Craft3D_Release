package craft3dgl.blaze3d.vertex;

import craft3dgl.blaze3d.vertex.VertexFormatElement.Type;
import craft3dgl.blaze3d.vertex.VertexFormatElement.Usage;

/**
 * MC 1.14.4 DefaultVertexFormat port 1:1.
 * Predefiniowane vertex formaty MC.
 */
public class DefaultVertexFormat {

    public static final VertexFormatElement ELEMENT_POSITION = new VertexFormatElement(0, Type.FLOAT, Usage.POSITION, 3);
    public static final VertexFormatElement ELEMENT_COLOR    = new VertexFormatElement(0, Type.UBYTE, Usage.COLOR, 4);
    public static final VertexFormatElement ELEMENT_UV0      = new VertexFormatElement(0, Type.FLOAT, Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_UV1      = new VertexFormatElement(1, Type.SHORT, Usage.UV, 2);
    public static final VertexFormatElement ELEMENT_NORMAL   = new VertexFormatElement(0, Type.BYTE,  Usage.NORMAL, 3);
    public static final VertexFormatElement ELEMENT_PADDING  = new VertexFormatElement(0, Type.BYTE,  Usage.PADDING, 1);

    // BLOCK: pos + color + uv0 + uv1(light)
    public static final VertexFormat BLOCK = new VertexFormat()
        .addElement(ELEMENT_POSITION)
        .addElement(ELEMENT_COLOR)
        .addElement(ELEMENT_UV0)
        .addElement(ELEMENT_UV1);

    // BLOCK_NORMALS: pos + color + uv0 + normal + padding
    public static final VertexFormat BLOCK_NORMALS = new VertexFormat()
        .addElement(ELEMENT_POSITION)
        .addElement(ELEMENT_COLOR)
        .addElement(ELEMENT_UV0)
        .addElement(ELEMENT_NORMAL)
        .addElement(ELEMENT_PADDING);

    // ENTITY: pos + uv0 + normal + padding
    public static final VertexFormat ENTITY = new VertexFormat()
        .addElement(ELEMENT_POSITION)
        .addElement(ELEMENT_UV0)
        .addElement(ELEMENT_NORMAL)
        .addElement(ELEMENT_PADDING);

    // PARTICLE: pos + uv0 + color + uv1(light)
    public static final VertexFormat PARTICLE = new VertexFormat()
        .addElement(ELEMENT_POSITION)
        .addElement(ELEMENT_UV0)
        .addElement(ELEMENT_COLOR)
        .addElement(ELEMENT_UV1);

    public static final VertexFormat POSITION = new VertexFormat().addElement(ELEMENT_POSITION);
    public static final VertexFormat POSITION_COLOR = new VertexFormat().addElement(ELEMENT_POSITION).addElement(ELEMENT_COLOR);
    public static final VertexFormat POSITION_TEX = new VertexFormat().addElement(ELEMENT_POSITION).addElement(ELEMENT_UV0);
    public static final VertexFormat POSITION_NORMAL = new VertexFormat().addElement(ELEMENT_POSITION).addElement(ELEMENT_NORMAL).addElement(ELEMENT_PADDING);
    public static final VertexFormat POSITION_TEX_COLOR = new VertexFormat().addElement(ELEMENT_POSITION).addElement(ELEMENT_UV0).addElement(ELEMENT_COLOR);
    public static final VertexFormat POSITION_TEX_NORMAL = new VertexFormat()
        .addElement(ELEMENT_POSITION)
        .addElement(ELEMENT_UV0)
        .addElement(ELEMENT_NORMAL)
        .addElement(ELEMENT_PADDING);
    public static final VertexFormat POSITION_TEX2_COLOR = new VertexFormat()
        .addElement(ELEMENT_POSITION)
        .addElement(ELEMENT_UV0)
        .addElement(ELEMENT_UV1)
        .addElement(ELEMENT_COLOR);
    public static final VertexFormat POSITION_TEX_COLOR_NORMAL = new VertexFormat()
        .addElement(ELEMENT_POSITION)
        .addElement(ELEMENT_UV0)
        .addElement(ELEMENT_COLOR)
        .addElement(ELEMENT_NORMAL)
        .addElement(ELEMENT_PADDING);
}
