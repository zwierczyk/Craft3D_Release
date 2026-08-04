package craft3dgl.world;

import static org.lwjgl.opengl.GL11.*;

/**
 * Rysuje slonce, ksiezyc i gwiazdy w tle jako duze billboardy w skyboxie.
 * Zawsze rysowane w 2D orto z pitch/yaw kamery zeby udawac obracajace niebo.
 */
public final class CelestialRenderer {
    private CelestialRenderer() {}

    /**
     * Rysuje slonce/ksiezyc/gwiazdy. dayFraction 0..1 (0=noc, 0.25=wschod, 0.5=poludnie, 0.75=zachod).
     * yaw/pitch gracza sluza do przesuniecia na ekranie (paralaksa).
     */
    public static void draw(int screenWidth, int screenHeight, double yaw, double pitch, double dayFraction) {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, screenWidth, screenHeight, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_FOG);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glShadeModel(GL_SMOOTH);

        // Slonce - pozycja zmienia sie z day fraction po ekranie (arka)
        double sunAngle = (dayFraction - 0.25) * Math.PI * 2; // -pi/2 = wschod
        double sunX = screenWidth * 0.5 + Math.cos(sunAngle) * screenWidth * 0.6;
        double sunY = screenHeight * 0.55 - Math.sin(sunAngle) * screenHeight * 0.7;
        // Paralaksa z yaw - lekki offset
        sunX += Math.sin(-yaw) * screenWidth * 0.15;
        sunY += pitch * screenHeight * 0.35;

        // Widocznosc slonca: 0..1 - najlepiej gdy jest na niebie
        double sunVis = Math.max(0, Math.sin(sunAngle + Math.PI * 0.5));
        if (sunVis > 0.01) {
            // Glow zewn (pomarancz)
            double gr = 90;
            glColor4f(1.0f, 0.85f, 0.55f, (float)(sunVis * 0.35));
            drawDisc(sunX, sunY, gr);
            // Slonce (jasnozolte)
            glColor4f(1.0f, 0.98f, 0.80f, (float)(sunVis * 0.98));
            drawDisc(sunX, sunY, 42);
            // Highlight bialy
            glColor4f(1f, 1f, 1f, (float)(sunVis * 0.9));
            drawDisc(sunX, sunY, 26);
        }

        // Ksiezyc - dokladnie po przeciwnej stronie
        double moonAngle = sunAngle + Math.PI;
        double moonX = screenWidth * 0.5 + Math.cos(moonAngle) * screenWidth * 0.6;
        double moonY = screenHeight * 0.55 - Math.sin(moonAngle) * screenHeight * 0.7;
        moonX += Math.sin(-yaw) * screenWidth * 0.15;
        moonY += pitch * screenHeight * 0.35;
        double moonVis = Math.max(0, Math.sin(moonAngle + Math.PI * 0.5));
        if (moonVis > 0.01) {
            // Glow niebieski
            glColor4f(0.75f, 0.85f, 1.0f, (float)(moonVis * 0.35));
            drawDisc(moonX, moonY, 60);
            // Ksiezyc jasnobialy
            glColor4f(0.96f, 0.97f, 1.0f, (float)(moonVis * 0.95));
            drawDisc(moonX, moonY, 32);
            // Kratery - male ciemniejsze plamy
            glColor4f(0.78f, 0.80f, 0.88f, (float)(moonVis * 0.85));
            drawDisc(moonX - 8, moonY - 4, 6);
            drawDisc(moonX + 6, moonY + 7, 4);
            drawDisc(moonX - 3, moonY + 9, 3);
        }

        // Gwiazdy - tylko w nocy (dayFraction blisko 0 lub 1)
        double nightAmount = 1.0 - Math.max(0, Math.min(1, Math.sin(dayFraction * Math.PI * 2 - Math.PI * 0.5) + 0.3));
        nightAmount = Math.max(0, Math.min(1, nightAmount - 0.2));
        if (nightAmount > 0.05) {
            drawStars(screenWidth, screenHeight, yaw, pitch, (float) nightAmount);
        }

        glShadeModel(GL_FLAT);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
        glEnable(GL_FOG);
        glEnable(GL_DEPTH_TEST);
        glColor4f(1, 1, 1, 1);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private static void drawDisc(double cx, double cy, double r) {
        int segs = 32;
        glBegin(GL_TRIANGLE_FAN);
        glVertex2d(cx, cy);
        for (int i = 0; i <= segs; i++) {
            double a = i * Math.PI * 2.0 / segs;
            glVertex2d(cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        glEnd();
    }

    private static void drawStars(int w, int h, double yaw, double pitch, float alpha) {
        // Deterministyczne "gwiazdy" - hash z i,j daje pozycje
        double offX = Math.sin(-yaw) * w * 0.25;
        double offY = pitch * h * 0.4;
        for (int i = 0; i < 140; i++) {
            int hash = (int)(i * 2654435761L) ^ (int)(i * 2246822519L);
            double sx = ((hash & 0xFFFF) / 65535.0) * w * 1.3 - w * 0.15;
            double sy = (((hash >>> 16) & 0xFFFF) / 65535.0) * h * 0.75;
            sx += offX; sy += offY;
            double br = 0.5 + ((hash >>> 8) & 15) / 30.0;
            double twinkle = 0.75 + 0.25 * Math.sin(i * 0.7 + System.currentTimeMillis() * 0.002);
            float a = (float)(alpha * br * twinkle);
            double r = 1.2 + ((hash >>> 12) & 3) * 0.6;
            glColor4f(1f, 1f, 1f, a * 0.4f);
            drawDisc(sx, sy, r * 2);
            glColor4f(1f, 1f, 1f, a);
            drawDisc(sx, sy, r);
        }
    }
}
