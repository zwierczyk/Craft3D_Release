package craft3dgl.physics;

import craft3dgl.combat.Hit;

import static craft3dgl.world.WorldConstants.AIR;
import static craft3dgl.world.WorldConstants.WATER;

/**
 * Raycast DDA (Digital Differential Analyzer). Pelne bloki korzystaja z
 * granic komorki, a bloki o niestandardowym ksztalcie (np. drzwi 3/16)
 * moga zwrocic dokladny selection box.
 */
public final class RayCaster {
    private static final double EPSILON = 1.0e-7;

    private RayCaster() {}

    public interface WorldAccessor {
        boolean inWorld(int x, int y, int z);
        int getBlock(int x, int y, int z);

        /**
         * Lokalny box {x0,y0,z0,x1,y1,z1}; null oznacza pelna komorke.
         */
        default double[] getSelectionBounds(int x, int y, int z) {
            return null;
        }
    }

    /** Glowny raycast - dla blokow swiata. */
    public static Hit cast(double ox, double oy, double oz,
                           double dx, double dy, double dz,
                           double maxDist, WorldAccessor world) {
        int ix = (int)Math.floor(ox), iy = (int)Math.floor(oy), iz = (int)Math.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double tDeltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double tDeltaZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double tMaxX = dx == 0 ? Double.POSITIVE_INFINITY : ((stepX > 0 ? ix + 1.0 : ix) - ox) / dx;
        double tMaxY = dy == 0 ? Double.POSITIVE_INFINITY : ((stepY > 0 ? iy + 1.0 : iy) - oy) / dy;
        double tMaxZ = dz == 0 ? Double.POSITIVE_INFINITY : ((stepZ > 0 ? iz + 1.0 : iz) - oz) / dz;
        double distance = 0.0;
        int nx = 0, ny = 0, nz = 0;

        for (int i = 0; i < 120 && distance <= maxDist; i++) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                ix += stepX;
                distance = tMaxX;
                tMaxX += tDeltaX;
                nx = -stepX; ny = 0; nz = 0;
            } else if (tMaxY < tMaxZ) {
                iy += stepY;
                distance = tMaxY;
                tMaxY += tDeltaY;
                nx = 0; ny = -stepY; nz = 0;
            } else {
                iz += stepZ;
                distance = tMaxZ;
                tMaxZ += tDeltaZ;
                nx = 0; ny = 0; nz = -stepZ;
            }

            if (distance > maxDist || !world.inWorld(ix, iy, iz)) break;
            int id = world.getBlock(ix, iy, iz);
            if (id == WATER || id == AIR) continue;

            double[] bounds = world.getSelectionBounds(ix, iy, iz);
            if (bounds == null) {
                return new Hit(true, ix, iy, iz, nx, ny, nz, id, distance);
            }

            RayIntersection exact = intersectAabb(ox, oy, oz, dx, dy, dz,
                    ix + bounds[0], iy + bounds[1], iz + bounds[2],
                    ix + bounds[3], iy + bounds[4], iz + bounds[5], maxDist);
            if (exact != null && exact.distance + EPSILON >= distance) {
                return new Hit(true, ix, iy, iz, exact.nx, exact.ny, exact.nz, id, exact.distance);
            }
        }
        return Hit.MISS;
    }

    /** Tylko dystans do pierwszej przeszkody (np. dla kamery 3rd person). */
    public static double castDist(double ox, double oy, double oz,
                                  double dx, double dy, double dz,
                                  double maxDist, WorldAccessor world, int leavesId) {
        int ix = (int)Math.floor(ox), iy = (int)Math.floor(oy), iz = (int)Math.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dx);
        double tDeltaY = dy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dy);
        double tDeltaZ = dz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dz);
        double tMaxX = dx == 0 ? Double.POSITIVE_INFINITY : ((stepX > 0 ? ix + 1.0 : ix) - ox) / dx;
        double tMaxY = dy == 0 ? Double.POSITIVE_INFINITY : ((stepY > 0 ? iy + 1.0 : iy) - oy) / dy;
        double tMaxZ = dz == 0 ? Double.POSITIVE_INFINITY : ((stepZ > 0 ? iz + 1.0 : iz) - oz) / dz;
        double distance = 0.0;

        for (int i = 0; i < 200 && distance <= maxDist; i++) {
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                ix += stepX; distance = tMaxX; tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                iy += stepY; distance = tMaxY; tMaxY += tDeltaY;
            } else {
                iz += stepZ; distance = tMaxZ; tMaxZ += tDeltaZ;
            }
            if (distance > maxDist) return maxDist;
            if (!world.inWorld(ix, iy, iz)) return distance;

            int id = world.getBlock(ix, iy, iz);
            if (id == AIR || id == WATER || id == leavesId) continue;
            double[] bounds = world.getSelectionBounds(ix, iy, iz);
            if (bounds == null) return distance;

            RayIntersection exact = intersectAabb(ox, oy, oz, dx, dy, dz,
                    ix + bounds[0], iy + bounds[1], iz + bounds[2],
                    ix + bounds[3], iy + bounds[4], iz + bounds[5], maxDist);
            if (exact != null && exact.distance + EPSILON >= distance) return exact.distance;
        }
        return maxDist;
    }

    private static RayIntersection intersectAabb(double ox, double oy, double oz,
                                                  double dx, double dy, double dz,
                                                  double minX, double minY, double minZ,
                                                  double maxX, double maxY, double maxZ,
                                                  double maxDistance) {
        double near = 0.0;
        double far = maxDistance;
        int hitNx = 0, hitNy = 0, hitNz = 0;

        if (Math.abs(dx) < EPSILON) {
            if (ox < minX || ox > maxX) return null;
        } else {
            double first = (minX - ox) / dx;
            double second = (maxX - ox) / dx;
            if (first > second) { double swap = first; first = second; second = swap; }
            if (first > near) {
                near = first;
                hitNx = dx > 0 ? -1 : 1; hitNy = 0; hitNz = 0;
            }
            far = Math.min(far, second);
            if (near > far) return null;
        }

        if (Math.abs(dy) < EPSILON) {
            if (oy < minY || oy > maxY) return null;
        } else {
            double first = (minY - oy) / dy;
            double second = (maxY - oy) / dy;
            if (first > second) { double swap = first; first = second; second = swap; }
            if (first > near) {
                near = first;
                hitNx = 0; hitNy = dy > 0 ? -1 : 1; hitNz = 0;
            }
            far = Math.min(far, second);
            if (near > far) return null;
        }

        if (Math.abs(dz) < EPSILON) {
            if (oz < minZ || oz > maxZ) return null;
        } else {
            double first = (minZ - oz) / dz;
            double second = (maxZ - oz) / dz;
            if (first > second) { double swap = first; first = second; second = swap; }
            if (first > near) {
                near = first;
                hitNx = 0; hitNy = 0; hitNz = dz > 0 ? -1 : 1;
            }
            far = Math.min(far, second);
            if (near > far) return null;
        }

        if (far < 0.0 || near > maxDistance) return null;
        return new RayIntersection(Math.max(0.0, near), hitNx, hitNy, hitNz);
    }

    private static final class RayIntersection {
        final double distance;
        final int nx, ny, nz;

        RayIntersection(double distance, int nx, int ny, int nz) {
            this.distance = distance;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
        }
    }
}
