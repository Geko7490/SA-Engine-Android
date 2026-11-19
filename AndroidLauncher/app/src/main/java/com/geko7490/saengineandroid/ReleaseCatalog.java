package com.geko7490.saengineandroid;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** V0.2 GitHub Release package catalogue. No embedded GitHub credentials. */
public final class ReleaseCatalog {
    public static final String OWNER = "Geko7490";
    public static final String REPOSITORY = "SA-Engine-Android";
    private static final String API = "https://api.github.com/repos/" + OWNER + "/" + REPOSITORY;
    private static final int MAX_RESPONSE = 2 * 1024 * 1024;

    public static final class PackageEntry {
        public final String tag, name, notes, kind, digest, apiUrl;
        public final long id, size;
        public final int minAndroid;
        public final boolean prerelease;

        PackageEntry(String tag, String name, String notes, String kind, String digest,
                     String apiUrl, long id, long size, int minAndroid, boolean prerelease) {
            this.tag = tag; this.name = name; this.notes = notes;
            this.kind = kind; this.digest = digest; this.apiUrl = apiUrl;
            this.id = id; this.size = size; this.minAndroid = minAndroid;
            this.prerelease = prerelease;
        }

        public boolean isEngine() { return "engine".equals(kind); }
        public boolean isFpsPlugin() { return "fps-plugin".equals(kind); }
        public String readableSize() {
            return String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0));
        }
        public String androidCompatibility() {
            return minAndroid > 0 ? "Android API " + minAndroid + "+" : "APK kontrolünde belirlenecek";
        }
    }

    public static List<PackageEntry> fetch(String token) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(API + "/releases?per_page=30").openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(25000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", "SAEngineAndroidLauncher");
        if (token != null && !token.trim().isEmpty())
            connection.setRequestProperty("Authorization", "Bearer " + token.trim());
        try {
            int status = connection.getResponseCode();
            if (status == 401 || status == 403 || status == 404)
                throw new IOException("GitHub erişimi reddedildi. Private depo için read-only Contents izinli token gerekli (HTTP " + status + ").");
            if (status != 200) throw new IOException("GitHub API yanıtı: HTTP " + status);
            byte[] bytes;
            try (InputStream stream = connection.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = stream.read(buffer)) != -1) {
                    if (out.size() + n > MAX_RESPONSE) throw new IOException("Release listesi çok büyük");
                    out.write(buffer, 0, n);
                }
                bytes = out.toByteArray();
            }
            JSONArray releases = new JSONArray(new String(bytes, StandardCharsets.UTF_8));
            return parseReleases(releases);
        } finally {
            connection.disconnect();
        }
    }

    /** Parse an offline fixture or GitHub API response without any network calls. */
    public static List<PackageEntry> parseReleases(JSONArray releases) throws JSONException {
        List<PackageEntry> results = new ArrayList<>();
                    for (int i = 0; i < releases.length(); i++) {
                        JSONObject release = releases.getJSONObject(i);
                        if (release.optBoolean("draft")) continue;
                        String tag = release.optString("tag_name", "");
                        String notes = release.optString("body", "");
                        int minAndroid = 0;
                        // Only explicit metadata is displayed; never invent compatibility.
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("minAndroidApi=(\\d+)").matcher(notes);
                        if (m.find()) {
                            try { minAndroid = Integer.parseInt(m.group(1)); } catch (NumberFormatException ignored) { }
                        }
                        JSONArray assets = release.optJSONArray("assets");
                        if (assets == null) continue;
                        for (int a = 0; a < assets.length(); a++) {
                            JSONObject asset = assets.getJSONObject(a);
                            String name = asset.optString("name", "");
                            String kind;
                            if (name.startsWith("SAEngineGame-") && name.endsWith(".apk")) kind = "engine";
                            else if (tag.equals("native-fps-dev") && name.equals("libSAEngineFPS.so")) kind = "fps-plugin";
                            else if (tag.equals("launcher-dev") && (name.equals("app-debug.apk") ||
                                    name.equals("SAEngineMobile-Launcher-debug.apk"))) kind = "launcher-preview";
                            else continue;
                            String digest = asset.optString("digest", "");
                            if (!digest.matches("sha256:[0-9a-fA-F]{64}")) continue;
                            long id = asset.optLong("id", 0);
                            long size = asset.optLong("size", 0);
                            String apiUrl = asset.optString("url", "");
                            if (id <= 0 || size <= 0 || !apiUrl.equals(API + "/releases/assets/" + id))
                                continue;
                            results.add(new PackageEntry(tag, name, notes, kind,
                                    digest.substring(7).toLowerCase(Locale.ROOT), apiUrl,
                                    id, size, minAndroid, release.optBoolean("prerelease")));
                        }
                    }
                    return results;
    }

    private ReleaseCatalog() { }
}
