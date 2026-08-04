package craft3dgl.blaze3d.shaders;

/**
 * MC 1.14.4 Effect interface 1:1.
 * Reprezentuje shader program (vertex + fragment).
 * Konkretna implementacja to np. EffectInstance (dodamy w Etapie 4).
 */
public interface Effect {
    int getId();
    void markDirty();
    Program getVertexProgram();
    Program getFragmentProgram();
}
