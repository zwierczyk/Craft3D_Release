package craft3dmodern.render;

import static org.lwjgl.opengl.GL11.GL_VIEWPORT;
import static org.lwjgl.opengl.GL11.glViewport;

/** Minimalny odpowiednik RenderSystem (czesc API 26.2), fixed-function juz nie istnieje. */
public final class RenderSystem {
    private RenderSystem() {}

    public static void viewport(int x, int y, int w, int h) {
        glViewport(x, y, w, h);
    }

    @SuppressWarnings("unused")
    public static void assertOnRenderThread() {
    }
}
