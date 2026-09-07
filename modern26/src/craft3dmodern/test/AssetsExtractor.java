package craft3dmodern.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import craft3dmodern.render.Texture;


public final class AssetsExtractor {
    public static void main(String[] args) throws IOException {
        File assetRoot = Texture.assetRoot();
        File zip = new File(assetRoot, "minecraft-26.2.zip");
        File version = new File(assetRoot, "minecraft/version.json");
        if (!zip.isFile()) {
            System.out.println("[assets] no " + zip.getPath() + " - skipping extraction");
            return;
        }
        if (version.isFile()) {
            String text = new String(Files.readAllBytes(version.toPath()));
            if (text.contains("26.2")) {
                System.out.println("[assets] already extracted (" + version.getPath() + ")");
                return;
            }
        }
        System.out.println("[assets] extracting " + zip.getPath() + " ...");
        try (ZipFile z = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> en = z.entries();
            int count = 0;
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                File out = new File(assetRoot, e.getName());
                File parent = out.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("cannot create " + parent);
                }
                try (InputStream in = z.getInputStream(e)) {
                    Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                count++;
            }
            System.out.println("[assets] extracted " + count + " files");
        }
    }

    private AssetsExtractor() {}
}
