package craft3dgl.ui;

import static org.lwjgl.opengl.GL11.*;

/** Minecraft 1.12-style crosshair with the crosshair attack-strength indicator. */
public final class Crosshair {
    private Crosshair() {}

    /** Kept for old call sites; vanilla 1.12 does not draw a custom hit ring. */
    public static void triggerHit() {}

    public static void draw(int width, int height, double miningProgress,
                            double currentTime, float attackStrength) {
        int cx = width / 2;
        int cy = height / 2;
        int arm = 8;
        int gap = 3;
        int thickness = 2;

        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Black one-pixel outline around the vanilla white cross.
        glColor4f(0f, 0f, 0f, 0.85f);
        UIStyle.quad(cx - 1, cy - gap - arm - 1, thickness + 2, arm + 2);
        UIStyle.quad(cx - 1, cy + gap - 1, thickness + 2, arm + 2);
        UIStyle.quad(cx - gap - arm - 1, cy - 1, arm + 2, thickness + 2);
        UIStyle.quad(cx + gap - 1, cy - 1, arm + 2, thickness + 2);
        UIStyle.quad(cx - 2, cy - 2, 4, 4);

        glColor4f(1f, 1f, 1f, 1f);
        UIStyle.quad(cx - 1, cy - gap - arm, thickness, arm);
        UIStyle.quad(cx - 1, cy + gap, thickness, arm);
        UIStyle.quad(cx - gap - arm, cy - 1, arm, thickness);
        UIStyle.quad(cx + gap, cy - 1, arm, thickness);
        UIStyle.quad(cx - 1, cy - 1, thickness, thickness);

        // GuiIngame's 17-pixel attack indicator, shown while the weapon recharges.
        float strength = Math.max(0.0f, Math.min(1.0f, attackStrength));
        if (strength < 1.0f) {
            int barX = cx - 9;
            int barY = cy + 17;
            int fill = Math.round(17.0f * strength);
            glColor4f(0f, 0f, 0f, 0.8f);
            UIStyle.quad(barX, barY, 19, 5);
            glColor4f(0.30f, 0.30f, 0.30f, 1f);
            UIStyle.quad(barX + 1, barY + 1, 17, 3);
            if (fill > 0) {
                glColor4f(1f, 1f, 1f, 1f);
                UIStyle.quad(barX + 1, barY + 1, fill, 3);
            }
        }

        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1f, 1f, 1f, 1f);
    }

    public static void draw(int width, int height, double miningProgress, double currentTime) {
        draw(width, height, miningProgress, currentTime, 1.0f);
    }

    public static void draw(int width, int height) {
        draw(width, height, 0.0, System.nanoTime() * 1e-9, 1.0f);
    }
}
