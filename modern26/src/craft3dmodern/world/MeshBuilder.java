package craft3dmodern.world;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import craft3dmodern.model.BlockModels;
import craft3dmodern.render.TextureAtlas;


public final class MeshBuilder {
    public static final int FLOATS_PER_VERTEX = 8;
    private static final float[] SHADE = {0.50f, 1.00f, 0.80f, 0.80f, 0.60f, 0.60f};
    private static final int[] DX = {0, 0, 0, 0, -1, 1};
    private static final int[] DY = {-1, 1, 0, 0, 0, 0};
    private static final int[] DZ = {0, 0, -1, 1, 0, 0};

    private MeshBuilder() {}

    public static float[] build(World w, BlockModels bm, TextureAtlas atlas) throws IOException {
        Map<Integer, BlockModels.Model> models = new HashMap<Integer, BlockModels.Model>();
        for (int id = 1; id < BlockIds.count(); id++) {
            String name = BlockIds.name(id);
            String variant = id == BlockIds.OAK_LOG ? "axis=y" : null;
            models.put(id, bm.modelFor(name, variant));
        }

        
        long faceCount = 0;
        for (int y = 0; y < w.sy; y++) {
            for (int z = 0; z < w.sz; z++) {
                for (int x = 0; x < w.sx; x++) {
                    int id = w.get(x, y, z);
                    if (id == BlockIds.AIR) continue;
                    BlockModels.Model m = models.get(id);
                    if (m == null) continue;
                    for (BlockModels.Face f : m.faces) {
                        int cd = f.cullDir >= 0 ? f.cullDir : f.dir;
                        int nb = w.get(x + DX[cd], y + DY[cd], z + DZ[cd]);
                        if (!BlockIds.occludes(nb)) faceCount++;
                    }
                }
            }
        }

        if (faceCount == 0) return new float[0];
        float[] out = new float[(int) (faceCount * 6 * FLOATS_PER_VERTEX)];
        int p = 0;

        
        for (int y = 0; y < w.sy; y++) {
            for (int z = 0; z < w.sz; z++) {
                for (int x = 0; x < w.sx; x++) {
                    int id = w.get(x, y, z);
                    if (id == BlockIds.AIR) continue;
                    BlockModels.Model m = models.get(id);
                    if (m == null) continue;
                    int tintRgb = BlockIds.tintRgb(id);
                    for (BlockModels.Face f : m.faces) {
                        int cd = f.cullDir >= 0 ? f.cullDir : f.dir;
                        int nb = w.get(x + DX[cd], y + DY[cd], z + DZ[cd]);
                        if (BlockIds.occludes(nb)) continue;
                        TextureAtlas.Entry e = f.tex == null ? null : atlas.entry(f.tex);
                        if (e == null) continue;
                        float shade = SHADE[f.dir];
                        boolean tinted = f.tintIndex >= 0 && e.gray;
                        float tr = (tinted ? ((tintRgb >>> 16) & 255) / 255f : 1f);
                        float tg = (tinted ? ((tintRgb >>> 8) & 255) / 255f : 1f);
                        float tb = (tinted ? (tintRgb & 255) / 255f : 1f);
                        float r = tr * shade, g = tg * shade, b = tb * shade;
                        float[] xs = new float[4], ys = new float[4], zs = new float[4];
                        float[] us = new float[4], vs = new float[4];
                        for (int i = 0; i < 4; i++) {
                            xs[i] = x + f.x[i] / 16f;
                            ys[i] = y + f.y[i] / 16f;
                            zs[i] = z + f.z[i] / 16f;
                            float lu = f.u[i] / 16f * e.tileW;
                            float lv = f.v[i] / 16f * e.tileW;
                            us[i] = (e.x + lu) / TextureAtlas.PAGE;
                            vs[i] = (e.y + lv) / TextureAtlas.PAGE;
                        }
                        
                        int[] order = {0, 1, 2, 0, 2, 3};
                        for (int oi = 0; oi < order.length; oi++) {
                            int vi = order[oi];
                            out[p++] = xs[vi];
                            out[p++] = ys[vi];
                            out[p++] = zs[vi];
                            out[p++] = us[vi];
                            out[p++] = vs[vi];
                            out[p++] = r;
                            out[p++] = g;
                            out[p++] = b;
                        }
                    }
                }
            }
        }
        return out;
    }
}
