package com.geko7490.saengineandroid;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** V0.3: resumable, bounded, SHA-256-verified HTTPS release asset downloader. */
public final class ResumableDownloader {
    private static final long MAX_APK_BYTES = 4L * 1024 * 1024 * 1024;
    private final Context context;
    private final AtomicBoolean cancel = new AtomicBoolean();

    public interface Listener {
        void onProgress(long bytes, long totalBytes, double bytesPerSecond, long etaSeconds);
    }

    public ResumableDownloader(Context context) { this.context = context.getApplicationContext(); }
    public void cancel() { cancel.set(true); }

    public File cachedFile(ReleaseCatalog.PackageEntry entry) throws IOException {
        File dir = new File(context.getCacheDir(), "engine-downloads");
        if (!dir.isDirectory() && !dir.mkdirs())
            throw new IOException("İndirme klasörü oluşturulamadı");
        return new File(dir, entry.id + "-" + entry.digest + (entry.isFpsPlugin() ? ".so" : ".apk"));
    }

    public File download(ReleaseCatalog.PackageEntry entry, String token, Listener listener) throws Exception {
        cancel.set(false);
        if (entry.size <= 0 || entry.size > MAX_APK_BYTES)
            throw new IOException("İndirme boyutu sınır dışında");
        File destination = cachedFile(entry);
        if (destination.isFile()) {
            if (destination.length() == entry.size && sha256(destination).equalsIgnoreCase(entry.digest))
                return destination;
            if (!destination.delete()) throw new IOException("Eski paket temizlenemedi");
        }
        File partial = new File(destination.getParentFile(), destination.getName() + ".part");
        long from = partial.isFile() ? partial.length() : 0;
        if (from > entry.size) {
            if (!partial.delete()) throw new IOException("Bozuk kısmi dosya silinemedi");
            from = 0;
        }
        if (from == entry.size) {
            if (sha256(partial).equalsIgnoreCase(entry.digest)) {
                if (!partial.renameTo(destination)) throw new IOException("Tamamlanan dosya taşınamadı");
                return destination;
            }
            if (!partial.delete()) throw new IOException("Bozuk indirme silinemedi");
            from = 0;
        }

        HttpURLConnection connection = openAsset(entry.apiUrl, token, from);
        try {
            int status = connection.getResponseCode();
            if (status != 200 && status != 206)
                throw new IOException("GitHub indirme HTTP " + status);
            boolean append = status == 206 && from > 0;
            if (append) {
                String range = connection.getHeaderField("Content-Range");
                if (range == null || !range.startsWith("bytes " + from + "-"))
                    throw new IOException("Sunucu beklenen aralıktan devam etmedi");
            }
            if (status == 200) from = 0;
            long total = entry.size;
            long startedBytes = from;
            long startedAt = System.nanoTime();
            long lastNotify = 0;
            try (InputStream input = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(partial, append)) {
                byte[] data = new byte[128 * 1024];
                int n;
                while ((n = input.read(data)) != -1) {
                    if (cancel.get()) throw new DownloadCancelled();
                    if (n == 0) continue;
                    from += n;
                    if (from > total || from > MAX_APK_BYTES) throw new IOException("Beklenmeyen APK veri uzunluğu");
                    out.write(data, 0, n);
                    long now = System.nanoTime();
                    if (listener != null && (now - lastNotify >= 350_000_000L || from == total)) {
                        double elapsed = Math.max(0.001, (now - startedAt) / 1_000_000_000.0);
                        double speed = (from - startedBytes) / elapsed;
                        listener.onProgress(from, total, speed,
                                speed < 1 ? -1 : (long) ((total - from) / speed));
                        lastNotify = now;
                    }
                }
                out.getFD().sync();
            }
            if (from != total || partial.length() != total)
                throw new IOException("Eksik veri: " + from + "/" + total);
            // Reject tampered or corrupted payloads, including resumed portions.
            if (!sha256(partial).equalsIgnoreCase(entry.digest)) {
                if (!partial.delete()) throw new IOException("Hash uyuşmadı, bozuk dosya silinemedi");
                throw new IOException("SHA-256 doğrulaması başarısız");
            }
            if (!partial.renameTo(destination))
                throw new IOException("APK doğrulandı ama final dosyaya taşınamadı");
            return destination;
        } finally {
            connection.disconnect();
        }
    }

    private static HttpURLConnection openAsset(String url, String token, long offset) throws IOException {
        if (!url.startsWith("https://api.github.com/repos/Geko7490/SA-Engine-Android/releases/assets/"))
            throw new IOException("Geçersiz release asset adresi");
        URL target = new URL(url);
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!"https".equalsIgnoreCase(target.getProtocol()))
                throw new IOException("Güvensiz indirme yönlendirmesi");
            String host = target.getHost().toLowerCase(Locale.ROOT);
            boolean isApi = host.equals("api.github.com");
            // GitHub releases sometimes redirect to storage endpoints.
            boolean isStorage = host.equals("objects.githubusercontent.com") ||
                    host.equals("release-assets.githubusercontent.com");
            if (!isApi && !isStorage) throw new IOException("Onaysız indirme sunucusu: " + host);
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(35000);
            connection.setRequestProperty("User-Agent", "SAEngineAndroidLauncher");
            connection.setRequestProperty("Accept", isApi ? "application/octet-stream" : "*/*");
            if (isApi && token != null && !token.trim().isEmpty())
                connection.setRequestProperty("Authorization", "Bearer " + token.trim());
            if (offset > 0) connection.setRequestProperty("Range", "bytes=" + offset + "-");
            int response = connection.getResponseCode();
            if (response >= 300 && response <= 308 && response != 304) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null) throw new IOException("Redirect location bulunamadı");
                target = new URL(target, location);
                continue;
            }
            if (response == 200 && isApi) {
                String contentType = connection.getContentType();
                if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) {
                    connection.disconnect();
                    throw new IOException("GitHub APK yerine JSON döndürdü; private repo için token erişimini denetle");
                }
            }
            return connection;
        }
        throw new IOException("Çok fazla indirme yönlendirmesi");
    }

    private static String sha256(File source) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(source)) {
            byte[] buffer = new byte[128 * 1024]; int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest())
            result.append(String.format(Locale.US, "%02x", value & 0xff));
        return result.toString();
    }

    public static final class DownloadCancelled extends IOException {
        public DownloadCancelled() { super("İndirme iptal edildi; kalan kısım sonra devam ettirilebilir"); }
    }
}
