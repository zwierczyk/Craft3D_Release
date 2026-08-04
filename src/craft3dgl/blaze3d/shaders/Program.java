package craft3dgl.blaze3d.shaders;

import craft3dgl.blaze3d.platform.GLX;
import craft3dgl.blaze3d.platform.TextureUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * MC 1.14.4 Program port 1:1.
 * Reprezentuje SKOMPILOWANY shader (vertex albo fragment).
 * Uzywany przez Effect - jeden Effect = 2 Program (vertex + fragment).
 */
public class Program {
    private final Program.Type type;
    private final String name;
    private final int id;
    private int references;

    private Program(Program.Type type, int id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public void attachToEffect(Effect effect) {
        this.references++;
        GLX.glAttachShader(effect.getId(), this.id);
    }

    public void close() {
        this.references--;
        if (this.references <= 0) {
            GLX.glDeleteShader(this.id);
            this.type.getPrograms().remove(this.name);
        }
    }

    public String getName() { return this.name; }
    public int getId() { return this.id; }
    public Program.Type getType() { return this.type; }

    /**
     * Kompiluje shader z InputStream. Rzuca IOException jesli compile failed.
     */
    public static Program compileShader(Program.Type type, String name, InputStream inputStream) throws IOException {
        String source = TextureUtil.readResourceAsString(inputStream);
        if (source == null) {
            throw new IOException("Could not load program " + type.getName());
        }
        // Ensure GLX initialized (idempotent)
        try { GLX.init(); } catch (Throwable ignored) {}
        int shaderId = GLX.glCreateShader(type.getGlType());
        if (shaderId == 0) {
            throw new IOException("glCreateShader returned 0 for " + type.getName() + " '" + name + "'");
        }
        GLX.glShaderSource(shaderId, source);
        GLX.glCompileShader(shaderId);
        if (GLX.glGetShaderi(shaderId, GLX.GL_COMPILE_STATUS) == 0) {
            int logLen = GLX.glGetShaderi(shaderId, org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH);
            String log = GLX.glGetShaderInfoLog(shaderId, Math.max(logLen, 32768));
            if (log != null) log = log.trim();
            System.err.println("[Program] COMPILE FAILED " + type.getName() + " '" + name + "' (logLen=" + logLen + "): " + log);
            throw new IOException("Couldn't compile " + type.getName() + " program '" + name + "': " + log);
        }
        Program p = new Program(type, shaderId, name);
        type.getPrograms().put(name, p);
        System.out.println("[Program] Compiled " + type.getName() + " '" + name + "' id=" + shaderId);
        return p;
    }

    public static enum Type {
        VERTEX("vertex", ".vsh", 35633),      // GL_VERTEX_SHADER
        FRAGMENT("fragment", ".fsh", 35632);  // GL_FRAGMENT_SHADER

        private final String name;
        private final String extension;
        private final int glType;
        private final Map<String, Program> programs = new HashMap<>();

        private Type(String name, String ext, int glType) {
            this.name = name;
            this.extension = ext;
            this.glType = glType;
        }

        public String getName() { return this.name; }
        public String getExtension() { return this.extension; }
        public int getGlType() { return this.glType; }
        public Map<String, Program> getPrograms() { return this.programs; }
    }
}
