package com.geko7490.saengineandroid;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * User-approved Android SAF installer for our original AML FPS module.
 *
 * Only the explicitly selected GTA SA mods folder is modified.
 * The owner must already have an AML-enabled, legally obtained GTA SA Android
 * installation. This app does NOT install/modify Rockstar's commercial APK.
 */
public final class NativeFpsModInstaller {
    private static final String PREFS = "native_fps_mod";
    private static final String KEY_MODS_FOLDER_URI = "owned_gtasa_mods_folder";
    public static final String MOD_NAME = "libSAEngineFPS.so";
    public static final String CONFIG_NAME = "SAEngineFPS.cfg";
    private static final String GAME_PACKAGE = "com.rockstargames.gtasa";

    private NativeFpsModInstaller() { }

    public static boolean allowedFps(int fps) {
        return fps == 30 || fps == 60 || fps == 90 || fps == 120;
    }

    public static boolean isValidTreeId(String id) {
        if (id == null) return false;
        String path = id.replace('\\', '/').toLowerCase(Locale.ROOT);
        int colon = path.indexOf(':');
        if (colon < 1) return false;
        String relative = path.substring(colon + 1);
        return relative.equals("android/data/com.rockstargames.gtasa/mods") ||
                relative.equals("android_unprotected/data/com.rockstargames.gtasa/mods");
    }

    private static DocumentFile checkedModsDir(Context app, Uri treeUri) throws IOException {
        if (treeUri == null || !isValidTreeId(DocumentsContract.getTreeDocumentId(treeUri)))
            throw new IOException("GTA SA AML mods klasörünü seç: .../com.rockstargames.gtasa/mods");
        DocumentFile folder = DocumentFile.fromTreeUri(app, treeUri);
        if (folder == null || !folder.isDirectory())
            throw new IOException("Seçilen mods klasörüne erişilemiyor");
        return folder;
    }

    private static DocumentFile findOrCreate(DocumentFile folder, String filename, String mime)
            throws IOException {
        DocumentFile file = folder.findFile(filename);
        if (file == null) file = folder.createFile(mime, filename);
        if (file == null || file.isDirectory())
            throw new IOException("Dosya oluşturulamadı: " + filename);
        return file;
    }

    public static void install(Context app, Uri treeUri, File verifiedPlugin, int fps)
            throws IOException {
        if (verifiedPlugin == null || !verifiedPlugin.isFile() || verifiedPlugin.length() < 1000)
            throw new IOException("Doğrulanmış ARM64 FPS mod dosyası bulunamadı");
        if (!allowedFps(fps)) throw new IOException("Desteklenmeyen FPS hedefi");
        DocumentFile folder = checkedModsDir(app, treeUri);
        DocumentFile file = findOrCreate(folder, MOD_NAME, "application/octet-stream");
        long sourceSize = verifiedPlugin.length();
        try (FileInputStream source = new FileInputStream(verifiedPlugin);
             OutputStream output = app.getContentResolver().openOutputStream(file.getUri(), "wt")) {
            if (output == null) throw new IOException("Mod dosyası yazılamıyor");
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = source.read(buffer)) != -1)
                output.write(buffer, 0, n);
            output.flush();
        }
        if (file.length() > 0 && file.length() != sourceSize)
            throw new IOException("Kopyalanan mod dosyası boyutu uyuşmuyor");
        saveFpsIntoFolder(app, folder, fps);
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_MODS_FOLDER_URI, treeUri.toString()).apply();
    }

    private static void saveFpsIntoFolder(Context app, DocumentFile folder, int fps)
            throws IOException {
        DocumentFile config = findOrCreate(folder, CONFIG_NAME, "text/plain");
        try (OutputStream stream = app.getContentResolver().openOutputStream(config.getUri(), "wt")) {
            if (stream == null) throw new IOException("FPS ayar dosyası oluşturulamadı");
            stream.write((fps + "\n").getBytes(StandardCharsets.US_ASCII));
            stream.flush();
        }
    }

    /** Called after the launcher FPS setting changes; fails closed if permission was revoked. */
    public static boolean updateStoredFps(Context app, int fps) {
        if (!allowedFps(fps)) return false;
        String saved = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MODS_FOLDER_URI, "");
        if (saved.isEmpty()) return false;
        try {
            Uri tree = Uri.parse(saved);
            DocumentFile folder = checkedModsDir(app, tree);
            saveFpsIntoFolder(app, folder, fps);
            return true;
        } catch (RuntimeException | IOException ignored) {
            return false;
        }
    }
}
