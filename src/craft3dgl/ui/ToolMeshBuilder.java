package craft3dgl.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import craft3dgl.save.AssetFinder;
import craft3dgl.MinecraftGL;

/**
 * MC-style ItemModelGenerator voxelizer.
 *
 * Bierze plaska teksture 16x16 (kilof/miecz/siekiera) i buduje z niej
 * 3D "kanapke" z prawdziwa grubosci - dla kazdego nieprzezroczystego pixela
 * generuje kwadrat na przod + tyl + boki. Rezultat: item wyglada 3D
 * z kazdego katu, bez dziurawych krawedzi.
 *
 * Cache: mesh dla kazdego itemu budowany raz i uzywany wielokrotnie.
 */
public final class ToolMeshBuilder {
    private ToolMeshBuilder() {}

    /** Jeden voxel 3D w mesh (piksel z tekstury * grubosc). */
    public static class Voxel {
        public final float x, y;       // pozycja w gridzie 0..15
        public final int rgba;         // kolor pixela (packed argb)
        public Voxel(float x, float y, int rgba) {
            this.x = x; this.y = y; this.rgba = rgba;
        }
    }

    /** Sasiedztwo pixela - czy sasiad jest przezroczysty (=potrzeba boku). */
    public static class VoxelMesh {
        public final Voxel[] voxels;
        public final boolean[] hasUp, hasDown, hasLeft, hasRight;
        public final int width, height;
        public VoxelMesh(int w, int h, Voxel[] v, boolean[] up, boolean[] down, boolean[] left, boolean[] right) {
            this.width = w; this.height = h;
            this.voxels = v;
            this.hasUp = up; this.hasDown = down; this.hasLeft = left; this.hasRight = right;
        }
    }

    private static final Map<Integer, VoxelMesh> CACHE = new HashMap<>();
    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        try {
            File dir = AssetFinder.findAssetDir("items", ToolMeshBuilder.class);
            if (dir == null) return;
            loadOne(dir, "wooden_pickaxe.png", MinecraftGL.ITEM_WOOD_PICKAXE);
            loadOne(dir, "stone_pickaxe.png",  MinecraftGL.ITEM_STONE_PICKAXE);
            loadOne(dir, "wooden_axe.png",     MinecraftGL.ITEM_WOOD_AXE);
            loadOne(dir, "stone_axe.png",      MinecraftGL.ITEM_STONE_AXE);
            loadOne(dir, "wooden_sword.png",   MinecraftGL.ITEM_WOOD_SWORD);
            loadOne(dir, "stone_sword.png",    MinecraftGL.ITEM_STONE_SWORD);
            loadOne(dir, "wooden_hoe.png",     MinecraftGL.ITEM_WOOD_HOE);
            loadOne(dir, "stone_hoe.png",      MinecraftGL.ITEM_STONE_HOE);
            loadOne(dir, "wooden_shovel.png",  MinecraftGL.ITEM_WOOD_SHOVEL);
            loadOne(dir, "stone_shovel.png",   MinecraftGL.ITEM_STONE_SHOVEL);
            loadOne(dir, "bread.png",          MinecraftGL.ITEM_BREAD);
            loadOne(dir, "emerald.png",        MinecraftGL.ITEM_EMERALD);
            loadOne(dir, "wheat_seeds.png",    MinecraftGL.ITEM_SEEDS);
            loadOne(dir, "wheat.png",          MinecraftGL.ITEM_WHEAT);
            loadOne(dir, "beef.png",           MinecraftGL.ITEM_BEEF);
            loadOne(dir, "mutton.png",         MinecraftGL.ITEM_MUTTON);
            loadOne(dir, "porkchop.png",       MinecraftGL.ITEM_PORK);
            loadOne(dir, "oak_door.png",        MinecraftGL.DOOR_BOTTOM);
            System.out.println("[ToolMeshBuilder] voxelized " + CACHE.size() + " tools");
            loaded = true;
        } catch (Throwable t) {
            System.err.println("[ToolMeshBuilder] load failed: " + t);
            t.printStackTrace();
        }
    }

    private static void loadOne(File dir, String filename, int itemId) {
        File f = new File(dir, filename);
        if (!f.isFile()) return;
        try {
            BufferedImage img = ImageIO.read(f);
            int w = img.getWidth(), h = img.getHeight();
            // Zbierz vosele
            java.util.List<Voxel> voxels = new java.util.ArrayList<>();
            java.util.List<Boolean> up = new java.util.ArrayList<>();
            java.util.List<Boolean> down = new java.util.ArrayList<>();
            java.util.List<Boolean> left = new java.util.ArrayList<>();
            java.util.List<Boolean> right = new java.util.ArrayList<>();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >> 24) & 0xFF;
                    if (a < 128) continue;   // przezroczysty pixel - skip
                    voxels.add(new Voxel(x, y, argb));
                    // Czy sasiad ponizej jest przezroczysty?
                    up.add(isTransparent(img, x, y - 1, w, h));
                    down.add(isTransparent(img, x, y + 1, w, h));
                    left.add(isTransparent(img, x - 1, y, w, h));
                    right.add(isTransparent(img, x + 1, y, w, h));
                }
            }
            Voxel[] va = voxels.toArray(new Voxel[0]);
            boolean[] upA = toBool(up), downA = toBool(down), leftA = toBool(left), rightA = toBool(right);
            CACHE.put(itemId, new VoxelMesh(w, h, va, upA, downA, leftA, rightA));
        } catch (Exception e) {
            System.err.println("[ToolMeshBuilder] " + filename + ": " + e);
        }
    }

    private static boolean isTransparent(BufferedImage img, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || x >= w || y >= h) return true;
        int a = (img.getRGB(x, y) >> 24) & 0xFF;
        return a < 128;
    }

    private static boolean[] toBool(java.util.List<Boolean> l) {
        boolean[] a = new boolean[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    public static VoxelMesh getMesh(int itemId) {
        return CACHE.get(itemId);
    }
}
