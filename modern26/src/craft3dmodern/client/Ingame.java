package craft3dmodern.client;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

import craft3dmodern.model.BlockModels;
import craft3dmodern.render.CelestialRenderer;
import craft3dmodern.render.CloudRenderer;
import craft3dmodern.render.IconRenderer;
import craft3dmodern.render.Mat4;
import craft3dmodern.render.OutlineRenderer;
import craft3dmodern.render.Texture;
import craft3dmodern.render.TextureAtlas;
import craft3dmodern.render.WorldRender;
import craft3dmodern.world.BlockIds;
import craft3dmodern.world.MeshBuilder;
import craft3dmodern.world.Player;
import craft3dmodern.world.Raycast;
import craft3dmodern.world.World;
import craft3dmodern.world.WorldGen;

public final class Ingame {
    public static final double REACH = 5.0;

    public long seed;
    public long ticks;

    private World world;
    private final BlockModels models;
    private TextureAtlas atlas;
    private int atlasTex = -1;
    private float[] mesh;
    private boolean meshDirty;

    private WorldRender render;
    private CelestialRenderer celestial;
    private CloudRenderer clouds;
    private OutlineRenderer outline;
    private IconRenderer icons;

    private Player player;
    private final int[] hotbar;
    private int selectedSlot = 0;
    private Raycast.Hit hit;
    private boolean leftWasDown = false;
    private boolean rightWasDown = false;
    private boolean middleWasDown = false;
    private double breakTimer = 0;
    private double placeTimer = 0;
    private boolean flying = false;
    private boolean sprinting = false;
    private boolean jumpWasDown = false;
    private double lastJumpEdge = -1;
    private float fov = 70f;

    public Ingame(long seed) throws IOException {
        this.seed = seed;
        this.models = new BlockModels();
        this.hotbar = BlockIds.HOTBAR.clone();
        resetWorld();
    }

    private void resetWorld() {
        world = WorldGen.generate(seed);
        int sx = world.sx / 2;
        int sz = world.sz / 2;
        double sy = world.topSolid(sx, sz) + 1.0;
        player = new Player(sx + 0.5, sy, sz + 0.5);
        player.yaw = (float) Math.toRadians(-35.0);
        player.pitch = (float) Math.toRadians(-7.0);
        buildAssets();
    }

