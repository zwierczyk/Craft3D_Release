package craft3dgl.blaze3d.shaders;

import craft3dgl.blaze3d.pipeline.RenderTarget;
import craft3dgl.blaze3d.platform.GLX;
import craft3dgl.blaze3d.platform.GlStateManager;
import craft3dgl.save.AssetFinder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MC 1.14.4 EffectInstance port (uproszczony).
 *
 * Konkretna implementacja Effect - laduje shader z 3 plikow:
 *   assets/shaders/program/{name}.json  (config: vertex, fragment, samplers, uniforms, blend)
 *   assets/shaders/program/{vertex}.vsh
 *   assets/shaders/program/{fragment}.fsh
 *
 * Uzycie:
 *   EffectInstance shader = new EffectInstance("blur");
 *   shader.safeGetUniform("Radius").set(2.0f);
 *   shader.setSampler("DiffuseSampler", renderTarget);
 *   shader.apply();
 *   // ... draw ...
 *   shader.clear();
 */
public class EffectInstance implements Effect, AutoCloseable {
    private static final AbstractUniform DUMMY_UNIFORM = new AbstractUniform();
    private static EffectInstance lastAppliedEffect;
    private static int lastProgramId = -1;

    private final Map<String, Object> samplerMap = new HashMap<>();
    private final List<String> samplerNames = new ArrayList<>();
    private final List<Integer> samplerLocations = new ArrayList<>();
    private final List<Uniform> uniforms = new ArrayList<>();
    private final List<Integer> uniformLocations = new ArrayList<>();
    private final Map<String, Uniform> uniformMap = new LinkedHashMap<>();
    private final int programId;
    private final String name;
    private final boolean cull;
    private boolean dirty;
    private final BlendMode blend;
    private final List<Integer> attributes;
    private final List<String> attributeNames;
    private final Program vertexProgram;
    private final Program fragmentProgram;

    public EffectInstance(String name) throws IOException {
        this(name, "shaders/program");
    }

    public EffectInstance(String name, String assetSubdir) throws IOException {
        this.name = name;
        File dir = AssetFinder.findAssetDir(assetSubdir, EffectInstance.class);
        File jsonFile = new File(dir, name + ".json");
        if (!jsonFile.isFile()) throw new IOException("Shader JSON not found: " + jsonFile);

        // Wczytaj JSON
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        }
        Object rootObj = JsonParser.parse(sb.toString());
        if (!(rootObj instanceof Map)) throw new IOException("Shader JSON root not object: " + jsonFile);

        String vertexName = JsonParser.getAsString(rootObj, "vertex");
        String fragmentName = JsonParser.getAsString(rootObj, "fragment");
        if (vertexName == null || fragmentName == null) {
            throw new IOException("Shader JSON missing vertex/fragment: " + jsonFile);
        }

        // Samplers
        List<Object> samplers = JsonParser.getAsArray(rootObj, "samplers");
        if (samplers != null) {
            for (Object s : samplers) {
                parseSamplerNode(s);
            }
        }

        // Attributes
        List<Object> attrs = JsonParser.getAsArray(rootObj, "attributes");
        if (attrs != null) {
            this.attributes = new ArrayList<>(attrs.size());
            this.attributeNames = new ArrayList<>(attrs.size());
            for (Object a : attrs) {
                if (a instanceof String) this.attributeNames.add((String) a);
            }
        } else {
            this.attributes = null;
            this.attributeNames = null;
        }

        // Blend
        Map<String, Object> blendJson = JsonParser.getAsObject(rootObj, "blend");
        this.blend = parseBlendNode(blendJson);
        this.cull = JsonParser.getAsBoolean(rootObj, "cull", true);

        // Kompiluj vertex + fragment
        this.vertexProgram = getOrCreate(dir, Program.Type.VERTEX, vertexName);
        this.fragmentProgram = getOrCreate(dir, Program.Type.FRAGMENT, fragmentName);
        this.programId = ProgramManager.getInstance().createProgram();
        ProgramManager.getInstance().linkProgram(this);

        // Parsuj uniformy (wymaga programId juz zlinkowanego - ale MC parsuje przed link,
        // updateLocations sie odbywa po link)
        List<Object> uniformsArr = JsonParser.getAsArray(rootObj, "uniforms");
        if (uniformsArr != null) {
            for (Object u : uniformsArr) {
                parseUniformNode(u);
            }
        }

        this.updateLocations();

