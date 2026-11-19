package com.geko7490.saengineandroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** V0.2-0.4: catalogue, resumable download and verified Android installer UI. */
public final class PackageCenterActivity extends Activity {
    private static final int REQUEST_FPS_MOD_FOLDER = 444;
    private static final int BACKGROUND = Color.rgb(12, 10, 20);
    private static final int CARD = Color.rgb(27, 21, 42);
    private static final int PURPLE = Color.rgb(148, 97, 253);
    private static final int TEXT = Color.rgb(237, 233, 255);
    private static final int MUTED = Color.rgb(174, 168, 192);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ResumableDownloader[] active = new ResumableDownloader[1];
    private LinearLayout content, packages;
    private EditText tokenInput;
    private TextView status;
    private volatile boolean busy;
    private String sessionToken = "";
    private File pendingFpsPlugin;
    private TextView pendingFpsStatus;

    private int dp(float x) { return (int) (x * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BACKGROUND);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(22), dp(18), dp(30));
        scroll.addView(content);
        setContentView(scroll);

        title(content, "PAKET MERKEZİ", 25, PURPLE);
        label(content, "V0.2 katalog  •  V0.3 indirme  •  V0.4 güvenli kurulum", 12, MUTED);
        label(content, "Kaynak: Geko7490 / SA-Engine-Android (GitHub Releases)", 14, TEXT);
        label(content, "Depo PRIVATE: Salt-okuma yetkili, yalnız bu repo için oluşturulmuş token kullan. Token telefona kaydedilmez.", 13, MUTED);

        tokenInput = new EditText(this);
        tokenInput.setTextColor(TEXT);
        tokenInput.setHintTextColor(MUTED);
        tokenInput.setSingleLine(true);
        tokenInput.setTextSize(14);
        tokenInput.setHint("GitHub read-only token (isteğe bağlı)");
        tokenInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        tokenInput.setBackground(shape(CARD));
        content.addView(tokenInput, new LinearLayout.LayoutParams(-1, dp(50)));
        label(content, "Sadece sürümleri listelemek için token giriş alanı. Şifreyi APK'ya veya GitHub'a kod olarak ekleme.", 11, MUTED);