    private void buildAssets() {
        try {
            Set<String> textures = new LinkedHashSet<String>();
            for (int id = 1; id < BlockIds.count(); id++) {
                String name = BlockIds.name(id);
                String variant = id == BlockIds.OAK_LOG ? "axis=y" : null;
                for (BlockModels.Face f : models.modelFor(name, variant).faces) {
                    if (f.tex != null) textures.add(f.tex);
                }
            }
            atlas = TextureAtlas.build(textures);
            mesh = MeshBuilder.build(world, models, atlas);
            meshDirty = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void glInit() {
        render = new WorldRender();
        render.upload(mesh);
        celestial = new CelestialRenderer();
        try {
            celestial.loadTextures();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            clouds = new CloudRenderer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        outline = new OutlineRenderer();
        icons = new IconRenderer();
        atlasTex = Texture.upload(atlas.page(), false);
        try {
            icons.bake(models, atlas, atlasTex);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void regenerate(long newSeed) {
        disposeGL();
        seed = newSeed;
        world = WorldGen.generate(seed);
        int sx = world.sx / 2;
        int sz = world.sz / 2;
        double sy = world.topSolid(sx, sz) + 1.0;
        player.x = sx + 0.5;
        player.y = sy;
        player.z = sz + 0.5;
        player.vx = player.vy = player.vz = 0;
        player.onGround = false;
        flying = false;
        sprinting = false;
        buildAssets();
        glInit();
    }

    public void update(Input in, double dt) {
        float sens = 0.0024f;
        player.yaw -= (float) (in.mouseDx * sens);
        player.pitch -= (float) (in.mouseDy * sens);
        float lim = (float) Math.toRadians(89);
        if (player.pitch > lim) player.pitch = lim;
        if (player.pitch < -lim) player.pitch = -lim;

        if (in.hotbar > 0 && in.hotbar <= hotbar.length) {
            selectedSlot = in.hotbar - 1;
        }
        if (in.wheel != 0) {
            selectedSlot = ((selectedSlot + (in.wheel > 0 ? 1 : -1)) % hotbar.length
                    + hotbar.length) % hotbar.length;
        }

        boolean jumpNow = in.jump;
        double now = ticks / 20.0;
        if (jumpNow && !jumpWasDown) {
            if (lastJumpEdge >= 0 && now - lastJumpEdge < 0.35) {
                flying = !flying;
            }
            lastJumpEdge = now;
        }
        jumpWasDown = jumpNow;
        if (flying && player.onGround && !jumpNow) {
            flying = false;
        }

        sprinting = in.sprint && in.forward && !in.sneak;
        fov += (((sprinting ? 84f : 70f) - fov) * Math.min(1f, (float) (dt * 8f)));

        player.update(world, dt, in.forward, in.back, in.strafeLeft, in.strafeRight,
                jumpNow, sprinting, in.sneak, flying);

        ticks += (long) (dt * 20.0);

        breakTimer -= dt;
        placeTimer -= dt;
        if (in.leftDown) {
            if (breakTimer <= 0) {
                breakBlock();
                breakTimer = 0.22;
            }
        }
        if (in.rightDown) {
            if (placeTimer <= 0) {
                placeBlock();
                placeTimer = 0.22;
            }
        }
        if (in.middleDown && !middleWasDown) {
            pickBlock();
        }
        leftWasDown = in.leftDown;
        rightWasDown = in.rightDown;
        middleWasDown = in.middleDown;

        hit = castHit();
    }

    private Raycast.Hit castHit() {
        return Raycast.cast(world, player.eyeX(), player.eyeY(), player.eyeZ(),
                player.dirX(), player.dirY(), player.dirZ(), REACH);
    }

    private void breakBlock() {
        Raycast.Hit h = castHit();
        if (h == null) return;
        world.set(h.x, h.y, h.z, BlockIds.AIR);
        rebuildMesh();
    }

    private void placeBlock() {
        Raycast.Hit h = castHit();
        if (h == null) return;
        int[] dx = {0, 0, 0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0, 0, 0};
        int[] dz = {0, 0, -1, 1, 0, 0};
        int px = h.x + dx[h.face];
        int py = h.y + dy[h.face];
        int pz = h.z + dz[h.face];
        if (!world.inBounds(px, py, pz)) return;
        if (world.get(px, py, pz) != BlockIds.AIR) return;
        int id = hotbar[selectedSlot];
        double hw = Player.WIDTH / 2;
        if (px + 1 > player.x - hw && px < player.x + hw
                && py + 1 > player.y && py < player.y + Player.HEIGHT
                && pz + 1 > player.z - hw && pz < player.z + hw) {
            return;
        }
        world.set(px, py, pz, id);
        rebuildMesh();
    }

    private void pickBlock() {
        Raycast.Hit h = castHit();
        if (h == null) return;
        int id = world.get(h.x, h.y, h.z);
        if (id > BlockIds.AIR && id < BlockIds.count()) {
            hotbar[selectedSlot] = id;
        }
    }

    private void rebuildMesh() {
        try {
            mesh = MeshBuilder.build(world, models, atlas);
            meshDirty = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int hotbarSlot(int i) {
        if (i < 0 || i >= hotbar.length) return BlockIds.STONE;
        return hotbar[i];
    }

    public int selectedBlock() {
        return hotbar[selectedSlot];
    }

    public int selectedSlotIndex() {
        return selectedSlot;
    }

    public World world() {
        return world;
    }

    public boolean isAirAt(int x, int y, int z) {
        return world.get(x, y, z) == BlockIds.AIR;
    }

    public void draw(float aspect) {
        if (meshDirty && render != null) {
            render.upload(mesh);
            meshDirty = false;
        }

        float dayF = skyFactor();
        float[] sky = skyColor(dayF);
        org.lwjgl.opengl.GL11.glClearColor(sky[0], sky[1], sky[2], 1f);
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
                | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);

        float cp = (float) Math.cos(player.pitch);
        double fx = -Math.sin(player.yaw) * cp;
        double fy = Math.sin(player.pitch);
        double fz = -Math.cos(player.yaw) * cp;
        double rl = Math.sqrt(fx * fx + fz * fz);
        double rxn = -fz / rl;
        double rzn = fx / rl;
        double ux = -(rzn * fy);
        double uy = rl;
        double uz = rxn * fy;
        double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
        ux /= ul;
        uy /= ul;
        uz /= ul;

        float[] p = Mat4.perspective(fov, aspect, 0.1f, 4000f);
        float[] v = Mat4.lookAt((float) player.eyeX(), (float) player.eyeY(), (float) player.eyeZ(),
                (float) (player.eyeX() + fx), (float) (player.eyeY() + fy), (float) (player.eyeZ() + fz),
                0f, 1f, 0f);
        FloatBuffer mvp = Mat4.columnMajor(Mat4.multiply(p, v));

        float ex = (float) player.eyeX();
        float ey = (float) player.eyeY();
        float ez = (float) player.eyeZ();

        if (celestial != null) {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            float th = (float) ((ticks % 24000L) / 24000.0 * Math.PI * 2.0);
            float sunEl = (float) Math.sin(th);
            float moonEl = (float) Math.sin(th + (float) Math.PI);
            if (sunEl > -0.15f) {
                drawCelestial(mvp, celestial.sunTex, th, 300, 46, ex, ey, ez,
                        (float) rxn, 0f, (float) rzn, (float) ux, (float) uy, (float) uz);
            }
            if (moonEl > -0.15f) {
                int phase = celestial.moonPhase((int) (ticks / 24000L));
                drawCelestial(mvp, celestial.moonTex[phase], th + (float) Math.PI, 300, 34,
                        ex, ey, ez, (float) rxn, 0f, (float) rzn, (float) ux, (float) uy, (float) uz);
            }
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        }

        if (clouds != null) {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            clouds.draw(mvp, ex, ey, ez, dayF, ticks);
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        }

        if (render != null && atlasTex >= 0) {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
            org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
            org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, atlasTex);
            render.render(mvp, ex, ey, ez, sky[0], sky[1], sky[2], dayF);
            if (outline != null && hit != null) {
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
                org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                        org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
                outline.draw(mvp, hit.x, hit.y, hit.z);
                org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
            }
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        }
    }

    private void drawCelestial(FloatBuffer mvp, int tex, float angle, float dist, float half,
                               float ex, float ey, float ez,
                               float rx, float ry, float rz, float ux, float uy, float uz) {
        float dx = (float) Math.cos(angle);
        float dy = (float) Math.sin(angle);
        celestial.draw(mvp, tex,
                ex + dx * dist, ey + dy * dist, ez,
                rx, ry, rz, half, ux, uy, uz, half);
    }

    private float skyFactor() {
        double a = (ticks % 24000L) / 24000.0 * Math.PI * 2.0;
        double e = Math.sin(a);
        double f = (e + 0.18) / 0.5;
        if (f < 0) f = 0;
        if (f > 1) f = 1;
        return (float) f;
    }

    private float[] skyColor(float dayF) {
        float r = 0.03f + (0.50f - 0.03f) * dayF;
        float g = 0.04f + (0.72f - 0.04f) * dayF;
        float b = 0.10f + (0.95f - 0.10f) * dayF;
        return new float[]{r, g, b};
    }

    public void drawIcon(int blockId, float cxYdown, float cyYdown, float cellPx, int fbw, int fbh) {
        if (icons != null) icons.draw(blockId, cxYdown, cyYdown, cellPx, fbw, fbh);
    }

    public void disposeGL() {
        if (render != null) render.dispose();
        if (celestial != null) celestial.dispose();
        if (clouds != null) clouds.dispose();
        if (outline != null) outline.dispose();
        if (icons != null) icons.dispose();
        if (atlasTex >= 0) Texture.dispose(atlasTex);
        render = null;
        celestial = null;
        clouds = null;
        outline = null;
        icons = null;
        atlasTex = -1;
    }
}
