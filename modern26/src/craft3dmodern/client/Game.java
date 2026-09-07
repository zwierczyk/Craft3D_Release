package craft3dmodern.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import craft3dmodern.client.font.FontRenderer;
import craft3dmodern.client.ui.Button;
import craft3dmodern.render.GuiBlit;
import craft3dmodern.render.Texture;

/**
 * Gra: ekrany GUI wzorowane na 26.2 (TitleScreen / SelectWorld / Options)
 * oraz widok w swiecie (M3): generowanie swiata, latanie, pauza.
 * Wszystkie wspolrzedne GUI w pikselach okna; przyciski 400x40 (200x20 w gui scale 2).
 */
public final class Game {
    public enum Screen { TITLE, WORLD, OPTIONS, INGAME }

    private GuiBlit gui;
    private FontRenderer font;
    private int logoTexture;
    private int logoW, logoH;
    private int crosshairTexture = -1;
    private float crosshairPx = 15;

    private Screen screen = Screen.TITLE;
    private boolean paused = false;         // pauza w INGAME
    private boolean optionsFromPause = false;

    private final List<String> splashes = new ArrayList<String>();
    private String splash = "";
    private float splashTimer = 0;
    private final Random random = new Random();
    private boolean quitRequested = false;

    // stany poprzedniej klatki (do krawedzi)
    private boolean mouseWasDown = false;
    private boolean escWasDown = false;
    private boolean regenWasDown = false;

    private Ingame ingame;

    public void init() throws IOException {
        I18n.init();
        gui = new GuiBlit();
        font = FontRenderer.load();
        font.upload();

        BufferedImage logo = Texture.decode("minecraft/textures/gui/title/minecraft.png");
        logoW = logo.getWidth();
        logoH = logo.getHeight();
        logoTexture = Texture.upload(logo, false);

        try {
            BufferedImage ch = Texture.decode("minecraft/textures/gui/sprites/hud/crosshair.png");
            crosshairTexture = Texture.upload(ch, false);
            crosshairPx = ch.getWidth();
        } catch (IOException e) {
            crosshairTexture = -1;
        }

        File splashesFile = new File(Texture.assetRoot(), "minecraft/texts/splashes.txt");
        if (splashesFile.isFile()) {
            for (String line : Files.readAllLines(splashesFile.toPath(), StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) splashes.add(line);
            }
        }
        pickSplash();
    }

    private void pickSplash() {
        if (!splashes.isEmpty()) splash = splashes.get(random.nextInt(splashes.size()));
        else splash = "";
    }

    public boolean wantsCursorCaptured() {
        return screen == Screen.INGAME && !paused;
    }

    private void enterGame() {
        try {
            if (ingame != null) { ingame.disposeGL(); ingame = null; }
            ingame = new Ingame(random.nextLong() & Long.MAX_VALUE);
            ingame.glInit();
            paused = false;
            screen = Screen.INGAME;
        } catch (Throwable t) {
            System.err.println("[Game] world init failed: " + t);
            t.printStackTrace();
            if (ingame != null) { ingame.disposeGL(); ingame = null; }
            screen = Screen.TITLE;
        }
    }

    private void leaveGame() {
        if (ingame != null) { ingame.disposeGL(); ingame = null; }
        paused = false;
        screen = Screen.TITLE;
    }

    /** Render klatki; zwraca true, gdy gra ma sie zamknac. */
    public boolean render(double dt, int fbw, int fbh, Input in) {
        gui.setScreenSize(fbw, fbh);

        splashTimer -= (float) dt;
        if (splashTimer <= 0) {
            pickSplash();
            splashTimer = 4.5f + random.nextFloat() * 4f;
        }

        boolean clicked = in.leftDown && !mouseWasDown;
        boolean esc = in.escDown && !escWasDown;
        boolean regen = in.regen && !regenWasDown;
        mouseWasDown = in.leftDown;
        escWasDown = in.escDown;
        regenWasDown = in.regen;

        // -- zmiany ekranu
        if (screen == Screen.INGAME) {
            if (esc) paused = !paused;
            if (!paused && regen) {
                try {
                    ingame.regenerate(random.nextLong() & Long.MAX_VALUE);
                } catch (Throwable t) {
                    System.err.println("[Game] regenerate failed: " + t);
                }
            }
            if (paused) {
                // pauza: nie ruszaj kamera
                in.mouseDx = 0;
                in.mouseDy = 0;
            } else {
                ingame.update(in, dt);
            }
        } else {
            if (esc) {
                if (screen == Screen.WORLD || screen == Screen.OPTIONS) {
                    screen = optionsFromPause ? Screen.INGAME : Screen.TITLE;
                    optionsFromPause = false;
                }
            }
        }

        if (screen == Screen.INGAME) {
            renderIngame(fbw, fbh, in, clicked);
        } else if (screen == Screen.TITLE) {
            renderTitle(fbw, fbh, in, clicked);
        } else if (screen == Screen.WORLD) {
            renderWorld(fbw, fbh, in, clicked);
        } else if (screen == Screen.OPTIONS) {
            renderOptions(fbw, fbh, in, clicked);
        }

        String version = "Minecraft 26.2  |  Craft3D Modern";
        gui.begin();
        font.draw(gui, version, 8, fbh - 20, 1, 0xFFFFFFFF, true);
        return quitRequested;
    }

