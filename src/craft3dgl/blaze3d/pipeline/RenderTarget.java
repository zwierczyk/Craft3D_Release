package craft3dgl.blaze3d.pipeline;

import craft3dgl.blaze3d.platform.GLX;
import craft3dgl.blaze3d.platform.GlStateManager;
import craft3dgl.blaze3d.platform.TextureUtil;
import craft3dgl.blaze3d.vertex.BufferBuilder;
import craft3dgl.blaze3d.vertex.DefaultVertexFormat;
import craft3dgl.blaze3d.vertex.Tesselator;

/**
 * MC 1.14.4 RenderTarget port 1:1.
 *
 * Framebuffer Object wrapper. Rysuj do RenderTarget zamiast bezposrednio do ekranu,
 * potem RenderTarget.blitToScreen() aby wyswietlic (opcjonalnie przez shader postprocess).
 *
 * Uzycie:
 *   RenderTarget rt = new RenderTarget(width, height, useDepth, isMacOS);
 *   rt.setClearColor(0.5, 0.7, 1.0, 1.0);
 *   rt.bindWrite(true);      // set framebuffer + viewport
 *   ... rysowanie sceny 3D ...
 *   rt.unbindWrite();
 *   rt.blitToScreen(screenW, screenH);   // rysuj FBO na ekran
 */
public class RenderTarget {
    public int width;
    public int height;
    public int viewWidth;
    public int viewHeight;
    public final boolean useDepth;
    public int frameBufferId;
    public int colorTextureId;
    public int depthBufferId;
    public final float[] clearChannels;
    public int filterMode;

    public RenderTarget(int i, int j, boolean useDepth, boolean errorCheck) {
        this.useDepth = useDepth;
        this.frameBufferId = -1;
        this.colorTextureId = -1;
        this.depthBufferId = -1;
        this.clearChannels = new float[]{1.0F, 1.0F, 1.0F, 0.0F};
        this.resize(i, j, errorCheck);
    }

