package craft3dgl.entities;

import java.util.ArrayList;
import java.util.Iterator;

import static org.lwjgl.opengl.GL11.*;

/**
 * System czasteczek - update fizyki + renderowanie billboardow.
 * Renderowanie posortowane: najpierw normalne alpha, potem additive (iskry) na koncu.
 */
public final class ParticleSystem {
    public final ArrayList<Particle> particles = new ArrayList<>();

    public void add(Particle p) {
        particles.add(p);
    }

    public void clear() {
        particles.clear();
    }

    public void update(double dt) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.age += dt;
            if (p.age >= p.maxAge) { it.remove(); continue; }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.z += p.vz * dt;
            p.vy -= p.gravity * dt;
            p.vx *= p.drag;
            p.vz *= p.drag;
            // smoke i splash unosi sie
            if (p.kind == 6) { p.vy += 1.6 * dt; } // dym idzie w gore
        }
    }

    public void draw(double playerYaw, double playerPitch) {
        if (particles.isEmpty()) return;
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        double cp = Math.cos(playerPitch);
        double fx = Math.sin(playerYaw) * cp;
        double fz = Math.cos(playerYaw) * cp;
        double rx = fz;
        double rz = -fx;

        // Pass 1: normalne blending
        for (Particle p : particles) {
            if (p.additive) continue;
            float alpha = (float) Math.max(0.0, 1.0 - p.age / p.maxAge);
            drawByKind(p, rx, rz, alpha);
        }
        // Pass 2: additive
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        for (Particle p : particles) {
            if (!p.additive) continue;
            float alpha = (float) Math.max(0.0, 1.0 - p.age / p.maxAge);
            drawByKind(p, rx, rz, alpha);
        }

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
        glColor4f(1, 1, 1, 1);
    }

    private static void drawByKind(Particle p, double rx, double rz, float alpha) {
        switch (p.kind) {
            case 0: drawHeart(p.x, p.y, p.z, rx, rz, alpha); break;
            case 1: drawDamage(p.x, p.y, p.z, rx, rz, alpha); break;
            case 2: drawBlockChunk(p, rx, rz, alpha); break;
            case 3: drawSplash(p, rx, rz, alpha); break;
            case 4: drawDust(p, rx, rz, alpha); break;
            case 5: drawSpark(p, rx, rz, alpha); break;
            case 6: drawSmoke(p, rx, rz, alpha); break;
            case 7: drawLeaf(p, rx, rz, alpha); break;
        }
    }

    private static void drawHeart(double x, double y, double z, double rx, double rz, float alpha) {
        double s = 0.18;
        int[][] heart = {
            {0, 1, 1, 0, 1, 1, 0},
            {1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 1, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0}
        };
        double px = s / 7.0;
        double py = s / 6.0;
        for (int yi = 0; yi < 6; yi++) {
            for (int xi = 0; xi < 7; xi++) {
                if (heart[yi][xi] == 0) continue;
                double cx = (xi - 3) * px;
                double cy = (2.5 - yi) * py;
                double x0 = x + rx * cx;
                double z0 = z + rz * cx;
                double x1 = x + rx * (cx + px);
                double z1 = z + rz * (cx + px);
                double y0 = y + cy;
                double y1 = y + cy + py;
                glColor4f(0.92f, 0.10f, 0.12f, alpha);
                glBegin(GL_QUADS);
                glVertex3d(x0, y0, z0);
                glVertex3d(x1, y0, z1);
                glVertex3d(x1, y1, z1);
                glVertex3d(x0, y1, z0);
                glEnd();
            }
        }
        glColor4f(1.0f, 0.65f, 0.65f, alpha);
        double hx = -2 * px;
        double hy = 1.5 * py;
        glBegin(GL_QUADS);
        glVertex3d(x + rx * hx, y + hy, z + rz * hx);
        glVertex3d(x + rx * (hx + px), y + hy, z + rz * (hx + px));
        glVertex3d(x + rx * (hx + px), y + hy + py, z + rz * (hx + px));
        glVertex3d(x + rx * hx, y + hy + py, z + rz * hx);
        glEnd();
    }

    private static void drawBlockChunk(Particle p, double rx, double rz, float alpha) {
        double s = p.size;
        double phi = p.age * 12.0;
        double cs = Math.cos(phi), sn = Math.sin(phi);
        glColor4f(p.r, p.g, p.b, alpha);
        glBegin(GL_QUADS);
        glVertex3d(p.x + rx * (-s * cs + s * sn), p.y - s, p.z + rz * (-s * cs + s * sn));
        glVertex3d(p.x + rx * (s * cs + s * sn), p.y - s, p.z + rz * (s * cs + s * sn));
        glVertex3d(p.x + rx * (s * cs - s * sn), p.y + s, p.z + rz * (s * cs - s * sn));
        glVertex3d(p.x + rx * (-s * cs - s * sn), p.y + s, p.z + rz * (-s * cs - s * sn));
        glEnd();
        glColor4f(p.r * 0.5f, p.g * 0.5f, p.b * 0.5f, alpha);
        glBegin(GL_LINE_LOOP);
        glVertex3d(p.x + rx * (-s * cs + s * sn), p.y - s, p.z + rz * (-s * cs + s * sn));
        glVertex3d(p.x + rx * (s * cs + s * sn), p.y - s, p.z + rz * (s * cs + s * sn));
        glVertex3d(p.x + rx * (s * cs - s * sn), p.y + s, p.z + rz * (s * cs - s * sn));
        glVertex3d(p.x + rx * (-s * cs - s * sn), p.y + s, p.z + rz * (-s * cs - s * sn));
        glEnd();
    }

    private static void drawSplash(Particle p, double rx, double rz, float alpha) {
        double s = p.size;
        // Jasny cyjan-niebieski
        glColor4f(0.60f, 0.85f, 1.00f, alpha * 0.85f);
        drawQuadBillboard(p.x, p.y, p.z, rx, rz, s);
        // Bialy blyszczyk w srodku
        glColor4f(1f, 1f, 1f, alpha * 0.6f);
        drawQuadBillboard(p.x, p.y, p.z, rx, rz, s * 0.5);
    }

    private static void drawDust(Particle p, double rx, double rz, float alpha) {
        double s = p.size;
        glColor4f(p.r, p.g, p.b, alpha * 0.7f);
        drawQuadBillboard(p.x, p.y, p.z, rx, rz, s);
    }

    private static void drawSpark(Particle p, double rx, double rz, float alpha) {
        double s = p.size;
        // Zewnetrzny glow
        glColor4f(1.0f, 0.55f, 0.15f, alpha * 0.5f);
        drawQuadBillboard(p.x, p.y, p.z, rx, rz, s * 2.0);
        // Wewnetrzny jasny
        glColor4f(1.0f, 0.95f, 0.55f, alpha);
        drawQuadBillboard(p.x, p.y, p.z, rx, rz, s);
        // Bialy hot spot
        glColor4f(1f, 1f, 1f, alpha);
        drawQuadBillboard(p.x, p.y, p.z, rx, rz, s * 0.4);
    }

    private static void drawSmoke(Particle p, double rx, double rz, float alpha) {
        double growth = 1.0 + p.age * 0.8;
        double s = p.size * growth;
        // Szary z zanikiem
        glColor4f(p.r, p.g, p.b, alpha * 0.55f);
        drawQuadBillboard(p.x, p.y, p.z, rx, rz, s);
    }

    private static void drawLeaf(Particle p, double rx, double rz, float alpha) {
        double s = p.size;
        double phi = p.age * 5.0;
        double cs = Math.cos(phi), sn = Math.sin(phi);
        glColor4f(p.r, p.g, p.b, alpha);
        glBegin(GL_QUADS);
        glVertex3d(p.x + rx * (-s * cs), p.y - s * 0.3, p.z + rz * (-s * cs));
        glVertex3d(p.x + rx * (s * cs), p.y - s * 0.3, p.z + rz * (s * cs));
        glVertex3d(p.x + rx * (s * cs - s * sn * 0.3), p.y + s * 0.6, p.z + rz * (s * cs - s * sn * 0.3));
        glVertex3d(p.x + rx * (-s * cs - s * sn * 0.3), p.y + s * 0.6, p.z + rz * (-s * cs - s * sn * 0.3));
        glEnd();
    }

    private static void drawDamage(double x, double y, double z, double rx, double rz, float alpha) {
        double s = 0.12;
        glColor4f(0.10f, 0.10f, 0.10f, alpha);
        drawQuadBillboard(x, y, z, rx, rz, s);
    }

    private static void drawQuadBillboard(double x, double y, double z, double rx, double rz, double s) {
        glBegin(GL_QUADS);
        glVertex3d(x + rx * (-s), y - s, z + rz * (-s));
        glVertex3d(x + rx * s, y - s, z + rz * s);
        glVertex3d(x + rx * s, y + s, z + rz * s);
        glVertex3d(x + rx * (-s), y + s, z + rz * (-s));
        glEnd();
    }
}
