package craft3dgl.save;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Znajduje katalogi assetów (np. /sounds/) zarówno przy uruchomieniu
 * w IDE jak i z jar-a.
 */
public final class AssetFinder {
    private AssetFinder() {}

    /** Znajdź folder assetów (np. "sounds"). Najpierw w CWD/assets, potem obok jar-a. */
    public static File findAssetDir(String child, Class<?> mainClass) {
        File cwd = new File("assets", child);
        if (cwd.isDirectory()) return cwd;
        try {
            File loc = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            File base = loc.isFile() ? loc.getParentFile() : loc;
            File dir = new File(new File(base, "assets"), child);
            if (dir.isDirectory()) return dir;
        } catch (URISyntaxException ignored) {}
        return cwd;
    }
}
