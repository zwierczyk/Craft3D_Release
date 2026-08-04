package craft3dgl.blaze3d.pipeline;

import craft3dgl.blaze3d.platform.GLX;
import craft3dgl.blaze3d.shaders.EffectInstance;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * PostProcessManager - CZYSTA WERSJA po debugu.
 *
 * Uzycie:
 *   pp.init(width, height);
 *   // per frame:
 *   pp.begin();
 *   // ... render sceny ...
 *   pp.end(width, height);
 *
 * Klawisze:
 *   F4  = toggle
 *   F6  = next shader
 */
public class PostProcessManager {
    private RenderTarget fbo;
    private EffectInstance currentShader;
    private String currentShaderName;
    private int width;
    private int height;
    private boolean initialized = false;
    private boolean fatalError = false;
    public boolean enabled = false;

    private PrintWriter logFile;

    public static final String[] AVAILABLE_SHADERS = {
        "passthrough", "invert", "grayscale", "blur"
    };
    private int shaderIndex = 0;

    private void log(String msg) {
        String line = "[PostProcess] " + msg;
        System.out.println(line);
        if (logFile != null) {
            try { logFile.println(line); logFile.flush(); } catch (Throwable ignored) {}
        }
    }

    private void logError(String msg, Throwable t) {
        String line = "[PostProcess ERROR] " + msg + ": " + t.getClass().getSimpleName() + " " + t.getMessage();
        System.err.println(line);
        if (logFile != null) {
            try { logFile.println(line); t.printStackTrace(logFile); logFile.flush(); } catch (Throwable ignored) {}
        }
    }

    private void openLog() {
        try {
            File logDir = new File("logs");
            if (!logDir.exists()) logDir.mkdirs();
            logFile = new PrintWriter(new FileWriter(new File(logDir, "postprocess.log"), false));
        } catch (Throwable ignored) {}
    }

    public boolean init(int width, int height) {
        if (initialized || fatalError) return initialized;
        openLog();
        log("init(" + width + "x" + height + ")");
        try {
            GLX.init();
            if (!GLX.hasShaders()) { log("no shader support"); fatalError = true; return false; }
        } catch (Throwable t) { logError("GLX.init failed", t); fatalError = true; return false; }

        try {
            this.width = width;
            this.height = height;
            this.fbo = new RenderTarget(width, height, true, false);
            this.fbo.setClearColor(0f, 0f, 0f, 1f);
        } catch (Throwable t) { logError("RenderTarget failed", t); fatalError = true; return false; }

        try {
            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, 0);
            this.currentShader = new EffectInstance("passthrough");
            this.currentShaderName = "passthrough";
        } catch (Throwable t) { logError("shader load failed", t); fatalError = true; return false; }

