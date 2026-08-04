package craft3dgl.blaze3d.renderer;

import org.lwjgl.opengl.GL11;
import craft3dgl.blaze3d.vertex.DefaultVertexFormat;

/**
 * MC 1.14.4-style predefiniowane RenderTypes.
 * Kazdy typ = konkretna kombinacja VertexFormat + GL mode + shardow stanu OpenGL.
 *
 * Odpowiedniki MC:
 *   SOLID              -> chunki opaque (kamien, ziemia, drewno)
 *   CUTOUT             -> chunki z alpha test (liscie, trawa, drzwi)
 *   CUTOUT_MIPPED      -> jak CUTOUT ale z mipmapami (liscie w vanilla)
 *   TRANSLUCENT        -> woda, szklo, ice (alpha blend + sort back-to-front)
 *   ENTITY_SOLID       -> mob solid (Steve, zombie)
 *   ENTITY_CUTOUT      -> mob z alpha (koc, rope)
 *   ENTITY_TRANSLUCENT -> mob polprzezroczysty
 *   LEASH              -> smycz (lines)
 *   LINES              -> outline blokow (F3+B)
 *   GUI                -> HUD, inventory, menu (position_tex_color, alpha blend, no depth)
 *   TEXT               -> font renderer
 */
public class RenderTypes {

    private static final int SMALL_BUFFER  = 256;
    private static final int MEDIUM_BUFFER = 131072;    // 128 KB
    private static final int LARGE_BUFFER  = 2097152;   // 2 MB (chunk layer)

    public static final RenderType SOLID = RenderType.builder(
            "solid", DefaultVertexFormat.BLOCK, GL11.GL_QUADS, LARGE_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.WRITE_DEPTH)
        .shard(RenderStates.CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.NO_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .build();

    public static final RenderType CUTOUT = RenderType.builder(
            "cutout", DefaultVertexFormat.BLOCK, GL11.GL_QUADS, LARGE_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)             // liscie i trawa nie cullowane
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.NO_BLEND)
        .shard(RenderStates.ALPHA_TEST_CUTOUT)   // discard alpha < 0.5
        .build();

    public static final RenderType CUTOUT_MIPPED = RenderType.builder(
            "cutout_mipped", DefaultVertexFormat.BLOCK, GL11.GL_QUADS, LARGE_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.WRITE_DEPTH)
        .shard(RenderStates.CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.NO_BLEND)
        .shard(RenderStates.ALPHA_TEST_CUTOUT)
        .build();

    public static final RenderType TRANSLUCENT = RenderType.builder(
            "translucent", DefaultVertexFormat.BLOCK, GL11.GL_QUADS, LARGE_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.NO_WRITE_DEPTH)      // nie pisze do depth (kolejnosc waznA)
        .shard(RenderStates.CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.ALPHA_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .sort(true)                              // sort back-to-front na uploadzie
        .build();

    public static final RenderType ENTITY_SOLID = RenderType.builder(
            "entity_solid", DefaultVertexFormat.ENTITY, GL11.GL_QUADS, MEDIUM_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.WRITE_DEPTH)
        .shard(RenderStates.CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.NO_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .build();

    public static final RenderType ENTITY_CUTOUT = RenderType.builder(
            "entity_cutout", DefaultVertexFormat.ENTITY, GL11.GL_QUADS, MEDIUM_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.NO_BLEND)
        .shard(RenderStates.ALPHA_TEST_CUTOUT)
        .build();

    public static final RenderType ENTITY_TRANSLUCENT = RenderType.builder(
            "entity_translucent", DefaultVertexFormat.ENTITY, GL11.GL_QUADS, MEDIUM_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.NO_WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.ALPHA_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .build();

    public static final RenderType LINES = RenderType.builder(
            "lines", DefaultVertexFormat.POSITION_COLOR, GL11.GL_LINES, SMALL_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.NO_WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.NO_TEXTURE)
        .shard(RenderStates.ALPHA_BLEND)
        .shard(RenderStates.lineWidth(2.0f))
        .build();

    public static final RenderType LEASH = RenderType.builder(
            "leash", DefaultVertexFormat.POSITION_COLOR, GL11.GL_TRIANGLE_STRIP, SMALL_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.NO_TEXTURE)
        .shard(RenderStates.NO_BLEND)
        .build();

    public static final RenderType GUI = RenderType.builder(
            "gui", DefaultVertexFormat.POSITION_TEX_COLOR, GL11.GL_QUADS, MEDIUM_BUFFER)
        .shard(RenderStates.NO_DEPTH_TEST)
        .shard(RenderStates.NO_WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.ALPHA_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .build();

    public static final RenderType GUI_NO_TEX = RenderType.builder(
            "gui_no_tex", DefaultVertexFormat.POSITION_COLOR, GL11.GL_QUADS, MEDIUM_BUFFER)
        .shard(RenderStates.NO_DEPTH_TEST)
        .shard(RenderStates.NO_WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.NO_TEXTURE)
        .shard(RenderStates.ALPHA_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .build();

    public static final RenderType TEXT = RenderType.builder(
            "text", DefaultVertexFormat.POSITION_TEX_COLOR, GL11.GL_QUADS, MEDIUM_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.NO_WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.ALPHA_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .build();

    public static final RenderType PARTICLE = RenderType.builder(
            "particle", DefaultVertexFormat.PARTICLE, GL11.GL_QUADS, MEDIUM_BUFFER)
        .shard(RenderStates.DEPTH_LEQUAL)
        .shard(RenderStates.NO_WRITE_DEPTH)
        .shard(RenderStates.NO_CULL)
        .shard(RenderStates.TEXTURE_2D)
        .shard(RenderStates.ALPHA_BLEND)
        .shard(RenderStates.NO_ALPHA_TEST)
        .build();

    /** Wszystkie chunk layers w kolejnosci renderowania (MC 1.14.4). */
    public static final RenderType[] CHUNK_LAYERS = new RenderType[] {
        SOLID, CUTOUT_MIPPED, CUTOUT, TRANSLUCENT
    };
}
