package craft3dgl.blaze3d.renderer;

import java.util.HashMap;
import java.util.Map;
import craft3dgl.blaze3d.shaders.EffectInstance;

/**
 * Render resources shared by the Minecraft 1.12 fixed-function path.
 * The later JSON core-shader experiment is retained as source code only; init()
 * deliberately creates the 1.12 lightmap without selecting/loading that
 * incompatible terrain pipeline.
 */
public class GameRenderer {
    private static final GameRenderer INSTANCE = new GameRenderer();
    private final Map<String, EffectInstance> shaders = new HashMap<>();
    private boolean initialized = false;
    /** White 1x1 lightmap - dummy fallback zanim damy prawdziwa 16x16 lightmapa. */
    private int whiteLightmapTexId = 0;
    /** Real 16x16 MC-style lightmap (X=block light, Y=sky light). Update per frame. */
    private craft3dgl.blaze3d.shadow.LightmapTexture lightmapTexture;
    /** MC-style animated water texture 16x16 (32 klatki). */
    private craft3dgl.blaze3d.shadow.WaterTexture waterTexture;
    public static GameRenderer getInstance() { return INSTANCE; }

    private GameRenderer() {}

    public boolean isInitialized() { return initialized; }

    /** Creates vanilla fixed-function resources after glfwMakeContextCurrent. */
    public void init() {
        if (initialized) return;
        System.out.println("[GameRenderer] init() - Minecraft 1.12 fixed-function lightmap");
        // White 1x1 lightmap fallback while the real map is being created.
        try {
            whiteLightmapTexId = org.lwjgl.opengl.GL11.glGenTextures();
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, whiteLightmapTexId);
            java.nio.ByteBuffer white = org.lwjgl.BufferUtils.createByteBuffer(4);
            white.put((byte)255).put((byte)255).put((byte)255).put((byte)255).flip();
            org.lwjgl.opengl.GL11.glTexImage2D(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0,
                org.lwjgl.opengl.GL11.GL_RGBA, 1, 1, 0,
                org.lwjgl.opengl.GL11.GL_RGBA, org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, white);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
                org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,
                org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER, org.lwjgl.opengl.GL11.GL_NEAREST);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0);
            System.out.println("[GameRenderer] white 1x1 lightmap texId=" + whiteLightmapTexId);
        } catch (Throwable t) {
            System.err.println("[GameRenderer] lightmap create failed: " + t);
        }
        // MC-style 16x16 lightmap texture (dynamiczna, update per frame)
        try {
            lightmapTexture = new craft3dgl.blaze3d.shadow.LightmapTexture();
        } catch (Throwable t) {
            System.err.println("[GameRenderer] lightmap create failed: " + t);
        }
        // Vanilla water_still.png animation (32 frames, two ticks each).
        try {
            waterTexture = new craft3dgl.blaze3d.shadow.WaterTexture();
        } catch (Throwable t) {
            System.err.println("[GameRenderer] water texture create failed: " + t);
        }
        // Core terrain shaders (GLSL 1.20, fixed-function compatible). Bez nich
        // rendertypeSolidShader() zwraca null i nowoczesny renderer cicho nic nie
        // rysuje - dlatego ladujemy je TUTAJ, raz na starcie.
        loadShader("rendertype_solid");
        loadShader("rendertype_cutout");
        loadShader("rendertype_translucent");
        initialized = true;
        System.out.println("[GameRenderer] init() DONE - vanilla lightmap/water ready");
    }

    public craft3dgl.blaze3d.shadow.LightmapTexture getLightmapTexture() { return lightmapTexture; }
    public int getLightmapTexId() { return lightmapTexture != null ? lightmapTexture.getTextureId() : whiteLightmapTexId; }
    public craft3dgl.blaze3d.shadow.WaterTexture getWaterTexture() { return waterTexture; }
    public int getWaterTexId() { return waterTexture != null ? waterTexture.getTextureId() : 0; }

    private void _dummy() {
    }

    public int getWhiteLightmapTexId() { return whiteLightmapTexId; }

    private void loadShader(String name) {
        try {
            EffectInstance shader = new EffectInstance(name, "shaders/core");
            shaders.put(name, shader);
            System.out.println("[GameRenderer]   loaded core/" + name);
        } catch (Exception e) {
            System.err.println("[GameRenderer]   FAILED core/" + name + ": " + e.getMessage());
        }
    }

    public EffectInstance getShader(String name) {
        return shaders.get(name);
    }

    public EffectInstance positionShader()          { return getShader("position"); }
    public EffectInstance positionColorShader()     { return getShader("position_color"); }
    public EffectInstance positionTexShader()       { return getShader("position_tex"); }
    public EffectInstance positionTexColorShader()  { return getShader("position_tex_color"); }
    private void _dummyEnd() {}

    public EffectInstance rendertypeSolidShader()   { return getShader("rendertype_solid"); }
    public EffectInstance rendertypeCutoutShader()  { return getShader("rendertype_cutout"); }
    public EffectInstance rendertypeTranslucentShader() { return getShader("rendertype_translucent"); }

    public void cleanup() {
        for (EffectInstance s : shaders.values()) {
            try { s.close(); } catch (Exception ignored) {}
        }
        shaders.clear();
        if (lightmapTexture != null) lightmapTexture.cleanup();
        if (waterTexture != null) waterTexture.cleanup();
        if (whiteLightmapTexId > 0) org.lwjgl.opengl.GL11.glDeleteTextures(whiteLightmapTexId);
        lightmapTexture = null;
        waterTexture = null;
        whiteLightmapTexId = 0;
        initialized = false;
    }
}
