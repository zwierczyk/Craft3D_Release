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
 * Gra: ekrany GUI wzorowane na 26.2 (TitleScreen / SelectWorld / Options).
 * Wszystkie wspolrzedne w pikselach okna; przyciski 400x40 (odpowiednik
 * 200x20 w gui scale 2).
 */
public final class Game {
    public enum Screen { TITLE, WORLD, OPTIONS }

    private GuiBlit gui;
    private FontRenderer font;
    private int logoTexture;
    private int logoW, logoH;

    private Screen screen = Screen.TITLE;
    private final List<String> splashes = new ArrayList<String>();
    private String splash = "";
    private float splashTimer = 0;
    private final Random random = new Random();
    private boolean quitRequested = false;

    // stan myszy/klawiszy
    private boolean mouseWasDown = false;
    private boolean escWasDown = false;

    public void init() throws IOException {
        I18n.init();
        gui = new GuiBlit();
        font = FontRenderer.load();
        font.upload();

        BufferedImage logo = Texture.decode("minecraft/textures/gui/title/minecraft.png");
        logoW = logo.getWidth();
        logoH = logo.getHeight();
        logoTexture = Texture.upload(logo, false);

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

    /** Render klatki; zwraca true, gdy gra ma sie zamknac. */
    public boolean render(double dt, int fbw, int fbh,
                          double mouseX, double mouseY,
                          boolean leftDown, boolean escDown) {
        gui.setScreenSize(fbw, fbh);
        gui.begin();

        float cx = fbw / 2f;
        splashTimer -= (float) dt;
        if (splashTimer <= 0) {
            pickSplash();
            splashTimer = 4.5f + random.nextFloat() * 4f;
        }

        boolean clicked = leftDown && !mouseWasDown;
        boolean esc = escDown && !escWasDown;
        mouseWasDown = leftDown;
        escWasDown = escDown;

        if (esc) {
            if (screen == Screen.WORLD || screen == Screen.OPTIONS) screen = Screen.TITLE;
        }

        if (screen == Screen.TITLE) {
            drawTitle(cx, fbw, fbh, (float) mouseX, (float) mouseY, clicked);
        } else if (screen == Screen.WORLD) {
            drawWorld(cx, fbw, fbh, (float) mouseX, (float) mouseY, clicked);
        } else {
            drawOptions(cx, fbw, fbh, (float) mouseX, (float) mouseY, clicked);
        }

        // wersja w lewym dolnym rogu
        String version = "Minecraft 26.2  |  Craft3D Modern";
        font.draw(gui, version, 8, fbh - 20, 1, 0xFFFFFFFF, true);
        return quitRequested;
    }

    // ------------------------------------------------------------------
    // TitleScreen (26.2: logo + przyciski + splash)
    // ------------------------------------------------------------------
    private void drawTitle(float cx, int fbw, int fbh, float mx, float my, boolean clicked) {
        float logoWidth = Math.min(fbw * 0.55f, 620f);
        float logoHeight = logoWidth * logoH / logoW;
        float logoX = (fbw - logoWidth) / 2f;
        float logoY = 40;
        gui.draw(logoTexture, logoX, logoY, logoWidth, logoHeight, 0, 0, 1, 1);

        // splash (zolty, bez rotacji na M2)
        if (!splash.isEmpty()) {
            float sw = font.width(splash, 2);
            font.draw(gui, splash, Math.min(fbw - 8 - sw, cx + 40), logoY + logoHeight + 6,
                    2, 0xFFFF00, true);
        }

        float bw = 400, bh = 40;
        float bx = cx - bw / 2;
        List<Button> buttons = new ArrayList<Button>();

        Button sp = new Button(bx, 300, bw, bh);
        sp.label = I18n.get("menu.singleplayer");
        buttons.add(sp);

        Button mp = new Button(bx, 352, bw, bh);
        mp.label = I18n.get("menu.multiplayer");
        mp.enabled = false;
        buttons.add(mp);

        Button options = new Button(cx - 200, 444, 196, 40);
        options.label = I18n.get("menu.options");
        buttons.add(options);
        Button quit = new Button(cx + 4, 444, 196, 40);
        quit.label = I18n.get("menu.quit");
        buttons.add(quit);
        Button lang = new Button(cx - 256, 444, 52, 40);
        lang.label = I18n.isPl() ? "EN" : "PL";
        buttons.add(lang);

        drawButtons(buttons, mx, my);
        if (clicked) {
            if (sp.hover(mx, my)) screen = Screen.WORLD;
            else if (options.hover(mx, my)) screen = Screen.OPTIONS;
            else if (lang.hover(mx, my)) toggleLanguage();
            else if (quit.hover(mx, my)) quitRequested = true;
        }
    }

    // ------------------------------------------------------------------
    // SelectWorldScreen (brak swiata; create w M3)
    // ------------------------------------------------------------------
    private void drawWorld(float cx, int fbw, int fbh, float mx, float my, boolean clicked) {
        String title = I18n.get("selectWorld.title");
        font.draw(gui, title, cx - font.width(title, 2) / 2, 26, 2, 0xFFFFFFFF, true);

        String empty = I18n.get("selectWorld.empty");
        font.draw(gui, empty, cx - font.width(empty, 2) / 2, 220, 2, 0xFFFFFFFF, true);

        float bw = 400, bh = 40;
        float bx = cx - bw / 2;
        List<Button> buttons = new ArrayList<Button>();
        Button select = new Button(bx, 360, bw, bh);
        select.label = I18n.get("selectWorld.select");
        select.enabled = false;
        buttons.add(select);
        Button create = new Button(bx, 412, bw, bh);
        create.label = I18n.get("selectWorld.create");
        create.enabled = false;
        buttons.add(create);
        Button back = new Button(bx, 520, bw, bh);
        back.label = I18n.get("gui.back");
        buttons.add(back);

        drawButtons(buttons, mx, my);
        if (clicked && back.hover(mx, my)) screen = Screen.TITLE;
    }

    // ------------------------------------------------------------------
    // OptionsScreen (minimalna wersja GuiOptions 26.2)
    // ------------------------------------------------------------------
    private void drawOptions(float cx, int fbw, int fbh, float mx, float my, boolean clicked) {
        String title = I18n.get("options.title");
        font.draw(gui, title, cx - font.width(title, 2) / 2, 26, 2, 0xFFFFFFFF, true);

        float bw = 400, bh = 40;
        float bx = cx - bw / 2;
        List<Button> buttons = new ArrayList<Button>();
        Button lang = new Button(bx, 160, bw, bh);
        lang.label = I18n.get("options.language") + "  "
                + (I18n.isPl() ? "English" : "Polski");
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

        drawButtons(buttons, mx, my);
        if (clicked) {
            if (lang.hover(mx, my)) toggleLanguage();
            else if (done.hover(mx, my)) screen = Screen.TITLE;
        }
    }

    private void drawButtons(List<Button> buttons, float mx, float my) {
        for (Button b : buttons) b.draw(gui, font, b.hover(mx, my));
    }

    private void toggleLanguage() {
        I18n.setLang(I18n.isPl() ? I18n.Lang.EN : I18n.Lang.PL);
    }

    public void dispose() {
        if (gui != null) gui.dispose();
    }
}
