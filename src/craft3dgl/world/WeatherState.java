package craft3dgl.world;

import java.util.Random;

/**
 * Server-side weather clock ported from World.updateWeather in MCP 9.40.
 * Timers are measured in Minecraft ticks and rain/thunder strengths fade by
 * exactly 0.01 per tick, as they do in Minecraft 1.12.
 */
public final class WeatherState {
    private final Random random;
    private int rainTime;
    private int thunderTime;
    private boolean raining;
    private boolean thundering;
    private float rainStrength;
    private float thunderStrength;
    private double partialTick;

    public WeatherState(long seed) {
        random = new Random(seed ^ 0x4f9939f508L);
        rainTime = random.nextInt(168000) + 12000;
        thunderTime = random.nextInt(168000) + 12000;
    }

    /** Advance using real seconds while retaining vanilla's 20 TPS timing. */
    public void update(double seconds) {
        partialTick += Math.max(0.0, Math.min(seconds, 1.0)) * 20.0;
        int ticks = (int) partialTick;
        partialTick -= ticks;
        for (int i = 0; i < ticks; i++) tick();
    }

    private void tick() {
        if (--thunderTime <= 0) {
            thundering = !thundering;
            thunderTime = thundering ? random.nextInt(12000) + 3600
                                     : random.nextInt(168000) + 12000;
        }
        if (--rainTime <= 0) {
            raining = !raining;
            rainTime = raining ? random.nextInt(12000) + 12000
                               : random.nextInt(168000) + 12000;
        }

        thunderStrength = clamp(thunderStrength + (thundering ? 0.01f : -0.01f));
        rainStrength = clamp(rainStrength + (raining ? 0.01f : -0.01f));
    }

    /** Implements /weather clear|rain|thunder and starts the normal fade. */
    public boolean setWeather(String kind) {
        if (kind == null) return false;
        if ("clear".equalsIgnoreCase(kind)) {
            raining = false;
            thundering = false;
            rainTime = 12000;
            thunderTime = 12000;
        } else if ("rain".equalsIgnoreCase(kind)) {
            raining = true;
            thundering = false;
            rainTime = 12000;
            thunderTime = 12000;
        } else if ("thunder".equalsIgnoreCase(kind)) {
            raining = true;
            thundering = true;
            rainTime = 12000;
            thunderTime = 12000;
        } else {
            return false;
        }
        return true;
    }

    public float getRainStrength() { return rainStrength; }
    public float getThunderStrength() { return thunderStrength * rainStrength; }
    public float getRawThunderStrength() { return thunderStrength; }
    public boolean isRaining() { return raining; }
    public boolean isThundering() { return thundering; }
    public int getRainTime() { return rainTime; }
    public int getThunderTime() { return thunderTime; }

    public void load(boolean raining, boolean thundering, int rainTime, int thunderTime,
                     float rainStrength, float thunderStrength) {
        this.raining = raining;
        this.thundering = thundering;
        this.rainTime = Math.max(1, rainTime);
        this.thunderTime = Math.max(1, thunderTime);
        this.rainStrength = clamp(rainStrength);
        this.thunderStrength = clamp(thunderStrength);
        this.partialTick = 0.0;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
