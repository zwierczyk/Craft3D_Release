package craft3dgl.ui;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * Tło głównego menu: niebo, słońce, chmury, góry parallax, ziemia + drzewka.
 */
public final class MenuBackgroundRenderer {

    private MenuBackgroundRenderer() {}

    private static int hash(int a, int b) {
        int h = a * 73428767 ^ b * 9122719;
        h ^= h >>> 13;
        h *= 1274126177;
        return h;
    }

    public static void draw(int width, int height) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        double time = glfwGetTime();
        // Niebo gradient
        glBegin(GL_QUADS);
        glColor3f(0.20f, 0.43f, 0.78f); glVertex2i(0, 0); glVertex2i(width, 0);
        glColor3f(0.62f, 0.82f, 1.00f); glVertex2i(width, height); glVertex2i(0, height);
        glEnd();
        // Słońce
        int sunX = width - 210;
        int sunY = 82;
        glColor4f(1.0f, 0.85f, 0.20f, 0.18f); UIStyle.quad(sunX - 34, sunY - 34, 128, 128);
        glColor4f(1.0f, 0.92f, 0.25f, 1.0f); UIStyle.quad(sunX, sunY, 64, 64);
        glColor4f(1.0f, 1.0f, 0.60f, 0.55f); UIStyle.quad(sunX + 8, sunY + 8, 20, 20);
        // Chmury parallax
        for (int i = 0; i < 7; i++) {
            int base = i * 230;
            int cx = (int)((base - time * (18 + i * 2)) % (width + 260));
            if (cx < -240) cx += width + 260;
            int cy = 70 + (i % 3) * 42;
            drawBlockCloud(cx, cy, 1.0f - i * 0.035f);
        }
        // Góry 2 warstwy
        drawMountainLayer(width, height, (int)(height * 0.54), 0.16f, 0.31f, 0.38f, 105, 0.82f);
        drawMountainLayer(width, height, (int)(height * 0.61), 0.12f, 0.24f, 0.22f, 78, 0.92f);
        // Ziemia
        int groundY = (int)(height * 0.74);
        for (int x = 0; x < width; x += 32) {
            int bump = ((x / 32) * 17) % 22;
            int gy = groundY + bump - 10;
            glColor4f(0.21f, 0.55f, 0.18f, 1f); UIStyle.quad(x, gy, 32, 10);
            glColor4f(0.37f, 0.23f, 0.12f, 1f); UIStyle.quad(x, gy + 10, 32, height - gy - 10);
            glColor4f(0.22f, 0.14f, 0.08f, 0.75f); UIStyle.quad(x, gy + 26, 32, 4);
        }
        // Drzewka
        drawMenuTree(80, groundY - 95, 1.25f);
        drawMenuTree(width - 145, groundY - 105, 1.35f);
        drawMenuTree(width - 300, groundY - 82, 1.0f);
        // Vignette
        glColor4f(0, 0, 0, 0.32f); UIStyle.quad(0, 0, width, 42);
        glColor4f(0, 0, 0, 0.34f); UIStyle.quad(0, height - 70, width, 70);
        glColor4f(0, 0, 0, 0.22f); UIStyle.quad(0, 0, 80, height);
        glColor4f(0, 0, 0, 0.22f); UIStyle.quad(width - 80, 0, 80, height);
        glDisable(GL_BLEND);
        glColor4f(1,1,1,1);
    }

    private static void drawBlockCloud(int x, int y, float shade) {
        glColor4f(shade, shade, shade, 0.78f);
        UIStyle.quad(x + 0, y + 18, 72, 24);
        UIStyle.quad(x + 42, y + 4, 78, 38);
        UIStyle.quad(x + 92, y + 18, 90, 24);
        UIStyle.quad(x + 130, y + 10, 52, 30);
        glColor4f(1f, 1f, 1f, 0.36f);
        UIStyle.quad(x + 48, y + 8, 30, 8);
        UIStyle.quad(x + 98, y + 20, 38, 7);
    }

    private static void drawMountainLayer(int width, int height, int baseY,
                                          float r, float g, float b, int step, float alpha) {
        glColor4f(r, g, b, alpha);
        glBegin(GL_TRIANGLES);
        for (int x = -step; x < width + step; x += step) {
            int h = 70 + Math.abs(hash(x / step, baseY) % 95);
            glVertex2i(x, baseY);
            glVertex2i(x + step / 2, baseY - h);
            glVertex2i(x + step, baseY);
        }
        glEnd();
        glColor4f(r * 0.75f, g * 0.75f, b * 0.75f, alpha);
        UIStyle.quad(0, baseY - 3, width, height - baseY + 3);
    }

    private static void drawMenuTree(int x, int y, float scale) {
        int s = (int)(16 * scale);
        glColor4f(0.36f, 0.20f, 0.09f, 1f);
        UIStyle.quad(x + s, y + s * 3, s, s * 4);
        glColor4f(0.08f, 0.36f, 0.12f, 1f);
        UIStyle.quad(x, y + s, s * 3, s * 3);
        UIStyle.quad(x + s, y, s * 3, s * 3);
        UIStyle.quad(x + s * 2, y + s, s * 3, s * 3);
        glColor4f(0.15f, 0.52f, 0.16f, 0.80f);
        UIStyle.quad(x + s, y + s, s, s);
        UIStyle.quad(x + s * 3, y + s * 2, s, s);
    }
}
