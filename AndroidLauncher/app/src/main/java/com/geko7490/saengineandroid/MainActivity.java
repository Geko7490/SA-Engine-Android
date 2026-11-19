package com.geko7490.saengineandroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is an actual Android launcher and game-data importer, NOT a playable GTA SA build.
 * The Unity game engine is not yet included.
 */
public final class MainActivity extends Activity {
    private static final int PICK_ZIP = 101;
    private static final int PICK_DIR = 102;
    private static final int BG = Color.rgb(12, 10, 20);
    private static final int PANEL = Color.rgb(25, 21, 39);
    private static final int PURPLE = Color.rgb(147, 94, 250);
    private static final int TEXT = Color.rgb(242, 238, 255);
    private static final int MUTED = Color.rgb(177, 171, 195);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Button> inputButtons = new ArrayList<>();
    private final List<Button> fpsButtons = new ArrayList<>();
    private final List<Button> qualityButtons = new ArrayList<>();
    private LinearLayout content;
    private TextView status;
    private TextView importText;
    private TextView settingsText;
    private boolean importing;
    private int fps;
    private int quality;

    private int dp(float v) { return (int) (getResources().getDisplayMetrics().density * v + .5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        fps = getPreferences(MODE_PRIVATE).getInt("fps", 60);
        quality = getPreferences(MODE_PRIVATE).getInt("quality", 1);
        saveSettingsForUnity();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(25), dp(18), dp(30));
        scroll.addView(content);
        setContentView(scroll);

        text("SA ENGINE", 29, PURPLE, true);
        text("MOBILE  /  0.1 PREVIEW", 13, MUTED, true);
        space(18);

        LinearLayout stateCard = card();
        label(stateCard, "MOTOR DURUMU", 13, PURPLE, true);
        label(stateCard, "Android launcher hazır", 21, TEXT, true);
        label(stateCard, "Unity oyun motoru APK'ya henüz entegre edilmedi. Bu sürüm oyun dosyalarını güvenli biçimde içe aktarır.", 14, MUTED, false);
        space(12);

        LinearLayout gameCard = card();
        label(gameCard, "GTA SAN ANDREAS DOSYALARI", 14, PURPLE, true);
        status = label(gameCard, "", 16, TEXT, true);
        importText = label(gameCard, "Kendi PC GTA SA oyun dosyalarını ZIP veya klasör olarak seç.", 13, MUTED, false);
        Button zip = action(gameCard, "ZIP DOSYASI SEÇ", () -> pickZip(), false);
        Button folder = action(gameCard, "KLASÖR SEÇ", () -> pickFolder(), false);
        Button verify = action(gameCard, "DOSYALARI KONTROL ET", () -> updateGameStatus(), true);
        inputButtons.add(zip);
        inputButtons.add(folder);
        inputButtons.add(verify);

        space(12);
        LinearLayout performance = card();
        label(performance, "PERFORMANS ÖN AYARLARI", 14, PURPLE, true);
        settingsText = label(performance, "", 14, TEXT, false);
        label(performance, "Hedef FPS", 12, MUTED, true);
        LinearLayout fpsRow = row(performance);
        for (int number : new int[]{30, 60, 90, 120}) {
            final int selected = number;
            fpsButtons.add(button(fpsRow, String.valueOf(number), number == fps, () -> {
                fps = selected;
                persistSettings();
            }));
        }
        label(performance, "Grafik kalitesi", 12, MUTED, true);
        LinearLayout qualityRow = row(performance);
        String[] labels = {"DÜŞÜK", "ORTA", "YÜKSEK"};
        for (int i = 0; i < labels.length; i++) {
            final int selected = i;
            qualityButtons.add(button(qualityRow, labels[i], i == quality, () -> {
                quality = selected;
                persistSettings();
            }));
        }
        label(performance, "Ayarlar gelecekte Unity motoru tarafından okunacak. Bu APK oyun içi FPS testi yapmaz.", 12, MUTED, false);

        space(12);
        LinearLayout info = card();
        label(info, "CİHAZ", 13, PURPLE, true);
        label(info, Build.MANUFACTURER + " " + Build.MODEL + "  |  Android " + Build.VERSION.RELEASE,
                13, TEXT, false);
        label(info, "Özel dosya erişim izni istemez. Android sistem dosya seçicisi kullanılır.", 12, MUTED, false);

        space(18);
        Button launch = action(content, "OYUN MOTORU  •  HENÜZ HAZIR DEĞİL", null, true);
        launch.setEnabled(false);
        launch.setAlpha(0.60f);
        text("Unofficial project. Rockstar / Take-Two ile bağlantılı değildir. Oyun verileri APK içinde yer almaz.",
                12, MUTED, false);
        updateGameStatus();
        refreshSettingsText();
    }