    // ------------------------------------------------------------------
    // INGAME
    // ------------------------------------------------------------------
    private void renderIngame(int fbw, int fbh, Input in, boolean clicked) {
        float aspect = fbw / (float) fbh;
        if (ingame != null) ingame.draw(aspect);

        gui.begin();
        if (!paused) {
            // celownik z realnych assetow 26.2
            if (crosshairTexture > 0) {
                float cx = fbw / 2f - crosshairPx / 2f;
                float cy = fbh / 2f - crosshairPx / 2f;
                gui.draw(crosshairTexture, cx, cy, crosshairPx, crosshairPx, 0, 0, 1, 1);
            }
            // debug/help (jak F3, ale uproszczone)
            String[] lines = {
                "seed " + ingame.seed,
                ingame.positionText(),
                "faces " + ingame.faceCount() + " | verts " + ingame.meshVertices(),
                "WASD - ruch | Mysz - patrz | Spacja - gora | C - dol | Shift - sprint | R - nowy swiat | ESC - pauza",
            };
            int y = 12;
            for (String line : lines) {
                font.draw(gui, line, 8, y, 1, 0xFFFFFFFF, true);
                y += 14;
            }
        } else {
            renderPause(fbw, fbh, in, clicked);
        }
    }

    private void renderPause(int fbw, int fbh, Input in, boolean clicked) {
        gui.rect(0, 0, fbw, fbh, 0f, 0f, 0f, 0.55f);
        float cx = fbw / 2f;
        String title = I18n.get("menu.paused");
        font.draw(gui, title, cx - font.width(title, 2) / 2, 60, 2, 0xFFFFFFFF, true);

        float bw = 400, bh = 40;
        float bx = cx - bw / 2;
        List<Button> buttons = new ArrayList<Button>();
        Button cont = new Button(bx, 170, bw, bh);
        cont.label = I18n.get("menu.returnToGame");
        buttons.add(cont);
        Button opt = new Button(bx, 222, bw, bh);
        opt.label = I18n.get("menu.options");
        buttons.add(opt);
        Button quit = new Button(bx, 274, bw, bh);
        quit.label = I18n.get("menu.returnToMenu");
        buttons.add(quit);
        drawButtons(buttons, in.mouseX, in.mouseY);
        if (clicked) {
            if (cont.hover(in.mouseX, in.mouseY)) {
                paused = false;
            } else if (opt.hover(in.mouseX, in.mouseY)) {
                optionsFromPause = true;
                screen = Screen.OPTIONS;
            } else if (quit.hover(in.mouseX, in.mouseY)) {
                leaveGame();
            }
        }
    }

    // ------------------------------------------------------------------
    // TitleScreen (26.2: logo + przyciski + splash)
    // ------------------------------------------------------------------
    private void renderTitle(int fbw, int fbh, Input in, boolean clicked) {
        float cx = fbw / 2f;
        float logoWidth = Math.min(fbw * 0.55f, 620f);
        float logoHeight = logoWidth * logoH / logoW;
        float logoX = (fbw - logoWidth) / 2f;
        float logoY = 40;
        gui.begin();
        gui.draw(logoTexture, logoX, logoY, logoWidth, logoHeight, 0, 0, 1, 1);

        if (!splash.isEmpty()) {
            float sw = font.width(splash, 2);
            font.draw(gui, splash, Math.min(fbw - 8 - sw, cx + 40), logoY + logoHeight + 6,
                    2, 0xFFFF00, true);
        }

        float bw = 400, bh = 40;
        float bx = cx - bw / 2;
        List<Button> buttons = new ArrayList<Button>();
        Button sp = new Button(bx, 320, bw, bh);
        sp.label = I18n.get("menu.singleplayer");
        buttons.add(sp);
        Button mp = new Button(bx, 372, bw, bh);
        mp.label = I18n.get("menu.multiplayer");
        mp.enabled = false;
        buttons.add(mp);
        Button options = new Button(cx - 200, 484, 196, 40);
        options.label = I18n.get("menu.options");
        buttons.add(options);
        Button quit = new Button(cx + 4, 484, 196, 40);
        quit.label = I18n.get("menu.quit");
        buttons.add(quit);
        Button lang = new Button(cx - 256, 484, 52, 40);
        lang.label = I18n.isPl() ? "EN" : "PL";
        buttons.add(lang);

        drawButtons(buttons, in.mouseX, in.mouseY);
        if (clicked) {
            if (sp.hover(in.mouseX, in.mouseY)) screen = Screen.WORLD;
            else if (options.hover(in.mouseX, in.mouseY)) {
                optionsFromPause = false;
                screen = Screen.OPTIONS;
            } else if (lang.hover(in.mouseX, in.mouseY)) toggleLanguage();
            else if (quit.hover(in.mouseX, in.mouseY)) quitRequested = true;
        }
    }

