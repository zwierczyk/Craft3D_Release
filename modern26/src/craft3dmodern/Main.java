package craft3dmodern;

import craft3dmodern.client.Game;
import craft3dmodern.render.RenderSystem;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * Craft3D Modern - startowa aplikacja.
 *
 * Nowoczesny silnik wzorowany na Minecraft Java Edition 26.2
 * (rendering GL 3.3 core-profile + shadery, assety vanilla 26.2).
 */
public final class Main {
    public static final int WIN_W = 1280;
    public static final int WIN_H = 720;

    private Main() {}

    public static void main(String[] args) {
        // Assety 26.2: rozpakuj jesli potrzeba (dziala tez z IntelliJ, bez build.bat).
        try {
            craft3dmodern.test.AssetsExtractor.main(new String[0]);
        } catch (Throwable t) {
            System.err.println("[Craft3D-Modern] asset extraction warning: " + t.getMessage());
        }

        if (!glfwInit()) {
            System.err.println("[Craft3D-Modern] GLFW init failed");
            System.exit(1);
        }
        GLFWErrorCallback.createPrint(System.err).set();

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // visible po utworzeniu kontekstu

        long window = glfwCreateWindow(WIN_W, WIN_H, "Craft3D Modern", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            System.err.println("[Craft3D-Modern] window creation failed");
            glfwTerminate();
            System.exit(1);
        }
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwShowWindow(window);
        glfwSwapInterval(1);

        System.out.println("[Craft3D-Modern] GL " + glGetString_glVersion()
                + " vendor=" + glGetString_glVendor()
                + " renderer=" + glGetString_glRenderer());

        Game game = new Game();
        game.init();

        long last = System.nanoTime();
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            long now = System.nanoTime();
            double dt = (now - last) / 1_000_000_000.0;
            last = now;
            if (dt > 0.05) dt = 0.05;

            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }

            glClearColor(0.16f, 0.19f, 0.23f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            RenderSystem.viewport(0, 0, WIN_W, WIN_H);
            game.render(dt);

            glfwSwapBuffers(window);
        }

        game.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private static String glGetString_glVersion() {
        return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION);
    }

    private static String glGetString_glVendor() {
        return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR);
    }

    private static String glGetString_glRenderer() {
        return org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER);
    }
}
