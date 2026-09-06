package craft3dgl.world;

import craft3dgl.save.AssetFinder;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fixed-function port of TileEntityChestRenderer + ModelChest from MCP 9.40.
 * Model dimensions, pivots, texture offsets and cubic lid easing match 1.12.
 */
public final class ChestRenderer {
    private static int texture = -1;
    private static int doubleTexture = -1;

    private ChestRenderer() {}

    public static void drawAll(byte[][][] world, Set<Long> chestKeys,
                               Map<Long, Integer> facings, int chestBlockId,
                               double cameraX, double cameraZ, double range,
                               int openX, int openY, int openZ, float lidProgress,
                               LightEngine lightEngine, float dayMultiplier) {
        ensureTexture();
        if (texture <= 0 || chestKeys == null || chestKeys.isEmpty()) return;
        double maxDistanceSq = range * range;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);

        HashSet<Long> rendered = new HashSet<Long>();
        for (Long keyObject : chestKeys) {
            if (keyObject == null || rendered.contains(keyObject)) continue;
            long key = keyObject.longValue();
            int x = ChestStorage.unpackX(key);
            int y = ChestStorage.unpackY(key);
            int z = ChestStorage.unpackZ(key);
            if (!isChest(world, x, y, z, chestBlockId)) continue;
            double dx = x + 0.5 - cameraX;
            double dz = z + 0.5 - cameraZ;
            if (dx * dx + dz * dz > maxDistanceSq) continue;

            long pairKey = findPairKey(world, chestKeys, x, y, z, chestBlockId, rendered);
            float light = lightEngine == null
                    ? 1f : lightEngine.sampleShade(x, y + 1, z, dayMultiplier);
            light = Math.max(0.25f, Math.min(1f, light));
            GL11.glColor4f(light, light, light, 1f);
            int facing = facingOf(facings, keyObject);
            boolean open = x == openX && y == openY && z == openZ;

            if (pairKey != Long.MIN_VALUE && doubleTexture > 0) {
                int pairX = ChestStorage.unpackX(pairKey);
                int pairZ = ChestStorage.unpackZ(pairKey);
                open |= pairX == openX && y == openY && pairZ == openZ;
                rendered.add(Long.valueOf(pairKey));
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, doubleTexture);
                if (pairX != x) {
                    if (facing != 0 && facing != 2) facing = 0;
                    int anchorX = facing == 2 ? Math.max(x, pairX) : Math.min(x, pairX);
                    drawLargeChest(anchorX, y, z, facing, open ? lidProgress : 0f);
                } else {
                    if (facing != 1 && facing != 3) facing = 1;
                    int anchorZ = facing == 3 ? Math.max(z, pairZ) : Math.min(z, pairZ);
                    drawLargeChest(x, y, anchorZ, facing, open ? lidProgress : 0f);
                }
            } else {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                drawChest(x, y, z, facing, open ? lidProgress : 0f);
            }
            rendered.add(keyObject);
        }

        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glPopAttrib();
    }

    private static boolean isChest(byte[][][] world, int x, int y, int z, int chestBlockId) {
        return x >= 0 && y >= 0 && z >= 0 && x < world.length
                && y < world[x].length && z < world[x][y].length
                && (world[x][y][z] & 255) == chestBlockId;
    }

    private static long findPairKey(byte[][][] world, Set<Long> keys, int x, int y, int z,
                                    int chestBlockId, Set<Long> rendered) {
        final int[] dx = {-1, 1, 0, 0};
        final int[] dz = {0, 0, -1, 1};
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i], nz = z + dz[i];
            if (!isChest(world, nx, y, nz, chestBlockId)) continue;
            Long neighbor = Long.valueOf(ChestStorage.packKey(nx, y, nz));
            if (keys.contains(neighbor) && !rendered.contains(neighbor)) return neighbor.longValue();
        }
        return Long.MIN_VALUE;
    }

    private static int facingOf(Map<Long, Integer> facings, Long key) {
        Integer value = facings == null ? null : facings.get(key);
        return value == null ? 0 : value.intValue() & 3;
    }

    /** Render the same 1.12 ModelChest as an inventory/held/dropped item. */
    public static boolean drawItemModel() {
        ensureTexture();
        if (texture <= 0) return false;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        drawChest(0, 0, 0, 0, 0f);
        GL11.glPopAttrib();
        return true;
    }

    /** GUI transform corresponding to RenderItem's gui display of a 3D block model. */
    public static boolean drawGuiItem(int x, int y, int size, int screenWidth, int screenHeight) {
        ensureTexture();
        if (texture <= 0) return false;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        // GuiContainer normally clears depth before RenderItem's 3D models.
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, screenWidth, screenHeight, 0.0, -100.0, 100.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(x + size * 0.5f, y + size * 0.86f, 0f);
        GL11.glScalef(size * 0.72f, -size * 0.72f, size * 0.72f);
        GL11.glRotatef(24f, 1f, 0f, 0f);
        GL11.glRotatef(45f, 0f, 1f, 0f);
        GL11.glTranslatef(-0.5f, 0f, -0.5f);
        drawItemModel();
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopAttrib();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        return true;
    }

    private static void drawChest(int worldX, int worldY, int worldZ,
                                  int facing, float progress) {
        GL11.glPushMatrix();
        GL11.glTranslatef(worldX, worldY + 1f, worldZ + 1f);
        GL11.glScalef(1f, -1f, -1f);
        GL11.glTranslatef(0.5f, 0.5f, 0.5f);
        // TileEntityChestRenderer metadata rotation: south 0, west 90,
        // north 180, east -90. Our facing order is S-W-N-E.
        float rotation = facing == 1 ? 90f : facing == 2 ? 180f : facing == 3 ? -90f : 0f;
        GL11.glRotatef(rotation, 0f, 1f, 0f);
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f);

        // TileEntityChestRenderer: f = 1-(1-progress)^3, angle=-f*PI/2.
        float closed = 1f - clamp01(progress);
        float eased = 1f - closed * closed * closed;
        float lidAngle = -(float) (eased * Math.PI / 2.0);

        // ModelChest.chestBelow: tex(0,19), box 14x10x14, pivot (1,6,1).
        GL11.glPushMatrix();
        GL11.glTranslatef(1f / 16f, 6f / 16f, 1f / 16f);
        drawModelBox(0, 19, 0, 0, 0, 14, 10, 14);
        GL11.glPopMatrix();

        // ModelChest.chestLid: tex(0,0), box 14x5x14, pivot (1,7,15).
        GL11.glPushMatrix();
        GL11.glTranslatef(1f / 16f, 7f / 16f, 15f / 16f);
        GL11.glRotatef((float) Math.toDegrees(lidAngle), 1f, 0f, 0f);
        drawModelBox(0, 0, 0, -5, -14, 14, 5, 14);
        GL11.glPopMatrix();

        // ModelChest.chestKnob follows the exact same lid rotation.
        GL11.glPushMatrix();
        GL11.glTranslatef(8f / 16f, 7f / 16f, 15f / 16f);
        GL11.glRotatef((float) Math.toDegrees(lidAngle), 1f, 0f, 0f);
        drawModelBox(0, 0, -1, -2, -15, 2, 4, 1);
        GL11.glPopMatrix();

        GL11.glPopMatrix();
    }

    /** ModelLargeChest from MCP 9.40: 30x10x14 body and 30x5x14 lid. */
    private static void drawLargeChest(int worldX, int worldY, int worldZ,
                                       int facing, float progress) {
        GL11.glPushMatrix();
        GL11.glTranslatef(worldX, worldY + 1f, worldZ + 1f);
        GL11.glScalef(1f, -1f, -1f);
        GL11.glTranslatef(0.5f, 0.5f, 0.5f);
        float rotation = facing == 1 ? 90f : facing == 2 ? 180f : facing == 3 ? -90f : 0f;
        GL11.glRotatef(rotation, 0f, 1f, 0f);
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f);

        float closed = 1f - clamp01(progress);
        float eased = 1f - closed * closed * closed;
        float lidAngle = -(float)(eased * Math.PI / 2.0);

        GL11.glPushMatrix();
        GL11.glTranslatef(1f / 16f, 6f / 16f, 1f / 16f);
        drawModelBox(0, 19, 0, 0, 0, 30, 10, 14, 128f);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslatef(1f / 16f, 7f / 16f, 15f / 16f);
        GL11.glRotatef((float)Math.toDegrees(lidAngle), 1f, 0f, 0f);
        drawModelBox(0, 0, 0, -5, -14, 30, 5, 14, 128f);
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslatef(16f / 16f, 7f / 16f, 15f / 16f);
        GL11.glRotatef((float)Math.toDegrees(lidAngle), 1f, 0f, 0f);
        drawModelBox(0, 0, -1, -2, -15, 2, 4, 1, 128f);
        GL11.glPopMatrix();
        GL11.glPopMatrix();
    }

    /** ModelBox's exact vertex order and UV unwrap, in model pixels. */
    private static void drawModelBox(int textureU, int textureV,
                                     float x, float y, float z,
                                     int width, int height, int depth) {
        drawModelBox(textureU, textureV, x, y, z, width, height, depth, 64f);
    }

    private static void drawModelBox(int textureU, int textureV,
                                     float x, float y, float z,
                                     int width, int height, int depth, float textureWidth) {
        float x0 = x / 16f, x1 = (x + width) / 16f;
        float y0 = y / 16f, y1 = (y + height) / 16f;
        float z0 = z / 16f, z1 = (z + depth) / 16f;
        int sideV = textureV + depth;

        // ModelBox.quadList[0..5]. The UV helper intentionally assigns u2 to
        // the first vertex, matching TexturedQuad's constructor in MCP 9.40.
        modelQuad(textureU + depth + width, sideV,
                textureU + depth + width + depth, sideV + height, textureWidth,
                x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1);
        modelQuad(textureU, sideV, textureU + depth, sideV + height, textureWidth,
                x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0);
        modelQuad(textureU + depth, textureV,
                textureU + depth + width, textureV + depth, textureWidth,
                x1,y0,z1, x0,y0,z1, x0,y0,z0, x1,y0,z0);
        modelQuad(textureU + depth + width, sideV,
                textureU + depth + width + width, textureV, textureWidth,
                x1,y1,z0, x0,y1,z0, x0,y1,z1, x1,y1,z1);
        modelQuad(textureU + depth, sideV,
                textureU + depth + width, sideV + height, textureWidth,
                x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0);
        modelQuad(textureU + depth + width + depth, sideV,
                textureU + depth + width + depth + width, sideV + height, textureWidth,
                x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1);
    }

    private static void modelQuad(int u1, int v1, int u2, int v2, float textureWidth,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3,
                                  float x4, float y4, float z4) {
        float left = u1 / textureWidth, right = u2 / textureWidth;
        float top = v1 / 64f, bottom = v2 / 64f;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(right, top);    GL11.glVertex3f(x1, y1, z1);
        GL11.glTexCoord2f(left, top);     GL11.glVertex3f(x2, y2, z2);
        GL11.glTexCoord2f(left, bottom);  GL11.glVertex3f(x3, y3, z3);
        GL11.glTexCoord2f(right, bottom); GL11.glVertex3f(x4, y4, z4);
        GL11.glEnd();
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static void ensureTexture() {
        if (texture != -1 && doubleTexture != -1) return;
        texture = loadTexture("normal.png");
        doubleTexture = loadTexture("normal_double.png");
    }

    private static int loadTexture(String name) {
        try {
            File entity = AssetFinder.findAssetDir("entity", ChestRenderer.class);
            if (entity == null) return 0;
            File file = new File(new File(entity, "chest"), name);
            BufferedImage image = ImageIO.read(file);
            if (image == null) return 0;
            int width = image.getWidth();
            int height = image.getHeight();
            ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    pixels.put((byte) ((argb >>> 16) & 255));
                    pixels.put((byte) ((argb >>> 8) & 255));
                    pixels.put((byte) (argb & 255));
                    pixels.put((byte) ((argb >>> 24) & 255));
                }
            }
            pixels.flip();
            int id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            System.out.println("[ChestRenderer] loaded " + file.getPath());
            return id;
        } catch (Throwable t) {
            System.err.println("[ChestRenderer] texture load failed: " + t.getMessage());
            return 0;
        }
    }
}