    private void pickZip() {
        if (importing) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_ZIP);
    }

    private void pickFolder() {
        if (importing) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_DIR);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request != PICK_ZIP && request != PICK_DIR) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // The temporary URI permission still allows this immediate import.
        }
        beginImport(uri, request == PICK_ZIP);
    }

    private void beginImport(Uri uri, boolean zip) {
        if (importing) return;
        importing = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        for (Button b : inputButtons) b.setEnabled(false);
        importText.setText("İçe aktarma başladı. Ekranı açık tut ve uygulamayı kapatma.");
        worker.execute(() -> {
            String problem = null;
            try {
                GameDataImporter importer = new GameDataImporter(this);
                if (zip) importer.importZip(uri, msg -> runOnUiThread(() -> importText.setText(msg)));
                else importer.importFolder(uri, msg -> runOnUiThread(() -> importText.setText(msg)));
            } catch (Exception error) {
                problem = error.getMessage();
                if (problem == null) problem = error.getClass().getSimpleName();
            }
            final String finalProblem = problem;
            runOnUiThread(() -> {
                importing = false;
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                for (Button b : inputButtons) b.setEnabled(true);
                updateGameStatus();
                importText.setText(finalProblem == null
                        ? "İçe aktarma tamamlandı. Oyun verileri hazır."
                        : "Aktarım hatası: " + finalProblem);
            });
        });
    }

    private void updateGameStatus() {
        try {
            GameDataImporter importer = new GameDataImporter(this);
            status.setText(GameDataImporter.isValidRoot(importer.getGameRoot())
                    ? "✓ Dosyalar bulundu ve temel klasörler doğrulandı"
                    : "— GTA SA oyun dosyaları henüz yüklenmedi");
        } catch (Exception ex) {
            status.setText("Depolama erişilemiyor: " + ex.getMessage());
        }
    }

    private void persistSettings() {
        getPreferences(MODE_PRIVATE).edit().putInt("fps", fps).putInt("quality", quality).apply();
        saveSettingsForUnity();
        refreshSettingsText();
        recreateChoices();
    }

    private void refreshSettingsText() {
        if (settingsText != null) {
            String qualityName = new String[]{"Düşük", "Orta", "Yüksek"}[Math.max(0, Math.min(quality, 2))];
            settingsText.setText(fps + " FPS hedefi  •  " + qualityName);
        }
    }

    private void recreateChoices() {
        int[] modes = {30, 60, 90, 120};
        for (int i = 0; i < fpsButtons.size(); i++) {
            boolean selected = modes[i] == fps;
            fpsButtons.get(i).setBackground(box(selected ? PURPLE : Color.rgb(49, 40, 66), 0, 0));
        }
        for (int i = 0; i < qualityButtons.size(); i++) {
            boolean selected = i == quality;
            qualityButtons.get(i).setBackground(box(selected ? PURPLE : Color.rgb(49, 40, 66), 0, 0));
        }
    }

    private void saveSettingsForUnity() {
        try {
            File root = getExternalFilesDir(null);
            if (root == null) return;
            JSONObject json = new JSONObject();
            json.put("fps", fps);
            json.put("quality", quality);
            json.put("schemaVersion", 1);
            try (FileOutputStream out = new FileOutputStream(new File(root, "sa_engine_mobile_settings.json"))) {
                out.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) { }
    }

    private LinearLayout card() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(box(PANEL, 1, Color.rgb(54, 43, 82)));
        content.addView(panel, new LinearLayout.LayoutParams(-1, -2));
        return panel;
    }

    private GradientDrawable box(int color, int strokeDp, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(5));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), strokeColor);
        return d;
    }

    private void space(int size) {
        View v = new View(this);
        content.addView(v, new LinearLayout.LayoutParams(1, dp(size)));
    }

    private TextView text(String contentText, int size, int color, boolean bold) {
        return label(content, contentText, size, color, bold);
    }

    private TextView label(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(9);
        parent.addView(view, p);
        return view;
    }

    private LinearLayout row(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(49)));
        return row;
    }

    private Button button(LinearLayout parent, String label, boolean selected, Runnable action) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(selected ? Color.WHITE : TEXT);
        b.setBackground(box(selected ? PURPLE : Color.rgb(49, 40, 66), 0, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(41), 1f);
        p.setMargins(dp(2), 0, dp(2), 0);
        parent.addView(b, p);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private Button action(LinearLayout parent, String label, Runnable callback, boolean outline) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextColor(TEXT);
        b.setTextSize(13);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setBackground(box(outline ? Color.rgb(48, 37, 67) : PURPLE, 0, 0));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(46));
        p.bottomMargin = dp(7);
        parent.addView(b, p);
        if (callback != null) b.setOnClickListener(v -> callback.run());
        return b;
    }
}
