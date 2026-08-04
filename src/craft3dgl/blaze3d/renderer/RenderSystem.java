package craft3dgl.blaze3d.renderer;

import craft3dgl.blaze3d.shaders.EffectInstance;

/**
 * MC 1.17+ style RenderSystem - globalny stan renderingu.
 * setShader(...) ustawia aktualny shader ktory bedzie uzywany.
 * setShaderColor(r,g,b,a) ustawia globalny ColorModulator.
 * setShaderFogColor/Start/End dla fog uniformow.
 */
public class RenderSystem {
    private static EffectInstance currentShader = null;
    private static final float[] shaderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
    private static final float[] fogColor = { 0.5f, 0.7f, 1.0f, 1.0f };
    private static float fogStart = 32.0f;
    private static float fogEnd = 128.0f;
    private static boolean fogEnabled = false;

    public static void setShader(EffectInstance shader) {
        currentShader = shader;
    }
    public static EffectInstance getShader() { return currentShader; }

    public static void setShaderColor(float r, float g, float b, float a) {
        shaderColor[0] = r; shaderColor[1] = g; shaderColor[2] = b; shaderColor[3] = a;
    }
    public static float[] getShaderColor() { return shaderColor; }

    public static void setShaderFogColor(float r, float g, float b, float a) {
        fogColor[0] = r; fogColor[1] = g; fogColor[2] = b; fogColor[3] = a;
    }
    public static float[] getShaderFogColor() { return fogColor; }

    public static void setShaderFogStart(float v) { fogStart = v; }
    public static void setShaderFogEnd(float v) { fogEnd = v; }
    public static float getShaderFogStart() { return fogStart; }
    public static float getShaderFogEnd() { return fogEnd; }

    public static void setFogEnabled(boolean e) { fogEnabled = e; }
    public static boolean isFogEnabled() { return fogEnabled; }

    /** Aplikuje aktualny shader + wszystkie uniformy. Wolaj przed draw. */
    public static void applyShader() {
        if (currentShader == null) return;
        // Ustaw uniformy przed apply
        craft3dgl.blaze3d.shaders.AbstractUniform cm = currentShader.safeGetUniform("ColorModulator");
        cm.set(shaderColor[0], shaderColor[1], shaderColor[2], shaderColor[3]);

        craft3dgl.blaze3d.shaders.AbstractUniform fc = currentShader.safeGetUniform("FogColor");
        fc.set(fogColor[0], fogColor[1], fogColor[2], fogColor[3]);

        craft3dgl.blaze3d.shaders.AbstractUniform fs = currentShader.safeGetUniform("FogStart");
        fs.set(fogStart);
        craft3dgl.blaze3d.shaders.AbstractUniform fe = currentShader.safeGetUniform("FogEnd");
        fe.set(fogEnabled ? fogEnd : 1000000.0f);

        currentShader.apply();
    }

    public static void clearShader() {
        if (currentShader != null) currentShader.clear();
    }
}
