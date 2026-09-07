package craft3dmodern.client;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import craft3dmodern.model.BlockModels;
import craft3dmodern.render.Mat4;
import craft3dmodern.render.Texture;
import craft3dmodern.render.TextureAtlas;
import craft3dmodern.render.WorldRender;
import craft3dmodern.world.BlockIds;
import craft3dmodern.world.MeshBuilder;
import craft3dmodern.world.World;
import craft3dmodern.world.WorldGen;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Widok w swiecie (M3): generowanie swiata, atlas, mesh i latanie kamera
 * (WASD + mysz, Space = gora, C = dol, Shift = sprint, R = nowy swiat).
 */
public final class Ingame {
    public long seed;

    private World world;
    private final BlockModels models;
    private TextureAtlas atlas;
    private float[] mesh;
    private WorldRender render;
    private int atlasTex = -1;

    // kamera (yaw/pitch w radianach)
    private double ex, ey, ez;
    private float yaw = (float) Math.toRadians(-35.0);
    private float pitch = (float) Math.toRadians(-6.0);

    public Ingame(long seed) throws IOException {
        this.seed = seed;
        this.models = new BlockModels();
        rebuild();
    }

    public void rebuild() throws IOException {
        world = WorldGen.generate(seed);
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
        int gx = world.sx / 2;
        int gz = world.sz / 2;
        ex = gx + 0.5;
        ez = gz + 0.5;
        ey = world.topSolid(gx, gz) + 2.0;
    }

    /** Inicjalizacja GL (program, VBO, tekstura atlasu). Kontekst GL musi byc aktywny. */
    public void glInit() {
        render = new WorldRender();
        render.upload(mesh);
        atlasTex = Texture.upload(atlas.page(), false);
    }

    public void regenerate(long newSeed) throws IOException {
        disposeGL();
        seed = newSeed;
        rebuild();
        glInit();
    }

    public boolean wantsCursorCaptured() {
        return true;
    }

    /** Aktualizuje kamere na podstawie wejscia. */
    public void update(Input in, double dt) {
        float sens = 0.0024f;
        yaw -= in.mouseDx * sens;
        pitch += in.mouseDy * sens;
        if (pitch > (float) Math.toRadians(89)) pitch = (float) Math.toRadians(89);
        if (pitch < (float) Math.toRadians(-89)) pitch = (float) Math.toRadians(-89);

        float cp = (float) Math.cos(pitch);
        double fx = -Math.sin(yaw) * cp;
        double fy = Math.sin(pitch);
        double fz = -Math.cos(yaw) * cp;
        double fxl = Math.sqrt(fx * fx + fz * fz);
        double ux = fx / fxl, uz = fz / fxl; // forward w poziomie
        double rx = -uz, rz = ux;            // prawo (obrot o -90 wokol Y)

        double speed = 9.0;
        if (in.sprint) speed *= 2.4;
        double moveX = 0, moveY = 0, moveZ = 0;
        if (in.forward) { moveX += ux; moveZ += uz; }
        if (in.back) { moveX -= ux; moveZ -= uz; }
        if (in.strafeRight) { moveX += rx; moveZ += rz; }
        if (in.strafeLeft) { moveX -= rx; moveZ -= rz; }
        if (in.up) moveY += 1;
        if (in.down) moveY -= 1;
        double len = Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (len > 0) { moveX /= len; moveZ /= len; }
        ex += moveX * speed * dt;
        ey += moveY * speed * dt;
        ez += moveZ * speed * dt;
    }

    /** Rysuje swiat; zwraca true gdy jakikolwiek blok widoczny. */
    public void draw(float aspect) {
        // niebo
        org.lwjgl.opengl.GL11.glClearColor(0.53f, 0.79f, 0.94f, 1f);
        org.lwjgl.opengl.GL11.glClear(org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT | org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT);
        if (render == null || atlasTex < 0) return;

        float cp = (float) Math.cos(pitch);
        double fx = -Math.sin(yaw) * cp;
        double fy = Math.sin(pitch);
        double fz = -Math.cos(yaw) * cp;

        float[] p = Mat4.perspective(72f, aspect, 0.1f, 700f);
        float[] v = Mat4.lookAt((float) ex, (float) ey, (float) ez,
                (float) (ex + fx), (float) (ey + fy), (float) (ez + fz),
                0f, 1f, 0f);

        glEnable(GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glDepthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        org.lwjgl.opengl.GL11.glBindTexture(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, atlasTex);
        render.render(Mat4.columnMajor(Mat4.multiply(p, v)));
        glDisable(GL_DEPTH_TEST);
    }

    public int faceCount() {
        return mesh == null ? 0 : mesh.length / (WorldRender.MeshVertex.FLOATS * 6);
    }

    public int meshVertices() {
        return mesh == null ? 0 : mesh.length / WorldRender.MeshVertex.FLOATS;
    }

    public String positionText() {
        return String.format("xyz %.1f / %.1f / %.1f", ex, ey, ez);
    }

    public void disposeGL() {
        if (render != null) { render.dispose(); render = null; }
        if (atlasTex >= 0) { Texture.dispose(atlasTex); atlasTex = -1; }
    }
}
