package craft3dgl.blaze3d.renderer;

/**
 * MC 1.14.4-style RenderStateShard - jeden aspekt stanu OpenGL.
 * Kazdy shard ma setupState() i clearState() (Runnable pary).
 * Uzywane przez RenderType do budowy pelnej konfiguracji rysowania.
 */
public class RenderStateShard {
    protected final String name;
    protected final Runnable setupState;
    protected final Runnable clearState;

    public RenderStateShard(String name, Runnable setupState, Runnable clearState) {
        this.name = name;
        this.setupState = setupState;
        this.clearState = clearState;
    }

    public void setupRenderState() {
        this.setupState.run();
    }

    public void clearRenderState() {
        this.clearState.run();
    }

    public String getName() { return name; }

    @Override public String toString() { return "RenderStateShard[" + name + "]"; }
}
