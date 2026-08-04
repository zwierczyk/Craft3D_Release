package craft3dgl.save;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Helpery I/O dla save/load. Tylko proste static funkcje - reszta zostaje w MinecraftGL
 * bo wymaga dostepu do 20+ pol (world, animals, villagers, inv, chest, door...).
 */
public final class SaveIO {
    public static final int MAGIC_V1 = 0x43334431;
    public static final int MAGIC_V2 = 0x43334432;  // dodano spawnedVillageCells + homeX/Z villagerow

    private SaveIO() {}

    public static void writeArray(DataOutputStream out, int[] arr) throws IOException {
        out.writeInt(arr.length);
        for (int v : arr) out.writeInt(v);
    }

    public static void readArray(DataInputStream in, int[] arr) throws IOException {
        int n = in.readInt();
        for (int i = 0; i < n; i++) {
            int v = in.readInt();
            if (i < arr.length) arr[i] = v;
        }
    }

    /** Lista wszystkich folderow w saves/ - nazwy zapisanych swiatow. */
    public static String[] listWorlds() {
        File dir = new File("saves");
        File[] files = dir.listFiles(File::isDirectory);
        if (files == null) return new String[0];
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = files[i].getName();
        return names;
    }

    /** Czy save dla danej nazwy istnieje. */
    public static boolean exists(String name) {
        File file = new File(new File("saves", name), "world.dat");
        return file.isFile();
    }

    /** Sciezka do save'a. */
    public static File saveFile(String name) {
        return new File(new File("saves", name), "world.dat");
    }

    /** Stworz katalog save'a. */
    public static void ensureSaveDir(String name) {
        new File("saves", name).mkdirs();
    }
}
