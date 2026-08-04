package craft3dgl.blaze3d.renderer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import craft3dgl.blaze3d.platform.GlStateManager;

/**
 * MC 1.14.4-style predefiniowane RenderStateShards.
 * Grupujemy tu wszystkie znane stany OpenGL uzywane przez RenderTypes.
 */
public class RenderStates {

    // ===== BLEND =====
    public static final RenderStateShard NO_BLEND = new RenderStateShard("no_blend",
        () -> GL11.glDisable(GL11.GL_BLEND),
        () -> {});

    public static final RenderStateShard ALPHA_BLEND = new RenderStateShard("alpha_blend",
        () -> {
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        },
        () -> {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        });

    public static final RenderStateShard ADDITIVE_BLEND = new RenderStateShard("additive_blend",
        () -> {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        },
        () -> {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        });

    // ===== CULL =====
    public static final RenderStateShard CULL = new RenderStateShard("cull",
        () -> GL11.glEnable(GL11.GL_CULL_FACE),
        () -> {});

    public static final RenderStateShard NO_CULL = new RenderStateShard("no_cull",
        () -> GL11.glDisable(GL11.GL_CULL_FACE),
        () -> GL11.glEnable(GL11.GL_CULL_FACE));

    // ===== DEPTH TEST =====
    public static final RenderStateShard DEPTH_LEQUAL = new RenderStateShard("depth_lequal",
        () -> { GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_LEQUAL); },
        () -> {});

    public static final RenderStateShard DEPTH_ALWAYS = new RenderStateShard("depth_always",
        () -> { GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_ALWAYS); },
        () -> GL11.glDepthFunc(GL11.GL_LEQUAL));

    public static final RenderStateShard NO_DEPTH_TEST = new RenderStateShard("no_depth_test",
        () -> GL11.glDisable(GL11.GL_DEPTH_TEST),
        () -> { GL11.glEnable(GL11.GL_DEPTH_TEST); GL11.glDepthFunc(GL11.GL_LEQUAL); });

    // ===== DEPTH WRITE =====
    public static final RenderStateShard WRITE_DEPTH = new RenderStateShard("write_depth",
        () -> GL11.glDepthMask(true),
        () -> {});

    public static final RenderStateShard NO_WRITE_DEPTH = new RenderStateShard("no_write_depth",
        () -> GL11.glDepthMask(false),
        () -> GL11.glDepthMask(true));

    // ===== TEXTURE =====
    public static final RenderStateShard TEXTURE_2D = new RenderStateShard("texture_2d",
        () -> {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        },
        () -> GL11.glDisable(GL11.GL_TEXTURE_2D));

    public static final RenderStateShard NO_TEXTURE = new RenderStateShard("no_texture",
        () -> GL11.glDisable(GL11.GL_TEXTURE_2D),
        () -> GL11.glEnable(GL11.GL_TEXTURE_2D));

    // ===== ALPHA TEST (cutout) =====
    public static final RenderStateShard ALPHA_TEST_CUTOUT = new RenderStateShard("alpha_test_cutout",
        () -> {
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.5f);
        },
        () -> GL11.glDisable(GL11.GL_ALPHA_TEST));

    public static final RenderStateShard NO_ALPHA_TEST = new RenderStateShard("no_alpha_test",
        () -> GL11.glDisable(GL11.GL_ALPHA_TEST),
        () -> {});

    // ===== LIGHTING =====
    public static final RenderStateShard LIGHTMAP = new RenderStateShard("lightmap",
        () -> GL11.glEnable(GL11.GL_LIGHTING),
        () -> GL11.glDisable(GL11.GL_LIGHTING));

    public static final RenderStateShard NO_LIGHTMAP = new RenderStateShard("no_lightmap",
        () -> GL11.glDisable(GL11.GL_LIGHTING),
        () -> {});

    // ===== LINE WIDTH =====
    public static RenderStateShard lineWidth(final float width) {
        return new RenderStateShard("line_width_" + width,
            () -> GL11.glLineWidth(width),
            () -> GL11.glLineWidth(1.0f));
    }

    // ===== POLYGON OFFSET (dla layerow) =====
    public static final RenderStateShard POLYGON_OFFSET = new RenderStateShard("polygon_offset",
        () -> {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1.0f, -10.0f);
        },
        () -> {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(0.0f, 0.0f);
        });

    public static final RenderStateShard NO_POLYGON_OFFSET = new RenderStateShard("no_polygon_offset",
        () -> {},
        () -> {});
}
