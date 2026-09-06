package craft3dgl.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import craft3dgl.save.AssetFinder;
import craft3dgl.MinecraftGL;

/**
 * Ladowanie tekstur narzedzi (kilofy, siekiery, miecze, motyki) z assets/items/.
 * Kazde narzedzie ma swoj tex id, uzywany przez drawToolIcon w hotbarze/inventory.
 */
public final class ToolTextures {
    private ToolTextures() {}

    private static final Map<Integer, Integer> ITEM_TO_TEX = new HashMap<>();
    private static boolean loaded = false;

    /** Ladowanie tekstur - raz na start, po init OpenGL. */
    public static void load() {
        if (loaded) return;
        try {
            File dir = AssetFinder.findAssetDir("items", ToolTextures.class);
            if (dir == null || !dir.isDirectory()) {
                System.err.println("[ToolTextures] items/ dir not found");
                return;
            }
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
            loadOne(dir, "oak_door.png",       MinecraftGL.DOOR_BOTTOM);
            loadOne(dir, "empty_armor_slot_helmet.png",     -100);
            loadOne(dir, "empty_armor_slot_chestplate.png", -101);
            loadOne(dir, "empty_armor_slot_leggings.png",   -102);
            loadOne(dir, "empty_armor_slot_boots.png",      -103);
            loadOne(dir, "empty_armor_slot_shield.png",     -104);
            System.out.println("[ToolTextures] loaded " + ITEM_TO_TEX.size() + " item textures");
            loaded = true;
        } catch (Throwable t) {
            System.err.println("[ToolTextures] load failed: " + t);
        }
    }

    private static void loadOne(File dir, String filename, int itemId) {
        File f = new File(dir, filename);
        if (!f.isFile()) {
            System.err.println("[ToolTextures] missing " + filename);
            return;
        }
        try {
            BufferedImage img = ImageIO.read(f);
            int w = img.getWidth(), h = img.getHeight();
            ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = img.getRGB(x, y);
                    buf.put((byte)((argb >> 16) & 0xFF));  // R
                    buf.put((byte)((argb >> 8)  & 0xFF));  // G
                    buf.put((byte)(argb & 0xFF));          // B
                    buf.put((byte)((argb >> 24) & 0xFF));  // A
                }
            }
            buf.flip();
            int tex = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w, h, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
            ITEM_TO_TEX.put(itemId, tex);
        } catch (Exception e) {
            System.err.println("[ToolTextures] error " + filename + ": " + e);
        }
    }

    /** Zwraca tex id lub -1 jesli nie zaladowano. */
    public static int getTexId(int itemId) {
        Integer id = ITEM_TO_TEX.get(itemId);
        return id != null ? id : -1;
    }

    /** Rysuje icon narzedzia w slocie inventory (16x16 texture rozciagnieta na quad). */
    public static boolean drawToolIcon(int itemId, int x, int y, int size) {
        int tex = getTexId(itemId);
        if (tex <= 0) return false;
        // Item rendering is called after terrain, GUI backgrounds and font draws.
        // Explicitly select vanilla's fixed-function texture unit/state so one icon
        // can never inherit another icon's texture or a world shader.
        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2i(x, y);
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2i(x + size, y);
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2i(x + size, y + size);
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2i(x, y + size);
        GL11.glEnd();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        return true;
    }

    /** Empty equipment slot sprites used by ContainerPlayer in Minecraft 1.12. */
    public static void drawEmptyArmorSlot(int armorIndex, int x, int y, int size) {
        if (armorIndex < 0 || armorIndex > 3) return;
        drawToolIcon(-100 - armorIndex, x, y, size);
    }

    public static void drawEmptyOffhandSlot(int x, int y, int size) {
        drawToolIcon(-104, x, y, size);
    }
}
