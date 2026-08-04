package craft3dgl.entities;

import static craft3dgl.world.WorldConstants.*;

/**
 * Helper kolizji dla zwierzat i villagerow:
 * - sprawdzanie czy bbox jest wolny
 * - znajdowanie podlogi pod bboxem
 * - test czy entity jest w wodzie
 *
 * Wymaga callbacka do solid() z MinecraftGL (bo solid uzywa doorMeta).
 */
public final class EntityCollision {
    private EntityCollision() {}

    public interface SolidCheck {
        boolean isSolid(int x, int y, int z);
        int getBlock(int x, int y, int z);
        boolean inWorld(int x, int y, int z);
    }

    /** Czy bbox jest wolny (bez kolizji ze stalymi blokami). */
    public static boolean isFreeAt(double cx, double cy, double cz,
                                    double radius, double height,
                                    SolidCheck w, int worldX, int worldY, int worldZ) {
        int minX = (int)Math.floor(cx - radius), maxX = (int)Math.floor(cx + radius);
        int minY = (int)Math.floor(cy);
        int maxY = (int)Math.floor(cy + height - 0.001);
        int minZ = (int)Math.floor(cz - radius), maxZ = (int)Math.floor(cz + radius);
        if (minX < 0 || minZ < 0 || maxX >= worldX || maxZ >= worldZ) return false;
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    if (y < 0 || y >= worldY) continue;
                    if (w.isSolid(x, y, z)) return false;
                }
        return true;
    }

    /** Znajdz najwyzsza podloge pod entity. Zwraca -1 jak nic ponizej. */
    public static double findFloorBelow(double cx, double startY, double cz,
                                         double radius, SolidCheck w) {
        int minX = (int)Math.floor(cx - radius), maxX = (int)Math.floor(cx + radius);
        int minZ = (int)Math.floor(cz - radius), maxZ = (int)Math.floor(cz + radius);
        int yStart = (int)Math.floor(startY - 0.001);
        int highest = -1;
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                for (int yy = yStart; yy >= 0; yy--) {
                    if (w.isSolid(x, yy, z)) {
                        if (yy > highest) highest = yy;
                        break;
                    }
                }
            }
        return highest < 0 ? -1 : (highest + 1);
    }

    /** Czy bbox entity zachodzi na wode. */
    public static boolean inWater(double cx, double cy, double cz,
                                   double radius, double height,
                                   SolidCheck w) {
        int minX = (int)Math.floor(cx - radius), maxX = (int)Math.floor(cx + radius);
        int minY = (int)Math.floor(cy);
        int maxY = (int)Math.floor(cy + height);
        int minZ = (int)Math.floor(cz - radius), maxZ = (int)Math.floor(cz + radius);
        for (int bx = minX; bx <= maxX; bx++)
            for (int by = minY; by <= maxY; by++)
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (!w.inWorld(bx, by, bz)) continue;
                    if (w.getBlock(bx, by, bz) == WATER) return true;
                }
        return false;
    }

    /** Smooth yaw rotation toward target (uwzglednia wrap +-PI). */
    public static double smoothYaw(double current, double target, double maxStep) {
        double diff = target - current;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        if (Math.abs(diff) < maxStep) return target;
        return current + Math.signum(diff) * maxStep;
    }

    /** Smooth Y position. */
    public static double smoothY(double currentDisplay, double actualY, double maxStep) {
        double dy = actualY - currentDisplay;
        if (Math.abs(dy) < maxStep) return actualY;
        return currentDisplay + Math.signum(dy) * maxStep;
    }
}
