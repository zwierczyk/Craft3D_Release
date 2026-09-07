package craft3dmodern.client;

import java.awt.image.BufferedImage;
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

    private static final int SPACING = 24;
    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 20;

    private GuiBlit gui;
    private FontRenderer font;
    private int logoTexture = -1;
    private int editionTexture = -1;
    private float logoW = 1f;
    private float logoH = 1f;
    private int crosshairTexture = -1;
    private float crosshairPx = 15;
    private int hotbarTexture = -1;
    private int hotbarSelTexture = -1;
    private int heartContainer = -1;
    private int heartFull = -1;

    private Screen screen = Screen.TITLE;
    private boolean paused = false;
    private boolean optionsFromPause = false;
    private long lastSeed = 0;
    private boolean quitRequested = false;

    private final Random random = new Random();
    private boolean mouseWasDown = false;
    private boolean escWasDown = false;
    private boolean regenWasDown = false;

    private Ingame ingame;
    private float s = 1f;
    private int lw = 320;
    private int lh = 240;

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
        try {
            BufferedImage hb = Texture.decode("minecraft/textures/gui/sprites/hud/hotbar.png");
            hotbarTexture = Texture.upload(hb, false);
        } catch (IOException e) {
            hotbarTexture = -1;
        }
        try {
            BufferedImage sel = Texture.decode("minecraft/textures/gui/sprites/hud/hotbar_selection.png");
            hotbarSelTexture = Texture.upload(sel, false);
        } catch (IOException e) {
            hotbarSelTexture = -1;
        }
        try {
            BufferedImage hc = Texture.decode("minecraft/textures/gui/sprites/hud/heart/container.png");
            heartContainer = Texture.upload(hc, false);
        } catch (IOException e) {
            heartContainer = -1;
        }
        try {
            BufferedImage hf = Texture.decode("minecraft/textures/gui/sprites/hud/heart/full.png");
            heartFull = Texture.upload(hf, false);
        } catch (IOException e) {
            heartFull = -1;
        }
    }

    public boolean wantsCursorCaptured() {
        return screen == Screen.INGAME && !paused;
    }

    private void enterGame(long seed) {
        try {
            if (ingame != null) ingame.disposeGL();
            ingame = new Ingame(seed);
            ingame.glInit();
            lastSeed = seed;
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
        lw = Math.max(1, Math.round(fbw / s));
        lh = Math.max(1, Math.round(fbh / s));
        gui.setScreenSize(fbw, fbh);
        gui.setGuiScale(s);

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
            if (!paused && regen && ingame != null) {
                try {
                    ingame.regenerate(random.nextLong() & Long.MAX_VALUE);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            if (paused) {
                in.mouseDx = 0;
                in.mouseDy = 0;
            } else if (ingame != null) {
                ingame.update(in, dt);
            }
        } else if (esc) {
            if (screen == Screen.WORLD || screen == Screen.OPTIONS) {
                screen = optionsFromPause ? Screen.INGAME : Screen.TITLE;
                optionsFromPause = false;
            }
        }

        if (screen == Screen.INGAME) {
            if (ingame == null) {
                screen = Screen.TITLE;
            } else {
                renderIngame(fbw, fbh, mx, my, clicked);
            }
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
        int scale = Math.round(fbh / 360f);
        if (scale < 1) scale = 1;
        if (scale > 4) scale = 4;
        while (scale > 1 && fbw / scale < 320) {
            scale--;
        }
        return scale;
    }

    private void renderIngame(int fbw, int fbh, float mx, float my, boolean clicked) {
        ingame.draw(fbw / (float) fbh);
        gui.begin();
        if (paused) {
            renderPause(mx, my, clicked);
        } else {
            drawHud();
            if (crosshairTexture > 0) {
                float c = crosshairPx;
                gui.draw(crosshairTexture, lw / 2f - c / 2f, lh / 2f - c / 2f, c, c, 0, 0, 1, 1);
            }
        }
    }

    private void drawHud() {
        if (ingame == null) return;
        float left = lw / 2f - 91f;
        float top = (lh - 22);
        if (hotbarTexture > 0) {
            gui.draw(hotbarTexture, left, top, 182f, 22f, 0, 0, 1, 1);
        }
        int sel = ingame.selectedSlotIndex();
        if (hotbarSelTexture > 0) {
            gui.draw(hotbarSelTexture, left + (sel * 20f - 1f), top - 1f, 24f, 23f, 0, 0, 1, 1);
        }
        if (hotbarTexture > 0) {
            for (int k = 0; k < 9; k++) {
                float cx = left + (11f + 20f * k);
                float cy = top + 11f;
                ingame.drawIcon(ingame.hotbarSlot(k), cx * s, cy * s, 20f * s, fbwPx(), fbwPx());
            }
        }
        if (heartContainer > 0) {
            float hx0 = left;
            float hy = lh - 39f;
            float hs = 9f;
            for (int i = 0; i < 10; i++) {
                gui.draw(heartContainer, hx0 + i * 8f, hy, hs, hs, 0, 0, 1, 1);
            }
            if (heartFull > 0) {
                for (int i = 0; i < 10; i++) {
                    gui.draw(heartFull, hx0 + i * 8f, hy, hs, hs, 0, 0, 1, 1);
                }
            }
        }
    }

    private int fbwPx() {
        return (int) (lw * s);
    }

    private void renderPause(float mx, float my, boolean clicked) {
        gui.rect(0, 0, lw, lh, 0f, 0f, 0f, 0.6f);
        float cx = lw / 2f;
        int top = lh / 4 + 48;
        Button resume = new Button(cx - 100, top, BUTTON_W, BUTTON_H);
        resume.label = I18n.get("menu.returnToGame");
        Button options = new Button(cx - 100, top + SPACING, BUTTON_W, BUTTON_H);
        options.label = I18n.get("menu.options");
        Button quit = new Button(cx - 100, top + 2 * SPACING, BUTTON_W, BUTTON_H);
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
        if (logoTexture > 0) {
            gui.draw(logoTexture, logoX, logoY, 256, 44, 0, 0, 256f / logoW, 44f / logoH);
        }
        if (editionTexture > 0) {
            gui.draw(editionTexture, lw / 2 - 64, logoY + 37, 128, 14, 0, 0, 128f / 512f, 14f / 64f);
        }

        int top = lh / 4 + 48;
        Button sp = new Button(lw / 2f - 100, top, BUTTON_W, BUTTON_H);
        sp.label = I18n.get("menu.singleplayer");
        Button mp = new Button(lw / 2f - 100, top + SPACING, BUTTON_W, BUTTON_H);
        mp.label = I18n.get("menu.multiplayer");
        mp.enabled = false;
        Button opt = new Button(lw / 2f - 100, top + 2 * SPACING, 98, BUTTON_H);
        opt.label = I18n.get("menu.options");
        Button quit = new Button(lw / 2f + 2, top + 2 * SPACING, 98, BUTTON_H);
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
    }

    private void renderWorld(float mx, float my, boolean clicked) {
        gui.begin();
        drawCenteredTitle(I18n.get("selectWorld.title"), 12);

        float boxTop = 32;
        float footTop = (lh - 64);
        gui.rect(0, boxTop, lw, footTop - boxTop, 0f, 0f, 0f, 0.45f);

        float cx = lw / 2f;
        float fy = lh - 58;
        Button play = new Button(cx - 204, fy, BUTTON_W, BUTTON_H);
        play.label = I18n.get("selectWorld.select");
        play.enabled = false;
        Button create = new Button(cx + 4, fy, BUTTON_W, BUTTON_H);
        create.label = I18n.get("selectWorld.create");
        Button back = new Button(lw / 2f - 35, lh - 30, 70, BUTTON_H);
        back.label = I18n.get("gui.back");
        List<Button> buttons = new ArrayList<Button>();
        buttons.add(play);
        buttons.add(create);
        buttons.add(back);
        drawButtons(buttons, mx, my);
        if (clicked) {
            if (create.hover(mx, my)) {
                enterGame(random.nextLong() & Long.MAX_VALUE);
            } else if (back.hover(mx, my)) {
                screen = Screen.TITLE;
            }
        }
    }

    private void renderOptions(float mx, float my, boolean clicked) {
        gui.begin();
        drawCenteredTitle(I18n.get("options.title"), 12);
        float cx = lw / 2f;
        int top = lh / 4;
        Button lang = new Button(cx - 100, top, BUTTON_W, BUTTON_H);
        lang.label = I18n.get("options.language") + ": " + (I18n.isPl() ? "English" : "Polski");
        Button sounds = new Button(cx - 100, top + SPACING, BUTTON_W, BUTTON_H);
        sounds.label = I18n.get("options.sounds");
        sounds.enabled = false;
        Button video = new Button(cx - 100, top + 2 * SPACING, BUTTON_W, BUTTON_H);
        video.label = I18n.get("options.video");
        video.enabled = false;
        Button done = new Button(cx - 100, lh - 32, BUTTON_W, BUTTON_H);
        done.label = I18n.get("gui.done");
        List<Button> buttons = new ArrayList<Button>();
        buttons.add(lang);
        buttons.add(sounds);
        buttons.add(video);
        buttons.add(done);
        drawButtons(buttons, mx, my);
        if (clicked) {
            if (lang.hover(mx, my)) {
                I18n.setLang(I18n.isPl() ? I18n.Lang.EN : I18n.Lang.PL);
            } else if (done.hover(mx, my)) {
                if (optionsFromPause) {
                    optionsFromPause = false;
                    screen = Screen.INGAME;
                } else {
                    screen = Screen.TITLE;
                }
            }
        }
    }

    private void drawCenteredTitle(String text, int y) {
        font.draw(gui, text, lw / 2f - font.width(text) / 2f, y, 1f, 0xFFFFFFFF, true);
    }

    private void drawButtons(List<Button> buttons, float mx, float my) {
        for (Button b : buttons) b.draw(gui, font, b.hover(mx, my), 1f);
    }

    public void dispose() {
        if (ingame != null) ingame.disposeGL();
        if (gui != null) gui.dispose();
    }
}
