package craft3dmodern.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL20.*;

/** Kompilacja shaderow (GLSL 330 core) + obsluga uniformow. */
public final class ShaderProgram {
    private final int program;

    private ShaderProgram(int program) {
        this.program = program;
    }

    public static ShaderProgram create(String vertexSrc, String fragmentSrc) {
        int vs = compile(GL_VERTEX_SHADER, vertexSrc);
        int fs = compile(GL_FRAGMENT_SHADER, fragmentSrc);
        int prog = glCreateProgram();
        glAttachShader(prog, vs);
        glAttachShader(prog, fs);
        glLinkProgram(prog);
        if (glGetProgrami(prog, GL_LINK_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Shader link error: " + glGetProgramInfoLog(prog));
        }
        glDeleteShader(vs);
        glDeleteShader(fs);
        return new ShaderProgram(prog);
    }

    public static ShaderProgram fromClasspath(String baseName) {
        String vs = readResource("/craft3dmodern/shaders/" + baseName + ".vsh");
        String fs = readResource("/craft3dmodern/shaders/" + baseName + ".fsh");
        return create(vs, fs);
    }

    private static int compile(int type, String src) {
        int shader = glCreateShader(type);
        glShaderSource(shader, src);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new IllegalStateException("Shader compile error: " + glGetShaderInfoLog(shader));
        }
        return shader;
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing shader resource " + path);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read shader " + path, e);
        }
    }

    public void use() {
        glUseProgram(program);
    }

    public int uniform(String name) {
        int loc = glGetUniformLocation(program, name);
        if (loc < 0) throw new IllegalStateException("missing uniform " + name);
        return loc;
    }

    public void uniform1i(String name, int v) {
        glUniform1i(uniform(name), v);
    }

    public void uniformMat4(String name, FloatBuffer mat) {
        glUniformMatrix4fv(uniform(name), false, mat);
    }

    /** Ustaw uniform vec2 (dla zewnetrznych klas nie trzymajacych instancji). */
    public static void uniform2f(ShaderProgram program, String name, float x, float y) {
        glUniform2f(program.uniform(name), x, y);
    }

    public void dispose() {
        glDeleteProgram(program);
    }

    public static FloatBuffer mat4(float m00, float m01, float m02, float m03,
                                   float m10, float m11, float m12, float m13,
                                   float m20, float m21, float m22, float m23,
                                   float m30, float m31, float m32, float m33) {
        FloatBuffer b = BufferUtils.createFloatBuffer(16);
        b.put(new float[]{m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33});
        b.flip();
        return b;
    }
}
