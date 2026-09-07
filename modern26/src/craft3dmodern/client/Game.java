package craft3dmodern.client;

import java.io.IOException;

import craft3dmodern.render.GuiBlit;
import craft3dmodern.render.Texture;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Proba koncepcyjna ekranu tytulowego (pierwowzor: TitleScreen 26.2):
 * ciemne tlo, logo, przyciski z widgets 26.2. Bez fontu - dopiero M2.
 */
public final class Game {
    private GuiBlit gui;
    private int texLogo = 0;
    private int texButton = 0;
    private int texButtonHover = 0;

    private long startNanos = System.nanoTime();
    private int frames = 0;
    private long fpsTimer = System.currentTimeMillis();

    public void init() {
        gui = new GuiBlit();
        gui.setScreenSize(1280, 720);
        try {
            texLogo = Texture.load("minecraft/textures/gui/title/minecraft.png", false);
            texButton = Texture.load("minecraft/textures/gui/sprites/widget/button.png", false);
            texButtonHover = Texture.load("minecraft/textures/gui/sprites/widget/button_highlighted.png", false);
            System.out.println("[Game] vanilla 26.2 textures loaded: logo=" + texLogo
                    + " button=" + texButton + " hover=" + texButtonHover);
        } catch (IOException e) {
            System.err.println("[Game] texture load failed: " + e.getMessage());
        }
    }

    public void render(double dt) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        gui.begin();

        // Tlo daje glClearColor w Main (ciemny granat, jak overlay menu 26.2).

        // Logo Minecraft 26.2 (1024x256): wycentrowane w gornej czesci.
        float logoW = 512f;
        float logoH = logoW * 256f / 1024f;
        gui.draw(texLogo, (1280 - logoW) / 2f, 60, logoW, logoH, 0, 0, 1, 1);

        // Dwa przyciski (widgets 26.2) - Singleplayer / Multiplayer pozycje pogladowe.
        float bw = 400f, bh = 40f;
        float bx = (1280 - bw) / 2f;
        gui.draw(texButtonHover, bx, 300, bw, bh, 0, 0, 1, 1);
        gui.draw(texButton, bx, 352, bw, bh, 0, 0, 1, 1);
        gui.draw(texButton, bx, 404, bw, bh, 0, 0, 1, 1);

        frames++;
        long now = System.currentTimeMillis();
        if (now - fpsTimer >= 1000) {
            System.out.println("[Game] fps=" + frames
                    + " uptime=" + (System.nanoTime() - startNanos) / 1_000_000_000.0 + "s");
            frames = 0;
            fpsTimer = now;
        }
    }

    public void dispose() {
        if (gui != null) gui.dispose();
    }
}
