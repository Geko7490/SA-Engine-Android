package com.geko7490.saengineandroid;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Imports legally owned PC game data into Unity's future persistentDataPath
 * (getExternalFilesDir(null)/GTASA). Original data is never bundled with the app.
 */
public final class GameDataImporter {
    private static final long MAX_BYTES = 24L * 1024L * 1024L * 1024L;
    private static final int MAX_FILES = 120000;
    private static final String ROOT_NAME = "GTASA";
    private static final String STAGING_NAME = "GTASA-staging";
    private static final String BACKUP_NAME = "GTASA-backup";

    public interface Progress {
        void update(String message);
    }

    private final Context context;
    private final File base;
    private long written;
    private int files;
    private long lastReport;

    public GameDataImporter(Context context) throws IOException {
        this.context = context.getApplicationContext();
        base = this.context.getExternalFilesDir(null);
        if (base == null) throw new IOException("Application external storage is unavailable");
    }

    public File getGameRoot() {
        return new File(base, ROOT_NAME);
    }

    public String getGamePath() {
        return getGameRoot().getAbsolutePath();
    }

    public static boolean isValidRoot(File directory) {
        if (!directory.isDirectory()) return false;
        File models = childIgnoreCase(directory, "models");
        File data = childIgnoreCase(directory, "data");
        return models != null && models.isDirectory() &&
                data != null && data.isDirectory() &&
                models.list() != null && models.list().length > 0 &&
                data.list() != null && data.list().length > 0;
    }

    private static File childIgnoreCase(File parent, String name) {
        File[] children = parent.listFiles();
        if (children == null) return null;
        for (File child : children)
            if (child.getName().equalsIgnoreCase(name)) return child;
        return null;
    }

