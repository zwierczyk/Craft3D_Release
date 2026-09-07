package craft3dmodern.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import craft3dmodern.render.Texture;
import craft3dmodern.util.Json;


public final class BlockModels {
    public static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;
    public static final String[] DIR_NAMES = {"down", "up", "north", "south", "west", "east"};

    
    public static final class Face {
        public final float[] x = new float[4];
        public final float[] y = new float[4];
        public final float[] z = new float[4];
        public final float[] u = new float[4];
        public final float[] v = new float[4];
        
        public final int dir;
        
        public final int cullDir;
        
        public final int tintIndex;
        
        public final String tex;

        Face(float[] xs, float[] ys, float[] zs, float[] us, float[] vs,
             int dir, int cullDir, int tintIndex, String tex) {
            System.arraycopy(xs, 0, x, 0, 4);
            System.arraycopy(ys, 0, y, 0, 4);
            System.arraycopy(zs, 0, z, 0, 4);
            System.arraycopy(us, 0, u, 0, 4);
            System.arraycopy(vs, 0, v, 0, 4);
            this.dir = dir;
            this.cullDir = cullDir;
            this.tintIndex = tintIndex;
            this.tex = tex;
        }
    }

    public static final class Model {
        public final List<Face> faces = new ArrayList<Face>();
    }

    
    private static final int[][][] VI = {
        {{0, 0, 1}, {0, 0, 0}, {1, 0, 0}, {1, 0, 1}}, 
        {{0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 1, 0}}, 
        {{1, 1, 0}, {1, 0, 0}, {0, 0, 0}, {0, 1, 0}}, 
        {{0, 1, 1}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}}, 
        {{0, 1, 0}, {0, 0, 0}, {0, 0, 1}, {0, 1, 1}}, 
        {{1, 1, 1}, {1, 0, 1}, {1, 0, 0}, {1, 1, 0}}, 
    };

    private final File assets; 
    private final Map<String, Map<String, Object>> rawCache = new HashMap<String, Map<String, Object>>();
    private final Map<String, List<Map<String, Object>>> chainCache = new HashMap<String, List<Map<String, Object>>>();

    public BlockModels() throws IOException {
        this(Texture.assetRoot());
    }

    public BlockModels(File assetsRoot) throws IOException {
        if (!new File(assetsRoot, "minecraft/models").isDirectory()) {
            throw new IOException("Brak assestow w " + assetsRoot);
        }
        this.assets = assetsRoot;
    }

    
    
    

    
    public Model modelFor(String blockName) throws IOException {
        return bakeState(blockName, null);
    }

    
    public Model modelFor(String blockName, String variant) throws IOException {
        return bakeState(blockName, variant);
    }

    
    public Set<String> texturesUsed(String... blockNames) throws IOException {
        Set<String> out = new HashSet<String>();
        for (String name : blockNames) {
            for (Face f : modelFor(name).faces) {
                if (f.tex != null) out.add(f.tex);
            }
        }
        return out;
    }

    
    
    

    private Model bakeState(String blockName, String wantedVariant) throws IOException {
        File stateFile = new File(assets, "minecraft/blockstates/" + blockName + ".json");
        Map<String, Object> state = Json.asObject(Json.parseFile(stateFile.toPath()));
        Object variantsObj = state.get("variants");
        if (!(variantsObj instanceof Map)) {
            throw new IOException("blockstate " + blockName + " nie ma variants (multipart nieobslugiwane)");
        }
        Map<String, Object> variants = Json.asObject(variantsObj);
        Object chosen = null;
        if (wantedVariant != null && variants.containsKey(wantedVariant)) {
            chosen = variants.get(wantedVariant);
        } else {
            for (Object o : variants.values()) { chosen = o; break; }
        }
        String modelName = null;
        if (chosen instanceof List) {
            List<?> list = (List<?>) chosen;
            if (!list.isEmpty()) modelName = firstModel(list.get(0));
        } else {
            modelName = firstModel(chosen);
        }
        if (modelName == null) throw new IOException("blockstate " + blockName + " bez modelu");
        return bakeModelFile(modelName);
    }

    private static String firstModel(Object o) {
        if (!(o instanceof Map)) return null;
        Object m = ((Map<?, ?>) o).get("model");
        return m instanceof String ? (String) m : null;
    }

    private Model bakeModelFile(String vanillaName) throws IOException {
        Model model = new Model();
        List<Map<String, Object>> chain = chain(normalizeModelName(vanillaName));

        
        Map<String, Object> owner = null;
        for (Map<String, Object> m : chain) {
            if (m.get("elements") instanceof List && !((List<?>) m.get("elements")).isEmpty()) {
                owner = m;
                break;
            }
        }
        if (owner == null) {
            
            return model;
        }
        List<?> elements = (List<?>) owner.get("elements");
        for (Object eo : elements) {
            if (!(eo instanceof Map)) continue;
            Map<String, Object> el = Json.asObject(eo);
            float[] from = float3(el.get("from"));
            float[] to = float3(el.get("to"));
            if (from == null || to == null) continue;
            Object facesObj = el.get("faces");
            if (!(facesObj instanceof Map)) continue;
            for (Map.Entry<String, Object> e : Json.asObject(facesObj).entrySet()) {
                int dir = dirIndex(e.getKey());
                if (dir < 0 || !(e.getValue() instanceof Map)) continue;
                Map<String, Object> fo = Json.asObject(e.getValue());
                Object texRef = fo.get("texture");
                if (!(texRef instanceof String)) continue;
                String tex = resolveTexture(chain, (String) texRef);
                if (tex == null) continue;

                float[] uvs = new float[]{0, 0, 16, 16};
                if (fo.get("uv") instanceof List) {
                    float[] uv = float4(fo.get("uv"));
                    if (uv != null) uvs = uv;
                }
                int rot = num(fo.get("rotation"), 0).intValue() / 90; 
                int cull = -1;
                Object cullObj = fo.get("cullface");
                if (cullObj instanceof String) cull = dirIndex((String) cullObj);
                int tint = num(fo.get("tintindex"), -1).intValue();

                float[] xs = new float[4], ys = new float[4], zs = new float[4];
                float[] us = new float[4], vs = new float[4];
                for (int i = 0; i < 4; i++) {
                    int ri = (i + rot) & 3;
                    xs[i] = VI[dir][i][0] == 0 ? from[0] : to[0];
                    ys[i] = VI[dir][i][1] == 0 ? from[1] : to[1];
                    zs[i] = VI[dir][i][2] == 0 ? from[2] : to[2];
                    us[i] = (ri == 0 || ri == 1) ? uvs[0] : uvs[2];
                    vs[i] = (ri == 0 || ri == 3) ? uvs[1] : uvs[3];
                }
                model.faces.add(new Face(xs, ys, zs, us, vs, dir, cull, tint, tex));
            }
        }
        return model;
    }

    
    
    

    
    private static String normalizeModelName(String vanillaName) {
        String s = vanillaName;
        if (s.startsWith("minecraft:")) s = s.substring("minecraft:".length());
        return s;
    }

    private Map<String, Object> raw(String fileBase) throws IOException {
        Map<String, Object> cached = rawCache.get(fileBase);
        if (cached != null) return cached;
        File f = new File(assets, "minecraft/models/" + fileBase + ".json");
        if (!f.isFile()) throw new IOException("brak modelu " + f);
        Map<String, Object> o = Json.asObject(Json.parseFile(f.toPath()));
        rawCache.put(fileBase, o);
        return o;
    }

    
    private List<Map<String, Object>> chain(String fileBase) throws IOException {
        List<Map<String, Object>> cached = chainCache.get(fileBase);
        if (cached != null) return cached;
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        Set<String> seen = new HashSet<String>();
        String cur = fileBase;
        while (cur != null && seen.add(cur)) {
            Map<String, Object> o = raw(cur);
            out.add(o);
            Object p = o.get("parent");
            cur = null;
            if (p instanceof String) {
                String parent = (String) p;
                cur = normalizeModelName(parent);
            }
        }
        chainCache.put(fileBase, out);
        return out;
    }

    private static String resolveTexture(List<Map<String, Object>> chain, String ref) {
        if (!ref.startsWith("#")) return normalizeTex(ref);
        Set<String> seen = new HashSet<String>();
        String cur = ref.substring(1);
        while (seen.add(cur)) {
            String val = null;
            for (Map<String, Object> m : chain) {
                Object t = m.get("textures");
                if (t instanceof Map && ((Map<?, ?>) t).containsKey(cur)) {
                    Object v = ((Map<?, ?>) t).get(cur);
                    if (v instanceof String) { val = (String) v; break; }
                }
            }
            if (val == null) return null;
            if (val.startsWith("#")) { cur = val.substring(1); continue; }
            return normalizeTex(val);
        }
        return null;
    }

    private static String normalizeTex(String v) {
        String s = v;
        if (s.startsWith("minecraft:")) s = s.substring("minecraft:".length());
        if (s.indexOf(':') >= 0) return null; 
        if (!s.startsWith("block/")) return null;
        return s;
    }

    
    
    

    private static int dirIndex(String name) {
        for (int i = 0; i < DIR_NAMES.length; i++) {
            if (DIR_NAMES[i].equals(name)) return i;
        }
        return -1;
    }

    private static float[] float3(Object o) {
        if (!(o instanceof List)) return null;
        List<?> l = (List<?>) o;
        if (l.size() < 3) return null;
        return new float[]{num(l.get(0), 0).floatValue(), num(l.get(1), 0).floatValue(), num(l.get(2), 0).floatValue()};
    }

    private static float[] float4(Object o) {
        if (!(o instanceof List)) return null;
        List<?> l = (List<?>) o;
        if (l.size() < 4) return null;
        return new float[]{num(l.get(0), 0).floatValue(), num(l.get(1), 0).floatValue(),
                          num(l.get(2), 0).floatValue(), num(l.get(3), 0).floatValue()};
    }

    private static Double num(Object o, double def) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return def;
    }
}
