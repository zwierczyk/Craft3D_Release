package craft3dgl.blaze3d.shaders;

import craft3dgl.blaze3d.platform.GLX;

/**
 * MC 1.14.4 ProgramManager port 1:1.
 * Singleton do zarzadzania OpenGL shader programami (create, link, release).
 */
public class ProgramManager {
    private static final ProgramManager INSTANCE = new ProgramManager();

    private ProgramManager() {}

    public static ProgramManager getInstance() {
        return INSTANCE;
    }

    /** Tworzy nowy shader program - zwraca OpenGL program handle. */
    public int createProgram() {
        return GLX.glCreateProgram();
    }

    /** Attach vertex+fragment, link. Wywolac PO utworzeniu programu i attachowaniu shaderow. */
    public void linkProgram(Effect effect) {
        System.out.println("[ProgramManager] linking programId=" + effect.getId() + " vertex=" + effect.getVertexProgram().getName() + " fragment=" + effect.getFragmentProgram().getName());
        effect.getVertexProgram().attachToEffect(effect);
        effect.getFragmentProgram().attachToEffect(effect);
        GLX.glLinkProgram(effect.getId());
        int status = GLX.glGetProgrami(effect.getId(), GLX.GL_LINK_STATUS);
        String plog = GLX.glGetProgramInfoLog(effect.getId(), 32768);
        if (plog != null && !plog.trim().isEmpty()) {
            System.out.println("[ProgramManager] Program info log: " + plog);
        }
        if (status == 0) {
            System.err.println("[ProgramManager] LINK FAILED for programId=" + effect.getId());
        } else {
            System.out.println("[ProgramManager] LINK SUCCESS programId=" + effect.getId());
        }
    }

    /** Zwolnij shader program (delete + close vertex/fragment). */
    public void releaseProgram(Effect effect) {
        effect.getVertexProgram().close();
        effect.getFragmentProgram().close();
        GLX.glDeleteProgram(effect.getId());
    }
}
