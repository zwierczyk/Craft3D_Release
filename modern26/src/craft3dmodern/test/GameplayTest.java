package craft3dmodern.test;

import java.util.ArrayList;
import java.util.List;

import craft3dmodern.client.Ingame;
import craft3dmodern.client.Input;
import craft3dmodern.world.BlockIds;
import craft3dmodern.world.Player;
import craft3dmodern.world.Raycast;
import craft3dmodern.world.World;
import craft3dmodern.world.WorldGen;

public final class GameplayTest {
    private static final List<String> FAILS = new ArrayList<String>();

    private static void check(boolean ok, String what) {
        if (ok) System.out.println("ok:   " + what);
        else FAILS.add(what);
    }

    public static void main(String[] args) throws Exception {
        World w = WorldGen.generate(99L);
        int cx = w.sx / 2;
        int cz = w.sz / 2;
        int ground = w.topSolid(cx, cz);
        Player p = new Player(cx + 0.5, ground + 1.0, cz + 0.5);
        p.yaw = 0;
        p.pitch = 0;
        for (int i = 0; i < 400; i++) {
            p.update(w, 0.05, false, false, false, false, false, false, false, false);
            if (p.onGround) break;
        }
        check(p.onGround, "gracz laduje na ziemi po spadku");
        check(Math.abs(p.y - (ground + 1.0)) < 0.05, "gracz stoi stabilnie na powierzchni (y=" + p.y + ")");

        double startY = p.y;
        boolean wentUp = false;
        for (int i = 0; i < 30; i++) {
            p.update(w, 0.05, false, false, false, false, true, false, false, false);
            if (p.y > startY + 0.5) wentUp = true;
        }
        check(wentUp, "skok unosi gracza (W=0.42/t analogia)");
        boolean landedBack = false;
        for (int i = 0; i < 200; i++) {
            p.update(w, 0.05, false, false, false, false, false, false, false, false);
            if (p.onGround && Math.abs(p.y - (ground + 1.0)) < 0.05) {
                landedBack = true;
                break;
            }
        }
        check(landedBack, "gracz wraca na ziemie po skoku");

        World flat = new World(16, 8, 32);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 32; z++) {
                flat.set(x, 0, z, BlockIds.STONE);
            }
        }
        Player fp = new Player(4.5, 1.0, 28.5);
        fp.yaw = 0;
        fp.pitch = 0;
        for (int i = 0; i < 40; i++) {
            fp.update(flat, 0.05, false, false, false, false, false, false, false, false);
        }
        check(fp.onGround, "gracz stoi na sztucznym plaskowyzu");
        double walkY = fp.y;
        for (int i = 0; i < 100; i++) {
            fp.update(flat, 0.05, true, false, false, false, false, false, false, false);
        }
        check(fp.onGround && Math.abs(fp.y - walkY) < 0.05, "chodzenie po plaskim utrzymuje wysokosc");

        World wall = new World(8, 8, 8);
        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 8; z++) wall.set(x, 0, z, BlockIds.STONE);
        }
        for (int y = 1; y < 4; y++) {
            for (int z = 0; z < 8; z++) wall.set(4, y, z, BlockIds.STONE);
        }
        Player wp = new Player(2.5, 1.0, 4.5);
        wp.yaw = (float) Math.toRadians(-90);
        wp.pitch = 0;
        for (int i = 0; i < 40 && !wp.onGround; i++) {
            wp.update(wall, 0.05, false, false, false, false, false, false, false, false);
        }
        for (int i = 0; i < 400; i++) {
            wp.update(wall, 0.05, true, false, false, false, false, false, false, false);
        }
        check(wp.x <= 4.0 - Player.WIDTH / 2 + 0.02, "sciana zatrzymuje gracza (x=" + wp.x + ")");

        boolean boxHit = wall.collidesBox(3.9, 0.0, 4.0, 4.1, 1.9, 5.0);
        boolean boxMiss = wall.collidesBox(0.0, 5.5, 0.0, 0.5, 6.0, 0.5);
        check(boxHit && !boxMiss, "kolizja AABB wykrywa blok tylko w kontakcie");

        World ray = new World(7, 7, 7);
        ray.set(3, 1, 3, BlockIds.STONE);
        Raycast.Hit hx = Raycast.cast(ray, 1.5, 1.5, 3.5, 1, 0, 0, 6.0);
        check(hx != null && hx.x == 3 && hx.y == 1 && hx.z == 3 && hx.face == 4,
                "raycast poziomy trafia w sciane wschodnia (face=4)");
        check(hx != null && Math.abs(hx.dist - 1.5) < 0.01, "raycast podaje odleglosc trafienia");
        Raycast.Hit hz = Raycast.cast(ray, 3.5, 1.5, 0.5, 0, 0, 1, 6.0);
        check(hz != null && hz.face == 2, "raycast z polnocy trafia w sciane poludniowa (face=2)");
        Raycast.Hit hy = Raycast.cast(ray, 3.5, 0.5, 3.5, 0, 1, 0, 6.0);
        check(hy != null && hy.y == 1 && hy.face == 0, "raycast pionowy trafia w spod (face=0)");
        Raycast.Hit miss = Raycast.cast(ray, 1.5, 1.5, 3.5, 1, 0, 0, 1.0);
        check(miss == null, "raycast szanuje zasieg");
        Raycast.Hit none = Raycast.cast(ray, 6.5, 6.5, 6.5, 1, 0, 0, 6.0);
        check(none == null, "raycast w pustce nie trafia");

        check(!BlockIds.occludes(BlockIds.GLASS), "szklo nie jest okluderem (przezroczyste)");
        check(BlockIds.occludes(BlockIds.STONE) && BlockIds.occludes(BlockIds.OAK_LEAVES),
                "kamien i liscie sa okluderami");
        World glassy = new World(3, 3, 3);
        glassy.set(1, 1, 1, BlockIds.GLASS);
        float[] mesh = craft3dmodern.world.MeshBuilder.build(glassy,
                new craft3dmodern.model.BlockModels(),
                craft3dmodern.render.TextureAtlas.build(blockTextures()));
        check(mesh.length > 0, "szklo jest renderowane (sciany emitowane)");

        Ingame ig = new Ingame(7L);
        check(ig.world() != null, "Ingame: swiat istnieje");
        check(ig.hotbarSlot(0) == BlockIds.STONE && ig.hotbarSlot(8) == BlockIds.GLASS,
                "Ingame: domyslny hotbar 9 pozycji");
        Input pitch = new Input();
        for (int i = 0; i < 400; i++) {
            pitch.mouseDy = 1.5;
            ig.update(pitch, 0.05);
        }
        int before = countBlocks(ig.world());
        Input pick = new Input();
        pick.leftDown = true;
        ig.update(pick, 0.05);
        check(countBlocks(ig.world()) == before - 1, "Ingame: klik niszczy 1 blok pod celownikiem");
        Input idle = new Input();
        for (int i = 0; i < 120; i++) ig.update(idle, 0.05);
        check(countBlocks(ig.world()) == before - 1, "Ingame: bez trzymania przycisku nic nie znika");

        Ingame ig2 = new Ingame(11L);
        int before2 = countBlocks(ig2.world());
        Input in = new Input();
        for (int i = 0; i < 300; i++) {
            in.forward = true;
            in.mouseDx = 0.4;
            in.mouseDy = 0.1;
            if (i % 60 == 0) in.hotbar = (i / 60) % 9 + 1;
            if (i % 40 == 0) in.jump = true;
            if (i % 40 == 39) in.jump = false;
            ig2.update(in, 0.05);
        }
        check(ig2.world() != null, "Ingame: symulacja 15s biegu przeszla bez wyjatku");
        check(countBlocks(ig2.world()) == before2, "Ingame: ruch gracza nie zmienia swiata");

        if (FAILS.isEmpty()) {
            System.out.println("ALL GAMEPLAY TESTS PASSED (OK)");
        } else {
            System.out.println(FAILS.size() + " FAILURES");
            for (String s : FAILS) System.out.println("  FAIL " + s);
            System.exit(1);
        }
    }

    private static java.util.Set<String> blockTextures() throws Exception {
        java.util.Set<String> set = new java.util.LinkedHashSet<String>();
        craft3dmodern.model.BlockModels bm = new craft3dmodern.model.BlockModels();
        for (int id = 1; id < BlockIds.count(); id++) {
            String name = BlockIds.name(id);
            String variant = id == BlockIds.OAK_LOG ? "axis=y" : null;
            for (craft3dmodern.model.BlockModels.Face f : bm.modelFor(name, variant).faces) {
                if (f.tex != null) set.add(f.tex);
            }
        }
        return set;
    }

    private static int countBlocks(World w) {
        int n = 0;
        for (int x = 0; x < w.sx; x++) {
            for (int y = 0; y < w.sy; y++) {
                for (int z = 0; z < w.sz; z++) {
                    if (w.get(x, y, z) != BlockIds.AIR) n++;
                }
            }
        }
        return n;
    }
}