        initialized = true;
        log("init SUCCESS");
        return true;
    }

    public void resize(int width, int height) {
        if (!initialized || fatalError) return;
        if (this.width == width && this.height == height) return;
        try {
            this.width = width;
            this.height = height;
            this.fbo.resize(width, height, false);
        } catch (Throwable t) { logError("resize failed", t); }
    }

    public void setShader(String name) {
        if (!initialized || fatalError) return;
        if (this.currentShader != null && name.equals(this.currentShaderName)) return;
        try {
            if (this.currentShader != null) {
                try { this.currentShader.close(); } catch (Throwable ignored) {}
                this.currentShader = null;
            }
            this.currentShader = new EffectInstance(name);
            this.currentShaderName = name;
            log("shader=" + name);
        } catch (Throwable t) { logError("setShader failed", t); this.currentShader = null; }
    }

    public void nextShader() {
        if (!initialized || fatalError) return;
        shaderIndex = (shaderIndex + 1) % AVAILABLE_SHADERS.length;
        setShader(AVAILABLE_SHADERS[shaderIndex]);
    }

    public void toggle() {
        if (fatalError) { System.out.println("[PostProcess] fatal error - disabled"); return; }
        if (!initialized) { System.out.println("[PostProcess] not initialized"); return; }
        this.enabled = !this.enabled;
        System.out.println("[PostProcess] enabled=" + this.enabled + " shader=" + this.currentShaderName);
    }

    public boolean isEnabled() {
        return this.enabled && initialized && !fatalError && this.currentShader != null;
    }
    public boolean isReady() { return initialized && !fatalError; }
    public boolean hasFatalError() { return fatalError; }
    public String getCurrentShaderName() { return this.currentShaderName; }

    /** Przekieruj rysowanie do FBO. */
    public void begin() {
        if (!isEnabled()) return;
        try {
            GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
            this.fbo.bindWrite(true);
            GL11.glClearColor(0f, 0f, 0f, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        } catch (Throwable t) {
            logError("begin() failed - disabling postprocess", t);
            this.enabled = false;
            try { GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, 0); } catch (Throwable ignored) {}
            try { GL11.glPopAttrib(); } catch (Throwable ignored) {}
        }
    }

    private int endCallCount = 0;
    /** Aplikuj shader na FBO i wyswietl na ekranie.
     *  KLUCZOWE: uzywamy glPushAttrib(ALL) + glPopAttrib() zeby GWARANTOWAC ze
     *  ZADEN state nie wychodzi z end() - reszta gry dostaje state sprzed end(). */
    public void end(int screenWidth, int screenHeight) {
        if (!isEnabled()) return;
        try {
            // Odepnij FBO + przywroc viewport z begin()
            this.fbo.unbindWrite();
            GL11.glPopAttrib(); // z begin() - viewport
            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, 0);
            try { GL11.glDrawBuffer(GL11.GL_BACK); } catch (Throwable ignored) {}

            // ZAPISZ CALY STAN - CAŁE end() bedzie zamkniete w attrib push/pop.
            // Gwarantuje ze KAZDA zmiana state (glDisable/glEnable/glColor/glUseProgram/...)
            // zostanie przywrocona przy glPopAttrib. Reszta gry NIC nie zauwazy.
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);

            try {
                // Ortho 2D
                GL11.glMatrixMode(GL11.GL_PROJECTION);
                GL11.glPushMatrix();
                GL11.glLoadIdentity();
                GL11.glOrtho(0.0, screenWidth, screenHeight, 0.0, -1.0, 1.0);
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                GL11.glLoadIdentity();

                try {
                    GL11.glViewport(0, 0, screenWidth, screenHeight);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GL11.glDepthMask(false);
                    GL11.glDisable(GL11.GL_FOG);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glDisable(GL11.GL_CULL_FACE);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GLX.glActiveTexture(GLX.GL_TEXTURE0);
                    GL11.glColor4f(1f, 1f, 1f, 1f);

                    // Bind FBO + apply shader
                    this.currentShader.setSampler("DiffuseSampler", this.fbo);
                    this.currentShader.apply();
                    GL11.glDisable(GL11.GL_CULL_FACE); // apply() moglo wlaczyc

                    // Fullscreen quad
                    GL11.glBegin(GL11.GL_QUADS);
                    GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(0f, 0f);
                    GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f((float) screenWidth, 0f);
                    GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f((float) screenWidth, (float) screenHeight);
                    GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(0f, (float) screenHeight);
                    GL11.glEnd();

                    // Wylacz shader NATYWNIE (nie przez GLX wrapper)
                    org.lwjgl.opengl.GL20.glUseProgram(0);
                    // Odepnij teksture na TEXTURE0
                    GLX.glActiveTexture(GLX.GL_TEXTURE0);
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
                } finally {
                    // Przywroc matrix stack
                    GL11.glMatrixMode(GL11.GL_PROJECTION);
                    GL11.glPopMatrix();
                    GL11.glMatrixMode(GL11.GL_MODELVIEW);
                    GL11.glPopMatrix();
                }
            } finally {
                // KLUCZOWE: przywroc CALY state - glEnable/glDisable/glColor/glUseProgram/...
                GL11.glPopAttrib();
                // Podwojne bezpieczenstwo: wymus shader off na wszelki wypadek
                try { org.lwjgl.opengl.GL20.glUseProgram(0); } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            logError("end() failed - disabling postprocess", t);
            this.enabled = false;
            try { GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, 0); } catch (Throwable ignored) {}
            try { org.lwjgl.opengl.GL20.glUseProgram(0); } catch (Throwable ignored) {}
        }
    }

    public void cleanup() {
        try {
            if (this.currentShader != null) { this.currentShader.close(); this.currentShader = null; }
            if (this.fbo != null) { this.fbo.destroyBuffers(); this.fbo = null; }
            if (logFile != null) { logFile.close(); logFile = null; }
        } catch (Throwable ignored) {}
        initialized = false;
    }
}
