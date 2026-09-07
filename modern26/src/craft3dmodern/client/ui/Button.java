package craft3dmodern.client.ui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import craft3dmodern.client.font.FontRenderer;
import craft3dmodern.render.GuiBlit;
import craft3dmodern.render.Texture;
import craft3dmodern.util.Json;

public final class Button {
    public static final int SRC_W = 200;
    public static final int SRC_H = 20;

    private static final Map<String, Sprite> CACHE = new HashMap<String, Sprite>();

    private static final class Sprite {
        int tex = -1;
        int border = 3;
    }

    private static Sprite sprite(String name) {
        Sprite sp = CACHE.get(name);
        if (sp != null) return sp;
        sp = new Sprite();
        try {
            String path = "minecraft/textures/gui/sprites/widget/" + name + ".png";
            BufferedImage img = javax.imageio.ImageIO.read(new File(Texture.assetRoot(), path));
            if (img != null) sp.tex = Texture.upload(img, false);
            File meta = new File(Texture.assetRoot(), path + ".mcmeta");
            if (meta.isFile()) {
                Object root = Json.parseFile(meta.toPath());
                Object gui = Json.asObject(root).get("gui");
                Object scaling = Json.asObject(gui).get("scaling");
                Object border = Json.asObject(scaling).get("border");
                if (border instanceof Number) sp.border = ((Number) border).intValue();
            }
        } catch (Throwable t) {
            System.err.println("[Button] cannot load " + name + ": " + t.getMessage());
        }
        CACHE.put(name, sp);
        return sp;
    }

    public float x, y, w, h;
    public String label = "";
    public boolean enabled = true;

    public Button(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean hover(float mx, float my) {
        return enabled && mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void draw(GuiBlit g, FontRenderer font, boolean mouseOver, float s) {
        String spriteName = !enabled ? "button_disabled"
                : (mouseOver ? "button_highlighted" : "button");
        Sprite sp = sprite(spriteName);
        float px = x * s;
        float py = y * s;
        float pw = w * s;
        float ph = h * s;
        if (sp.tex <= 0) {
            g.rect(px, py, pw, ph, 0.3f, 0.3f, 0.3f, 1f);
        } else {
            drawNine(g, sp.tex, sp.border, pw / SRC_W, px, py, pw, ph);
        }
        if (label != null && !label.isEmpty()) {
            int color = !enabled ? 0xFFA0A0A0 : (mouseOver ? 0xFFFFFFA0 : 0xFFE0E0E0);
            float textW = font.width(label) * s;
            float textH = font.height() * s;
            font.draw(g, label, px + (pw - textW) / 2f, py + (ph - textH) / 2f, s, color, true);
        }
    }

    private static void drawNine(GuiBlit g, int tex, int border, float f,
                                 float x, float y, float w, float h) {
        float b = border * f;
        float midW = Math.max(0, w - 2 * b);
        float midH = Math.max(0, h - 2 * b);
        int bS = border;
        float[][] quads = {
                {0, 0, b, b, 0, 0, bS, bS},
                {b, 0, midW, b, bS, 0, SRC_W - bS, bS},
                {w - b, 0, b, b, SRC_W - bS, 0, SRC_W, bS},
                {0, b, b, midH, 0, bS, bS, SRC_H - bS},
                {b, b, midW, midH, bS, bS, SRC_W - bS, SRC_H - bS},
                {w - b, b, b, midH, SRC_W - bS, bS, SRC_W, SRC_H - bS},
                {0, h - b, b, b, 0, SRC_H - bS, bS, SRC_H},
                {b, h - b, midW, b, bS, SRC_H - bS, SRC_W - bS, SRC_H},
                {w - b, h - b, b, b, SRC_W - bS, SRC_H - bS, SRC_W, SRC_H},
        };
        for (float[] q : quads) {
            if (q[2] <= 0 || q[3] <= 0) continue;
            g.draw(tex, q[0] + x, q[1] + y, q[2], q[3],
                    q[4] / SRC_W, q[5] / SRC_H, q[6] / SRC_W, q[7] / SRC_H);
        }
    }
}
