package craft3dgl;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Minecraft 1.12 sound-event player.
 *
 * The event/variant table comes from 1.12.2 sounds.json (see
 * assets/sounds/events.properties). Assets are PCM WAV conversions of the
 * original OGG files so the game needs no platform-specific Vorbis plug-in.
 * Pitch is applied by software resampling and event/category volume is mixed
 * into the PCM samples before Java Sound receives them.
 */
final class SoundEngine {
    private static final float OUTPUT_RATE = 44100.0f;

    private static final class Variant {
        final String path;
        final float volume;

        Variant(String path, float volume) {
            this.path = path;
            this.volume = volume;
        }
    }

    private static final class SoundData {
        final byte[] pcm;
        final float sampleRate;
        final int channels;

        SoundData(byte[] pcm, float sampleRate, int channels) {
            this.pcm = pcm;
            this.sampleRate = sampleRate;
            this.channels = channels;
        }
    }

    volatile boolean enabled = true;
    volatile double masterVolume = 1.0;
    final Random rnd = new Random();
    File soundDir;

    final HashMap<String, Double> categoryVolume = new HashMap<>();
    private final Map<String, Variant[]> events = new HashMap<>();
    private final Map<String, SoundData> cache = new ConcurrentHashMap<>();
    private final Set<String> warnedMissing = new HashSet<>();
    private final ExecutorService players = Executors.newFixedThreadPool(8, new ThreadFactory() {
        private int number;
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "minecraft-sound-" + (++number));
            thread.setDaemon(true);
            return thread;
        }
    });

    SoundEngine() {
        categoryVolume.put("music", 1.0);
        categoryVolume.put("blocks", 1.0);
        categoryVolume.put("hostile", 1.0);
        categoryVolume.put("animals", 1.0);
        categoryVolume.put("players", 1.0);
        categoryVolume.put("ambient", 1.0);
        categoryVolume.put("weather", 1.0);
        categoryVolume.put("ui", 1.0);
    }

    double catVol(String cat) {
        Double value = categoryVolume.get(cat);
        return value == null ? 1.0 : value;
    }

    void setCategoryVolume(String cat, double value) {
        categoryVolume.put(cat, Math.max(0, Math.min(1, value)));
    }

    void setSoundDir(File dir) {
        soundDir = dir;
        cache.clear();
        loadEventTable();
    }

    void setVolume(double value) {
        masterVolume = Math.max(0, Math.min(1, value));
        enabled = masterVolume > 0.001;
    }

    int volumePercent() {
        return (int) Math.round(masterVolume * 100);
    }

    double mix(String category) {
        return masterVolume * catVol(category);
    }

    // SoundEvents and call-site volume/pitch values below mirror MCP 9.40.
    void playClick()           { playEvent("ui.button.click", "ui", 1.0f, 1.0f); }
    void playJump()            { /* Vanilla 1.12 has no separate jump sound. */ }
    void playPlace(int block)  { playEvent(blockEvent(block, "place"), "blocks", 1.0f, 0.8f); }
    void playBreak(int block)  { playEvent(blockEvent(block, "break"), "blocks", 1.0f, 0.8f); }
    void playStep(int block)   { playEvent(blockEvent(block, "step"), "players", 0.15f, 1.0f); }
    void playEat()             { playEvent("entity.generic.eat", "players", rnd.nextBoolean() ? 1.0f : 0.5f, triangularPitch(0.2f, 1.0f)); }
    void playBurp()            { playEvent("entity.player.burp", "players", 0.5f, 0.9f + rnd.nextFloat() * 0.1f); }
    void playHurt()            { playEvent("entity.player.hurt", "players", 1.0f, triangularPitch(0.2f, 1.0f)); }
    void playAttackWeak()      { playEvent("entity.player.attack.weak", "players", 1.0f, 1.0f); }
    void playAttackStrong()    { playEvent("entity.player.attack.strong", "players", 1.0f, 1.0f); }
    void playAttackKnockback() { playEvent("entity.player.attack.knockback", "players", 1.0f, 1.0f); }
    void playAttackCritical()  { playEvent("entity.player.attack.crit", "players", 1.0f, 1.0f); }
    void playAttackSweep()     { playEvent("entity.player.attack.sweep", "players", 1.0f, 1.0f); }
    void playAttackNoDamage()  { playEvent("entity.player.attack.nodamage", "players", 1.0f, 1.0f); }
    void playAnimal()          { playCow(); }
    void playCow()             { playEvent("entity.cow.hurt", "animals", 0.4f, triangularPitch(0.2f, 1.0f)); }
    void playPig()             { playEvent("entity.pig.hurt", "animals", 1.0f, triangularPitch(0.2f, 1.0f)); }
    void playSheep()           { playEvent("entity.sheep.hurt", "animals", 1.0f, triangularPitch(0.2f, 1.0f)); }
    void playVillagerHurt()    { playEvent("entity.villager.hurt", "animals", 1.0f, triangularPitch(0.2f, 1.0f)); }
    void playVillagerYes()     { playEvent("entity.villager.yes", "animals", 1.0f, triangularPitch(0.2f, 1.0f)); }
    void playVillagerNo()      { playEvent("entity.villager.no", "animals", 1.0f, triangularPitch(0.2f, 1.0f)); }
    void playXpPickup()        { playEvent("entity.experience_orb.pickup", "players", 0.1f, triangularPitch(0.35f, 0.9f)); }
    void playLevelUp()         { playEvent("entity.player.levelup", "players", 0.75f, 1.0f); }
    void playItemPickup()      { playEvent("entity.item.pickup", "players", 0.2f, (triangularPitch(0.7f, 1.0f)) * 2.0f); }
    void playDoorOpen()        { playEvent("block.wooden_door.open", "blocks", 1.0f, 0.9f + rnd.nextFloat() * 0.1f); }
    void playDoorClose()       { playEvent("block.wooden_door.close", "blocks", 1.0f, 0.9f + rnd.nextFloat() * 0.1f); }
    void playChestOpen()       { playEvent("block.chest.open", "blocks", 0.5f, 0.9f + rnd.nextFloat() * 0.1f); }
    void playChestClose()      { playEvent("block.chest.close", "blocks", 0.5f, 0.9f + rnd.nextFloat() * 0.1f); }
    void playSplash()          { playEvent("entity.player.splash", "players", 1.0f, triangularPitch(0.4f, 1.0f)); }
    void playRain(boolean above, float strength) {
        float fade = Math.max(0f, Math.min(1f, strength));
        playEvent(above ? "weather.rain.above" : "weather.rain", "weather",
                (above ? 0.1f : 0.2f) * fade, above ? 0.5f : 1.0f);
    }

    private float triangularPitch(float spread, float base) {
        return (rnd.nextFloat() - rnd.nextFloat()) * spread + base;
    }

    private String blockEvent(int block, String action) {
        switch (block) {
            case MinecraftGL.GRASS:
            case MinecraftGL.LEAVES:
            case MinecraftGL.TALL_GRASS:
            case MinecraftGL.WHEAT_0:
            case MinecraftGL.WHEAT_1:
            case MinecraftGL.WHEAT_2:
            case MinecraftGL.WHEAT_3:
                return "block.grass." + action;       // SoundType.PLANT
            case MinecraftGL.DIRT:
            case MinecraftGL.FARMLAND:
                return "block.gravel." + action;      // SoundType.GROUND
            case MinecraftGL.SAND:
                return "block.sand." + action;        // SoundType.SAND
            case MinecraftGL.WOOD:
            case MinecraftGL.PLANKS:
            case MinecraftGL.CRAFTING_TABLE:
            case MinecraftGL.DOOR_BOTTOM:
            case MinecraftGL.DOOR_TOP:
            case MinecraftGL.CHEST:
                return "block.wood." + action;        // SoundType.WOOD
            case MinecraftGL.GLASS:
                return "block.glass." + action;        // SoundType.GLASS
            case MinecraftGL.WATER:
                return "entity.player.splash";
            default:
                return "block.stone." + action;       // SoundType.STONE
        }
    }

    private synchronized void loadEventTable() {
        events.clear();
        warnedMissing.clear();
        if (soundDir == null) return;
        File table = new File(soundDir, "events.properties");
        if (!table.isFile()) {
            System.err.println("[SoundEngine] Missing Minecraft 1.12 event table: " + table);
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(table)) {
            properties.load(input);
            for (String event : properties.stringPropertyNames()) {
                String[] entries = properties.getProperty(event).split(",");
                ArrayList<Variant> variants = new ArrayList<>();
                for (String entry : entries) {
                    String value = entry.trim();
                    if (value.isEmpty()) continue;
                    int at = value.lastIndexOf('@');
                    float volume = 1.0f;
                    if (at >= 0) {
                        volume = Float.parseFloat(value.substring(at + 1));
                        value = value.substring(0, at);
                    }
                    variants.add(new Variant(value, volume));
                }
                events.put(event, variants.toArray(new Variant[variants.size()]));
            }
        } catch (Exception exception) {
            System.err.println("[SoundEngine] Cannot load Minecraft 1.12 sounds: " + exception.getMessage());
            events.clear();
        }
    }

    private void playEvent(String event, String category, float volume, float pitch) {
        if (!enabled || soundDir == null) return;
        double mixed = mix(category) * volume;
        if (mixed <= 0.001) return;
        Variant[] variants = events.get(event);
        if (variants == null || variants.length == 0) {
            warnMissing(event);
            return;
        }
        Variant variant = variants[rnd.nextInt(variants.length)];
        File file = new File(soundDir, variant.path + ".wav");
        if (!file.isFile()) {
            warnMissing(file.getPath());
            return;
        }
        final float outputVolume = (float) Math.max(0.0, Math.min(1.0, mixed * variant.volume));
        final float outputPitch = Math.max(0.25f, Math.min(4.0f, pitch));
        players.execute(() -> playPcm(file, outputVolume, outputPitch));
    }

    private synchronized void warnMissing(String name) {
        if (warnedMissing.add(name)) {
            System.err.println("[SoundEngine] Missing Minecraft 1.12 sound: " + name);
        }
    }

    private void playPcm(File file, float volume, float pitch) {
        try {
            SoundData sound = cache.get(file.getPath());
            if (sound == null) {
                sound = readPcm(file);
                if (sound == null) return;
                cache.put(file.getPath(), sound);
            }
            byte[] output = resample(sound, volume, pitch);
            AudioFormat format = new AudioFormat(OUTPUT_RATE, 16, sound.channels, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, Math.min(output.length, 16384));
            line.start();
            line.write(output, 0, output.length);
            line.drain();
            line.close();
        } catch (Exception exception) {
            warnMissing(file.getPath() + " (playback: " + exception.getMessage() + ")");
        }
    }

    private SoundData readPcm(File file) {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) bytes.write(buffer, 0, read);
            }
            byte[] wav = bytes.toByteArray();
            if (wav.length < 44 || !tag(wav, 0, "RIFF") || !tag(wav, 8, "WAVE")) {
                throw new IllegalArgumentException("not a PCM WAV file");
            }
            int channels = 0;
            int sampleRate = 0;
            int bits = 0;
            int dataOffset = -1;
            int dataLength = 0;
            for (int offset = 12; offset + 8 <= wav.length; ) {
                int size = little32(wav, offset + 4);
                int start = offset + 8;
                if (size < 0 || start + size > wav.length) break;
                if (tag(wav, offset, "fmt ") && size >= 16) {
                    if (little16(wav, start) != 1) throw new IllegalArgumentException("WAV is not PCM");
                    channels = little16(wav, start + 2);
                    sampleRate = little32(wav, start + 4);
                    bits = little16(wav, start + 14);
                } else if (tag(wav, offset, "data")) {
                    dataOffset = start;
                    dataLength = size;
                }
                offset = start + size + (size & 1);
            }
            if (channels < 1 || channels > 2 || sampleRate <= 0 || bits != 16 || dataOffset < 0) {
                throw new IllegalArgumentException("unsupported WAV format");
            }
            byte[] pcm = new byte[dataLength];
            System.arraycopy(wav, dataOffset, pcm, 0, dataLength);
            return new SoundData(pcm, sampleRate, channels);
        } catch (Exception exception) {
            warnMissing(file.getPath() + " (decode: " + exception.getMessage() + ")");
            return null;
        }
    }

    private static boolean tag(byte[] bytes, int offset, String value) {
        if (offset < 0 || offset + value.length() > bytes.length) return false;
        for (int i = 0; i < value.length(); i++) {
            if ((bytes[offset + i] & 255) != value.charAt(i)) return false;
        }
        return true;
    }

    private static int little16(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | ((bytes[offset + 1] & 255) << 8);
    }

    private static int little32(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | ((bytes[offset + 1] & 255) << 8)
                | ((bytes[offset + 2] & 255) << 16) | ((bytes[offset + 3] & 255) << 24);
    }

    private byte[] resample(SoundData sound, float volume, float pitch) {
        int sourceFrames = sound.pcm.length / (sound.channels * 2);
        double sourceStep = sound.sampleRate * pitch / OUTPUT_RATE;
        int outputFrames = Math.max(1, (int) Math.ceil(sourceFrames / sourceStep));
        byte[] output = new byte[outputFrames * sound.channels * 2];
        for (int frame = 0; frame < outputFrames; frame++) {
            double sourcePosition = frame * sourceStep;
            int first = Math.min(sourceFrames - 1, (int) sourcePosition);
            int second = Math.min(sourceFrames - 1, first + 1);
            double fraction = sourcePosition - first;
            for (int channel = 0; channel < sound.channels; channel++) {
                int a = sample16(sound.pcm, (first * sound.channels + channel) * 2);
                int b = sample16(sound.pcm, (second * sound.channels + channel) * 2);
                int sample = (int) Math.round((a + (b - a) * fraction) * volume);
                sample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
                int offset = (frame * sound.channels + channel) * 2;
                output[offset] = (byte) (sample & 255);
                output[offset + 1] = (byte) ((sample >>> 8) & 255);
            }
        }
        return output;
    }

    private static int sample16(byte[] data, int offset) {
        return (short) ((data[offset] & 255) | (data[offset + 1] << 8));
    }
}
