package com.geko7490.saengineandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** GTA SA 2.11.311 Mod Center + ADB/Shizuku on-device shell console. */
public final class ModCenterActivity extends Activity {
    private static final int PICK_MOD = 801;
    private static final int BG = 0xff100d1b;
    private static final int PANEL = 0xff221b35;
    private static final int PRIMARY = 0xff955cff;
    private static final int FG = 0xffede7ff;
    private static final int DIM = 0xffbcb1d2;
    private final ExecutorService jobs = Executors.newSingleThreadExecutor();
    private ShizukuBridge bridge;
    private TextView status, archiveInfo, output;
    private EditText command;
    private Button installButton;
    private GraphicsArchive.Selection archive;
    private volatile boolean occupied;

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        bridge = new ShizukuBridge(this, this::refresh);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(30));
        scroll.addView(root);
        setContentView(scroll);

        label(root, "SA ENGINE  /  MOD CENTER", 23, PRIMARY, true);
        label(root, "GTA San Andreas Android 2.11.311  •  Grafik Modları  •  Shizuku / ADB shell", 13, DIM, false);
        LinearLayout conn = card(root);
        label(conn, "CİHAZ & OYUN", 15, PRIMARY, true);
        status = label(conn, "Shizuku bağlantısı kontrol ediliyor", 14, FG, false);
        button(conn, "SHIZUKU'YU BAĞLA / İZİN VER", () -> bridge.connect());
        button(conn, "ADB ERİŞİM TESTİ (SADECE OKU)", this::diagnostics);

        LinearLayout mods = card(root);
        label(mods, "GRAFİK KATALOĞU", 15, PRIMARY, true);
        label(mods, "Kaynak indirmeleri yazarlarının kendi sitelerinden yapılır. Oyun sürümü 2.11.311 ile uyumluluk henüz doğrulanmadı.", 13, DIM, false);
        addMod(mods, "PS2 Graphics v5.0", "PS2 atmosferi / gökyüzü / timecyc + colorcycle",
                "https://libertycity.net/files/gta-san-andreas-ios-android/146765-ps2-graphics-v5.0.html");
        addMod(mods, "Timecyc uuk ENB (2026)", "Gün batımı / kontrast / gökyüzü renkleri (timecyc)",
                "https://libertycity.net/files/gta-san-andreas-ios-android/242422-timecyc-uuk-enb.html");
        button(mods, "İNDİRDİĞİN MOD ZIP'İNİ SEÇ", this::pickArchive);
        archiveInfo = label(mods, "Henüz mod ZIP seçilmedi", 13, DIM, false);
        installButton = button(mods, "GTA SA'YA UYARLA • YEDEKLE VE KUR", this::installConfirm);
        installButton.setEnabled(false);
        button(mods, "ORİJİNAL GRAFİK DOSYALARINI GERİ YÜKLE", this::restoreConfirm);

        LinearLayout shellBox = card(root);
        label(shellBox, "ADB SHELL KONSOLU", 15, PRIMARY, true);
        label(shellBox, "Shizuku başladığında komutlar PC olmadan cihazda shell UID ile çalışır. Root yetkisi verilmez. İstediğin komutu yalnızca sen onayladıktan sonra çalıştırır.", 13, DIM, false);
        command = new EditText(this);
        command.setHint("Örnek: id  veya  pm path com.rockstargames.gtasa");
        command.setTextColor(FG);
        command.setHintTextColor(DIM);
        command.setTextSize(13);
        command.setMinLines(2);
        command.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        command.setBackground(bg(0xff302342));
        command.setPadding(dp(12), dp(10), dp(12), dp(10));
        shellBox.addView(command, new LinearLayout.LayoutParams(-1, -2));
        button(shellBox, "SHELL KOMUTUNU ÇALIŞTIR", this::shellConfirm);
        output = label(shellBox, "Konsol hazır. Önce Shizuku bağlantısı kurulmalı.", 12, FG, false);
        output.setTextIsSelectable(true);
        refresh();
    }
    private void addMod(LinearLayout root, String name, String desc, String url) {
        LinearLayout row = cardInline(root);
        label(row, name, 15, FG, true);
        label(row, desc, 12, DIM, false);
        button(row, "KAYNAĞINDAN İNDİR", () -> {
            Intent web = new Intent(this, ModSourceActivity.class);
            web.putExtra("url", url);
            web.putExtra("title", name);
            startActivity(web);
        });
    }
    private String gameVersion() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo("com.rockstargames.gtasa", 0);
            return info.versionName == null ? "?" : info.versionName;
        } catch (Exception ignored) { return "Kurulu değil"; }
    }
    private void refresh() {
        if (status == null || installButton == null) return;
        String v = gameVersion();
        status.setText("GTA SA: " + v + "\nShizuku: " + bridge.status() +
                "\nADB shell: " + (bridge.ready() ? "Bağlandı" : "Bağlı değil"));
        installButton.setEnabled(!occupied && bridge.ready() && archive != null);
        installButton.setAlpha(installButton.isEnabled() ? 1 : .45f);
    }
    private void diagnostics() {
        if (!bridge.ready()) { output.setText("Önce Shizuku'yu başlatıp bağla"); return; }
        perform("Dizin ve paket yetkileri kontrol ediliyor...", () -> GraphicsInstaller.diagnostics(bridge));
    }
    private void shellConfirm() {
        if (!bridge.ready()) { output.setText("Shizuku bağlantısı yok"); return; }
        String entered = command.getText().toString().trim();
        if (entered.startsWith("adb shell ")) entered = entered.substring(10);
        if (entered.isEmpty()) { output.setText("Komut boş"); return; }
        final String finalCommand = entered;
        new AlertDialog.Builder(this).setTitle("ADB shell komutunu çalıştır?")
                .setMessage(finalCommand + "\n\nBu komut shell kullanıcı yetkisiyle çalışacak. Dosyaları değiştirebilir; yalnızca güvendiğin komutları çalıştır.")
                .setNegativeButton("İptal", null)
                .setPositiveButton("ÇALIŞTIR", (d, w) ->
                        perform("$ " + finalCommand, () -> bridge.exec(finalCommand))).show();
    }
    private void pickArchive() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_MOD);
    }
    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != PICK_MOD || result != RESULT_OK || data == null || data.getData() == null) return;
        final Uri uri = data.getData();
        perform("ZIP analiz ediliyor...", () -> {
            archive = GraphicsArchive.fromZip(this, uri);
            runOnUiThread(() -> {
                archiveInfo.setText("Hazır: " + android.text.TextUtils.join(", ", archive.names) +
                        "  (" + archive.bytes + " bayt)");
                refresh();
            });
            return "Mod dosyaları bulundu: " + android.text.TextUtils.join(", ", archive.names) +
                "\nNot: Bunların 2.11.311 tarafından okunacağı henüz doğrulanmış değil.";
        });
    }
    private void installConfirm() {
        if (archive == null || !bridge.ready()) return;
        if (!"2.11.311".equals(gameVersion())) {
            output.setText("Kurulum durduruldu: oyun sürümü GTA SA 2.11.311 olmalı.");
            return;
        }
        new AlertDialog.Builder(this).setTitle("Grafik modunu yükle?")
                .setMessage("2.11.311 mod uyumluluğu doğrulanmadı. Yalnızca oyun dışındaki /Android/data/.../files/data dosyaları değiştirilecek. HEDEF DOSYALAR MEVCUT DEĞİLSE DEĞİŞİKLİK YAPILMAZ. Orijinal dosyalar önce yedeklenecek. Oyunu tamamen kapat.\n\nMod: " +
                    android.text.TextUtils.join(", ", archive.names))
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("YEDEKLE VE KUR", (d, w) ->
                    perform("Yedekleme ve kurulum...", () -> GraphicsInstaller.install(this, bridge, archive)))
                .show();
    }
    private void restoreConfirm() {
        if (!bridge.ready()) { output.setText("Geri yüklemek için Shizuku gerekli"); return; }
        if (!GraphicsInstaller.hasActiveBackup(this)) { output.setText("Aktif yedek bulunamadı"); return; }
        new AlertDialog.Builder(this).setTitle("Orijinal dosyaları geri yükle?")
                .setMessage("Önce GTA SA'yı kapat. Modun değiştirdiği dosyalar yedekten geri yüklenecek.")
                .setNegativeButton("İptal", null)
                .setPositiveButton("GERİ YÜKLE", (d, w) ->
                        perform("Orijinal grafikler geri yükleniyor...", () ->
                                GraphicsInstaller.restore(this, bridge))).show();
    }
    interface Task { String run() throws Exception; }
    private void perform(String message, Task task) {
        if (occupied) return;
        occupied = true;
        output.setText(message);
        refresh();
        jobs.execute(() -> {
            String messageResult;
            try { messageResult = task.run(); }
            catch (Exception e) { messageResult = "HATA: " + e.getClass().getSimpleName() + ": " + e.getMessage(); }
            final String result = messageResult;
            runOnUiThread(() -> {
                occupied = false;
                output.setText(result);
                refresh();
            });
        });
    }
    private GradientDrawable bg(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(5));
        d.setStroke(dp(1), 0xff45315f);
        return d;
    }
    private LinearLayout card(LinearLayout root) {
        LinearLayout box = cardInline(root);
        return box;
    }
    private LinearLayout cardInline(LinearLayout root) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(12));
        box.setBackground(bg(PANEL));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(12);
        root.addView(box, p);
        return box;
    }
    private TextView label(LinearLayout root, String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setPadding(0, dp(3), 0, dp(9));
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(v);
        return v;
    }
    private Button button(LinearLayout root, String title, Runnable onClick) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(FG);
        b.setBackground(bg(PRIMARY));
        b.setTextSize(13);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(47));
        params.bottomMargin = dp(7);
        root.addView(b, params);
        b.setOnClickListener(v -> onClick.run());
        return b;
    }
    @Override protected void onDestroy() {
        bridge.close();
        jobs.shutdownNow();
        super.onDestroy();
    }
}
