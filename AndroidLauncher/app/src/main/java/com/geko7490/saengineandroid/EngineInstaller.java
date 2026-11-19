package com.geko7490.saengineandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.core.content.FileProvider;

import java.io.File;
import java.security.MessageDigest;
import java.util.Locale;

/** V0.4: verify engine package, signer trust and Android version before user-approved install. */
public final class EngineInstaller {
    public static final String ENGINE_PACKAGE = "com.geko7490.saenginegame";
    private static final String PREFS = "engine_installer_security";
    private static final String PINNED = "trusted_engine_cert_sha256";

    public static final class InspectResult {
        public final String packageName, versionName, signerSha256;
        public final long versionCode;
        public final int minSdk;
        public final boolean installed, update, downgrade;
        InspectResult(String pkg, String name, String digest, long code, int minSdk,
                      boolean installed, boolean update, boolean downgrade) {
            this.packageName = pkg; this.versionName = name; this.signerSha256 = digest;
            this.versionCode = code; this.minSdk = minSdk;
            this.installed = installed; this.update = update; this.downgrade = downgrade;
        }
    }

    private EngineInstaller() { }

    public static InspectResult inspect(Activity activity, File apk) throws Exception {
        if (!apk.isFile() || apk.length() <= 0) throw new IllegalArgumentException("APK dosyası boş");
        PackageManager pm = activity.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(apk.getAbsolutePath(),
                PackageManager.GET_SIGNING_CERTIFICATES);
        if (info == null) throw new IllegalArgumentException("Geçerli Android APK değil");
        if (!ENGINE_PACKAGE.equals(info.packageName))
            throw new SecurityException("Motor paketi beklenenden farklı: " + info.packageName);
        String cert = sha256(getCertificate(info));
        int minSdk = info.applicationInfo != null ? info.applicationInfo.minSdkVersion : 0;
        if (minSdk > Build.VERSION.SDK_INT)
            throw new IllegalArgumentException("Bu motor için Android API " + minSdk + " gerekiyor");
        String pinned = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).getString(PINNED, "");
        if (!pinned.isEmpty() && !pinned.equals(cert))
            throw new SecurityException("Motor imzası önceden onaylanan sertifikadan farklı");
        PackageInfo installed = installedPackage(activity);
        if (installed != null) {
            String installedCert = sha256(getCertificate(installed));
            if (!installedCert.equals(cert))
                throw new SecurityException("Yüklü motorla farklı imza: güncelleme engellendi");
        }
        long version = info.getLongVersionCode();
        long oldVersion = installed == null ? -1 : installed.getLongVersionCode();
        return new InspectResult(info.packageName, info.versionName, cert,
                version, minSdk, installed != null, installed != null && version > oldVersion,
                installed != null && version < oldVersion);
    }

    private static Signature getCertificate(PackageInfo info) {
        if (info.signingInfo == null) throw new SecurityException("APK imza bilgisi bulunamadı");
        Signature[] signatures = info.signingInfo.hasMultipleSigners()
                ? info.signingInfo.getApkContentsSigners()
                : info.signingInfo.getSigningCertificateHistory();
        if (signatures == null || signatures.length != 1)
            throw new SecurityException("Tekil, doğrulanabilir APK sertifikası bekleniyor");
        return signatures[0];
    }

    private static String sha256(Signature signature) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(signature.toByteArray());
        StringBuilder builder = new StringBuilder();
        for (byte b : hash) builder.append(String.format(Locale.US, "%02x", b & 0xff));
        return builder.toString();
    }

    public static PackageInfo installedPackage(Activity activity) {
        try {
            return activity.getPackageManager().getPackageInfo(ENGINE_PACKAGE,
                    PackageManager.GET_SIGNING_CERTIFICATES);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static void confirmInstall(Activity activity, File apk, InspectResult report) {
        if (report.downgrade) {
            new AlertDialog.Builder(activity).setTitle("Eski sürüm")
                    .setMessage("Yüklü motorun eski sürümle üzerine yazılması güvenli değil.")
                    .setPositiveButton("Tamam", null).show();
            return;
        }
        String alreadyPinned = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                .getString(PINNED, "");
        String detail = "Sürüm: " + report.versionName + " (" + report.versionCode + ")" +
                "\nAndroid API: " + report.minSdk +
                "\nSertifika SHA-256: " + report.signerSha256 +
                (alreadyPinned.isEmpty() ? "\n\nİlk kurulum: imzayı güvenilir olarak kaydetmek istiyor musun?" :
                        "\n\nİmza önceki onayınla eşleşiyor.") +
                "\n\nKurulum Android sistem onayı gerektirir.";
        new AlertDialog.Builder(activity)
                .setTitle(report.update ? "Motoru güncelle" : "Motor APK'sını kur")
                .setMessage(detail)
                .setNegativeButton("İptal", null)
                .setPositiveButton("Kurulumu aç", (dialog, which) -> {
                    if (alreadyPinned.isEmpty())
                        activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                                .edit().putString(PINNED, report.signerSha256).apply();
                    openAndroidInstaller(activity, apk);
                }).show();
    }

    private static void openAndroidInstaller(Activity activity, File apk) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Kurulum izni gerekli")
                    .setMessage("Android, bu uygulamanın APK yükleme isteğini onaylamanı istiyor. İzni verdikten sonra Kur'a tekrar bas.")
                    .setNegativeButton("İptal", null)
                    .setPositiveButton("Ayarları aç", (d, w) -> {
                        Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(settings);
                    }).show();
            return;
        }
        Uri uri = FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".apkfiles", apk);
        Intent install = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        install.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        activity.startActivityForResult(install, 501);
    }
}
