package craft3dgl.blaze3d.platform;

import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ARBMultitexture;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.EXTBlendFuncSeparate;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLCapabilities;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * MC 1.14.4 GLX port - uproszczony (tylko potrzebne metody dla shaderow + VBO + FBO).
 * Wybiera odpowiedni backend (Core/ARB/EXT) w zaleznosci od GL capabilities.
 */
public class GLX {

    // === Framebuffer constants ===
    public static int GL_FRAMEBUFFER;
    public static int GL_RENDERBUFFER;
    public static int GL_COLOR_ATTACHMENT0;
    public static int GL_DEPTH_ATTACHMENT;
    public static int GL_FRAMEBUFFER_COMPLETE;

    // === Shader constants ===
    public static int GL_LINK_STATUS;
    public static int GL_COMPILE_STATUS;
    public static int GL_VERTEX_SHADER;
    public static int GL_FRAGMENT_SHADER;

    // === Multitexture ===
    public static int GL_TEXTURE0;
    public static int GL_TEXTURE1;
    public static int GL_TEXTURE2;

    // === VBO ===
    public static int GL_ARRAY_BUFFER;
    public static int GL_STATIC_DRAW;

    // === Backend flags ===
    private static FboMode fboMode;
    private static boolean useShaderArb;
    private static boolean useVboArb;
    private static boolean useMultitextureArb;
    private static boolean separateBlend;
    private static boolean useSeparateBlendExt;
    private static boolean hasShaders;
    private static boolean initialized = false;

    public static enum FboMode { BASE, ARB, EXT }

    /** Inicjalizacja - wywolaj RAZ po utworzeniu OpenGL contextu. */
    public static void init() {
        if (initialized) return;
        initialized = true;
        GLCapabilities caps = GL.getCapabilities();

        // Multitexture
        useMultitextureArb = caps.GL_ARB_multitexture && !caps.OpenGL13;
        GL_TEXTURE0 = 33984;
        GL_TEXTURE1 = 33985;
        GL_TEXTURE2 = 33986;

        // Framebuffer
        if (caps.OpenGL30) {
            fboMode = FboMode.BASE;
        } else if (caps.GL_ARB_framebuffer_object) {
            fboMode = FboMode.ARB;
        } else if (caps.GL_EXT_framebuffer_object) {
            fboMode = FboMode.EXT;
        } else {
            throw new IllegalStateException("No framebuffer support");
        }
        GL_FRAMEBUFFER = 36160;
        GL_RENDERBUFFER = 36161;
        GL_COLOR_ATTACHMENT0 = 36064;
        GL_DEPTH_ATTACHMENT = 36096;
        GL_FRAMEBUFFER_COMPLETE = 36053;

        // Shaders
        boolean isOpenGl21 = caps.OpenGL21;
        hasShaders = isOpenGl21 || (caps.GL_ARB_vertex_shader && caps.GL_ARB_fragment_shader && caps.GL_ARB_shader_objects);
        if (hasShaders) {
            useShaderArb = !isOpenGl21;
        }
        GL_LINK_STATUS = 35714;
        GL_COMPILE_STATUS = 35713;
        GL_VERTEX_SHADER = 35633;
        GL_FRAGMENT_SHADER = 35632;

        // VBO
        useVboArb = !caps.OpenGL15 && caps.GL_ARB_vertex_buffer_object;
        GL_STATIC_DRAW = 35044;
        GL_ARRAY_BUFFER = 34962;

        // Blend
        useSeparateBlendExt = caps.GL_EXT_blend_func_separate && !caps.OpenGL14;
        separateBlend = caps.OpenGL14 || caps.GL_EXT_blend_func_separate;

        System.out.println("[GLX] Init OK - fboMode=" + fboMode + " useShaderArb=" + useShaderArb +
            " useVboArb=" + useVboArb + " hasShaders=" + hasShaders);
    }

    public static boolean hasShaders() { return hasShaders; }
    public static FboMode getFboMode() { return fboMode; }

    // === SHADERS ===
    public static int glCreateShader(int i) {
        return useShaderArb ? ARBShaderObjects.glCreateShaderObjectARB(i) : GL20.glCreateShader(i);
    }
    public static void glShaderSource(int i, CharSequence s) {
        if (useShaderArb) ARBShaderObjects.glShaderSourceARB(i, s);
        else GL20.glShaderSource(i, s);
    }
    public static void glCompileShader(int i) {
        if (useShaderArb) ARBShaderObjects.glCompileShaderARB(i);
        else GL20.glCompileShader(i);
    }
    public static int glGetShaderi(int i, int j) {
        return useShaderArb ? ARBShaderObjects.glGetObjectParameteriARB(i, j) : GL20.glGetShaderi(i, j);
    }
    public static String glGetShaderInfoLog(int i, int j) {
        return useShaderArb ? ARBShaderObjects.glGetInfoLogARB(i, j) : GL20.glGetShaderInfoLog(i, j);
    }
    public static void glDeleteShader(int i) {
        if (useShaderArb) ARBShaderObjects.glDeleteObjectARB(i);
        else GL20.glDeleteShader(i);
    }
    public static void glAttachShader(int i, int j) {
        if (useShaderArb) ARBShaderObjects.glAttachObjectARB(i, j);
        else GL20.glAttachShader(i, j);
    }

