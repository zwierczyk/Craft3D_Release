package craft3dgl.world;

import java.util.Random;

import static craft3dgl.world.WorldConstants.*;

/**
 * Generator drzew: dąb (oak), duży dąb, sosna (pine).
 * Wymaga callbacka do setBlockRaw (bo nie ma bezpośredniego dostępu do world[][][]).
 */
public final class TreeGenerator {
    private TreeGenerator() {}

    public interface BlockSetter {
        void setBlockRaw(int x, int y, int z, int id);
        int getBlockRaw(int x, int y, int z);
        boolean inWorld(int x, int y, int z);
    }

    public static void makeOakTree(int x, int y, int z, Random rand, BlockSetter w) {
        int trunk = 5 + rand.nextInt(3);
        for (int i = 0; i < trunk; i++) w.setBlockRaw(x, y + i, z, WOOD);
        int top = y + trunk;
        for (int dx = -3; dx <= 3; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -3; dz <= 3; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 0.75);
                    if (d < 3.05) {
                        int bx = x + dx, by = top + dy, bz = z + dz;
                        if (w.inWorld(bx, by, bz) && w.getBlockRaw(bx, by, bz) == AIR) {
                            w.setBlockRaw(bx, by, bz, LEAVES);
                        }
                    }
                }
    }

    public static void makeBigOakTree(int x, int y, int z, Random rand, BlockSetter w) {
        int trunk = 7 + rand.nextInt(3);
        for (int i = 0; i < trunk; i++) {
            w.setBlockRaw(x, y + i, z, WOOD);
            if (i < trunk - 2) {
                w.setBlockRaw(x + 1, y + i, z, WOOD);
                w.setBlockRaw(x, y + i, z + 1, WOOD);
                w.setBlockRaw(x + 1, y + i, z + 1, WOOD);
            }
        }
        int top = y + trunk;
        for (int dx = -4; dx <= 5; dx++)
            for (int dy = -3; dy <= 3; dy++)
                for (int dz = -4; dz <= 5; dz++) {
                    double cx = dx - 0.5, cz = dz - 0.5;
                    double d = Math.sqrt(cx * cx + cz * cz + dy * dy * 0.65);
                    if (d < 4.05) {
                        int bx = x + dx, by = top + dy, bz = z + dz;
                        if (w.inWorld(bx, by, bz) && w.getBlockRaw(bx, by, bz) == AIR) {
                            w.setBlockRaw(bx, by, bz, LEAVES);
                        }
                    }
                }
        // Branches
        for (int b = 0; b < 4; b++) {
            int sx = b == 0 ? 1 : b == 1 ? -1 : 0;
            int sz = b == 2 ? 1 : b == 3 ? -1 : 0;
            for (int i = 1; i <= 3; i++) {
                w.setBlockRaw(x + sx * i, y + trunk - 2 + i / 2, z + sz * i, WOOD);
            }
        }
    }

    public static void makePineTree(int x, int y, int z, Random rand, BlockSetter w) {
        int trunk = 8 + rand.nextInt(5);
        for (int i = 0; i < trunk; i++) w.setBlockRaw(x, y + i, z, WOOD);
        for (int layer = 0; layer < trunk - 2; layer++) {
            int cy = y + trunk - layer;
            int radius = Math.max(1, 4 - layer / 2);
            if (layer > trunk / 2) radius = Math.max(1, 3 - (layer - trunk / 2) / 2);
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) <= radius + 1) {
                        int bx = x + dx, bz = z + dz;
                        if (w.inWorld(bx, cy, bz) && w.getBlockRaw(bx, cy, bz) == AIR) {
                            w.setBlockRaw(bx, cy, bz, LEAVES);
                        }
                    }
                }
        }
        w.setBlockRaw(x, y + trunk + 1, z, LEAVES);
    }
}
