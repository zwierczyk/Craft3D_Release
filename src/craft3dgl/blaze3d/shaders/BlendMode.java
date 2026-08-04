package craft3dgl.blaze3d.shaders;

import craft3dgl.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL14;

/**
 * MC 1.14.4 BlendMode port 1:1.
 *
 * Reprezentuje blending state OpenGL. Parsuje wartosci z JSON shader configu:
 *   "blend": { "func": "add", "srcrgb": "srcalpha", "dstrgb": "1-srcalpha" }
 *
 * apply() ustawia glBlendFunc + glBlendEquation w OpenGL.
 */
public class BlendMode {
    private final int srcColorFactor;
    private final int destColorFactor;
    private final int srcAlphaFactor;
    private final int destAlphaFactor;
    private final int blendFunc;
    private final boolean separateBlend;
    private final boolean opaque;

    private static BlendMode lastApplied;

    /** Default: nie robi blend (opaque). */
    public BlendMode() {
        this.blendFunc = 32774; // GL_FUNC_ADD
        this.srcColorFactor = 1; // GL_ONE
        this.destColorFactor = 0; // GL_ZERO
        this.srcAlphaFactor = 1;
        this.destAlphaFactor = 0;
        this.opaque = true;
        this.separateBlend = false;
    }

    /** Blend z jednakowym RGB i alpha factor. */
    public BlendMode(int srcRgb, int dstRgb, int func) {
        this.blendFunc = func;
        this.srcColorFactor = srcRgb;
        this.destColorFactor = dstRgb;
        this.srcAlphaFactor = srcRgb;
        this.destAlphaFactor = dstRgb;
        this.opaque = false;
        this.separateBlend = false;
    }

    /** Blend z oddzielnym RGB i alpha factor. */
    public BlendMode(int srcRgb, int dstRgb, int srcA, int dstA, int func) {
        this.blendFunc = func;
        this.srcColorFactor = srcRgb;
        this.destColorFactor = dstRgb;
        this.srcAlphaFactor = srcA;
        this.destAlphaFactor = dstA;
        this.opaque = false;
        this.separateBlend = true;
    }

    /** Aplikuj ten blend state do OpenGL. Nie robi nic jesli juz aktywny. */
    public void apply() {
        if (this.equals(lastApplied)) return;
        if (lastApplied == null || this.opaque != lastApplied.opaque) {
            lastApplied = this;
            if (this.opaque) {
                GlStateManager.disableBlend();
                return;
            }
            GlStateManager.enableBlend();
        }
        GL14.glBlendEquation(this.blendFunc);
        if (this.separateBlend) {
            GlStateManager.blendFuncSeparate(this.srcColorFactor, this.destColorFactor,
                this.srcAlphaFactor, this.destAlphaFactor);
        } else {
            GlStateManager.blendFunc(this.srcColorFactor, this.destColorFactor);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BlendMode)) return false;
        BlendMode m = (BlendMode) o;
        if (this.blendFunc != m.blendFunc) return false;
        if (this.destAlphaFactor != m.destAlphaFactor) return false;
        if (this.destColorFactor != m.destColorFactor) return false;
        if (this.opaque != m.opaque) return false;
        if (this.separateBlend != m.separateBlend) return false;
        if (this.srcAlphaFactor != m.srcAlphaFactor) return false;
        return this.srcColorFactor == m.srcColorFactor;
    }

    public int hashCode() {
        int r = this.srcColorFactor;
        r = 31 * r + this.destColorFactor;
        r = 31 * r + this.srcAlphaFactor;
        r = 31 * r + this.destAlphaFactor;
        r = 31 * r + this.blendFunc;
        r = 31 * r + (this.separateBlend ? 1 : 0);
        r = 31 * r + (this.opaque ? 1 : 0);
        return r;
    }

    public boolean isOpaque() { return this.opaque; }

    /** Parsuje "add", "subtract", "reversesubtract", "min", "max" na int (glBlendEquation). */
    public static int stringToBlendFunc(String s) {
        String n = s.trim().toLowerCase();
        if (n.equals("add")) return 32774;             // GL_FUNC_ADD
        if (n.equals("subtract")) return 32778;         // GL_FUNC_SUBTRACT
        if (n.equals("reversesubtract")) return 32779;  // GL_FUNC_REVERSE_SUBTRACT
        if (n.equals("reverse_subtract")) return 32779;
        if (n.equals("min")) return 32775;              // GL_MIN
        if (n.equals("max")) return 32776;              // GL_MAX
        return 32774;
    }

    /** Parsuje "one", "srcalpha", "1-srcalpha", itd. na int (glBlendFunc factor). */
    public static int stringToBlendFactor(String s) {
        String n = s.trim().toLowerCase().replaceAll("_", "").replaceAll("one minus", "1-");
        if (n.equals("0") || n.equals("zero")) return 0;                    // GL_ZERO
        if (n.equals("1") || n.equals("one")) return 1;                     // GL_ONE
        if (n.equals("srccolor")) return 768;                                // GL_SRC_COLOR
        if (n.equals("1-srccolor")) return 769;                              // GL_ONE_MINUS_SRC_COLOR
        if (n.equals("dstcolor")) return 774;                                // GL_DST_COLOR
        if (n.equals("1-dstcolor")) return 775;                              // GL_ONE_MINUS_DST_COLOR
        if (n.equals("srcalpha")) return 770;                                // GL_SRC_ALPHA
        if (n.equals("1-srcalpha")) return 771;                              // GL_ONE_MINUS_SRC_ALPHA
        if (n.equals("dstalpha")) return 772;                                // GL_DST_ALPHA
        if (n.equals("1-dstalpha")) return 773;                              // GL_ONE_MINUS_DST_ALPHA
        if (n.equals("constantcolor")) return 32769;                         // GL_CONSTANT_COLOR
        if (n.equals("1-constantcolor")) return 32770;                       // GL_ONE_MINUS_CONSTANT_COLOR
        if (n.equals("constantalpha")) return 32771;                         // GL_CONSTANT_ALPHA
        if (n.equals("1-constantalpha")) return 32772;                       // GL_ONE_MINUS_CONSTANT_ALPHA
        if (n.equals("srcalphasaturate")) return 776;                        // GL_SRC_ALPHA_SATURATE
        return -1;
    }
}
