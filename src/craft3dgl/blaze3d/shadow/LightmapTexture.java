package craft3dgl.blaze3d.shadow;

import java.nio.ByteBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/**
 * MC-style 16x16 lightmap texture.
 *   U axis = block light (torch, glowstone) 0..15
 *   V axis = sky light 0..15
 *   Kolor pixela = jak jasno rysowac blok o tym poziomie oswietlenia.
 *
 * W dzien: prawy gorny pixel (bl=15, sky=15) = jasny bialy
 * W nocy: sky-light effective = sky*0.15, wiec cala prawa kolumna sciemnia sie
 * Torches (bl=15) - zawsze jasne, ciepla barwa
 *
 * Update per frame - reaguje na czas dnia.
 */
public class LightmapTexture {
    private final int textureId;
    private static final int SIZE = 16;
    private final ByteBuffer buffer;
    private int debugFrameCnt = 0;

    public LightmapTexture() {
        textureId = GL11.glGenTextures();
        buffer = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
        // Init - white
        for (int i = 0; i < SIZE * SIZE * 4; i++) buffer.put((byte)255);
        buffer.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
            SIZE, SIZE, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_CLAMP_TO_EDGE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        System.out.println("[LightmapTexture] Created 16x16 texId=" + textureId);
    }

    /** Backward compat overload. */
    public void update(float dayMult) {
        update(dayMult, 0f);
    }

    /**
     * Update lightmap texture based on time of day + night vision boost.
     * dayMult: 0.15 = noc, 1.0 = dzien
     * nightVisionBoost: 0.0 = brak, 0.85 = widoczne w calkowitej ciemnosci
     */
    public void update(float dayMult, float nightVisionBoost) {
        buffer.clear();
        // MC 1.14-style: minShade w nocy 0.05, w dzien 0.20 (widoczne ciemne strony bloków)
        // Noc jest naprawde ciemna - trzeba pochodni.
        // Night vision = wymusza minimum shade np. 0.85
        float minShade = 0.05f + dayMult * 0.15f;
        if (nightVisionBoost > minShade) minShade = nightVisionBoost;
        for (int sky = 0; sky < SIZE; sky++) {
            float skyEff = sky * dayMult / 15f;
            for (int bl = 0; bl < SIZE; bl++) {
                float blLev = bl / 15f;
                float lv = Math.max(skyEff, blLev);
                // Krzywa gamma jak MC: pow(lv, 1.4) - ostrzejsza gamma, ciemniejsze ciemne
                float shade = Math.max(minShade, (float)Math.pow(lv, 1.4) * 0.90f + minShade * 0.5f);
                // Torch/block light = cieply kolor (ciepla poziomka), sky = neutralny
                // Mieszamy proporcjonalnie
                float torchWeight = (blLev > skyEff) ? 1.0f : blLev / Math.max(0.01f, lv);
                float skyWeight = 1f - torchWeight;
                float r = shade * (skyWeight * 1.00f + torchWeight * 1.00f);
                float g = shade * (skyWeight * 1.00f + torchWeight * 0.80f);
                float b = shade * (skyWeight * 1.00f + torchWeight * 0.55f);
                buffer.put((byte)(Math.min(255, r * 255)));
                buffer.put((byte)(Math.min(255, g * 255)));
                buffer.put((byte)(Math.min(255, b * 255)));
                buffer.put((byte)255);
            }
        }
        buffer.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, SIZE, SIZE,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        // DEBUG: co ~120 klatek wypisz kluczowe pixele lightmapy
        debugFrameCnt++;
        if (debugFrameCnt % 120 == 0) {
            int centerPixel = (15 * SIZE + 15) * 4;  // sky=15, bl=15 -> pixel [15,15]
            int r15 = buffer.get(centerPixel) & 0xFF;
            int r0sky15 = buffer.get((15 * SIZE + 0) * 4) & 0xFF; // sky=15, bl=0
            int r0sky0 = buffer.get(0) & 0xFF; // sky=0, bl=0
            System.out.println("[LightmapDBG] dayMult=" + String.format("%.3f", dayMult)
                + " sky15bl15=" + r15 + " sky15bl0=" + r0sky15 + " sky0bl0=" + r0sky0);
        }
    }

    public int getTextureId() { return textureId; }

    public void cleanup() {
        GL11.glDeleteTextures(textureId);
    }
}