    public void importZip(Uri uri, Progress progress) throws IOException {
        reset();
        File staging = beginStaging();
        try {
            try (InputStream raw = context.getContentResolver().openInputStream(uri)) {
                if (raw == null) throw new IOException("Could not open selected ZIP");
                try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(raw, 64 * 1024))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        String relative = checkedPath(entry.getName());
                        if (!relative.isEmpty()) {
                            File target = checkedFile(staging, relative);
                            if (entry.isDirectory()) {
                                if (!target.isDirectory() && !target.mkdirs())
                                    throw new IOException("Unable to create directory: " + relative);
                            } else {
                                copyStream(zis, target, progress);
                            }
                        }
                        zis.closeEntry();
                    }
                }
            }
            commit(staging);
            progress.update("Game files imported and verified (" + files + " files)");
        } catch (IOException | RuntimeException error) {
            deleteTree(staging);
            throw error;
        }
    }

    public void importFolder(Uri treeUri, Progress progress) throws IOException {
        reset();
        File staging = beginStaging();
        try {
            ContentResolver resolver = context.getContentResolver();
            String documentId = DocumentsContract.getTreeDocumentId(treeUri);
            Deque<FolderNode> queue = new ArrayDeque<>();
            queue.add(new FolderNode(documentId, ""));
            while (!queue.isEmpty()) {
                FolderNode node = queue.removeFirst();
                Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, node.documentId);
                String[] columns = {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                };
                try (Cursor cursor = resolver.query(childrenUri, columns, null, null, null)) {
                    if (cursor == null) throw new IOException("Cannot read selected folder");
                    while (cursor.moveToNext()) {
                        String id = cursor.getString(0);
                        String name = cursor.getString(1);
                        String mime = cursor.getString(2);
                        String safeName = checkedSegment(name);
                        String relative = node.path.isEmpty() ? safeName : node.path + "/" + safeName;
                        File dest = checkedFile(staging, relative);
                        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                            if (!dest.isDirectory() && !dest.mkdirs())
                                throw new IOException("Cannot create folder: " + relative);
                            queue.addLast(new FolderNode(id, relative));
                        } else {
                            Uri childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
                            try (InputStream input = resolver.openInputStream(childUri)) {
                                if (input == null) throw new IOException("Cannot open: " + relative);
                                copyStream(input, dest, progress);
                            }
                        }
                    }
                }
            }
            commit(staging);
            progress.update("Game folder imported and verified (" + files + " files)");
        } catch (IOException | RuntimeException error) {
            deleteTree(staging);
            throw error;
        }
    }

    private static final class FolderNode {
        final String documentId;
        final String path;
        FolderNode(String documentId, String path) {
            this.documentId = documentId;
            this.path = path;
        }
    }

    private void reset() {
        written = 0L;
        files = 0;
        lastReport = 0L;
    }

    private File beginStaging() throws IOException {
        File staging = new File(base, STAGING_NAME);
        deleteTree(staging);
        if (!staging.mkdirs()) throw new IOException("Cannot prepare temporary import folder");
        return staging;
    }

    private static String checkedSegment(String value) throws IOException {
        if (value == null || value.isEmpty() || value.equals(".") || value.equals("..") ||
                value.contains("/") || value.contains("\\") || value.indexOf('\0') >= 0)
            throw new IOException("Unsafe file or folder name");
        return value;
    }

    private static String checkedPath(String value) throws IOException {
        if (value == null) throw new IOException("Missing ZIP name");
        String path = value.replace('\\', '/');
        if (path.startsWith("/") || path.indexOf('\0') >= 0 || path.contains(":"))
            throw new IOException("Unsafe ZIP entry path");
        StringBuilder result = new StringBuilder();
        for (String part : path.split("/")) {
            if (part.isEmpty()) continue;
            checkedSegment(part);
            if (result.length() > 0) result.append('/');
            result.append(part);
        }
        return result.toString();
    }

    private static File checkedFile(File root, String relative) throws IOException {
        File target = new File(root, relative);
        String basePath = root.getCanonicalPath() + File.separator;
        String fullPath = target.getCanonicalPath();
        if (!fullPath.startsWith(basePath))
            throw new IOException("Unsafe path outside game directory");
        return target;
    }

    private void copyStream(InputStream input, File target, Progress progress) throws IOException {
        if (++files > MAX_FILES) throw new IOException("Too many files in selected data");
        File parent = target.getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs()))
            throw new IOException("Cannot create file directory");
        byte[] buffer = new byte[64 * 1024];
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(target), 64 * 1024)) {
            int n;
            while ((n = input.read(buffer)) != -1) {
                written += n;
                if (written > MAX_BYTES) throw new IOException("Import exceeds 24 GiB safety limit");
                output.write(buffer, 0, n);
                if (written - lastReport > 32L * 1024L * 1024L) {
                    lastReport = written;
                    progress.update("Import: " + files + " files, " + (written / (1024 * 1024)) + " MiB");
                }
            }
        }
    }

    private void commit(File staging) throws IOException {
        File candidate = staging;
        if (!isValidRoot(candidate)) {
            File[] entries = staging.listFiles();
            if (entries != null && entries.length == 1 && entries[0].isDirectory() &&
                    isValidRoot(entries[0])) candidate = entries[0];
        }
        if (!isValidRoot(candidate))
            throw new IOException("Not a PC GTA SA directory: models/ and data/ are required");

        File previous = new File(base, ROOT_NAME);
        File backup = new File(base, BACKUP_NAME);
        deleteTree(backup);
        if (previous.exists() && !previous.renameTo(backup))
            throw new IOException("Cannot make a backup of existing game files");
        if (!candidate.renameTo(previous)) {
            if (backup.exists() && !backup.renameTo(previous))
                throw new IOException("Failed to install; existing game backup at " + backup);
            throw new IOException("Could not commit imported files");
        }
        if (staging.exists()) deleteTree(staging);
        if (backup.exists()) deleteTree(backup);
    }

    private static void deleteTree(File root) throws IOException {
        if (!root.exists()) return;
        if (root.isDirectory()) {
            File[] children = root.listFiles();
            if (children == null) throw new IOException("Cannot read temporary folder");
            for (File child : children) deleteTree(child);
        }
        if (!root.delete()) throw new IOException("Cannot clean temporary path: " + root.getName());
    }
}
