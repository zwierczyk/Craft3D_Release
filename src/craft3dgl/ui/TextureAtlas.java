package craft3dgl.ui;

import org.lwjgl.BufferUtils;

import craft3dgl.save.AssetFinder;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * Minecraft 1.12 block texture atlas. Vanilla PNGs are scaled with nearest
 * filtering and padded to avoid mip bleeding; procedural tiles are retained
 * only as an emergency missing-asset fallback.
 */
public final class TextureAtlas {
    public static final int TILE = 32;
    public static final int ATLAS_PAD = 2;
    public static final int ATLAS_TILE = TILE + ATLAS_PAD * 2;
    public static final int ATLAS_COLS = 24;
    public static final int ATLAS_ROWS = 1;

    private TextureAtlas() {}

    public static double atlasU0(int tile) {
        return (tile * ATLAS_TILE + ATLAS_PAD + 0.5) / (double)(ATLAS_COLS * ATLAS_TILE);
    }
    public static double atlasU1(int tile) {
        return (tile * ATLAS_TILE + ATLAS_PAD + TILE - 0.5) / (double)(ATLAS_COLS * ATLAS_TILE);
    }
    public static double atlasV0() {
        return (ATLAS_PAD + 0.5) / (double)(ATLAS_ROWS * ATLAS_TILE);
    }
    public static double atlasV1() {
        return (ATLAS_PAD + TILE - 0.5) / (double)(ATLAS_ROWS * ATLAS_TILE);
    }

