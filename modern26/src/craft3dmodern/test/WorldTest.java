package craft3dmodern.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import craft3dmodern.model.BlockModels;
import craft3dmodern.render.Texture;
import craft3dmodern.render.TextureAtlas;
import craft3dmodern.world.BlockIds;
import craft3dmodern.world.MeshBuilder;
import craft3dmodern.world.World;
import craft3dmodern.world.WorldGen;


public final class WorldTest {
    private static final List<String> FAILS = new ArrayList<String>();

    private static void check(boolean ok, String what) {
        if (ok) System.out.println("ok:   " + what);
        else FAILS.add(what);
    }

    public static void main(String[] args) throws Exception {
        BlockModels bm = new BlockModels();

        
        BlockModels.Model stone = bm.modelFor("stone");
        check(!stone.faces.isEmpty() && stone.faces.size() <= 6, "stone: pelny szescian (6 scian), faktycznie " + stone.faces.size());
        check(stone.faces.size() == 6, "stone: dokladnie 6 scian");
        for (BlockModels.Face f : stone.faces) check(f.tex != null, "stone face " + f.dir + " ma teksture " + f.tex);

        BlockModels.Model grass = bm.modelFor("grass_block");
        boolean hasOverlay = false;
        boolean hasTint = false;
        for (BlockModels.Face f : grass.faces) {
            if (f.tex != null && f.tex.contains("overlay")) hasOverlay = true;
            if (f.tintIndex >= 0) hasTint = true;
        }
        check(hasOverlay && hasTint, "grass_block: overlay + tintindex wg modelu 26.2");

        BlockModels.Model log = bm.modelFor("oak_log", "axis=y");
        check(!log.faces.isEmpty(), "oak_log axis=y ma sciany");
        boolean topTex = false;
        for (BlockModels.Face f : log.faces) {
            if (f.tex != null && f.tex.endsWith("oak_log_top")) topTex = true;
        }
        check(topTex, "oak_log axis=y uzywa oak_log_top (gora/dol)");

        BlockModels.Model leavesModel = bm.modelFor("oak_leaves");
        check(!leavesModel.faces.isEmpty(), "oak_leaves ma sciany");

        
        Set<String> texSet = new LinkedHashSet<String>();
        for (int id = 1; id < BlockIds.count(); id++) {
            String name = BlockIds.name(id);
            String variant = id == BlockIds.OAK_LOG ? "axis=y" : null;
            for (BlockModels.Face f : bm.modelFor(name, variant).faces) {
                if (f.tex != null) texSet.add(f.tex);
            }
        }
        File root = Texture.assetRoot();
        for (String t : texSet) {
            File png = new File(root, "minecraft/textures/" + t + ".png");
            check(png.isFile(), "tekstura " + t + ".png istnieje");
        }

        
        TextureAtlas at = TextureAtlas.build(texSet);
        check(at.count() == texSet.size(), "atlas zawiera wszystkie " + texSet.size() + " tekstury");
        check(at.entry("block/stone") != null, "atlas ma stone");
        check(at.entry("block/grass_block_side_overlay") != null, "atlas ma overlay trawy");
        check(at.entry("block/grass_block_side_overlay").gray, "overlay trawy jest szary (tint)");
        check(at.entry("block/oak_leaves").gray, "liscie sa szare (tint)");
        check(at.entry("block/stone").x >= 0 && at.page().getWidth() == TextureAtlas.PAGE, "strona atlasu 512x512");

        
        World one = new World(3, 3, 3);
        one.set(1, 1, 1, BlockIds.STONE);
        float[] meshOne = MeshBuilder.build(one, bm, at);
        check(meshOne.length == 6 * 6 * MeshBuilder.FLOATS_PER_VERTEX,
                "1 blok = 6 scian x 2 trojkaty (" + meshOne.length + " floatow)");

        
        World cube = new World(3, 3, 3);
        for (int x = 0; x < 3; x++) for (int y = 0; y < 3; y++) for (int z = 0; z < 3; z++) cube.set(x, y, z, BlockIds.STONE);
        float[] meshCube = MeshBuilder.build(cube, bm, at);
        check(meshCube.length == 54 * 6 * MeshBuilder.FLOATS_PER_VERTEX,
                "pelnia 3x3x3: tylko skorupa (54 sciany), wewnetrzne wycullowane");

        
        World plat = new World(5, 6, 5);
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 0; y <= 4; y++) plat.set(x, y, z, BlockIds.STONE);
            }
        }
        float[] meshPlat = MeshBuilder.build(plat, bm, at);
        check(meshPlat.length == 150 * 6 * MeshBuilder.FLOATS_PER_VERTEX,
                "plateau 5x5x5: gora+dol+boki zewnetrzne (150 scian), wnetrze wycullowane");

        
        World w = WorldGen.generate(12345L);
        int nonAir = 0;
        int topMin = 999, topMax = -1;
        for (int x = 0; x < w.sx; x++) {
            for (int z = 0; z < w.sz; z++) {
                int t = w.topSolid(x, z);
                if (t >= 0) {
                    nonAir++;
                    topMin = Math.min(topMin, t);
                    topMax = Math.max(topMax, t);
                }
            }
        }
        check(nonAir == w.sx * w.sz, "kazda kolumna ma teren (" + nonAir + " kolumn)");
        check(topMax - topMin >= 4, "urozmaicona wysokosc terenu (" + topMin + ".." + topMax + ")");
        int leafBlocks = 0, logs = 0;
        for (int x = 0; x < w.sx; x++) {
            for (int y = 0; y < w.sy; y++) {
                for (int z = 0; z < w.sz; z++) {
                    int id = w.get(x, y, z);
                    if (id == BlockIds.OAK_LEAVES) leafBlocks++;
                    if (id == BlockIds.OAK_LOG) logs++;
                }
            }
        }
        check(logs > 0 && leafBlocks > 0, "swiat ma drzewa (logi=" + logs + ", liscie=" + leafBlocks + ")");
        boolean sandSeen = false;
        outer:
        for (int x = 0; x < w.sx; x++) {
            for (int y = 0; y < w.sy; y++) {
                for (int z = 0; z < w.sz; z++) {
                    if (w.get(x, y, z) == BlockIds.SAND) { sandSeen = true; break outer; }
                }
            }
        }
        check(sandSeen, "plaza z piasku istnieje (SAND)");

        
        float[] mesh = MeshBuilder.build(w, bm, at);
        long faces = mesh.length / (long) (6 * MeshBuilder.FLOATS_PER_VERTEX);
        check(mesh.length > 0 && faces > 2000, "mesh swiata: " + faces + " scian (>2000)");
        check(mesh.length % (6 * MeshBuilder.FLOATS_PER_VERTEX) == 0, "mesh jest wielokrotnoscia sciany");

        
        boolean bad = false;
        boolean tintedSeen = false;
        for (int i = 0; i < mesh.length; i += MeshBuilder.FLOATS_PER_VERTEX) {
            float x = mesh[i], y = mesh[i + 1], z = mesh[i + 2];
            float u = mesh[i + 3], v = mesh[i + 4];
            if (x < 0 || x > w.sx || y < 0 || y > w.sy || z < 0 || z > w.sz) bad = true;
            if (u < 0 || u > 1 || v < 0 || v > 1) bad = true;
            float r = mesh[i + 5], g = mesh[i + 6], b = mesh[i + 7];
            if (r < 0 || r > 1 || g < 0 || g > 1 || b < 0 || b > 1) bad = true;
            if (Math.abs(r - g) > 0.1f || Math.abs(g - b) > 0.1f) tintedSeen = true;
        }
        check(!bad, "wszystkie wierzcholki/uv/kolory w zakresie");
        check(tintedSeen, "tint trawy widoczny w kolorach meshu (nie tylko szarosci)");

        if (FAILS.isEmpty()) {
            System.out.println("ALL WORLD TESTS PASSED (OK)");
        } else {
            System.out.println(FAILS.size() + " FAILURES");
            for (String s : FAILS) System.out.println("  FAIL " + s);
            System.exit(1);
        }
    }
}
