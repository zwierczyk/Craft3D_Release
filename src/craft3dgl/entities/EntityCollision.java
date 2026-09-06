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

        /** Lokalny box {x0,y0,z0,x1,y1,z1} dla blokow czesciowych. */
        default double[] getCollisionBounds(int x, int y, int z) { return null; }
    }

    /** Czy bbox jest wolny (bez kolizji ze stalymi i czesciowymi blokami). */
    public static boolean isFreeAt(double cx, double cy, double cz,
                                    double radius, double height,
                                    SolidCheck w, int worldX, int worldY, int worldZ) {
        double entityX0 = cx - radius, entityX1 = cx + radius;
        double entityY0 = cy, entityY1 = cy + height;
        double entityZ0 = cz - radius, entityZ1 = cz + radius;
        int minX = (int)Math.floor(entityX0), maxX = (int)Math.floor(entityX1);
        int minY = (int)Math.floor(entityY0);
        int maxY = (int)Math.floor(entityY1 - 0.001);
        int minZ = (int)Math.floor(entityZ0), maxZ = (int)Math.floor(entityZ1);
        if (minX < 0 || minZ < 0 || maxX >= worldX || maxZ >= worldZ) return false;
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++) {
                    if (y < 0 || y >= worldY) continue;
                    if (w.isSolid(x, y, z)) return false;
                    double[] box = w.getCollisionBounds(x, y, z);
                    if (box != null
                            && entityX1 > x + box[0] && entityX0 < x + box[3]
                            && entityY1 > y + box[1] && entityY0 < y + box[4]
                            && entityZ1 > z + box[2] && entityZ0 < z + box[5]) {
                        return false;
                    }
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
