package craft3dgl.save;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;

/**
 * Zapis informacji o crashu do pliku logs/crash.log.
 */
public final class CrashLogger {
    private CrashLogger() {}

    public static void write(Throwable t) {
        try {
            File dir = new File("logs");
            dir.mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(new File(dir, "crash.log"), true))) {
                pw.println("==== Craft3D crash " + new Date() + " ====");
                t.printStackTrace(pw);
                pw.println();
            }
        } catch (Exception ignored) {}
    }
}
