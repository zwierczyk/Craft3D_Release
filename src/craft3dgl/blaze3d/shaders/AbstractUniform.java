package craft3dgl.blaze3d.shaders;

import craft3dgl.blaze3d.math.Matrix4f;

/**
 * MC 1.14.4 AbstractUniform port 1:1.
 * Bazowa klasa dla Uniform - default implementation nic nie robi.
 */
public class AbstractUniform {
    public void set(float f) { }
    public void set(float f, float g) { }
    public void set(float f, float g, float h) { }
    public void set(float f, float g, float h, float i) { }
    public void setSafe(float f, float g, float h, float i) { }
    public void setSafe(int i, int j, int k, int l) { }
    public void set(float[] fs) { }
    public void set(Matrix4f matrix4f) { }
}
