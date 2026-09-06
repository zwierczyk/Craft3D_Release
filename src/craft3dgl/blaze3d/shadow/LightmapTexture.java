package craft3dgl.blaze3d.shadow;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * 16x16 combined sky/block-light texture using Minecraft 1.12's
 * EntityRenderer.updateLightmap colour equations.
 */
public class LightmapTexture {
    private static final int SIZE = 16;
    private final int textureId;
    private final ByteBuffer buffer;
    private final float[] brightness = new float[16];

    public LightmapTexture() {
        textureId = GL11.glGenTextures();
        buffer = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        for (int level = 0; level < 16; level++) {
            float darkness = 1.0f - level / 15.0f;
            brightness[level] = (1.0f - darkness) / (darkness * 3.0f + 1.0f);
        }
        for (int i = 0; i < SIZE * SIZE * 4; i++) buffer.put((byte) 255);
        buffer.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                SIZE, SIZE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    public void update(float dayMult) {
        update(dayMult, 0f);
    }

    public void update(float dayMult, float nightVisionBoost) {
        buffer.clear();
        // dayMult is World.getSunBrightness (0.2 at night, 1.0 at noon).
        float sunBrightness = clamp(dayMult);
        float skyMultiplier = sunBrightness * 0.95f + 0.05f;
        float nightVision = clamp(nightVisionBoost / 0.85f);

        for (int sky = 0; sky < SIZE; sky++) {
            float skyLight = brightness[sky] * skyMultiplier;
            for (int block = 0; block < SIZE; block++) {
                // torchFlickerX is omitted, but vanilla's +1.5 base multiplier is retained.
                float blockLight = brightness[block] * 1.5f;
                float skyColour = skyLight * (sunBrightness * 0.65f + 0.35f);
                float blockGreen = blockLight * ((blockLight * 0.6f + 0.4f) * 0.6f + 0.4f);
                float blockBlue = blockLight * (blockLight * blockLight * 0.6f + 0.4f);

                float red = (skyColour + blockLight) * 0.96f + 0.03f;
                float green = (skyColour + blockGreen) * 0.96f + 0.03f;
                float blue = (skyLight + blockBlue) * 0.96f + 0.03f;

                if (nightVision > 0.0f) {
                    float maxComponent = Math.max(red, Math.max(green, blue));
                    if (maxComponent > 0.0f) {
                        float normalize = 1.0f / maxComponent;
                        red = red * (1.0f - nightVision) + red * normalize * nightVision;
                        green = green * (1.0f - nightVision) + green * normalize * nightVision;
                        blue = blue * (1.0f - nightVision) + blue * normalize * nightVision;
                    }
                }

                // Vanilla performs this final 0.96 + 0.03 pass after gamma correction.
                red = clamp(red) * 0.96f + 0.03f;
                green = clamp(green) * 0.96f + 0.03f;
                blue = clamp(blue) * 0.96f + 0.03f;
                buffer.put((byte) Math.round(clamp(red) * 255.0f));
                buffer.put((byte) Math.round(clamp(green) * 255.0f));
                buffer.put((byte) Math.round(clamp(blue) * 255.0f));
                buffer.put((byte) 255);
            }
        }
        buffer.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, SIZE, SIZE,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public int getTextureId() {
        return textureId;
    }

    public void cleanup() {
        GL11.glDeleteTextures(textureId);
    }
}
