package com.geko7490.saengineandroid;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.IOException;
import java.util.List;

/** Explicit opt-in external-file installer. Never edits APKs or private /data/user/0. */
public final class GraphicsInstaller {
    private static final String PREFERENCES = "graphics_installer";
    private static final String BACKUP = "current_backup";
    private static final String FILENAMES = "current_files";
    private static final String TARGET = "/sdcard/Android/data/com.rockstargames.gtasa/files/data";

    private GraphicsInstaller() { }
    public static String shellQuote(String raw) {
        return "'" + raw.replace("'", "'\\''") + "'";
    }
    public static boolean success(String result) { return result != null && result.startsWith("exit=0\n"); }

    public static String diagnostics(ShizukuBridge shell) throws Exception {
        String script = "echo '--- ACCESS ---'; id; " +
            "echo '--- GTA SA version ---'; dumpsys package com.rockstargames.gtasa | grep -m 1 versionName; " +
            "echo '--- GAME EXTERNAL DATA ---'; ls -ld " +
                shellQuote("/sdcard/Android/data/com.rockstargames.gtasa") + " " +
                shellQuote("/sdcard/Android/data/com.rockstargames.gtasa/files") + " " +
                shellQuote(TARGET) + " 2>&1; " +
            "echo '--- TIME CYCLE FILES ---'; ls -l " + shellQuote(TARGET) +
                "/timecyc.dat 2>&1; " +
            "echo '--- INTERNAL ACCESS (read-only) ---'; ls -ld /data/user/0/com.rockstargames.gtasa 2>&1";
        return shell.exec(script);
    }

    public static boolean hasActiveBackup(Context context) {
        return !context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getString(BACKUP, "").isEmpty();
    }

    public static String install(Context context, ShizukuBridge shell, GraphicsArchive.Selection pack)
            throws Exception {
        if (!shell.ready()) throw new IOException("Shizuku shell bağlantısı yok");
        if (hasActiveBackup(context))
            throw new IOException("Başka bir grafik modu etkin. Yeni moddan önce Geri Al ile orijinali geri yükle.");
        if (pack == null || pack.names.isEmpty())
            throw new IOException("Kurulacak grafik dosyası yok");
        File root = context.getExternalFilesDir("ModCenterBackups");
        if (root == null) throw new IOException("Yedek depolaması erişilemiyor");
        File backup = new File(root, "backup-" + System.currentTimeMillis());
        String parent = backup.getAbsolutePath();
        String origin = pack.dir.getAbsolutePath();
        String target = TARGET;
        StringBuilder script = new StringBuilder();
        script.append("set -eu\n")
                .append("[ -d ").append(shellQuote(target)).append(" ] || { echo 'NO_EXTERNAL_DATA_DIRECTORY'; exit 30; }\n")
                .append("[ -d ").append(shellQuote(origin)).append(" ] || { echo 'STAGING_UNREADABLE'; exit 31; }\n")
                .append("mkdir -p ").append(shellQuote(parent)).append("\n");
        for (String name : pack.names) {
            if (!name.matches("timecyc\\.dat|timecycp\\.dat|colorcycle\\.dat"))
                throw new IOException("Beklenmeyen mod dosyası");
            String src = origin + "/" + name, dst = target + "/" + name, bak = parent + "/" + name;
            script.append("[ -f ").append(shellQuote(dst))
                    .append(" ] || { echo 'TARGET_FILE_NOT_FOUND: ").append(name).append("'; exit 32; }\n")
                    .append("cp -p ").append(shellQuote(dst)).append(" ").append(shellQuote(bak)).append("\n")
                    .append("[ -s ").append(shellQuote(bak)).append(" ] || { echo 'BACKUP_FAILED'; exit 33; }\n")
                    .append("[ -s ").append(shellQuote(src)).append(" ] || { echo 'MOD_SOURCE_MISSING'; exit 34; }\n");
        }
        // Ensure ALL backups exist before the FIRST write. On failure, restore all.
        script.append("restore_original() {\n");
        for (String name : pack.names) {
            script.append("[ ! -f ").append(shellQuote(parent + "/" + name))
                    .append(" ] || cp -p ").append(shellQuote(parent + "/" + name)).append(" ")
                    .append(shellQuote(target + "/" + name)).append(" || true\n");
        }
        script.append("}\ntrap 'restore_original' EXIT\n");
        for (String name : pack.names) {
            String src = origin + "/" + name, dst = target + "/" + name, tmp = dst + ".saengine-tmp";
            script.append("cp ").append(shellQuote(src)).append(" ").append(shellQuote(tmp)).append("\n")
                    .append("cmp -s ").append(shellQuote(src)).append(" ").append(shellQuote(tmp)).append("\n")
                    .append("mv -f ").append(shellQuote(tmp)).append(" ").append(shellQuote(dst)).append("\n");
        }
        script.append("trap - EXIT\necho 'SUCCESS: copied graphics data with backup'\n");
        String result = shell.exec(script.toString());
        if (!success(result)) throw new IOException(result);
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
                .putString(BACKUP, parent).putString(FILENAMES, android.text.TextUtils.join(",", pack.names))
                .apply();
        return result + "\nDİKKAT: GTA SA 2.11.311'in bu harici dosyaları gerçekten okuyup okumadığı henüz doğrulanmadı.";
    }

    public static String restore(Context context, ShizukuBridge shell) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String bakDir = prefs.getString(BACKUP, "");
        String names = prefs.getString(FILENAMES, "");
        File root = context.getExternalFilesDir("ModCenterBackups");
        if (bakDir.isEmpty() || root == null || !new File(bakDir).getCanonicalPath()
                .startsWith(root.getCanonicalPath() + File.separator))
            throw new IOException("Geri alınacak yedek bulunamadı");
        StringBuilder sh = new StringBuilder("set -eu\n");
        for (String name : names.split(",")) {
            if (!name.matches("timecyc\\.dat|timecycp\\.dat|colorcycle\\.dat"))
                throw new IOException("Güvensiz yedek kaydı");
            sh.append("test -s ").append(shellQuote(bakDir + "/" + name)).append("\n")
              .append("cp -p ").append(shellQuote(bakDir + "/" + name)).append(" ")
              .append(shellQuote(TARGET + "/" + name + ".saengine-restore")).append("\n")
              .append("mv -f ").append(shellQuote(TARGET + "/" + name + ".saengine-restore"))
              .append(" ").append(shellQuote(TARGET + "/" + name)).append("\n");
        }
        sh.append("echo 'RESTORED: original files'\n");
        String result = shell.exec(sh.toString());
        if (success(result)) prefs.edit().remove(BACKUP).remove(FILENAMES).apply();
        return result;
    }
}
