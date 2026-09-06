package craft3dgl;

import javax.sound.sampled.*;
import java.io.File;
import java.util.HashMap;
import java.util.Random;

final class SoundEngine {
    volatile boolean enabled = true;
    volatile double masterVolume = 1.0;
    final float sampleRate = 44100f;
    final Random rnd = new Random();
    File soundDir;

    final HashMap<String, Double> categoryVolume = new HashMap<>();

    SoundEngine() {
        categoryVolume.put("music", 1.0);
        categoryVolume.put("blocks", 1.0);
        categoryVolume.put("hostile", 1.0);
        categoryVolume.put("animals", 1.0);
        categoryVolume.put("players", 1.0);
        categoryVolume.put("ambient", 1.0);
        categoryVolume.put("ui", 1.0);
    }

    double catVol(String cat) {
        Double v = categoryVolume.get(cat);
        return v == null ? 1.0 : v;
    }

    void setCategoryVolume(String cat, double v) {
        categoryVolume.put(cat, Math.max(0, Math.min(1, v)));
    }

    void setSoundDir(File dir) { soundDir = dir; }

    void setVolume(double v) {
        masterVolume = Math.max(0, Math.min(1, v));
        enabled = masterVolume > 0.001;
    }

    int volumePercent() { return (int) Math.round(masterVolume * 100); }

    double mix(String cat) { return masterVolume * catVol(cat); }

    void playClick()           { if (playFile("click.wav")) return; tone(760, 0.035, 0.16 * mix("ui"), 0); }
    void playJump()            { if (playFile("jump.wav")) return; sweep(260, 520, 0.10, 0.22 * mix("players")); }
    void playPlace(int block)  { if (playFile("place.wav")) return; noiseThump(block == 4 || block == 7 ? 150 : 110, 0.09, 0.24 * mix("blocks")); }
    void playBreak(int block)  { if (playFile("break.wav")) return; noiseCrack(block == 3 ? 0.16 : 0.12, (block == 5 ? 0.14 : 0.28) * mix("blocks")); }
    void playStep(int block)   { if (playFile("step.wav")) return; if (block == 0) block = 3; double vol = block == 5 ? 0.08 : 0.13; noiseThump(block == 6 ? 180 : 130, 0.055, vol * mix("players")); }
    void playEat()             { if (playFile("eat.wav")) return; noiseThump(260, 0.12, 0.18 * mix("players")); tone(520, 0.04, 0.08 * mix("players"), 0); }
    void playHurt()            { if (playFile("hurt.wav")) return; sweep(320, 120, 0.16, 0.22 * mix("players")); }
    void playAttackWeak()      { if (playFile("attack_weak.wav")) return; noiseThump(105, 0.055, 0.09 * mix("players")); }
    void playAttackStrong()    { if (playFile("attack_strong.wav")) return; noiseCrack(0.075, 0.17 * mix("players")); }
    void playAttackKnockback() { if (playFile("attack_knockback.wav")) return; sweep(180, 95, 0.10, 0.17 * mix("players")); }
    void playAttackCritical()  { if (playFile("attack_crit.wav")) return; sweep(520, 210, 0.09, 0.18 * mix("players")); }
    void playAttackSweep()     { if (playFile("attack_sweep.wav")) return; sweep(150, 420, 0.12, 0.14 * mix("players")); }
    void playAttackNoDamage()  { if (playFile("attack_nodamage.wav")) return; noiseThump(80, 0.045, 0.07 * mix("players")); }
    void playAnimal()          { if (playFile("animal.wav")) return; sweep(180 + rnd.nextInt(80), 130 + rnd.nextInt(80), 0.11, 0.12 * mix("animals")); }
    void playCow()             { if (playFile("cow.wav")) return; playAnimal(); }
    void playPig()             { if (playFile("pig.wav")) return; playAnimal(); }
    void playSheep()           { if (playFile("sheep.wav")) return; playAnimal(); }
    void playXpPickup()        { if (playFile("experience_orb.wav")) return; if (playFile("xp.wav")) return; tone(1600, 0.05, 0.12 * mix("players"), 0); }
    void playLevelUp()         { if (playFile("levelup.wav")) return; sweep(523, 1047, 0.5, 0.20 * mix("players")); }
    void playDoorOpen()        { if (playFile("door_open.wav")) return; noiseThump(180, 0.15, 0.16 * mix("blocks")); }
    void playDoorClose()       { if (playFile("door_close.wav")) return; noiseThump(100, 0.10, 0.20 * mix("blocks")); }
    void playSplash()          { if (playFile("splash.wav")) return; noiseCrack(0.25, 0.14 * mix("players")); }

