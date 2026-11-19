package com.geko7490.saengineandroid;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Strictly imports supported plain-text GTA SA timecycle files.
 * No scripts, APK, .so, path traversal or opaque archives are accepted.
 */
public final class GraphicsArchive {
    private static final long MAX_TOTAL = 4 * 1024 * 1024;
    private static final int MAX_ENTRIES = 100;
    public static final class Selection {
        public final File dir;
        public final List<String> names;
        public final long bytes;
        public Selection(File dir, List<String> names, long bytes) {
            this.dir = dir; this.names = names; this.bytes = bytes;
        }
    }
    private static boolean allowedName(String n) {
        return n.equals("timecyc.dat") || n.equals("colorcycle.dat") || n.equals("timecycp.dat");
    }
    public static boolean allowedPath(String path) {
        if (path == null || path.isEmpty() || path.indexOf('\0') >= 0 ||
                path.startsWith("/") || path.contains("\\") || path.contains(":")) return false;
        for (String part : path.split("/"))
            if (part.equals("..") || part.equals(".") || part.isEmpty()) return false;
        return true;
    }
    private static void deleteTree(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteTree(c);
        }
        f.delete();
    }
    public static Selection fromZip(Context context, Uri archive) throws IOException {
        File parent = context.getExternalFilesDir("ModCenterStaging");
        if (parent == null) throw new IOException("Mod geçici depolaması erişilemedi");
        File dir = new File(parent, "current");
        if (dir.exists()) deleteTree(dir);
        if (!dir.mkdirs()) throw new IOException("Mod staging klasörü açılamadı");
        List<String> names = new ArrayList<>();
        long total = 0;
        int entries = 0;
        try (InputStream stream = context.getContentResolver().openInputStream(archive)) {
            if (stream == null) throw new IOException("ZIP dosyası okunamadı");
            try (ZipInputStream zip = new ZipInputStream(stream)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (++entries > MAX_ENTRIES) throw new IOException("ZIP çok fazla dosya içeriyor");
                    String path = entry.getName();
                    if (!allowedPath(path)) {
                        // ZIP directory entries usually have trailing slash, allow safe directories.
                        if (!(entry.isDirectory() && allowedPath(path.substring(0, path.length() - 1))))
                            throw new IOException("ZIP güvensiz dosya yolu içeriyor");
                    }
                    if (entry.isDirectory()) { zip.closeEntry(); continue; }
                    String[] parts = path.split("/");
                    String fileName = parts[parts.length - 1].toLowerCase(Locale.ROOT);
                    if (!allowedName(fileName)) { zip.closeEntry(); continue; }
                    if (names.contains(fileName)) throw new IOException("ZIP'te tekrar eden " + fileName);
                    File output = new File(dir, fileName);
                    byte[] buffer = new byte[8192];
                    long fileBytes = 0;
                    int n;
                    try (FileOutputStream out = new FileOutputStream(output)) {
                        while ((n = zip.read(buffer)) > 0) {
                            fileBytes += n;
                            total += n;
                            if (fileBytes > 1024 * 1024 || total > MAX_TOTAL)
                                throw new IOException("Grafik ayar dosyası çok büyük");
                            out.write(buffer, 0, n);
                        }
                        out.getFD().sync();
                    }
                    if (fileBytes < 32) throw new IOException("Grafik dosyası çok küçük veya boş: " + fileName);
                    names.add(fileName);
                    zip.closeEntry();
                }
            }
        } catch (IOException | RuntimeException e) {
            deleteTree(dir);
            throw e;
        }
        if (names.isEmpty()) {
            deleteTree(dir);
            throw new IOException("ZIP'te timecyc.dat, colorcycle.dat veya timecycp.dat yok. Bu paket için özel yükleyici gerekiyor.");
        }
        return new Selection(dir, names, total);
    }
    private GraphicsArchive() { }
}
