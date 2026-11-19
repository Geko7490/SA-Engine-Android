package com.geko7490.saengineandroid;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/** In-app official source browser. DownloadManager saves the author's own ZIP to Downloads. */
public final class ModSourceActivity extends Activity {
    private WebView browser;
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        final String source = getIntent().getStringExtra("url");
        if (!isSecure(source) || !Uri.parse(source).getHost().endsWith("libertycity.net")) {
            finish(); return;
        }
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xff100d1b);
        TextView heading = new TextView(this);
        heading.setText(getIntent().getStringExtra("title"));
        heading.setTextColor(0xffeeeaff);
        heading.setTextSize(18);
        heading.setPadding(18, 20, 18, 10);
        page.addView(heading);
        TextView note = new TextView(this);
        note.setText("Orijinal mod sayfası. 'Download' düğmesi siteye aittir. İndirilen ZIP'i Mod Center'dan seç.");
        note.setTextColor(0xffbdb2d0);
        note.setPadding(18, 0, 18, 15);
        page.addView(note);
        Button outside = new Button(this);
        outside.setText("SAYFAYI TARAYICIDA AÇ");
        outside.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(source))));
        page.addView(outside);
        browser = new WebView(this);
        browser.setBackgroundColor(Color.WHITE);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        browser.getSettings().setAllowFileAccess(false);
        browser.getSettings().setAllowContentAccess(false);
        browser.setWebChromeClient(new WebChromeClient());
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isSecure(request.getUrl().toString());
            }
        });
        browser.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (!isSecure(url)) {
                Toast.makeText(this, "Güvensiz indirme reddedildi", Toast.LENGTH_LONG).show(); return;
            }
            new AlertDialog.Builder(this).setTitle("Mod ZIP dosyasını indir?")
                .setMessage("Dosya yazarının sunucusundan indirilecek: " + Uri.parse(url).getHost() +
                         "\nOyun sürüm uyumluluğu garanti edilmez.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("İndir", (d, w) -> {
                    try {
                        String suggested = URLUtil.guessFileName(url, contentDisposition, mimeType);
                        if (!suggested.toLowerCase(java.util.Locale.ROOT).endsWith(".zip"))
                            suggested = "gta-sa-graphics-" + System.currentTimeMillis() + ".zip";
                        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                        req.setTitle("GTA SA grafik modu");
                        req.setDescription("Yazar kaynağından indiriliyor");
                        req.setMimeType("application/zip");
                        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, suggested);
                        req.addRequestHeader("User-Agent", userAgent == null ? "SAEngineModCenter" : userAgent);
                        String cookie = CookieManager.getInstance().getCookie(url);
                        if (cookie != null && !cookie.isEmpty())
                            req.addRequestHeader("Cookie", cookie);
                        DownloadManager mgr = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        mgr.enqueue(req);
                        Toast.makeText(this, "İndirme başladı. Sonra Mod Center > ZIP Seç.", Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Toast.makeText(this, "İndirme başlatılamadı: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }).show();
        });
        page.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);
        browser.loadUrl(source);
    }
    private static boolean isSecure(String url) {
        try { return url != null && "https".equalsIgnoreCase(Uri.parse(url).getScheme()) &&
                Uri.parse(url).getHost() != null; }
        catch (Exception e) { return false; }
    }
    @Override public void onBackPressed() {
        if (browser != null && browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (browser != null) browser.destroy();
        super.onDestroy();
    }
}