    // === PROGRAMS ===
    public static int glCreateProgram() {
        return useShaderArb ? ARBShaderObjects.glCreateProgramObjectARB() : GL20.glCreateProgram();
    }
    public static void glDeleteProgram(int i) {
        if (useShaderArb) ARBShaderObjects.glDeleteObjectARB(i);
        else GL20.glDeleteProgram(i);
    }
    public static void glLinkProgram(int i) {
        if (useShaderArb) ARBShaderObjects.glLinkProgramARB(i);
        else GL20.glLinkProgram(i);
    }
    public static void glUseProgram(int i) {
        if (useShaderArb) ARBShaderObjects.glUseProgramObjectARB(i);
        else GL20.glUseProgram(i);
    }
    public static int glGetProgrami(int i, int j) {
        return useShaderArb ? ARBShaderObjects.glGetObjectParameteriARB(i, j) : GL20.glGetProgrami(i, j);
    }
    public static String glGetProgramInfoLog(int i, int j) {
        return useShaderArb ? ARBShaderObjects.glGetInfoLogARB(i, j) : GL20.glGetProgramInfoLog(i, j);
    }
    public static int glGetUniformLocation(int i, CharSequence s) {
        return useShaderArb ? ARBShaderObjects.glGetUniformLocationARB(i, s) : GL20.glGetUniformLocation(i, s);
    }
    public static int glGetAttribLocation(int i, CharSequence s) {
        return useShaderArb ? ARBVertexShader.glGetAttribLocationARB(i, s) : GL20.glGetAttribLocation(i, s);
    }

    // === UNIFORMS ===
    public static void glUniform1i(int i, int j) {
        if (useShaderArb) ARBShaderObjects.glUniform1iARB(i, j);
        else GL20.glUniform1i(i, j);
    }
    public static void glUniform1(int i, IntBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform1ivARB(i, b);
        else GL20.glUniform1iv(i, b);
    }
    public static void glUniform1(int i, FloatBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform1fvARB(i, b);
        else GL20.glUniform1fv(i, b);
    }
    public static void glUniform2(int i, IntBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform2ivARB(i, b);
        else GL20.glUniform2iv(i, b);
    }
    public static void glUniform2(int i, FloatBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform2fvARB(i, b);
        else GL20.glUniform2fv(i, b);
    }
    public static void glUniform3(int i, IntBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform3ivARB(i, b);
        else GL20.glUniform3iv(i, b);
    }
    public static void glUniform3(int i, FloatBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform3fvARB(i, b);
        else GL20.glUniform3fv(i, b);
    }
    public static void glUniform4(int i, IntBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform4ivARB(i, b);
        else GL20.glUniform4iv(i, b);
    }
    public static void glUniform4(int i, FloatBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniform4fvARB(i, b);
        else GL20.glUniform4fv(i, b);
    }
    public static void glUniformMatrix2(int i, boolean bl, FloatBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniformMatrix2fvARB(i, bl, b);
        else GL20.glUniformMatrix2fv(i, bl, b);
    }
    public static void glUniformMatrix3(int i, boolean bl, FloatBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniformMatrix3fvARB(i, bl, b);
        else GL20.glUniformMatrix3fv(i, bl, b);
    }
    public static void glUniformMatrix4(int i, boolean bl, FloatBuffer b) {
        if (useShaderArb) ARBShaderObjects.glUniformMatrix4fvARB(i, bl, b);
        else GL20.glUniformMatrix4fv(i, bl, b);
    }

    // === VBO ===
    public static int glGenBuffers() {
        return useVboArb ? ARBVertexBufferObject.glGenBuffersARB() : GL15.glGenBuffers();
    }
    public static void glBindBuffer(int i, int j) {
        if (useVboArb) ARBVertexBufferObject.glBindBufferARB(i, j);
        else GL15.glBindBuffer(i, j);
    }
    public static void glBufferData(int i, ByteBuffer b, int j) {
        if (useVboArb) ARBVertexBufferObject.glBufferDataARB(i, b, j);
        else GL15.glBufferData(i, b, j);
    }
    public static void glDeleteBuffers(int i) {
        if (useVboArb) ARBVertexBufferObject.glDeleteBuffersARB(i);
        else GL15.glDeleteBuffers(i);
    }

