package craft3dgl.world;

import craft3dgl.combat.Hit;

import static craft3dgl.world.WorldConstants.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renderer efektu niszczenia bloku - klasyczne MC pekniecia w 10 stopniach
 * (0-9) oraz gladki neon outline wokol trafionego bloku.
 */
public final class BlockBreakRenderer {
    private BlockBreakRenderer() {}

    /** Rysuje outline i pekniecia dla trafionego bloku. */
    public static void draw(Hit h, Hit miningHit, double miningProgress,
                            DoorSystem doorSystem) {
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_FOG);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        boolean isMining = miningHit != null && miningHit.x == h.x
                && miningHit.y == h.y && miningHit.z == h.z;

        // === DOOR SPECIAL CASE ===
        if (h.block == DOOR_BOTTOM || h.block == DOOR_TOP) {
            drawDoorOutline(h, doorSystem, isMining, miningProgress);
            glEnable(GL_FOG);
            glEnable(GL_TEXTURE_2D);
            glColor4f(1, 1, 1, 1);
            return;
        }

        double e = 0.005;
        double x0 = h.x - e, x1 = h.x + 1 + e;
        double y0 = h.y - e, y1 = h.y + 1 + e;
        double z0 = h.z - e, z1 = h.z + 1 + e;

        // === NEON HIGHLIGHT wokol calego bloku - subtelny biały glow ===
        if (isMining) {
            // W trakcie kopania - cieplejszy pomaranczowo-czerwony (progresywny)
            float r = (float)(1.0 - miningProgress * 0.3);
            float g = (float)(0.6 - miningProgress * 0.3);
            float b = (float)(0.2 - miningProgress * 0.15);
            drawBlockOutline(x0, y0, z0, x1, y1, z1, r, g, b, 4);
        } else {
            // Bez kopania - subtelny bialy outline
            drawBlockOutline(x0, y0, z0, x1, y1, z1, 0.05f, 0.05f, 0.05f, 2);
        }

        // === PEKNIECIA na trafionej scianie (jak MC destroy stage 0-9) ===
        if (isMining) {
            drawCrackPattern(h, miningProgress);
        }

        glLineWidth(1f);
        glEnable(GL_FOG);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    /** Rysuje pełny outline calego bloku (wszystkie 12 krawędzi) z glow. */
    private static void drawBlockOutline(double x0, double y0, double z0,
                                          double x1, double y1, double z1,
                                          float r, float g, float b, float lineWidth) {
        // Cienki, wyrazny outline (bez blur bo w OpenGL 2.1 nie ma glow shaderow)
        glLineWidth(lineWidth);
        glColor4f(r, g, b, 0.95f);
        glBegin(GL_LINES);
        // Dolna ramka
        glVertex3d(x0, y0, z0); glVertex3d(x1, y0, z0);
        glVertex3d(x1, y0, z0); glVertex3d(x1, y0, z1);
        glVertex3d(x1, y0, z1); glVertex3d(x0, y0, z1);
        glVertex3d(x0, y0, z1); glVertex3d(x0, y0, z0);
        // Gorna ramka
        glVertex3d(x0, y1, z0); glVertex3d(x1, y1, z0);
        glVertex3d(x1, y1, z0); glVertex3d(x1, y1, z1);
        glVertex3d(x1, y1, z1); glVertex3d(x0, y1, z1);
        glVertex3d(x0, y1, z1); glVertex3d(x0, y1, z0);
        // Pionowe
        glVertex3d(x0, y0, z0); glVertex3d(x0, y1, z0);
        glVertex3d(x1, y0, z0); glVertex3d(x1, y1, z0);
        glVertex3d(x1, y0, z1); glVertex3d(x1, y1, z1);
        glVertex3d(x0, y0, z1); glVertex3d(x0, y1, z1);
        glEnd();
    }

    /**
     * Rysuje wzór pęknięć na scianie bloku w 10 etapach (0-9).
     * Wzór jest półprzezroczysty - ciemny na wierzchu tekstury.
     * Im większa progress, tym więcej i grubszych pęknięć.
     */
    private static void drawCrackPattern(Hit h, double progress) {
        int stage = Math.min(9, (int)(progress * 10));  // 0-9 (jak w MC)

        // Wybierz ścianę na której rysować (tę na którą patrzymy)
        double e = 0.008;
        double sx0, sy0, sz0, sx1, sy1, sz1;
        // 4 wierzchołki sciany (do rysowania pol-przezroczystych quadow z peknieciami)

        // Ciemne quady z peknieciami - rysujemy jako polprzezrocyste ciemne linie
        glLineWidth(1.5f + stage * 0.35f);
        glColor4f(0.0f, 0.0f, 0.0f, 0.35f + stage * 0.06f);
        glBegin(GL_LINES);

        // Wzor peknien - deterministyczne dla stagie (jak w MC destroy stage 0-9)
        // Rysujemy w koordynatach lokalnych 0..1 na scianie, mapujemy na 3D
        double[][] patterns = getCrackPattern(stage);
        for (double[] line : patterns) {
            double a1 = line[0], b1 = line[1];  // punkt 1 (lokalne 0..1)
            double a2 = line[2], b2 = line[3];  // punkt 2
            drawCrackLine(h, a1, b1, a2, b2, e);
        }
        glEnd();
        glLineWidth(1f);
    }

