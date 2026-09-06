package craft3dgl.world;

import java.util.HashMap;

/**
 * Stan debowych drzwi zgodny z zachowaniem BlockDoor/ItemDoor z Minecraft 1.12.
 * Obie polowki drzwi przechowuja ten sam scalony stan:
 *
 *   bity 0-1: facing w kolejnosci MC S-W-N-E
 *   bit 2:     open
 *   bit 3:     right hinge (brak bitu = left hinge)
 *
 * Stan zawiasu jest wybierany raz przy stawianiu. Nie zalezy od strony,
 * z ktorej gracz pozniej otwiera drzwi.
 */
public final class DoorSystem {
    public static final int FACING_MASK = 3;
    public static final int OPEN_BIT = 4;
    public static final int RIGHT_HINGE_BIT = 8;
    public static final double THICKNESS = 3.0 / 16.0;

    public final HashMap<Long, Integer> meta = new HashMap<>();

    /** Dostep do blokow potrzebny przez algorytm wyboru zawiasu z ItemDoor. */
    public interface PlacementWorld {
        boolean isNormalCube(int x, int y, int z);
        boolean isDoor(int x, int y, int z);
    }

    public static long packKey(int x, int y, int z) {
        return ((long)(x & 0x3FF) << 22) | ((long)(y & 0x3FF) << 11) | (long)(z & 0x3FF);
    }

    public int getMeta(int x, int y, int z) {
        Integer value = meta.get(packKey(x, y, z));
        return value == null ? 0 : value;
    }

    public void setMeta(int x, int y, int z, int value) {
        meta.put(packKey(x, y, z), value & 15);
    }

    public void removeMeta(int x, int y, int z) {
        meta.remove(packKey(x, y, z));
    }

    public void clear() {
        meta.clear();
    }

    public static int makeMeta(int facing, boolean open, boolean rightHinge) {
        return (facing & FACING_MASK)
                | (open ? OPEN_BIT : 0)
                | (rightHinge ? RIGHT_HINGE_BIT : 0);
    }

    public static int facing(int meta) {
        return meta & FACING_MASK;
    }

    public static boolean isOpen(int meta) {
        return (meta & OPEN_BIT) != 0;
    }

    public static boolean isRightHinge(int meta) {
        return (meta & RIGHT_HINGE_BIT) != 0;
    }

    /** Zmienia tylko OPEN; facing i zawias pozostaja niezmienne. */
    public static int withOpen(int meta, boolean open) {
        return open ? (meta | OPEN_BIT) : (meta & ~OPEN_BIT);
    }

    /**
     * Craft3D ma dodatni yaw skierowany odwrotnie niz Minecraft, dlatego
     * konwertujemy znak przed zastosowaniem minecraftowej kolejnosci S-W-N-E.
     */
    public static int yawToFacing(double yawRadians) {
        return ((int)Math.floor(Math.toDegrees(-yawRadians) / 90.0 + 0.5)) & FACING_MASK;
    }

    /** Wektor kierunku dla facing S-W-N-E. */
    public static int[] frontOffset(int facing) {
        switch (facing & FACING_MASK) {
            case 0: return new int[]{0, 1};   // south
            case 1: return new int[]{-1, 0};  // west
            case 2: return new int[]{0, -1};  // north
            default: return new int[]{1, 0};  // east
        }
    }

    public static int rotateY(int facing) {
        return (facing + 1) & FACING_MASK;
    }

    public static int rotateYCCW(int facing) {
        return (facing + 3) & FACING_MASK;
    }

    /**
     * Wierne przeniesienie decyzji o zawiasie z ItemDoor.placeDoor (MC 1.12):
     * pozycja klikniecia wybiera strone domyslna, sasiednie pelne bloki oraz
     * drugie drzwi wymuszaja poprawny zawias dla podwojnych drzwi.
     */
    public static boolean chooseRightHinge(int x, int y, int z, int facing,
                                           double hitX, double hitZ,
                                           PlacementWorld world) {
        int[] front = frontOffset(facing);
        boolean rightHinge = (front[0] < 0 && hitZ < 0.5)
                || (front[0] > 0 && hitZ > 0.5)
                || (front[1] < 0 && hitX > 0.5)
                || (front[1] > 0 && hitX < 0.5);

        int[] right = frontOffset(rotateY(facing));
        int[] left = frontOffset(rotateYCCW(facing));
        int rightX = x + right[0], rightZ = z + right[1];
        int leftX = x + left[0], leftZ = z + left[1];

        int leftBlocks = (world.isNormalCube(leftX, y, leftZ) ? 1 : 0)
                + (world.isNormalCube(leftX, y + 1, leftZ) ? 1 : 0);
        int rightBlocks = (world.isNormalCube(rightX, y, rightZ) ? 1 : 0)
                + (world.isNormalCube(rightX, y + 1, rightZ) ? 1 : 0);
        boolean doorOnLeft = world.isDoor(leftX, y, leftZ)
                || world.isDoor(leftX, y + 1, leftZ);
        boolean doorOnRight = world.isDoor(rightX, y, rightZ)
                || world.isDoor(rightX, y + 1, rightZ);

        if ((!doorOnLeft || doorOnRight) && rightBlocks <= leftBlocks) {
            if ((doorOnRight && !doorOnLeft) || rightBlocks < leftBlocks) {
                rightHinge = false;
            }
        } else {
            rightHinge = true;
        }
        return rightHinge;
    }

    /**
     * AABB drzwi w lokalnych wspolrzednych komorki: {x0, z0, x1, z1}.
     * Ksztalty i grubosc 3/16 odpowiadaja BlockDoor.getBoundingBox z MC 1.12.
     */
    public static double[] doorAabb(int meta) {
        int facing = facing(meta);
        boolean closed = !isOpen(meta);
        boolean rightHinge = isRightHinge(meta);

        switch (facing) {
            case 3: // east
                return closed ? eastAabb() : (rightHinge ? northAabb() : southAabb());
            case 0: // south
                return closed ? southAabb() : (rightHinge ? eastAabb() : westAabb());
            case 1: // west
                return closed ? westAabb() : (rightHinge ? southAabb() : northAabb());
            case 2: // north
            default:
                return closed ? northAabb() : (rightHinge ? westAabb() : eastAabb());
        }
    }

    /** Lokalny selection/collision box: {x0,y0,z0,x1,y1,z1}. */
    public static double[] doorBox(int meta) {
        double[] box = doorAabb(meta);
        return new double[]{box[0], 0.0, box[1], box[2], 1.0, box[3]};
    }

    private static double[] southAabb() {
        return new double[]{0.0, 0.0, 1.0, THICKNESS};
    }

    private static double[] northAabb() {
        return new double[]{0.0, 1.0 - THICKNESS, 1.0, 1.0};
    }

    private static double[] westAabb() {
        return new double[]{1.0 - THICKNESS, 0.0, 1.0, 1.0};
    }

    private static double[] eastAabb() {
        return new double[]{0.0, 0.0, THICKNESS, 1.0};
    }
}
