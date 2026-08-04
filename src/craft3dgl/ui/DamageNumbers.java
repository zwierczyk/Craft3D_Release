package craft3dgl.ui;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Popup liczb obrazen ktore leca do gory z zanikiem. Rysowane w 2D nad HUD.
 * Wymaga pozycji ekranowej (projekcja swiatowa -> screen musi byc juz obliczona po stronie MinecraftGL).
 */
public final class DamageNumbers {
    public static class Popup {
        public double worldX, worldY, worldZ;
        public String text;
        public double age;
        public double maxAge;
        public float r, g, b;
        public double floatSpeed;
        public double offsetY; // opcjonalny pixel offset
    }

    private static final ArrayList<Popup> pops = new ArrayList<>();

    public static void spawn(double wx, double wy, double wz, String text, float r, float g, float b) {
        Popup p = new Popup();
        p.worldX = wx; p.worldY = wy; p.worldZ = wz;
        p.text = text;
        p.maxAge = 1.2;
        p.r = r; p.g = g; p.b = b;
        p.floatSpeed = 35;
        pops.add(p);
    }

    public static void clear() { pops.clear(); }

    public static ArrayList<Popup> getPopups() { return pops; }

    public static void update(double dt) {
        Iterator<Popup> it = pops.iterator();
        while (it.hasNext()) {
            Popup p = it.next();
            p.age += dt;
            p.offsetY += p.floatSpeed * dt;
            p.floatSpeed *= 0.95;
            if (p.age >= p.maxAge) it.remove();
        }
    }
}