    /** Mapuje 2D punkt na scianie (a,b w [0,1]) do 3D w swiecie. */
    private static void drawCrackLine(Hit h, double a1, double b1, double a2, double b2, double e) {
        double x0 = h.x, y0 = h.y, z0 = h.z;
        if (h.ny != 0) {
            // Gora/dol - a=x, b=z
            double yy = h.ny > 0 ? y0 + 1 + e : y0 - e;
            glVertex3d(x0 + a1, yy, z0 + b1);
            glVertex3d(x0 + a2, yy, z0 + b2);
        } else if (h.nx != 0) {
            // Lewo/prawo - a=z, b=y
            double xx = h.nx > 0 ? x0 + 1 + e : x0 - e;
            glVertex3d(xx, y0 + b1, z0 + a1);
            glVertex3d(xx, y0 + b2, z0 + a2);
        } else {
            // Przod/tyl - a=x, b=y
            double zz = h.nz > 0 ? z0 + 1 + e : z0 - e;
            glVertex3d(x0 + a1, y0 + b1, zz);
            glVertex3d(x0 + a2, y0 + b2, zz);
        }
    }

    /** Wzor peknien dla danego stage 0-9 (deterministyczny, jak w MC). */
    private static double[][] getCrackPattern(int stage) {
        // Ilość linii rośnie z etapami; każda kolejna dodaje więcej pęknięć
        // Format: {a1, b1, a2, b2} gdzie a,b w [0,1] to pozycja na ścianie
        double[][] allLines = {
            // Stage 0 - jedna cienka
            {0.3, 0.2, 0.7, 0.5},
            // Stage 1 - dodaj kolejną
            {0.2, 0.7, 0.6, 0.4},
            // Stage 2 - kierunek prawie pionowy
            {0.5, 0.1, 0.4, 0.9},
            // Stage 3 - poprzeczna
            {0.1, 0.4, 0.9, 0.5},
            // Stage 4 - dodaj krotka
            {0.6, 0.7, 0.8, 0.6},
            // Stage 5 - dolna czesc
            {0.2, 0.8, 0.5, 0.7},
            // Stage 6 - gora prawa
            {0.7, 0.1, 0.9, 0.3},
            // Stage 7 - drobna
            {0.15, 0.15, 0.35, 0.35},
            // Stage 8 - lewa strona
            {0.05, 0.5, 0.25, 0.6},
            // Stage 9 - koncowa
            {0.5, 0.5, 0.7, 0.85},
            // Dodatkowe drobne (rysuje sie od stage 3+)
            {0.35, 0.45, 0.55, 0.55},
            {0.65, 0.35, 0.75, 0.45},
            {0.4, 0.25, 0.5, 0.35},
        };
        int count = Math.min(stage + 1 + stage / 3, allLines.length);
        double[][] result = new double[count][];
        for (int i = 0; i < count; i++) result[i] = allLines[i];
        return result;
    }

    private static void drawDoorOutline(Hit h, DoorSystem doorSystem, boolean isMining, double progress) {
        int meta = doorSystem.getMeta(h.x, h.y, h.z);
        double[] aabb = DoorSystem.doorAabb(meta);
        double e = 0.005;
        double dx0 = h.x + aabb[0] - e, dz0 = h.z + aabb[1] - e;
        double dx1 = h.x + aabb[2] + e, dz1 = h.z + aabb[3] + e;
        double dy0 = h.y - e, dy1 = h.y + 1 + e;
        float r = 0.05f, g = 0.05f, b = 0.05f;
        if (isMining) {
            r = (float)(1.0 - progress * 0.3);
            g = (float)(0.6 - progress * 0.3);
            b = (float)(0.2 - progress * 0.15);
        }
        drawBlockOutline(dx0, dy0, dz0, dx1, dy1, dz1, r, g, b, 3);
        if (isMining) {
            int stage = Math.min(9, (int)(progress * 10));
            glLineWidth(1.5f + stage * 0.35f);
            glColor4f(0.0f, 0.0f, 0.0f, 0.35f + stage * 0.06f);
            glBegin(GL_LINES);
            for (double[] line : getCrackPattern(stage)) {
                // Rysuj pekniecia na frontowej ścianie panelu drzwi
                double a1 = line[0], b1 = line[1];
                double a2 = line[2], b2 = line[3];
                double sx1 = dx0 + a1 * (dx1 - dx0);
                double sx2 = dx0 + a2 * (dx1 - dx0);
                double sy1 = dy0 + b1 * (dy1 - dy0);
                double sy2 = dy0 + b2 * (dy1 - dy0);
                glVertex3d(sx1, sy1, dz1);
                glVertex3d(sx2, sy2, dz1);
            }
            glEnd();
        }
    }
}