    // ------------------------------------------------------------------
    // SelectWorldScreen (M3: od razu tworzy swiat i wchodzi do niego)
    // ------------------------------------------------------------------
    private void renderWorld(int fbw, int fbh, Input in, boolean clicked) {
        float cx = fbw / 2f;
        String title = I18n.get("selectWorld.title");
        gui.begin();
        font.draw(gui, title, cx - font.width(title, 2) / 2, 26, 2, 0xFFFFFFFF, true);

        // "lista" swiata: jeden slot z nowym swiatem
        float listX = cx - 220;
        float listW = 440;
        gui.rect(listX, 120, listW, 120, 0f, 0f, 0f, 0.35f);
        font.draw(gui, I18n.get("selectWorld.newWorld"), listX + 24, 140, 1.6f, 0xFFFFFFFF, true);
        font.draw(gui, "Craft3D Modern | M3: procedural, 26.2 assety", listX + 24, 178, 1, 0xFFAAAAAA, true);

        float bw = 400, bh = 40;
        float bx = cx - bw / 2;
        List<Button> buttons = new ArrayList<Button>();
        Button create = new Button(bx, 280, bw, bh);
        create.label = I18n.get("selectWorld.create");
        buttons.add(create);
        Button select = new Button(bx, 332, bw, bh);
        select.label = I18n.get("selectWorld.select");
        select.enabled = false;
        buttons.add(select);
        Button back = new Button(bx, 480, bw, bh);
        back.label = I18n.get("gui.back");
        buttons.add(back);

        drawButtons(buttons, in.mouseX, in.mouseY);
        if (clicked) {
            if (create.hover(in.mouseX, in.mouseY)) enterGame();
            else if (back.hover(in.mouseX, in.mouseY)) screen = Screen.TITLE;
        }
    }

    // ------------------------------------------------------------------
    // OptionsScreen (minimalna wersja GuiOptions 26.2)
    // ------------------------------------------------------------------
    private void renderOptions(int fbw, int fbh, Input in, boolean clicked) {
        float cx = fbw / 2f;
        String title = I18n.get("options.title");
        gui.begin();
        font.draw(gui, title, cx - font.width(title, 2) / 2, 26, 2, 0xFFFFFFFF, true);

        float bw = 400, bh = 40;
        float bx = cx - bw / 2;
        List<Button> buttons = new ArrayList<Button>();
        Button lang = new Button(bx, 160, bw, bh);
        lang.label = I18n.get("options.language") + "  " + (I18n.isPl() ? "English" : "Polski");
        buttons.add(lang);
        Button sounds = new Button(bx, 212, bw, bh);
        sounds.label = I18n.get("options.sounds");
        sounds.enabled = false;
        buttons.add(sounds);
        Button video = new Button(bx, 264, bw, bh);
        video.label = I18n.get("options.video");
        video.enabled = false;
        buttons.add(video);
        Button done = new Button(bx, 420, bw, bh);
        done.label = I18n.get("gui.done");
        buttons.add(done);

        drawButtons(buttons, in.mouseX, in.mouseY);
        if (clicked) {
            if (lang.hover(in.mouseX, in.mouseY)) toggleLanguage();
            else if (done.hover(in.mouseX, in.mouseY)) {
                if (optionsFromPause) {
                    optionsFromPause = false;
                    screen = Screen.INGAME; // z powrotem do pauzy
                } else {
                    screen = Screen.TITLE;
                }
            }
        }
    }

    private void drawButtons(List<Button> buttons, float mx, float my) {
        for (Button b : buttons) b.draw(gui, font, b.hover(mx, my));
    }

    private void toggleLanguage() {
        I18n.setLang(I18n.isPl() ? I18n.Lang.EN : I18n.Lang.PL);
    }

    public void dispose() {
        if (ingame != null) ingame.disposeGL();
        if (gui != null) gui.dispose();
    }
}
