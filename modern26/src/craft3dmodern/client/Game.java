package craft3dmodern.client;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import craft3dmodern.client.font.FontRenderer;
import craft3dmodern.client.ui.Button;
import craft3dmodern.render.GuiBlit;
import craft3dmodern.render.Texture;

public final class Game {
    public enum Screen { TITLE, WORLD, OPTIONS, INGAME }

    private static final int LINE = 24;
    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 20;

    private GuiBlit gui;
    private FontRenderer font;
    private int logoTexture;
    private int editionTexture;
    private int logoW, logoH;
    private int crosshairTexture = -1;
    private float crosshairPx = 15;

    private Screen screen = Screen.TITLE;
    private boolean paused = false;
    private boolean optionsFromPause = false;

    private final List<String> splashes = new ArrayList<String>();
    private String splash = "";
    private float splashTimer = 0;
    private final Random random = new Random();
    private boolean quitRequested = false;

    private boolean mouseWasDown = false;
    private boolean escWasDown = false;
    private boolean regenWasDown = false;

    private Ingame ingame;
    private float s = 2f;
    private int lw = 640, lh = 360;

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
            BufferedImage edition = Texture.decode("minecraft/textures/gui/title/edition.png");
            editionTexture = Texture.upload(edition, false);
        } catch (IOException e) {
            editionTexture = -1;
        }
        try {
            BufferedImage ch = Texture.decode("minecraft/textures/gui/sprites/hud/crosshair.png");
            crosshairTexture = Texture.upload(ch, false);
            crosshairPx = ch.getWidth();
        } catch (IOException e) {
            crosshairTexture = -1;
        }
        File splashesFile = new File(Texture.assetRoot(), "minecraft/texts/splashes.txt");
        if (splashesFile.isFile()) {
            for (String line : java.nio.file.Files.readAllLines(
                    splashesFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
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
            if (ingame != null) ingame.disposeGL();
            ingame = new Ingame(random.nextLong() & Long.MAX_VALUE);
            ingame.glInit();
            paused = false;
            screen = Screen.INGAME;
        } catch (Throwable t) {
            t.printStackTrace();
            if (ingame != null) ingame.disposeGL();
            ingame = null;
            screen = Screen.TITLE;
        }
    }

    private void leaveGame() {
        if (ingame != null) ingame.disposeGL();
        ingame = null;
        paused = false;
        screen = Screen.TITLE;
    }

    public boolean render(double dt, int fbw, int fbh, Input in) {
        s = guiScale(fbw, fbh);
        lw = Math.round(fbw / s);
        lh = Math.round(fbh / s);
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

        float mx = in.mouseX / s;
        float my = in.mouseY / s;

        if (screen == Screen.INGAME) {
            if (esc) paused = !paused;
            if (!paused && regen) {
                try {
                    ingame.regenerate(random.nextLong() & Long.MAX_VALUE);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            if (paused) {
                in.mouseDx = 0;
                in.mouseDy = 0;
            } else {
                ingame.update(in, dt);
            }
        } else if (esc) {
            if (screen == Screen.WORLD || screen == Screen.OPTIONS) {
                screen = optionsFromPause ? Screen.INGAME : Screen.TITLE;
                optionsFromPause = false;
            }
        }

        if (screen == Screen.INGAME) {
            renderIngame(fbw, fbh, mx, my, clicked);
        } else if (screen == Screen.TITLE) {
            renderTitle(mx, my, clicked);
        } else if (screen == Screen.WORLD) {
            renderWorld(mx, my, clicked);
        } else if (screen == Screen.OPTIONS) {
            renderOptions(mx, my, clicked);
        }
        return quitRequested;
    }

    private static int guiScale(int fbw, int fbh) {
        int scale = 1;
        while (scale < 8 && fbh / (scale + 1) >= 240 && fbw / (scale + 1) >= 320) {
            scale++;
        }
        return scale;
    }

    private void renderIngame(int fbw, int fbh, float mx, float my, boolean clicked) {
        if (ingame == null) {
            screen = Screen.TITLE;
            return;
        }
        ingame.draw(fbw / (float) fbh);
        gui.begin();
        if (paused) {
            renderPause(mx, my, clicked);
        } else if (crosshairTexture > 0) {
            gui.draw(crosshairTexture, fbw / 2f - crosshairPx / 2f, fbh / 2f - crosshairPx / 2f,
                    crosshairPx, crosshairPx, 0, 0, 1, 1);
        }
    }

    private void renderPause(float mx, float my, boolean clicked) {
        gui.rect(0, 0, lw * s, lh * s, 0f, 0f, 0f, 0.6f);
        float cx = lw / 2f;
        int top = lh / 4 + 48;
        Button resume = new Button(cx - 100, top, BUTTON_W, BUTTON_H);
        resume.label = I18n.get("menu.returnToGame");
        Button options = new Button(cx - 100, top + LINE, BUTTON_W, BUTTON_H);
        options.label = I18n.get("menu.options");
        Button quit = new Button(cx - 100, top + 2 * LINE, BUTTON_W, BUTTON_H);
        quit.label = I18n.get("menu.returnToMenu");
        List<Button> buttons = new ArrayList<Button>();
        buttons.add(resume);
        buttons.add(options);
        buttons.add(quit);
        drawButtons(buttons, mx, my);
        if (clicked) {
            if (resume.hover(mx, my)) {
                paused = false;
            } else if (options.hover(mx, my)) {
                optionsFromPause = true;
                screen = Screen.OPTIONS;
            } else if (quit.hover(mx, my)) {
                leaveGame();
            }
        }
    }

    private void renderTitle(float mx, float my, boolean clicked) {
        gui.begin();
        int logoX = lw / 2 - 128;
        int logoY = 30;
        gui.draw(logoTexture, logoX * s, logoY * s, 256 * s, 44 * s,
                0f, 0f, 256f / logoW, 44f / logoH);
        if (editionTexture > 0) {
            int edX = lw / 2 - 64;
            int edY = logoY + 44 - 7;
            gui.draw(editionTexture, edX * s, edY * s, 128 * s, 14 * s,
                    0f, 0f, 128f / 512f, 14f / 64f);
        }

        if (!splash.isEmpty()) {
            float textW = font.width(splash);
            float scale = 1.7f * 100f / (textW + 32f);
            if (scale > 1.5f) scale = 1.5f;
            float sx = (lw / 2f + 123f) * s;
            float sy = 70f * s;
            font.draw(gui, splash, sx - font.width(splash, scale * s) / 2f, sy, scale * s, 0xFFFFFF00, true);
        }

        int top = lh / 4 + 48;
        Button sp = new Button(lw / 2f - 100, top, BUTTON_W, BUTTON_H);
        sp.label = I18n.get("menu.singleplayer");
        Button mp = new Button(lw / 2f - 100, top + LINE, BUTTON_W, BUTTON_H);
        mp.label = I18n.get("menu.multiplayer");
        mp.enabled = false;
        Button opt = new Button(lw / 2f - 100, top + 2 * LINE, 98, BUTTON_H);
        opt.label = I18n.get("menu.options");
        Button quit = new Button(lw / 2f + 2, top + 2 * LINE, 98, BUTTON_H);
        quit.label = I18n.get("menu.quit");
        List<Button> buttons = new ArrayList<Button>();
        buttons.add(sp);
        buttons.add(mp);
        buttons.add(opt);
        buttons.add(quit);
        drawButtons(buttons, mx, my);
        if (clicked) {
            if (sp.hover(mx, my)) {
                screen = Screen.WORLD;
            } else if (opt.hover(mx, my)) {
                optionsFromPause = false;
                screen = Screen.OPTIONS;
            } else if (quit.hover(mx, my)) {
                quitRequested = true;
            }
        }
        drawCopyright();
    }

    private void renderWorld(float mx, float my, boolean clicked) {
        gui.begin();
        drawCenteredTitle(I18n.get("selectWorld.title"), 10);

        int searchY = 36;
        gui.rect(lw / 2f * s - 100 * s, searchY * s, 200 * s, BUTTON_H * s, 0f, 0f, 0f, 0.35f);
        font.draw(gui, I18n.get("selectWorld.search"), lw / 2f * s - font.width(I18n.get("selectWorld.search")) * s / 2f,
                (searchY + 6) * s, s, 0xFF808080, true);

        float boxTop = (searchY + BUTTON_H + 4) * s;
        float footTop = (lh - 56) * s;
        gui.rect(0, boxTop, lw * s, footTop - boxTop, 0f, 0f, 0f, 0.45f);

        float cx = lw / 2f;
        float fy = lh - 40;
        Button play = new Button(cx - 204, fy, BUTTON_W, BUTTON_H);
        play.label = I18n.get("selectWorld.select");
        play.enabled = false;
        Button create = new Button(cx + 4, fy, BUTTON_W, BUTTON_H);
        create.label = I18n.get("selectWorld.create");
        Button back = new Button(lw / 2f - 35, fy + LINE, 70, BUTTON_H);
        back.label = I18n.get("gui.back");
        List<Button> buttons = new ArrayList<Button>();
        buttons.add(play);
        buttons.add(create);
        buttons.add(back);
        drawButtons(buttons, mx, my);
        if (clicked) {
            if (create.hover(mx, my)) enterGame();
            else if (back.hover(mx, my)) screen = Screen.TITLE;
        }
        drawCopyright();
    }

    private void renderOptions(float mx, float my, boolean clicked) {
        gui.begin();
        drawCenteredTitle(I18n.get("options.title"), 10);
        float cx = lw / 2f;
        int top = lh / 4;
        Button lang = new Button(cx - 100, top, BUTTON_W, BUTTON_H);
        lang.label = I18n.get("options.language") + "  " + (I18n.isPl() ? "English" : "Polski");
        Button sounds = new Button(cx - 100, top + LINE, BUTTON_W, BUTTON_H);
        sounds.label = I18n.get("options.sounds");
        sounds.enabled = false;
        Button video = new Button(cx - 100, top + 2 * LINE, BUTTON_W, BUTTON_H);
        video.label = I18n.get("options.video");
        video.enabled = false;
        Button done = new Button(cx - 100, top + 5 * LINE, BUTTON_W, BUTTON_H);
        done.label = I18n.get("gui.done");
        List<Button> buttons = new ArrayList<Button>();
        buttons.add(lang);
        buttons.add(sounds);
        buttons.add(video);
        buttons.add(done);
        drawButtons(buttons, mx, my);
        if (clicked) {
            if (lang.hover(mx, my)) toggleLanguage();
            else if (done.hover(mx, my)) {
                if (optionsFromPause) {
                    optionsFromPause = false;
                    screen = Screen.INGAME;
                } else {
                    screen = Screen.TITLE;
                }
            }
        }
        drawCopyright();
    }

    private void drawCenteredTitle(String text, int y) {
        float scale = s;
        font.draw(gui, text, lw / 2f * s - font.width(text, scale) / 2f, y * s, scale, 0xFFFFFFFF, true);
    }

    private void drawCopyright() {
        String c = I18n.get("title.credits");
        float textW = font.width(c) * s;
        font.draw(gui, c, (lw - 2) * s - textW, (lh - 8) * s, s, 0xFFFFFFFF, true);
    }

    private void drawButtons(List<Button> buttons, float mx, float my) {
        for (Button b : buttons) b.draw(gui, font, b.hover(mx, my), s);
    }

    private void toggleLanguage() {
        I18n.setLang(I18n.isPl() ? I18n.Lang.EN : I18n.Lang.PL);
    }

    public void dispose() {
        if (ingame != null) ingame.disposeGL();
        if (gui != null) gui.dispose();
    }
}
