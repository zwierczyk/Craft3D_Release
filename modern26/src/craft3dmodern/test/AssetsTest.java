package craft3dmodern.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import craft3dmodern.render.Texture;

/** Headless sanity test: prawdziwe assety vanilla 26.2 dekoduja sie poprawnie. */
public final class AssetsTest {
    private static final List<String> FAILS = new ArrayList<>();

    private static void check(boolean ok, String what) {
        if (ok) System.out.println("ok:   " + what);
        else FAILS.add(what);
    }

    public static void main(String[] args) throws Exception {
        String[] mustDecode = {
                "minecraft/textures/gui/title/minecraft.png",
                "minecraft/textures/gui/title/background/panorama_0.png",
                "minecraft/textures/gui/title/background/panorama_overlay.png",
                "minecraft/textures/gui/sprites/widget/button.png",
                "minecraft/textures/gui/sprites/widget/button_highlighted.png",
                "minecraft/textures/font/ascii.png",
                "minecraft/textures/font/accented.png",
                "minecraft/textures/block/grass_block_top.png",
                "minecraft/textures/block/stone.png",
        };
        for (String p : mustDecode) {
            try {
                BufferedImage img = Texture.decode(p);
                check(img.getWidth() > 0 && img.getHeight() > 0, "decode " + p + " (" + img.getWidth() + "x" + img.getHeight() + ")");
            } catch (IOException e) {
                check(false, "decode " + p + " -> " + e.getMessage());
            }
        }

        // Wszystkie PNG w gui sprites + gui title musza sie dekodowac.
        int pngs = 0, decoded = 0;
        File root = new File(Texture.assetRoot(), "minecraft/textures/gui");
        if (root.isDirectory()) {
            List<File> files = new ArrayList<>();
            collect(root, files);
            for (File f : files) {
                if (!f.getName().endsWith(".png")) continue;
                pngs++;
                try {
                    BufferedImage img = Texture.decode(rel(f));
                    if (img != null && img.getWidth() > 0) decoded++;
                } catch (IOException e) {
                    check(false, "decode gui " + rel(f));
                }
            }
        }
        check(pngs > 50, "found gui png count " + pngs);
        check(decoded == pngs, "all gui pngs decode (" + decoded + "/" + pngs + ")");

        // wersja + tlumaczenia
        File version = new File(Texture.assetRoot(), "version.json");
        String vjson = new String(Files.readAllBytes(version.toPath()), StandardCharsets.UTF_8);
        check(vjson.contains("\"id\" : \"26.2\"") || vjson.contains("\"id\": \"26.2\"") || vjson.contains("26.2"),
                "version.json is 26.2");

        File lang = new File(Texture.assetRoot(), "minecraft/lang/en_us.json");
        String en = new String(Files.readAllBytes(lang.toPath()), StandardCharsets.UTF_8);
        check(en.contains("options.title"), "en_us.json has options.title");
        check(en.contains("menu.singleplayer"), "en_us.json has menu.singleplayer");
        check(en.contains("options.sounds.title"), "en_us.json has options.sounds.title");

        // modele
        File model = new File(Texture.assetRoot(), "minecraft/models/block/grass_block.json");
        check(model.isFile(), "model grass_block.json exists");
        File rootVersion = new File(Texture.assetRoot(), "version.json");
        check(rootVersion.isFile(), "version.json file exists");

        if (FAILS.isEmpty()) {
            System.out.println("ALL ASSET TESTS PASSED (OK)");
        } else {
            System.out.println(FAILS.size() + " FAILURES");
            for (String f : FAILS) System.out.println("  FAIL " + f);
            System.exit(1);
        }
    }

    private static String rel(File f) {
        File root = Texture.assetRoot();
        return root.toPath().relativize(f.toPath()).toString().replace('\\', '/');
    }

    private static void collect(File dir, List<File> out) {
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            if (k.isDirectory()) collect(k, out);
            else out.add(k);
        }
    }
}
