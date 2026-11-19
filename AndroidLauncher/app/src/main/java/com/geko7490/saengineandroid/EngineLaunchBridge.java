package com.geko7490.saengineandroid;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/** V0.5: find installed game package and grant it read-only data URIs when starting. */
public final class EngineLaunchBridge {
    public static final String ACTION = "com.geko7490.saenginegame.action.START_FROM_LAUNCHER";
    public static final String EXTRA_FPS = "com.geko7490.saenginegame.extra.FPS";
    public static final String EXTRA_QUALITY = "com.geko7490.saenginegame.extra.QUALITY";
    public static final String EXTRA_LIST_URI = "com.geko7490.saenginegame.extra.LIST_URI";
    public static final String EXTRA_FILE_URI = "com.geko7490.saenginegame.extra.FILE_URI";

    private EngineLaunchBridge() { }

    public static boolean isInstalled(Activity host) {
        return EngineInstaller.installedPackage(host) != null;
    }

    public static boolean start(Activity host, int fps, int quality) {
        if (!isInstalled(host)) {
            Toast.makeText(host, "Oyun motoru APK'sı henüz kurulu değil", Toast.LENGTH_LONG).show();
            return false;
        }
        try {
            GameDataImporter files = new GameDataImporter(host);
            if (!GameDataImporter.isValidRoot(files.getGameRoot())) {
                Toast.makeText(host, "Önce kendi GTA SA PC dosyalarını içe aktar", Toast.LENGTH_LONG).show();
                return false;
            }
            Intent launch = host.getPackageManager().getLaunchIntentForPackage(EngineInstaller.ENGINE_PACKAGE);
            if (launch == null) {
                Toast.makeText(host, "Motorun başlatılabilir Android Activity'si bulunamadı", Toast.LENGTH_LONG).show();
                return false;
            }
            int grantFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
            Uri list = GameDataProvider.LIST_ROOT;
            Uri file = GameDataProvider.FILE_ROOT;
            host.grantUriPermission(EngineInstaller.ENGINE_PACKAGE, list, grantFlags);
            host.grantUriPermission(EngineInstaller.ENGINE_PACKAGE, file, grantFlags);
            launch.setAction(ACTION);
            launch.putExtra(EXTRA_FPS, fps);
            launch.putExtra(EXTRA_QUALITY, quality);
            launch.putExtra(EXTRA_LIST_URI, list.toString());
            launch.putExtra(EXTRA_FILE_URI, file.toString());
            launch.addFlags(grantFlags);
            launch.setClipData(ClipData.newUri(host.getContentResolver(), "SA Engine data", file));
            host.startActivity(launch);
            return true;
        } catch (Exception e) {
            Toast.makeText(host, "Motor başlatılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