    public void resize(int i, int j, boolean errorCheck) {
        if (!GLX.isUsingFBOs()) {
            this.viewWidth = i;
            this.viewHeight = j;
        } else {
            GlStateManager.enableDepthTest();
            if (this.frameBufferId >= 0) {
                this.destroyBuffers();
            }
            this.createBuffers(i, j, errorCheck);
            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, 0);
        }
    }

    public void destroyBuffers() {
        if (GLX.isUsingFBOs()) {
            this.unbindRead();
            this.unbindWrite();
            if (this.depthBufferId > -1) {
                GLX.glDeleteRenderbuffers(this.depthBufferId);
                this.depthBufferId = -1;
            }
            if (this.colorTextureId > -1) {
                TextureUtil.releaseTextureId(this.colorTextureId);
                this.colorTextureId = -1;
            }
            if (this.frameBufferId > -1) {
                GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, 0);
                GLX.glDeleteFramebuffers(this.frameBufferId);
                this.frameBufferId = -1;
            }
        }
    }

    public void createBuffers(int i, int j, boolean errorCheck) {
        this.viewWidth = i;
        this.viewHeight = j;
        this.width = i;
        this.height = j;
        if (!GLX.isUsingFBOs()) {
            this.clear(errorCheck);
        } else {
            this.frameBufferId = GLX.glGenFramebuffers();
            this.colorTextureId = TextureUtil.generateTextureId();
            if (this.useDepth) {
                this.depthBufferId = GLX.glGenRenderbuffers();
            }
            this.setFilterMode(9728); // GL_NEAREST
            GlStateManager.bindTexture(this.colorTextureId);
            GlStateManager.texImage2D(3553, 0, 32856, this.width, this.height, 0, 6408, 5121, null);
            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, this.frameBufferId);
            GLX.glFramebufferTexture2D(GLX.GL_FRAMEBUFFER, GLX.GL_COLOR_ATTACHMENT0, 3553, this.colorTextureId, 0);
            if (this.useDepth) {
                GLX.glBindRenderbuffer(GLX.GL_RENDERBUFFER, this.depthBufferId);
                GLX.glRenderbufferStorage(GLX.GL_RENDERBUFFER, 33190, this.width, this.height); // GL_DEPTH_COMPONENT24
                GLX.glFramebufferRenderbuffer(GLX.GL_FRAMEBUFFER, GLX.GL_DEPTH_ATTACHMENT, GLX.GL_RENDERBUFFER, this.depthBufferId);
            }
            this.checkStatus();
            this.clear(errorCheck);
            this.unbindRead();
        }
    }

    public void setFilterMode(int i) {
        if (GLX.isUsingFBOs()) {
            this.filterMode = i;
            GlStateManager.bindTexture(this.colorTextureId);
            GlStateManager.texParameter(3553, 10241, i); // GL_TEXTURE_MIN_FILTER
            GlStateManager.texParameter(3553, 10240, i); // GL_TEXTURE_MAG_FILTER
            GlStateManager.texParameter(3553, 10242, 10496); // GL_TEXTURE_WRAP_S = GL_CLAMP
            GlStateManager.texParameter(3553, 10243, 10496); // GL_TEXTURE_WRAP_T = GL_CLAMP
            GlStateManager.bindTexture(0);
        }
    }

    public void checkStatus() {
        int i = GLX.glCheckFramebufferStatus(GLX.GL_FRAMEBUFFER);
        if (i != GLX.GL_FRAMEBUFFER_COMPLETE) {
            if (i == GLX.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT");
            } else if (i == GLX.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT");
            } else if (i == GLX.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER");
            } else if (i == GLX.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER) {
                throw new RuntimeException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER");
            } else {
                throw new RuntimeException("glCheckFramebufferStatus returned unknown status: " + i);
            }
        }
    }

    public void bindRead() {
        if (GLX.isUsingFBOs()) {
            GlStateManager.bindTexture(this.colorTextureId);
        }
    }

    public void unbindRead() {
        if (GLX.isUsingFBOs()) {
            GlStateManager.bindTexture(0);
        }
    }

    public void bindWrite(boolean setViewport) {
        if (GLX.isUsingFBOs()) {
            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, this.frameBufferId);
            if (setViewport) {
                GlStateManager.viewport(0, 0, this.viewWidth, this.viewHeight);
            }
        }
    }

    public void unbindWrite() {
        if (GLX.isUsingFBOs()) {
            GLX.glBindFramebuffer(GLX.GL_FRAMEBUFFER, 0);
        }
    }

    public void setClearColor(float r, float g, float b, float a) {
        this.clearChannels[0] = r;
        this.clearChannels[1] = g;
        this.clearChannels[2] = b;
        this.clearChannels[3] = a;
    }

    public void blitToScreen(int i, int j) {
        this.blitToScreen(i, j, true);
    }

    public void blitToScreen(int i, int j, boolean disableBlend) {
        if (GLX.isUsingFBOs()) {
            GlStateManager.colorMask(true, true, true, false);
            GlStateManager.disableDepthTest();
            GlStateManager.depthMask(false);
            GlStateManager.matrixMode(5889); // GL_PROJECTION
            GlStateManager.loadIdentity();
            GlStateManager.ortho(0.0, i, j, 0.0, 1000.0, 3000.0);
            GlStateManager.matrixMode(5888); // GL_MODELVIEW
            GlStateManager.loadIdentity();
            GlStateManager.translatef(0f, 0f, -2000f);
            GlStateManager.viewport(0, 0, i, j);
            GlStateManager.enableTexture();
            GlStateManager.disableLighting();
            GlStateManager.disableAlphaTest();
            if (disableBlend) {
                GlStateManager.disableBlend();
                GlStateManager.enableColorMaterial();
            }
            GlStateManager.color4f(1f, 1f, 1f, 1f);
            this.bindRead();
            float f = i;
            float g = j;
            float h = (float)this.viewWidth / this.width;
            float k = (float)this.viewHeight / this.height;
            Tesselator tess = Tesselator.getInstance();
            BufferBuilder bb = tess.getBuilder();
            bb.begin(7, DefaultVertexFormat.POSITION_TEX_COLOR); // GL_QUADS
            bb.vertex(0.0, g, 0.0).uv(0.0, 0.0).color(255, 255, 255, 255).endVertex();
            bb.vertex(f, g, 0.0).uv(h, 0.0).color(255, 255, 255, 255).endVertex();
            bb.vertex(f, 0.0, 0.0).uv(h, k).color(255, 255, 255, 255).endVertex();
            bb.vertex(0.0, 0.0, 0.0).uv(0.0, k).color(255, 255, 255, 255).endVertex();
            tess.end();
            this.unbindRead();
            GlStateManager.depthMask(true);
            GlStateManager.colorMask(true, true, true, true);
        }
    }

    public void clear(boolean errorCheck) {
        this.bindWrite(true);
        GlStateManager.clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
        int mask = 16384; // GL_COLOR_BUFFER_BIT
        if (this.useDepth) {
            GlStateManager.clearDepth(1.0);
            mask |= 256; // GL_DEPTH_BUFFER_BIT
        }
        GlStateManager.clear(mask, errorCheck);
        this.unbindWrite();
    }
}
