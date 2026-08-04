package craft3dgl.physics;

import craft3dgl.combat.Hit;

import static craft3dgl.world.WorldConstants.AIR;
import static craft3dgl.world.WorldConstants.WATER;

/**
 * Raycast DDA (Digital Differential Analyzer) - wystrzeliwuje promień
 * z punktu w kierunku i zwraca pierwszy trafiony blok.
 */
public final class RayCaster {
    private RayCaster() {}

    public interface WorldAccessor {
        boolean inWorld(int x, int y, int z);
        int getBlock(int x, int y, int z);
    }

    /** Glowny raycast - dla bloków świata. */
    public static Hit cast(double ox, double oy, double oz,
                           double dx, double dy, double dz,
                           double maxDist, WorldAccessor w) {
        int ix = (int) Math.floor(ox), iy = (int) Math.floor(oy), iz = (int) Math.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? 1e30 : Math.abs(1 / dx);
        double tDeltaY = dy == 0 ? 1e30 : Math.abs(1 / dy);
        double tDeltaZ = dz == 0 ? 1e30 : Math.abs(1 / dz);
        double tMaxX = dx == 0 ? 1e30 : ((stepX > 0 ? ix + 1.0 : ix) - ox) / dx;
        double tMaxY = dy == 0 ? 1e30 : ((stepY > 0 ? iy + 1.0 : iy) - oy) / dy;
        double tMaxZ = dz == 0 ? 1e30 : ((stepZ > 0 ? iz + 1.0 : iz) - oz) / dz;
        double dist = 0;
        int nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < 120 && dist < maxDist; i++) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) { ix += stepX; dist = tMaxX; tMaxX += tDeltaX; nx = -stepX; ny = 0; nz = 0; }
            else if (tMaxY < tMaxZ) { iy += stepY; dist = tMaxY; tMaxY += tDeltaY; nx = 0; ny = -stepY; nz = 0; }
            else { iz += stepZ; dist = tMaxZ; tMaxZ += tDeltaZ; nx = 0; ny = 0; nz = -stepZ; }
            if (!w.inWorld(ix, iy, iz)) break;
            int id = w.getBlock(ix, iy, iz);
            if (id == WATER || id == AIR) continue;
            return new Hit(true, ix, iy, iz, nx, ny, nz, id, dist);
        }
        return Hit.MISS;
    }

    /** Tylko dystans do pierwszej przeszkody (np. dla kamery 3rd person). */
    public static double castDist(double ox, double oy, double oz,
                                  double dx, double dy, double dz,
                                  double maxDist, WorldAccessor w, int leavesId) {
        int ix = (int) Math.floor(ox), iy = (int) Math.floor(oy), iz = (int) Math.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDX = dx == 0 ? 1e30 : Math.abs(1 / dx);
        double tDY = dy == 0 ? 1e30 : Math.abs(1 / dy);
        double tDZ = dz == 0 ? 1e30 : Math.abs(1 / dz);
        double tMX = dx == 0 ? 1e30 : ((stepX > 0 ? ix + 1.0 : ix) - ox) / dx;
        double tMY = dy == 0 ? 1e30 : ((stepY > 0 ? iy + 1.0 : iy) - oy) / dy;
        double tMZ = dz == 0 ? 1e30 : ((stepZ > 0 ? iz + 1.0 : iz) - oz) / dz;
        double dist = 0;
        for (int i = 0; i < 200 && dist < maxDist; i++) {
            if (tMX < tMY && tMX < tMZ) { ix += stepX; dist = tMX; tMX += tDX; }
            else if (tMY < tMZ) { iy += stepY; dist = tMY; tMY += tDY; }
            else { iz += stepZ; dist = tMZ; tMZ += tDZ; }
            if (!w.inWorld(ix, iy, iz)) return dist;
            int id = w.getBlock(ix, iy, iz);
            if (id != AIR && id != WATER && id != leavesId) return dist;
        }
        return maxDist;
    }
}