    boolean playFile(String name) {
        if (!enabled || soundDir == null) return false;
        File file = new File(soundDir, name);
        if (!file.isFile()) return false;
        Thread t = new Thread(() -> {
            try {
                AudioInputStream in = AudioSystem.getAudioInputStream(file);
                Clip clip = AudioSystem.getClip();
                clip.open(in);
                clip.start();
                Thread.sleep(Math.max(20, clip.getMicrosecondLength() / 1000));
                clip.close();
                in.close();
            } catch (Exception ignored) {}
        }, "voxel-wav");
        t.setDaemon(true);
        t.start();
        return true;
    }

    void tone(double freq, double seconds, double volume, int waveform) {
        if (!enabled) return;
        int samples = Math.max(1, (int) (sampleRate * seconds));
        byte[] data = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = i / sampleRate;
            double env = 1.0 - (double) i / samples;
            double s = Math.sin(2 * Math.PI * freq * t);
            if (waveform == 1) s = s > 0 ? 1 : -1;
            write16(data, i, s * env * volume);
        }
        play(data);
    }

    void sweep(double start, double end, double seconds, double volume) {
        if (!enabled) return;
        int samples = Math.max(1, (int) (sampleRate * seconds));
        byte[] data = new byte[samples * 2];
        double phase = 0;
        for (int i = 0; i < samples; i++) {
            double a = (double) i / samples;
            double freq = start + (end - start) * a;
            phase += 2 * Math.PI * freq / sampleRate;
            double env = Math.sin(Math.PI * a) * (1.0 - a * 0.25);
            write16(data, i, Math.sin(phase) * env * volume);
        }
        play(data);
    }

    void noiseThump(double baseFreq, double seconds, double volume) {
        if (!enabled) return;
        int samples = Math.max(1, (int) (sampleRate * seconds));
        byte[] data = new byte[samples * 2];
        double phase = 0, last = 0;
        for (int i = 0; i < samples; i++) {
            double a = (double) i / samples;
            phase += 2 * Math.PI * (baseFreq * (1.0 - a * 0.35)) / sampleRate;
            last = last * 0.55 + (rnd.nextDouble() * 2 - 1) * 0.45;
            double env = Math.pow(1.0 - a, 1.8);
            write16(data, i, (Math.sin(phase) * 0.42 + last * 0.58) * env * volume);
        }
        play(data);
    }

    void noiseCrack(double seconds, double volume) {
        if (!enabled) return;
        int samples = Math.max(1, (int) (sampleRate * seconds));
        byte[] data = new byte[samples * 2];
        double last = 0;
        for (int i = 0; i < samples; i++) {
            double a = (double) i / samples;
            double env = Math.pow(1.0 - a, 2.2);
            double n = rnd.nextDouble() * 2 - 1;
            last = last * 0.25 + n * 0.75;
            if ((i % 700) < 60) last += (rnd.nextDouble() * 2 - 1) * 0.8;
            write16(data, i, last * env * volume);
        }
        play(data);
    }

    void write16(byte[] data, int sample, double value) {
        value = Math.max(-1, Math.min(1, value));
        short v = (short) (value * 32767);
        data[sample * 2] = (byte) (v & 255);
        data[sample * 2 + 1] = (byte) ((v >> 8) & 255);
    }

    void play(byte[] data) {
        if (!enabled) return;
        Thread t = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, data.length);
                line.start();
                line.write(data, 0, data.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {}
        }, "voxel-sound");
        t.setDaemon(true);
        t.start();
    }
}
