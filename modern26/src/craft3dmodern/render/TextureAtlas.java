package craft3dmodern.render;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Skleja tekstury blokow w jeden atlas (bez GL - czysty BufferedImage),
 * co pozwala testowac go headless.
 */
public final class TextureAtlas {
    public static final int PAGE = 512;
    /** dodatkowy przezroczysty piksel wokol kazdego kafelka (na mipmapy w przyszlosci) */
    public static final int PAD = 1;

    public static final class Entry {
        public final String tex;      // "block/stone"
        public final int x, y, w, h;  // polozenie kafelka na stronie
        public final boolean gray;    // tekstura szara (wymaga tintu)
        public final int tileW;       // rozmiar w pikselach (0..w)
        Entry(String tex, int x, int y, int w, int h, boolean gray) {
            this.tex = tex;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.gray = gray;
            this.tileW = w - 2 * PAD;
        }
    }

    private final Map<String, Entry> entries = new LinkedHashMap<String, Entry>();
    private final BufferedImage page;

    private TextureAtlas(BufferedImage page) {
        this.page = page;
    }

    public static TextureAtlas build(Collection<String> textureAssetNames) throws IOException {
        BufferedImage page = new BufferedImage(PAGE, PAGE, BufferedImage.TYPE_INT_ARGB);
        TextureAtlas at = new TextureAtlas(page);
        int stride = 18;
        int perRow = (PAGE - PAD) / stride;
        int i = 0;
        for (String name : textureAssetNames) {
            BufferedImage img = Texture.decode("minecraft/textures/" + name + ".png");
            int w = img.getWidth();
            int h = img.getHeight();
            if (w != h || w < 1 || w > 32) {
                throw new IOException("Nieobslugiwana tekstura bloku: " + name + " (" + w + "x" + h + ")");
            }
            int cell = w + 2 * PAD;
            int col = i % perRow;
            int row = i / perRow;
            int ox = PAD + col * stride;
            int oy = PAD + row * stride;
            if (oy + cell > PAGE) throw new IOException("atlas za maly dla " + i + " tekstur");
            for (int y = 0; y < w; y++) {
                for (int x = 0; x < w; x++) {
                    page.setRGB(ox + x, oy + y, img.getRGB(x, y));
                }
            }
            at.entries.put(name, new Entry(name, ox, oy, cell, cell, isGray(img)));
            i++;
        }
        return at;
    }

    private static boolean isGray(BufferedImage img) {
        int n = img.getWidth() * img.getHeight();
        long rs = 0, gs = 0, bs = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int p = img.getRGB(x, y);
                rs += (p >>> 16) & 255;
                gs += (p >>> 8) & 255;
                bs += p & 255;
            }
        }
        double ar = rs / (double) n, ag = gs / (double) n, ab = bs / (double) n;
        double mx = Math.max(ar, Math.max(ag, ab));
        double mn = Math.min(ar, Math.min(ag, ab));
        return mx - mn < 8.0;
    }

    public BufferedImage page() {
        return page;
    }

    public int count() {
        return entries.size();
    }

    public Entry entry(String tex) {
        return entries.get(tex);
    }

    public Collection<Entry> all() {
        return entries.values();
    }
}