    public static int createTextureAtlas() {
        BufferedImage img = new BufferedImage(ATLAS_COLS * ATLAS_TILE, ATLAS_ROWS * ATLAS_TILE, BufferedImage.TYPE_INT_ARGB);
        // Vanilla 1.12 textures. Plains biome tint comes from grass.png and
        // foliage.png at temperature 0.8 / rainfall 0.4.
        makeTilePngTinted(img, 0, "grass_top", 0x91BD59, 0x2f8a32, 0x155c26, "grass_top");
        makeTilePng(img, 1, "grass_side", 0x7a4e2d, 0x3c9a34, "grass_side");
        makeTilePng(img, 2, "dirt", 0x7b4d2e, 0x5a3823, "dirt");
        makeTilePng(img, 3, "stone", 0x787b82, 0x4f5157, "stone");
        makeTilePng(img, 4, "log_side", 0x8b5728, 0x5b351a, "log_side");
        makeTilePng(img, 5, "log_top", 0xa66c35, 0x5b351a, "log_top");
        makeTilePngTinted(img, 6, "oak_leaves", 0x77AB2F, 0x278b3c, 0x155c26, "leaves");
        makeTilePng(img, 7, "sand", 0xdcc47a, 0xb69b55, "sand");
        makeTilePng(img, 8, "planks", 0xa86f39, 0x5c361c, "planks");
        makeTilePng(img, 9, "craft_top", 0xa86f39, 0x4c2d18, "craft_top");
        makeTilePng(img, 10, "craft_side", 0x965f2d, 0x4c2d18, "craft_side");
        makeTilePng(img, 11, "water", 0x3c78d8, 0x1d4f9a, "water");
        makeTilePng(img, 12, "oak_door_bottom", 0xa56b32, 0x4c2d18, "door_bottom");
        makeTilePng(img, 13, "oak_door_top", 0xa56b32, 0x4c2d18, "door_top");
        // Chest blocks are rendered as MCP tile entities; these two atlas
        // entries remain only for legacy item thumbnails.
        makeTile(img, 14, 0, 0x9c6d35, 0x4a2d10, "chest_top");
        makeTile(img, 15, 0, 0xa97338, 0x4a2d10, "chest_side");
        makeTilePng(img, 16, "farmland", 0x5a3823, 0x3a2417, "farmland");
        makeTilePngTinted(img, 17, "tall_grass", 0x91BD59, 0x4faa3a, 0x2f8a32, "tall_grass");
        makeTilePng(img, 18, "wheat_0", 0x4faa3a, 0x2f7a32, "wheat_0");
        makeTilePng(img, 19, "wheat_1", 0x6fb83a, 0x3f9c32, "wheat_1");
        makeTilePng(img, 20, "wheat_2", 0x9fb83a, 0x6f9c32, "wheat_2");
        makeTilePng(img, 21, "wheat_3", 0xe8c248, 0xa68830, "wheat_3");
        makeTilePng(img, 22, "craft_front", 0x965f2d, 0x4c2d18, "craft_side");

        int tex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex);
        // TextureUtil.uploadTextureMipmap with blur=false in Minecraft 1.12.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        ByteBuffer buf = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
        for (int yy = 0; yy < img.getHeight(); yy++) {
            for (int xx = 0; xx < img.getWidth(); xx++) {
                int argb = img.getRGB(xx, yy);
                buf.put((byte) ((argb >> 16) & 255));
                buf.put((byte) ((argb >> 8) & 255));
                buf.put((byte) (argb & 255));
                buf.put((byte) ((argb >> 24) & 255));
            }
        }
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, img.getWidth(), img.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        try { glGenerateMipmap(GL_TEXTURE_2D); } catch (Throwable ignored) {}
        return tex;
    }

    /**
     * Ladujemy PNG z assets/blocks/<name>.png (16x16 albo 32x32) i wklejamy jako
     * tile w atlasie. Skalowanie NEAREST przez Graphics2D. Zwraca true jesli PNG istnial.
     */
    /**
     * Ladujemy PNG z assets/blocks/. Jesli tint != 0, mnozy kazdy pixel przez kolor tint
     * (0xRRGGBB). Uzywane dla trawy (szary PNG * zielony tint = zielona trawa).
     */
    private static boolean loadPngTile(BufferedImage atlas, int tileIndex, String pngName, int tint) {
        try {
            File dir = AssetFinder.findAssetDir("blocks", TextureAtlas.class);
            File file = new File(dir, pngName + ".png");
            if (!file.isFile()) return false;
            BufferedImage src = ImageIO.read(file);
            if (src == null) return false;
            // Animated vanilla textures store frames vertically. The fixed
            // 1.12 atlas currently displays frame zero rather than shrinking
            // the complete strip into one tile.
            if (src.getHeight() > src.getWidth()) {
                src = src.getSubimage(0, 0, src.getWidth(), src.getWidth());
            }

            // Jesli tint podany, przemnoz pixele PRZED wklejeniem do atlasu
            if (tint != 0) {
                int tr = (tint >> 16) & 0xff;
                int tg = (tint >> 8) & 0xff;
                int tb = tint & 0xff;
                BufferedImage tinted = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
                for (int yy = 0; yy < src.getHeight(); yy++) {
                    for (int xx = 0; xx < src.getWidth(); xx++) {
                        int argb = src.getRGB(xx, yy);
                        int a = (argb >> 24) & 0xff;
                        if (a == 0) { tinted.setRGB(xx, yy, argb); continue; }
                        int r = (argb >> 16) & 0xff;
                        int g = (argb >> 8) & 0xff;
                        int b = argb & 0xff;
                        // Multiply: srcColor * tintColor / 255
                        int nr = (r * tr) / 255;
                        int ng = (g * tg) / 255;
                        int nb = (b * tb) / 255;
                        tinted.setRGB(xx, yy, (a << 24) | (nr << 16) | (ng << 8) | nb);
                    }
                }
                src = tinted;
            }

            int x0 = tileIndex * ATLAS_TILE + ATLAS_PAD;
            int y0 = ATLAS_PAD;

            Graphics2D g = atlas.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(src, x0, y0, TILE, TILE, null);
            // Padding
            g.drawImage(src, x0, y0 - ATLAS_PAD, x0 + TILE, y0, 0, 0, src.getWidth(), 1, null);
            g.drawImage(src, x0, y0 + TILE, x0 + TILE, y0 + TILE + ATLAS_PAD,
                       0, src.getHeight() - 1, src.getWidth(), src.getHeight(), null);
            g.drawImage(src, x0 - ATLAS_PAD, y0, x0, y0 + TILE, 0, 0, 1, src.getHeight(), null);
            g.drawImage(src, x0 + TILE, y0, x0 + TILE + ATLAS_PAD, y0 + TILE,
                       src.getWidth() - 1, 0, src.getWidth(), src.getHeight(), null);
            g.dispose();
            return true;
        } catch (Throwable t) {
            System.err.println("[Atlas] blad ladowania " + pngName + ": " + t.getMessage());
            return false;
        }
    }

    /** Wersja bez tint. */
    private static boolean loadPngTile(BufferedImage atlas, int tileIndex, String pngName) {
        return loadPngTile(atlas, tileIndex, pngName, 0);
    }

    /**
     * makeTile z fallback - najpierw probuje zaladowac PNG, jesli brak, generuje proceduralnie.
     */
    private static void makeTilePng(BufferedImage img, int tileIndex, String pngName,
                                    int colA, int colB, String procName) {
        if (!loadPngTile(img, tileIndex, pngName, 0)) {
            makeTile(img, tileIndex, 0, colA, colB, procName);
        }
    }

    /**
     * makeTile z tintem - PNG jest grayscale, mnozony przez tintColor.
     * Dla trawy: PNG szary * zielony_biome = zielona trawa.
     */
    private static void makeTilePngTinted(BufferedImage img, int tileIndex, String pngName,
                                          int tintColor, int colA, int colB, String procName) {
        if (!loadPngTile(img, tileIndex, pngName, tintColor)) {
            makeTile(img, tileIndex, 0, colA, colB, procName);
        }
    }

    private static int hash(int a, int b) {
        int h = a * 73428767 ^ b * 9122719;
        h ^= h >>> 13;
        h *= 1274126177;
        return h;
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public static int jitter(int rgb, int amount) {
        int r = clampInt(((rgb >> 16) & 255) + amount, 0, 255);
        int g = clampInt(((rgb >> 8) & 255) + amount, 0, 255);
        int b = clampInt((rgb & 255) + amount, 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    public static int shadeColor(int rgb, double mul) {
        int r = clampInt((int)(((rgb >> 16) & 255) * mul), 0, 255);
        int g = clampInt((int)(((rgb >> 8) & 255) * mul), 0, 255);
        int b = clampInt((int)((rgb & 255) * mul), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    @SuppressWarnings("unused")
    private static void makeTile(BufferedImage img, int tx, int ty, int base, int dark, String type) {
        int ox = tx * ATLAS_TILE + ATLAS_PAD;
        int oy = ty * ATLAS_TILE + ATLAS_PAD;
        Random r = new Random(type.hashCode());
        for (int py = 0; py < TILE; py++) {
            for (int px = 0; px < TILE; px++) {
                int h = hash(px + type.hashCode(), py * 31 + type.length());
                int color = jitter(base, (h & 31) - 15);
                if ("grass_top".equals(type)) {
                    // Bogatsza trawa z jasniejszymi + ciemniejszymi zdzblami
                    color = jitter(((h >> 4) & 3) == 0 ? 0x3f9f35 : ((h >> 6) & 3) == 0 ? 0x68b84c : 0x4fac3f, (h & 21) - 10);
                    if (((px + py * 2 + h) & 31) < 3) color = 0x2f7f2d;
                    if (((px * 7 + py * 5 + h) & 63) == 0) color = 0x8bd05c;
                    // Kwiatki - biały (stokrotki) i zolty (mniszek) rzadko
                    int flowerHash = (h >> 12) & 127;
                    if (flowerHash == 0) color = 0xffffff;         // biala stokrotka
                    if (flowerHash == 1) color = 0xf0c810;         // zolty
                    if (flowerHash == 2) color = 0xe84040;         // czerwony mak
                    // Cień od kwiatka - subtelny
                    if (flowerHash == 64) color = 0x2a6a2a;
                } else if ("grass_side".equals(type)) {
                    color = jitter(0x7b4d2e, (h & 19) - 9);
                    int cap = 7;
                    if (py < cap) color = jitter(0x3f9f35, (h & 13) - 6);
                    else if (py < cap + 3 && ((px + h) & 7) < 2) color = jitter(0x3f9f35, (h & 11) - 5);
                } else if ("dirt".equals(type)) {
                    // Ziemia z kamyczkami i korzeniami
                    color = jitter(0x7b4d2e, (h & 39) - 20);
                    if (((h >> 5) & 17) == 0) color = 0x5a3823;
                    if (((h >> 9) & 23) == 0) color = 0xa36638;
                    if (((px + py + h) & 63) == 0) color = 0x3f2a1e;
                    // Male szare kamyczki
                    if ((h & 255) == 0) color = 0x8a8580;
                    if ((h & 255) == 1) color = 0x707068;
                    // Ciemne "korzenie" - poziome linie
                    if (((h >> 3) & 127) == 0) color = 0x2a1a10;
                } else if ("stone".equals(type)) {
                    // Kamień z żyłami rudy (deko) i pęknięciami
                    color = jitter(0x777a82, (h & 17) - 8);
                    if (((h >> 7) & 23) == 0) color = shadeColor(color, 0.88);
                    if (((h >> 11) & 31) == 0) color = shadeColor(color, 1.08);
                    // Ciemne pęknięcia
                    int crackH = (px * 3 + py * 5 + h) & 127;
                    if (crackH == 0 || crackH == 1) color = 0x3a3d42;
                    // Punkty rudy - węgiel (ciemne) i żelazo (pomarańczowe)
                    int oreH = (h >> 9) & 255;
                    if (oreH == 0) color = 0x1a1c20;    // węgiel
                    if (oreH == 1) color = 0x252830;
                    if (oreH == 200) color = 0xa87050;  // rdza żelaza
                } else if ("log_side".equals(type)) {
                    // Wzór słojów drewna - falisty
                    int stripe = (px / 5) % 2 == 0 ? 18 : -18;
                    color = jitter(0x8b5728, stripe + (h & 13) - 6);
                    // Ciemne pionowe pasma słojów
                    if (px == 6 || px == 17 || px == 27) color = 0x4b2b16;
                    if (px == 3 || px == 14 || px == 24) color = 0x9a6534;
                    // Sęk (jasny)
                    double kx = px - 17, ky = py - 16;
                    if (kx*kx/18 + ky*ky/10 < 1) color = ((h & 1) == 0) ? 0x5b351a : 0xb07435;
                    // Dodatkowy mały sęk
                    double kx2 = px - 8, ky2 = py - 6;
                    if (kx2*kx2/4 + ky2*ky2/3 < 1) color = 0x6d4020;
                } else if ("log_top".equals(type)) {
                    double dx = px - (TILE - 1) / 2.0;
                    double dy = py - (TILE - 1) / 2.0;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    int ring = ((int)(d * 1.45)) & 1;
                    color = jitter(ring == 0 ? 0xb97a3b : 0x7b4b22, (h & 15) - 7);
                    if (d > TILE * 0.43) color = 0x563019;
                    if (Math.abs(dx) < 1 && Math.abs(dy) < 1) color = 0x5b351a;
                } else if ("leaves".equals(type)) {
                    color = jitter(0x2f8f3f, (h & 11) - 5);
                    if (((h >> 6) & 9) == 0) color = 0x267a35;
                    if (((h >> 10) & 15) == 0) color = 0x3fa34d;
                } else if ("sand".equals(type)) {
                    // Piasek z falkami wydm i drobnymi ziarenkami
                    color = jitter(0xdcc47a, (h & 13) - 6);
                    if (((h >> 6) & 31) == 0) color = 0xcdb36b;
                    if (((h >> 10) & 31) == 0) color = 0xead48f;
                    // Falki wydm - subtelne poziome
                    if ((py + (px / 3)) % 6 == 0) color = shadeColor(color, 0.92);
                    if ((py + (px / 3)) % 6 == 3) color = shadeColor(color, 1.08);
                    // Ciemne drobinki (małe kamyki)
                    if ((h & 511) == 0) color = 0x8b7548;
                } else if ("water".equals(type)) {
                    color = jitter(0x3c78d8, (h & 15) - 7);
                    if (((px + py + h) & 15) < 2) color = 0x69a6ff;
                    if (((px * 3 - py + h) & 31) == 0) color = 0x1d4f9a;
                } else if ("planks".equals(type)) {
                    // Deski z widocznymi pasami
                    color = jitter(0xa86f39, (h & 27) - 13);
                    // Poziome pasy między deskami
                    if (py == 10 || py == 21) color = 0x5c361c;
                    // Pionowa linia (jak deski są przycięte)
                    if (px == 15 && py < 10) color = 0x5c361c;
                    if (px == 8 && py >= 10 && py < 21) color = 0x5c361c;
                    if (px == 22 && py >= 21) color = 0x5c361c;
                    // Sęk na deskach
                    if (((h >> 5) & 63) == 0) color = 0x3b2314;
                    if (((px + h) & 63) == 0) color = 0xc58a4b;
                    // Metalowe nity w rogach desek
                    if ((px == 3 && py == 3) || (px == 27 && py == 3)
                        || (px == 3 && py == 14) || (px == 27 && py == 14)
                        || (px == 3 && py == 25) || (px == 27 && py == 25)) {
                        color = 0x606060;
                    }
                } else if (type.startsWith("chest")) {
                    color = jitter(0x9c6d35, (h & 17) - 8);
                    if (px == 0 || py == 0 || px == TILE - 1 || py == TILE - 1) color = 0x3a2210;
                    if ("chest_top".equals(type)) {
                        if (py >= TILE/2 - 1 && py <= TILE/2) color = 0x4a2d10;
                        if ((px == 4 || px == TILE - 5) && (py == 6 || py == TILE - 7)) color = 0xa0a0a8;
                    } else {
                        if (px >= TILE/2 - 2 && px <= TILE/2 + 1 && py >= TILE/2 - 1 && py <= TILE/2 + 2) color = 0x2a1810;
                        if (px >= TILE/2 - 1 && px <= TILE/2 && py >= TILE/2 && py <= TILE/2 + 1) color = 0xc4a050;
                        if (py == TILE/2 - 3 || py == TILE/2 + 4) color = 0x6a3f1a;
                    }
                } else if (type.startsWith("door")) {
                    color = jitter(0xa56b32, (h & 17) - 8);
                    if (px == 0 || px == TILE - 1) color = 0x3a2210;
                    if (px == 6 || px == 12 || px == 18 || px == 25) color = 0x6b3f20;
                    if (py == 0 || py == TILE - 1) color = 0x3a2210;
                    if ("door_top".equals(type) && py >= 5 && py <= 7) color = 0x4c2d18;
                    if ("door_bottom".equals(type) && py >= TILE - 8 && py <= TILE - 6) color = 0x6b3f20;
                    if ("door_bottom".equals(type) && px >= 23 && px <= 26 && py >= 14 && py <= 17) color = 0xe4c761;
                } else if ("farmland".equals(type)) {
                    color = jitter(0x5a3823, (h & 21) - 10);
                    if (((h >> 5) & 17) == 0) color = 0x3f2a1e;
                    if ((px & 7) < 2) color = jitter(0x6e4a2f, (h & 11) - 5);
                    if (px >= 12 && px <= 19 && py >= 12 && py <= 19) color = shadeColor(color, 0.85);
                } else if ("tall_grass".equals(type)) {
                    color = 0x000000;
                    boolean isX = false;
                    int diag1 = px + py - TILE;
                    int diag2 = px - py;
                    if (Math.abs(diag1) < 3 || Math.abs(diag2) < 3) {
                        isX = true;
                        color = jitter(0x4faa3a, (h & 13) - 6);
                        if (py < TILE / 3) color = jitter(0x6fc34a, (h & 9) - 4);
                    }
                    if (((h >> 4) & 31) == 0 && py > TILE / 3 && py < TILE - 3) {
                        if ((px & 3) == 0) { isX = true; color = jitter(0x3f9c32, (h & 7) - 3); }
                    }
                    if (!isX) color = 0;
                } else if (type.startsWith("wheat_")) {
                    int stage = type.charAt(6) - '0';
                    color = 0;
                    int topY = TILE - (int)(TILE * (0.25 + stage * 0.25));
                    if (py >= topY) {
                        if (px == 6 || px == 10 || px == 14 || px == 18 || px == 22 || px == 26) {
                            color = jitter(stage <= 1 ? 0x4faa3a : (stage == 2 ? 0x9fb83a : 0xe8c248),
                                          (h & 11) - 5);
                        }
                        if (stage == 3 && py < topY + 8) {
                            if (px == 5 || px == 7 || px == 9 || px == 11 || px == 13 || px == 15
                                || px == 17 || px == 19 || px == 21 || px == 23 || px == 25 || px == 27) {
                                color = jitter(0xc8a040, (h & 9) - 4);
                            }
                        }
                    }
                } else if (type.startsWith("craft")) {
                    color = jitter(0x9b6330, (h & 21) - 10);
                    if (px < 4 || px > TILE - 5 || py < 4 || py > TILE - 5) color = 0x4c2d18;
                    if ("craft_top".equals(type)) {
                        if (px == 10 || px == 21 || py == 10 || py == 21) color = 0x6b3f20;
                        if ((px > 12 && px < 20 && py > 12 && py < 20)) color = 0xb98042;
                    } else {
                        if ((px == 9 || px == 22) && py > 7 && py < 25) color = 0xd59a55;
                        if (py == 15) color = 0x4c2d18;
                    }
                }
                boolean alphaTile = "tall_grass".equals(type) || type.startsWith("wheat_");
                if (!"leaves".equals(type) && !"water".equals(type) && !alphaTile) {
                    if (px == 0 || py == 0) color = shadeColor(color, 1.12);
                    if (px == TILE - 1 || py == TILE - 1) color = shadeColor(color, 0.70);
                    if (px == 1 || py == 1) color = shadeColor(color, 1.04);
                    if (px == TILE - 2 || py == TILE - 2) color = shadeColor(color, 0.82);
                }
                int alpha = (alphaTile && color == 0) ? 0 : 0xff;
                img.setRGB(ox + px, oy + py, (alpha << 24) | color);
            }
        }
        // Padding/gutter dla atlasu
        for (int i = 0; i < TILE; i++) {
            img.setRGB(ox - 1, oy + i, img.getRGB(ox, oy + i));
            img.setRGB(ox - 2, oy + i, img.getRGB(ox, oy + i));
            img.setRGB(ox + TILE, oy + i, img.getRGB(ox + TILE - 1, oy + i));
            img.setRGB(ox + TILE + 1, oy + i, img.getRGB(ox + TILE - 1, oy + i));
            img.setRGB(ox + i, oy - 1, img.getRGB(ox + i, oy));
            img.setRGB(ox + i, oy - 2, img.getRGB(ox + i, oy));
            img.setRGB(ox + i, oy + TILE, img.getRGB(ox + i, oy + TILE - 1));
            img.setRGB(ox + i, oy + TILE + 1, img.getRGB(ox + i, oy + TILE - 1));
        }
        for (int dx = -2; dx <= -1; dx++) for (int dy = -2; dy <= -1; dy++) img.setRGB(ox + dx, oy + dy, img.getRGB(ox, oy));
        for (int dx = TILE; dx <= TILE + 1; dx++) for (int dy = -2; dy <= -1; dy++) img.setRGB(ox + dx, oy + dy, img.getRGB(ox + TILE - 1, oy));
        for (int dx = -2; dx <= -1; dx++) for (int dy = TILE; dy <= TILE + 1; dy++) img.setRGB(ox + dx, oy + dy, img.getRGB(ox, oy + TILE - 1));
        for (int dx = TILE; dx <= TILE + 1; dx++) for (int dy = TILE; dy <= TILE + 1; dy++) img.setRGB(ox + dx, oy + dy, img.getRGB(ox + TILE - 1, oy + TILE - 1));
    }
}
