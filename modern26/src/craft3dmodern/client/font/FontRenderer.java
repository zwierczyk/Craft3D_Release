package craft3dmodern.client.font;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import craft3dmodern.render.GuiBlit;
import craft3dmodern.render.Texture;
import craft3dmodern.util.Json;


public final class FontRenderer {
    
    public static final class Glyph {
        public String texPath;
        public float u0, v0, u1, v1;
        public float w, h;
        public float adv;
    }

    private final Map<Character, Glyph> glyphs = new HashMap<Character, Glyph>();
    private final Map<String, Integer> textures = new HashMap<String, Integer>();
    private final int height; 

    private FontRenderer(int height) {
        this.height = height;
    }

    
    public static FontRenderer load() throws IOException {
        File asset = Texture.assetRoot();
        File fontDir = new File(new File(new File(asset, "minecraft"), "font"), "default.json");
        if (!fontDir.isFile()) throw new IOException("missing font/default.json at " + fontDir);
        Object root = Json.parseFile(fontDir.toPath());
        List<Object> providers = Json.asList(Json.asObject(root).get("providers"));

        
        FontRenderer fr = new FontRenderer(8);
        Set<String> resolving = new LinkedHashSet<String>();
        fr.collect(fontDir, providers, resolving, 0);
        return fr;
    }

    @SuppressWarnings("unchecked")
    private void collect(File fontDir, List<Object> providers, Set<String> resolving, int depth)
            throws IOException {
        if (depth > 6 || providers == null) return;
        for (Object o : providers) {
            Map<String, Object> p = Json.asObject(o);
            String type = (String) p.get("type");
            if ("reference".equals(type)) {
                String id = (String) p.get("id");
                if (id == null || !resolving.add(id)) continue;
                File f = resolveFontRef(new File(fontDir.getParentFile(), "default.json"), id);
                if (f != null && f.isFile()) {
                    Object sub = Json.parseFile(f.toPath());
                    List<Object> subProviders = Json.asList(Json.asObject(sub).get("providers"));
                    collect(f, subProviders, resolving, depth + 1);
                }
                resolving.remove(id);
            } else if ("bitmap".equals(type)) {
                readBitmapProvider(p);
            } else if ("space".equals(type)) {
                Map<String, Object> advances = Json.asObject(p.get("advances"));
                for (Map.Entry<String, Object> e : advances.entrySet()) {
                    if (e.getKey().isEmpty()) continue;
                    char c = e.getKey().charAt(0);
                    Glyph g = glyphs.get(c);
                    if (g == null) g = new Glyph();
                    g.adv = ((Number) e.getValue()).floatValue();
                    g.texPath = null; 
                    g.w = g.h = 0;
                    glyphs.put(c, g);
                }
            }
            
        }
    }

    private File resolveFontRef(File fallbackDir, String id) {
        
        String path = id;
        if (path.startsWith("minecraft:")) path = path.substring("minecraft:".length());
        if (!path.endsWith(".json")) path = path + ".json";
        File f = new File(fallbackDir.getParentFile(), path.replace('/', File.separatorChar));
        return f;
    }

    @SuppressWarnings("unchecked")
    private void readBitmapProvider(Map<String, Object> p) throws IOException {
        String file = (String) p.get("file"); 
        if (file == null) return;
        List<Object> rowsObj = Json.asList(p.get("chars"));
        int paramHeight = p.containsKey("height") ? ((Number) p.get("height")).intValue() : 8;
        int paramAscent = p.containsKey("ascent") ? ((Number) p.get("ascent")).intValue() : 7;

        String asset = toTextureAsset(file);
        File imageFile = new File(Texture.assetRoot(), asset);
        if (!imageFile.isFile()) {
            System.err.println("[font] missing texture " + asset);
            return;
        }
        BufferedImage img;
        try {
            img = javax.imageio.ImageIO.read(imageFile);
        } catch (IOException e) {
            System.err.println("[font] cannot decode " + asset + ": " + e.getMessage());
            return;
        }
        if (rowsObj.isEmpty() || img == null) return;
        List<String> rows = new ArrayList<String>();
        for (Object r : rowsObj) rows.add((String) r);

        int imgW = img.getWidth();
        int imgH = img.getHeight();
        int cols = rows.get(0).length();
        int rowsN = rows.size();
        if (cols == 0 || rowsN == 0) return;
        int gw = imgW / cols;
        int gh = imgH / rowsN;
        if (gw <= 0 || gh <= 0) return;
        float pixelScale = (float) paramHeight / (float) gh;
        float uStep = (float) gw / imgW;
        float vStep = (float) gh / imgH;

            for (int r = 0; r < rowsN; r++) {
            String line = rows.get(r);
            for (int c = 0; c < line.length() && c < cols; c++) {
                char ch = line.charAt(c);
                if (ch == 0 || glyphs.containsKey(ch)) continue; 
                int actualW = actualGlyphWidth(img, gw, gh, c, r);
                Glyph g = new Glyph();
                g.texPath = asset;
                g.u0 = c * uStep;
                g.v0 = r * vStep;
                g.u1 = g.u0 + uStep;
                g.v1 = g.v0 + vStep;
                g.w = gw * pixelScale;
                g.h = gh * pixelScale;
                g.adv = (int) (0.5 + actualW * pixelScale) + 1;
                
                glyphs.put(ch, g);
            }
        }
    }

