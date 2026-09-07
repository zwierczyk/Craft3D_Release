package craft3dmodern;

import craft3dmodern.client.Game;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;

/**
 * Craft3D Modern - startowa aplikacja (OpenGL 3.3 core + shadery).
 */
public final class Main {
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

        long window = glfwCreateWindow(1280, 720, "Craft3D Modern (Minecraft 26.2 era)", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL) {
            System.err.println("[Craft3D-Modern] window creation failed");
            glfwTerminate();
            System.exit(1);
        }
        glfwMakeContextCurrent(window);
        GL.createCapabilities();
        glfwSwapInterval(1);

        System.out.println("[Craft3D-Modern] GL " + glGetString(GL_VERSION)
                + " vendor=" + glGetString(GL_VENDOR)
                + " renderer=" + glGetString(GL_RENDERER));

        Game game = new Game();
        try {
            game.init();
        } catch (Throwable t) {
            System.err.println("[Craft3D-Modern] init failed: " + t);
            t.printStackTrace();
            game.dispose();
            glfwDestroyWindow(window);
            glfwTerminate();
            System.exit(1);
        }

        long last = System.nanoTime();
        boolean escWasDown = false;
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            long now = System.nanoTime();
            double dt = (now - last) / 1_000_000_000.0;
            last = now;
            if (dt > 0.05) dt = 0.05;

            int[] fw = new int[1], fh = new int[1];
            glfwGetFramebufferSize(window, fw, fh);
            int fbw = Math.max(1, fw[0]);
            int fbh = Math.max(1, fh[0]);
            org.lwjgl.opengl.GL11.glViewport(0, 0, fbw, fbh);

            double[] mx = new double[1], my = new double[1];
            glfwGetCursorPos(window, mx, my);
            boolean left = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
            boolean esc = glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS;

            glClearColor(0.106f, 0.106f, 0.118f, 1f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            boolean quit = game.render(dt, fbw, fbh, mx[0], my[0], left, esc);
            if (quit) glfwSetWindowShouldClose(window, true);

            glfwSwapBuffers(window);
        }

        game.dispose();
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