        addAction(content, "SÜRÜMLERİ YÜKLE", this::loadReleases);
        status = label(content, "Henüz sorgulanmadı.", 13, MUTED);
        packages = new LinearLayout(this);
        packages.setOrientation(LinearLayout.VERTICAL);
        content.addView(packages);
        addAction(content, "GERİ", this::finish);
    }

    private void loadReleases() {
        if (busy) return;
        sessionToken = tokenInput.getText().toString().trim();
        // Remove password from input immediately after capturing it for this activity.
        tokenInput.setText("");
        busy = true;
        status.setText("GitHub Releases kontrol ediliyor...");
        packages.removeAllViews();
        final String token = sessionToken;
        worker.execute(() -> {
            try {
                List<ReleaseCatalog.PackageEntry> found = ReleaseCatalog.fetch(token);
                runOnUiThread(() -> {
                    busy = false;
                    status.setText(found.isEmpty() ? "Motor paketi bulunamadı. Henüz oynanabilir motor release'i yayınlanmamış olabilir." :
                            found.size() + " doğrulanabilir paket bulundu.");
                    for (ReleaseCatalog.PackageEntry entry : found) renderEntry(entry);
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    busy = false;
                    status.setText("Katalog hatası: " + ex.getMessage());
                });
            }
        });
    }

    private void renderEntry(ReleaseCatalog.PackageEntry item) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(14));
        panel.setBackground(shape(CARD));
        LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, -2);
        margins.bottomMargin = dp(12);
        packages.addView(panel, margins);

        title(panel, item.isEngine() ? "OYUN MOTORU" :
                item.isFpsPlugin() ? "GTA SA FPS MODÜLÜ • ARM64" : "LAUNCHER ÖNİZLEME", 15, PURPLE);
        label(panel, item.tag + "  •  " + item.readableSize(), 14, TEXT);
        label(panel, item.name + "\n" + (item.isFpsPlugin() ?
                "GTA SA Android 2.10 ARM64 (2.11 deneysel) + AML/SAUtils gerekir" : item.androidCompatibility()) +
                (item.prerelease ? "  •  Test sürümü" : ""), 12, MUTED);
        if (!item.notes.isEmpty()) label(panel,
                item.notes.length() > 380 ? item.notes.substring(0, 380) + "…" : item.notes,
                12, MUTED);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        panel.addView(bar, new LinearLayout.LayoutParams(-1, dp(8)));
        TextView progress = label(panel, "İndirme hazır.", 12, MUTED);
        Button download = addAction(panel, item.isEngine() ? "İNDİR VE DOĞRULA" :
                item.isFpsPlugin() ? "FPS MODUNU İNDİR VE KUR" : "TEST APK'SINI İNDİR", null);
        Button cancel = addAction(panel, "İPTAL ET", null);
        cancel.setEnabled(false);
        cancel.setAlpha(.45f);

        download.setOnClickListener(v -> {
            if (busy) return;
            busy = true;
            download.setEnabled(false);
            cancel.setEnabled(true);
            cancel.setAlpha(1f);
            final String token = sessionToken;
            ResumableDownloader downloader = new ResumableDownloader(this);
            active[0] = downloader;
            worker.execute(() -> {
                try {
                    File file = downloader.download(item, token, (done, total, speed, eta) ->
                            runOnUiThread(() -> {
                                int permille = (int) (1000d * done / Math.max(1L, total));
                                bar.setProgress(permille);
                                String etaText = eta < 0 ? "--" : (eta / 60) + "dk " + (eta % 60) + "sn";
                                progress.setText(String.format(Locale.US,
                                        "%.1f/%.1f MB  •  %.1f MB/sn  •  %s",
                                        done / 1048576.0, total / 1048576.0,
                                        speed / 1048576.0, etaText));
                            }));
                    runOnUiThread(() -> {
                        progress.setText("SHA-256 doğrulandı: " + file.getName());
                        bar.setProgress(1000);
                        if (item.isEngine()) {
                            try {
                                EngineInstaller.InspectResult inspected = EngineInstaller.inspect(this, file);
                                EngineInstaller.confirmInstall(this, file, inspected);
                            } catch (Exception ex) {
                                progress.setText("Kurulum reddedildi: " + ex.getMessage());
                            }
                        } else if (item.isFpsPlugin()) {
                            pendingFpsPlugin = file;
                            pendingFpsStatus = progress;
                            progress.setText("Mod doğrulandı. GTA SA AML mods klasörünü seç.");
                            Intent folder = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                            folder.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                            startActivityForResult(folder, REQUEST_FPS_MOD_FOLDER);
                        } else {
                            progress.setText("Launcher test APK indirildi ve doğrulandı. Bu, GTA SA oyun motoru değildir.");
                        }
                    });
                } catch (Exception ex) {
                    runOnUiThread(() -> progress.setText("İndirme sorunu: " + ex.getMessage()));
                } finally {
                    runOnUiThread(() -> {
                        busy = false;
                        active[0] = null;
                        download.setEnabled(true);
                        cancel.setEnabled(false);
                        cancel.setAlpha(.45f);
                    });
                }
            });
        });
        cancel.setOnClickListener(v -> {
            ResumableDownloader running = active[0];
            if (running != null) running.cancel();
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FPS_MOD_FOLDER) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            if (pendingFpsStatus != null) pendingFpsStatus.setText("Mod kurulumu iptal edildi.");
            pendingFpsPlugin = null;
            return;
        }
        final Uri folder = data.getData();
        final File verified = pendingFpsPlugin;
        final TextView statusView = pendingFpsStatus;
        pendingFpsPlugin = null;
        try {
            int flags = data.getFlags() &
                    (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(folder, flags);
        } catch (SecurityException ignored) {
            // A current grant is still enough for immediate installation.
        }
        if (verified == null) return;
        int requestedFps = getSharedPreferences("sa_engine_settings", MODE_PRIVATE).getInt("fps", 60);
        worker.execute(() -> {
            try {
                NativeFpsModInstaller.install(this, folder, verified, requestedFps);
                runOnUiThread(() -> {
                    if (statusView != null)
                        statusView.setText("FPS mod dosyası ve " + requestedFps +
                            " FPS ayarı AML mods klasörüne kopyalandı. AML/SAUtils kuruluysa GTA SA'yı yeniden başlat.");
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    if (statusView != null) statusView.setText("Mod kurulum hatası: " + ex.getMessage());
                });
            }
        });
    }

    @Override protected void onDestroy() {
        if (active[0] != null) active[0].cancel();
        worker.shutdownNow();
        sessionToken = "";
        super.onDestroy();
    }

    private GradientDrawable shape(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(5));
        drawable.setStroke(dp(1), Color.rgb(62, 46, 89));
        return drawable;
    }
    private TextView title(LinearLayout parent, String text, int size, int color) {
        TextView view = label(parent, text, size, color);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }
    private TextView label(LinearLayout parent, String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setPadding(0, dp(5), 0, dp(7));
        parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }
    private Button addAction(LinearLayout parent, String title, Runnable action) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setBackground(shape(PURPLE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(46));
        params.bottomMargin = dp(7);
        parent.addView(b, params);
        if (action != null) b.setOnClickListener(v -> action.run());
        return b;
    }
}