        // Attributes locations
        if (this.attributeNames != null) {
            for (String attrName : this.attributeNames) {
                int loc = GLX.glGetAttribLocation(this.programId, attrName);
                this.attributes.add(loc);
            }
        }

        this.markDirty();
    }

    private static Program getOrCreate(File dir, Program.Type type, String name) throws IOException {
        // DEBUG: zawsze recompile - nie cachuj
        Program old = type.getPrograms().get(name);
        if (old != null) {
            try { old.close(); } catch (Throwable ignored) {}
            type.getPrograms().remove(name);
        }
        File file = new File(dir, name + type.getExtension());
        if (!file.isFile()) throw new IOException("Shader " + type.getName() + " not found: " + file);
        try (InputStream is = new FileInputStream(file)) {
            return Program.compileShader(type, name, is);
        }
    }

    public static BlendMode parseBlendNode(Map<String, Object> json) {
        if (json == null) return new BlendMode();

        int func = 32774;  // GL_FUNC_ADD
        int srcRgb = 1;
        int dstRgb = 0;
        int srcAlpha = 1;
        int dstAlpha = 0;
        boolean isDefault = true;
        boolean hasAlpha = false;

        String s = JsonParser.getAsString(json, "func");
        if (s != null) {
            func = BlendMode.stringToBlendFunc(s);
            if (func != 32774) isDefault = false;
        }
        s = JsonParser.getAsString(json, "srcrgb");
        if (s != null) {
            srcRgb = BlendMode.stringToBlendFactor(s);
            if (srcRgb != 1) isDefault = false;
        }
        s = JsonParser.getAsString(json, "dstrgb");
        if (s != null) {
            dstRgb = BlendMode.stringToBlendFactor(s);
            if (dstRgb != 0) isDefault = false;
        }
        s = JsonParser.getAsString(json, "srcalpha");
        if (s != null) {
            srcAlpha = BlendMode.stringToBlendFactor(s);
            if (srcAlpha != 1) isDefault = false;
            hasAlpha = true;
        }
        s = JsonParser.getAsString(json, "dstalpha");
        if (s != null) {
            dstAlpha = BlendMode.stringToBlendFactor(s);
            if (dstAlpha != 0) isDefault = false;
            hasAlpha = true;
        }
        if (isDefault) return new BlendMode();
        return hasAlpha ? new BlendMode(srcRgb, dstRgb, srcAlpha, dstAlpha, func)
                        : new BlendMode(srcRgb, dstRgb, func);
    }

    public void close() {
        for (Uniform u : this.uniforms) u.close();
        ProgramManager.getInstance().releaseProgram(this);
    }

    public void clear() {
        GLX.glUseProgram(0);
        lastProgramId = -1;
        lastAppliedEffect = null;
        for (int i = 0; i < this.samplerNames.size(); i++) {
            String samplerName = this.samplerNames.get(i);
            if (this.samplerMap.get(samplerName) != null) {
                int unit = extractSamplerUnit(samplerName, i);
                GlStateManager.activeTexture(GLX.GL_TEXTURE0 + unit);
                GlStateManager.bindTexture(0);
            }
        }
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);
    }

    private static int applyCount = 0;
    public void apply() {
        this.dirty = false;
        lastAppliedEffect = this;
        this.blend.apply();

        boolean debugApply = (applyCount++ < 3);

        // ZAWSZE wywoluj glUseProgram - NVIDIA reusuje programId po glDeleteProgram
        GLX.glUseProgram(this.programId);
        lastProgramId = this.programId;

        if (this.cull) GlStateManager.enableCull();
        else GlStateManager.disableCull();

        for (int i = 0; i < this.samplerNames.size(); i++) {
            String samplerName = this.samplerNames.get(i);
            if (this.samplerMap.get(samplerName) != null) {
                // Texture unit = numer z nazwy sampler (Sampler0->0, Sampler2->2, DiffuseSampler->i)
                int unit = extractSamplerUnit(samplerName, i);
                GlStateManager.activeTexture(GLX.GL_TEXTURE0 + unit);
                GlStateManager.enableTexture();
                Object o = this.samplerMap.get(samplerName);
                int texId = -1;
                if (o instanceof RenderTarget) {
                    texId = ((RenderTarget) o).colorTextureId;
                } else if (o instanceof Integer) {
                    texId = (Integer) o;
                }
                if (texId != -1) {
                    GlStateManager.bindTexture(texId);
                    int loc = GLX.glGetUniformLocation(this.programId, samplerName);
                    GLX.glUniform1i(loc, unit);
                    if (debugApply) System.out.println("[Shader " + this.name + "] apply sampler " + samplerName
                        + " unit=" + unit + " texId=" + texId + " loc=" + loc);
                }
            }
        }
        // Powrot do unit 0 zeby nie zaburzyc pozniejszych operacji
        GlStateManager.activeTexture(GLX.GL_TEXTURE0);

        for (Uniform u : this.uniforms) u.upload();
    }

    @Override
    public void markDirty() {
        this.dirty = true;
    }

    public Uniform getUniform(String name) {
        return this.uniformMap.get(name);
    }

    public AbstractUniform safeGetUniform(String name) {
        Uniform u = this.getUniform(name);
        return u == null ? DUMMY_UNIFORM : u;
    }

    /** Wyciaga numer texture unit z nazwy sampler: "Sampler0"->0, "Sampler2"->2, inne->fallback. */
    private static int extractSamplerUnit(String name, int fallback) {
        // Parse trailing digits
        int end = name.length();
        int start = end;
        while (start > 0 && Character.isDigit(name.charAt(start - 1))) start--;
        if (start < end) {
            try { return Integer.parseInt(name.substring(start, end)); } catch (Exception ignored) {}
        }
        return fallback;
    }

    private void updateLocations() {
        // Samplery
        this.samplerLocations.clear();
        java.util.Iterator<String> it = this.samplerNames.iterator();
        while (it.hasNext()) {
            String s = it.next();
            int loc = GLX.glGetUniformLocation(this.programId, s);
            if (loc == -1) {
                System.err.println("[Shader " + this.name + "] sampler '" + s + "' not found in program");
                this.samplerMap.remove(s);
                it.remove();
            } else {
                this.samplerLocations.add(loc);
                System.out.println("[Shader " + this.name + "] sampler '" + s + "' location=" + loc);
            }
        }
        for (Uniform u : this.uniforms) {
            String s = u.getName();
            int loc = GLX.glGetUniformLocation(this.programId, s);
            if (loc == -1) {
                System.err.println("[Shader " + this.name + "] uniform '" + s + "' not found in program");
            } else {
                this.uniformLocations.add(loc);
                u.setLocation(loc);
                this.uniformMap.put(s, u);
            }
        }
    }

    private void parseSamplerNode(Object node) {
        String samplerName = JsonParser.getAsString(node, "name");
        if (samplerName == null) return;
        String file = JsonParser.getAsString(node, "file");
        if (file == null) {
            this.samplerMap.put(samplerName, null);
        }
        this.samplerNames.add(samplerName);
    }

    public void setSampler(String name, Object o) {
        this.samplerMap.remove(name);
        this.samplerMap.put(name, o);
        this.markDirty();
    }

    private void parseUniformNode(Object node) {
        String uniformName = JsonParser.getAsString(node, "name");
        String typeStr = JsonParser.getAsString(node, "type");
        int type = Uniform.getTypeFromString(typeStr);
        int count = JsonParser.getAsInt(node, "count", 0);
        List<Object> values = JsonParser.getAsArray(node, "values");
        float[] fs = new float[Math.max(count, 16)];
        int k = 0;
        if (values != null) {
            for (Object v : values) {
                fs[k++] = JsonParser.asFloat(v);
            }
            // Jesli tylko 1 value dla count>1, replicate
            if (count > 1 && values.size() == 1) {
                while (k < count) { fs[k] = fs[0]; k++; }
            }
        }

        int l = count > 1 && count <= 4 && type < 8 ? count - 1 : 0;
        Uniform u = new Uniform(uniformName, type + l, count, this);
        if (type <= 3) {
            u.setSafe((int)fs[0], (int)fs[1], (int)fs[2], (int)fs[3]);
        } else if (type <= 7) {
            u.setSafe(fs[0], fs[1], fs[2], fs[3]);
        } else {
            u.set(fs);
        }
        this.uniforms.add(u);
    }

    @Override
    public Program getVertexProgram() { return this.vertexProgram; }
    @Override
    public Program getFragmentProgram() { return this.fragmentProgram; }
    @Override
    public int getId() { return this.programId; }
    public String getName() { return this.name; }
    public boolean isDirty() { return this.dirty; }
    public BlendMode getBlend() { return this.blend; }
}