    private static String toTextureAsset(String fileId) {
        String p = fileId;
        if (p.startsWith("minecraft:")) p = p.substring("minecraft:".length()); 
        if (p.startsWith("textures/")) p = p.substring("textures/".length());
        if (!p.startsWith("font/") && !p.startsWith("textures/")) p = "font/" + p;
        return "minecraft/textures/" + p;
    }

    
    private static int actualGlyphWidth(BufferedImage img, int gw, int gh, int gx, int gy) {
        for (int w = gw - 1; w >= 0; w--) {
            int px = gx * gw + w;
            for (int y = 0; y < gh; y++) {
                int py = gy * gh + y;
                int argb = img.getRGB(Math.min(px, img.getWidth() - 1), Math.min(py, img.getHeight() - 1));
                if (((argb >>> 24) & 255) != 0) return w + 1;
            }
        }
        return 0;
    }

    
    public void upload() throws IOException {
        Map<String, Integer> ids = new HashMap<String, Integer>();
        for (Glyph g : glyphs.values()) {
            if (g.texPath == null) continue;
            Integer tex = ids.get(g.texPath);
            if (tex == null) {
                File f = new File(Texture.assetRoot(), g.texPath);
                BufferedImage img = javax.imageio.ImageIO.read(f);
                if (img == null) { System.err.println("[font] skip " + g.texPath); continue; }
                tex = Integer.valueOf(Texture.upload(img, false));
                ids.put(g.texPath, tex);
            }
        }
        textures.putAll(ids);
    }

    public int height() {
        return height;
    }

    
    public float width(String s) {
        float w = 0;
        for (int i = 0; i < s.length(); i++) {
            Glyph g = glyphs.get(s.charAt(i));
            w += g == null ? 0 : g.adv;
        }
        return w;
    }

    public float width(String s, float scale) {
        return width(s) * scale;
    }

    public boolean has(char c) {
        return glyphs.containsKey(c);
    }

    public Glyph glyph(char c) {
        return glyphs.get(c);
    }

    
    public void draw(GuiBlit g, String s, float x, float y, float scale, int argb, boolean shadow) {
        float a = ((argb >>> 24) & 255) / 255f;
        if (shadow) {
            drawRgba(g, s, x + scale, y + scale, scale, 0f, 0f, 0f, a);
        }
        float r = ((argb >>> 16) & 255) / 255f;
        float gr = ((argb >>> 8) & 255) / 255f;
        float b = (argb & 255) / 255f;
        drawRgba(g, s, x, y, scale, r, gr, b, a);
    }

    private void drawRgba(GuiBlit blit, String s, float x, float y, float scale,
                          float r, float gr, float b, float a) {
        float cx = x;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            Glyph gl = glyphs.get(ch);
            if (gl == null) continue;
            if (gl.texPath == null || gl.w <= 0) {
                cx += gl.adv * scale;
                continue;
            }
            Integer tex = textures.get(gl.texPath);
            if (tex == null) { cx += gl.adv * scale; continue; }
            float dw = gl.w * scale;
            float dh = gl.h * scale;
            blit.drawTinted(tex, cx, y, dw, dh, gl.u0, gl.v0, gl.u1, gl.v1, r, gr, b, a);
            cx += gl.adv * scale;
        }
    }
}
