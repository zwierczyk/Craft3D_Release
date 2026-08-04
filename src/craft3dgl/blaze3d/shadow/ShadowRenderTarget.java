package craft3dgl.blaze3d.shadow;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

/**
 * Shadow map FBO - depth-only render target ktory shader moze samplowac.
 *
 * MC 1.14 nie ma shadow maps by default (Iris/Optifine dodaja), ale technika standardowa.
 * Uzycie:
 *   ShadowRenderTarget shadow = new ShadowRenderTarget(2048);
 *   shadow.bindForWriting();     // render sceny z pozycji slonca do depth
 *   // ... render...
 *   shadow.unbindWriting();
 *   // W main pass:
 *   shadow.bindForReading(GL_TEXTURE0 + 3);  // shader ma Sampler3=shadow map
 *   // ... shader sampluje shadowMap.z vs currentDepth zeby wiedziec czy w cieniu
 */
public class ShadowRenderTarget {
    public final int size;         // np. 2048x2048
    public int fboId;
    public int depthTextureId;
    private int prevViewport0, prevViewport1, prevViewport2, prevViewport3;

    public ShadowRenderTarget(int size) {
        this.size = size;
        create();
    }

    private void create() {
        // 1. Depth TEXTURE (nie renderbuffer!) - zeby shader mogl czytac
        depthTextureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT,
            size, size, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (java.nio.ByteBuffer) null);

        // Filter NEAREST (albo LINEAR dla PCF)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_CLAMP_TO_EDGE);

        // Border color = biale (poza shadow map = pelne swiatlo)
        java.nio.FloatBuffer border = org.lwjgl.BufferUtils.createFloatBuffer(4);
        border.put(1f).put(1f).put(1f).put(1f).flip();
        GL11.glTexParameterfv(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_BORDER_COLOR, border);

        // 2. FBO
        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT,
            GL11.GL_TEXTURE_2D, depthTextureId, 0);

        // Nie chcemy color buffer - tylko depth
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);

        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("[ShadowRenderTarget] FBO NOT COMPLETE status=" + status);
        } else {
            System.out.println("[ShadowRenderTarget] Created " + size + "x" + size
                + " fbo=" + fboId + " depthTex=" + depthTextureId);
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /** Bind FBO dla WRITE (depth-only pass z pozycji slonca). */
    public void bindForWriting() {
        // Zapisz viewport
        java.nio.IntBuffer vp = org.lwjgl.BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        prevViewport0 = vp.get(0); prevViewport1 = vp.get(1);
        prevViewport2 = vp.get(2); prevViewport3 = vp.get(3);

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
        GL11.glViewport(0, 0, size, size);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    /** Zwolnij FBO write, przywroc default framebuffer + viewport. */
    public void unbindWriting() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glViewport(prevViewport0, prevViewport1, prevViewport2, prevViewport3);
    }

    /** Bind depth texture na dany unit dla READ w main pass. */
    public void bindForReading(int textureUnit) {
        org.lwjgl.opengl.GL13.glActiveTexture(textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, depthTextureId);
    }

    public int getDepthTextureId() { return depthTextureId; }
    public int getSize() { return size; }

    public void cleanup() {
        if (depthTextureId > 0) {
            GL11.glDeleteTextures(depthTextureId);
            depthTextureId = 0;
        }
        if (fboId > 0) {
            GL30.glDeleteFramebuffers(fboId);
            fboId = 0;
        }
    }
}