    // === FRAMEBUFFER ===
    public static int glGenFramebuffers() {
        switch (fboMode) {
            case BASE: return GL30.glGenFramebuffers();
            case ARB: return ARBFramebufferObject.glGenFramebuffers();
            case EXT: return EXTFramebufferObject.glGenFramebuffersEXT();
            default: return -1;
        }
    }
    public static void glBindFramebuffer(int i, int j) {
        switch (fboMode) {
            case BASE: GL30.glBindFramebuffer(i, j); break;
            case ARB: ARBFramebufferObject.glBindFramebuffer(i, j); break;
            case EXT: EXTFramebufferObject.glBindFramebufferEXT(i, j); break;
        }
    }
    public static void glDeleteFramebuffers(int i) {
        switch (fboMode) {
            case BASE: GL30.glDeleteFramebuffers(i); break;
            case ARB: ARBFramebufferObject.glDeleteFramebuffers(i); break;
            case EXT: EXTFramebufferObject.glDeleteFramebuffersEXT(i); break;
        }
    }
    public static int glGenRenderbuffers() {
        switch (fboMode) {
            case BASE: return GL30.glGenRenderbuffers();
            case ARB: return ARBFramebufferObject.glGenRenderbuffers();
            case EXT: return EXTFramebufferObject.glGenRenderbuffersEXT();
            default: return -1;
        }
    }
    public static void glBindRenderbuffer(int i, int j) {
        switch (fboMode) {
            case BASE: GL30.glBindRenderbuffer(i, j); break;
            case ARB: ARBFramebufferObject.glBindRenderbuffer(i, j); break;
            case EXT: EXTFramebufferObject.glBindRenderbufferEXT(i, j); break;
        }
    }
    public static void glRenderbufferStorage(int i, int j, int k, int l) {
        switch (fboMode) {
            case BASE: GL30.glRenderbufferStorage(i, j, k, l); break;
            case ARB: ARBFramebufferObject.glRenderbufferStorage(i, j, k, l); break;
            case EXT: EXTFramebufferObject.glRenderbufferStorageEXT(i, j, k, l); break;
        }
    }
    public static void glFramebufferRenderbuffer(int i, int j, int k, int l) {
        switch (fboMode) {
            case BASE: GL30.glFramebufferRenderbuffer(i, j, k, l); break;
            case ARB: ARBFramebufferObject.glFramebufferRenderbuffer(i, j, k, l); break;
            case EXT: EXTFramebufferObject.glFramebufferRenderbufferEXT(i, j, k, l); break;
        }
    }
    public static void glFramebufferTexture2D(int i, int j, int k, int l, int m) {
        switch (fboMode) {
            case BASE: GL30.glFramebufferTexture2D(i, j, k, l, m); break;
            case ARB: ARBFramebufferObject.glFramebufferTexture2D(i, j, k, l, m); break;
            case EXT: EXTFramebufferObject.glFramebufferTexture2DEXT(i, j, k, l, m); break;
        }
    }
    public static int glCheckFramebufferStatus(int i) {
        switch (fboMode) {
            case BASE: return GL30.glCheckFramebufferStatus(i);
            case ARB: return ARBFramebufferObject.glCheckFramebufferStatus(i);
            case EXT: return EXTFramebufferObject.glCheckFramebufferStatusEXT(i);
            default: return -1;
        }
    }
    public static void glDeleteRenderbuffers(int i) {
        switch (fboMode) {
            case BASE: GL30.glDeleteRenderbuffers(i); break;
            case ARB: ARBFramebufferObject.glDeleteRenderbuffers(i); break;
            case EXT: EXTFramebufferObject.glDeleteRenderbuffersEXT(i); break;
        }
    }

    // === MULTITEXTURE ===
    public static void glActiveTexture(int i) {
        if (useMultitextureArb) ARBMultitexture.glActiveTextureARB(i);
        else GL13.glActiveTexture(i);
    }

    // === BLEND ===
    public static void glBlendFuncSeparate(int i, int j, int k, int l) {
        if (separateBlend) {
            if (useSeparateBlendExt) EXTBlendFuncSeparate.glBlendFuncSeparateEXT(i, j, k, l);
            else GL14.glBlendFuncSeparate(i, j, k, l);
        } else {
            GL11.glBlendFunc(i, j);
        }
    }

    // === EXTRA: brakujace stale FBO error codes ===
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 36054;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 36055;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 36059;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 36060;

    /** MC RenderTarget uzywa - w oryginale bylo useFbo=true, tu to samo. */
    public static boolean isUsingFBOs() {
        return true;
    }

    // === MAKE HELPERS (MC uzywa do lambda-init) ===
    public static <T> T make(java.util.function.Supplier<T> supplier) {
        return supplier.get();
    }

    public static <T> T make(T object, java.util.function.Consumer<T> consumer) {
        consumer.accept(object);
        return object;
    }
}
