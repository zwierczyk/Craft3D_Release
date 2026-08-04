package craft3dgl.blaze3d.renderer;

import java.util.LinkedHashMap;
import java.util.Map;
import craft3dgl.blaze3d.vertex.BufferBuilder;

/**
 * MC 1.14.4-style MultiBufferSource - zwraca BufferBuilder dla danego RenderType.
 * Renderer entities uzywa tego zamiast bezposrednio Tesselatora.
 */
public interface MultiBufferSource {
    BufferBuilder getBuffer(RenderType renderType);

    static BufferSourceImpl immediate(BufferBuilder fallback) {
        return new BufferSourceImpl(fallback);
    }

    /**
     * Prosty impl - jeden BufferBuilder na kazdy RenderType (leniwie tworzony).
     * end() konczy wszystkie w kolejnosci "solid -> cutout -> translucent -> gui".
     */
    class BufferSourceImpl implements MultiBufferSource {
        private final BufferBuilder fallback;
        private final Map<RenderType, BufferBuilder> fixedBuffers = new LinkedHashMap<>();
        private RenderType currentType = null;

        public BufferSourceImpl(BufferBuilder fallback) {
            this.fallback = fallback;
        }

        public void addFixedBuffer(RenderType type, BufferBuilder buffer) {
            fixedBuffers.put(type, buffer);
        }

        @Override
        public BufferBuilder getBuffer(RenderType type) {
            BufferBuilder buffer = fixedBuffers.get(type);
            if (buffer == null) buffer = fallback;

            if (currentType != null && !currentType.equals(type)) {
                // uploadujemy poprzedni buforek jesli mial inne parametry
                if (!fixedBuffers.containsKey(currentType)) {
                    endBatch(currentType);
                }
            }
            if (!buffer.isBuilding()) {
                buffer.begin(type.mode(), type.format());
            }
            currentType = type;
            return buffer;
        }

        public void endBatch(RenderType type) {
            BufferBuilder buffer = fixedBuffers.getOrDefault(type, fallback);
            if (buffer.isBuilding()) {
                type.draw(buffer);
            }
            if (type.equals(currentType)) currentType = null;
        }

        public void endBatch() {
            if (currentType != null) endBatch(currentType);
            // uploadujemy tez fixed buffers po kolei
            for (Map.Entry<RenderType, BufferBuilder> e : fixedBuffers.entrySet()) {
                if (e.getValue().isBuilding()) {
                    e.getKey().draw(e.getValue());
                }
            }
        }
    }
}
