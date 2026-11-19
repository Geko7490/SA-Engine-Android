package com.geko7490.saenginegame;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

/** Companion half of the launcher's read-only file bridge (no game data bundled). */
public final class GameDataReceiver {
    private static final long MAX_SIZE = 24L * 1024 * 1024 * 1024;
    private static final int MAX_FILES = 120000;
    private static final int PAGE_SIZE = 150;
    private static volatile boolean busy;
    private static volatile String error = "";
    private static volatile boolean started;

    private GameDataReceiver() { }

    public static synchronized void start(Activity activity) {
        if (started) return;
        started = true;
        String list = activity.getIntent().getStringExtra(
                "com.geko7490.saenginegame.extra.LIST_URI");
        String files = activity.getIntent().getStringExtra(
                "com.geko7490.saenginegame.extra.FILE_URI");
        File external = activity.getExternalFilesDir(null);
        if (external == null) {
            error = "Game application storage is unavailable";
            return;
        }
        File gameDir = new File(external, "GTASA");
        if (validGameRoot(gameDir)) return;
        if (list == null || files == null) return; // allow manual game-data setup
        if (!list.equals("content://com.geko7490.saengineandroid.gamedata/list") ||
                !files.equals("content://com.geko7490.saengineandroid.gamedata/files")) {
            error = "Unrecognized launcher data provider URI";
            return;
        }
        busy = true;
        final Uri listRoot = Uri.parse(list);
        final Uri fileRoot = Uri.parse(files);
        Thread copyThread = new Thread(() -> {
            try {
                importFromProvider(activity.getContentResolver(), external, listRoot, fileRoot);
                error = "";
            } catch (Exception e) {
                error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            } finally {
                busy = false;
            }
        }, "SAEngineGameDataTransfer");
        copyThread.start();
    }

    public static boolean isBusy() { return busy; }
    public static String getError() { return error; }
    public static boolean wasStarted() { return started; }

    private static final class Node {
        final Uri dirUri, fileUri;
        final File targetDir;
        Node(Uri dirUri, Uri fileUri, File targetDir) {
            this.dirUri = dirUri;
            this.fileUri = fileUri;
            this.targetDir = targetDir;
        }
    }

    private static void importFromProvider(ContentResolver resolver, File external, Uri listRoot, Uri fileRoot)
            throws IOException {
        File staging = new File(external, "GTASA-receiving");
        File previous = new File(external, "GTASA");
        File backup = new File(external, "GTASA-previous");
        deleteTree(staging);
        if (!staging.mkdirs()) throw new IOException("Could not create engine staging folder");
        int fileCount = 0;
        long bytes = 0;
        try {
            Deque<Node> queue = new ArrayDeque<>();
            queue.add(new Node(listRoot, fileRoot, staging));
            while (!queue.isEmpty()) {
                Node node = queue.removeFirst();
                if (!node.targetDir.isDirectory() && !node.targetDir.mkdirs())
                    throw new IOException("Cannot create game data directory");
                int offset = 0;
                while (true) {
                    Uri page = node.dirUri.buildUpon().appendQueryParameter("offset",
                            String.valueOf(offset)).build();
                    int returned = 0;
                    try (Cursor cursor = resolver.query(page, null, null, null, null)) {
                        if (cursor == null) throw new IOException("Game file provider unavailable");
                        int nameIndex = cursor.getColumnIndexOrThrow("name");
                        int directoryIndex = cursor.getColumnIndexOrThrow("is_directory");
                        while (cursor.moveToNext()) {
                            String name = checkedSegment(cursor.getString(nameIndex));
                            boolean directory = cursor.getInt(directoryIndex) != 0;
                            File destination = checkedChild(staging, node.targetDir, name);
                            Uri childFiles = node.fileUri.buildUpon().appendPath(name).build();
                            if (directory) {
                                Uri childDirs = node.dirUri.buildUpon().appendPath(name).build();
                                queue.addLast(new Node(childDirs, childFiles, destination));
                            } else {
                                if (++fileCount > MAX_FILES)
                                    throw new IOException("Too many game files");
                                try (InputStream in = resolver.openInputStream(childFiles)) {
                                    if (in == null) throw new IOException("Cannot open game asset: " + name);
                                    try (OutputStream out = new BufferedOutputStream(
                                            new FileOutputStream(destination), 64 * 1024)) {
                                        byte[] buffer = new byte[64 * 1024];
                                        int n;
                                        while ((n = in.read(buffer)) != -1) {
                                            bytes += n;
                                            if (bytes > MAX_SIZE) throw new IOException("Game data exceeds safety limit");
                                            out.write(buffer, 0, n);
                                        }
                                    }
                                }
                            }
                            returned++;
                        }
                    }
                    if (returned < PAGE_SIZE) break;
                    offset += returned;
                }
            }
            if (!validGameRoot(staging))
                throw new IOException("Imported game data has no valid models/ and data/ folders");
            deleteTree(backup);
            if (previous.exists() && !previous.renameTo(backup))
                throw new IOException("Could not backup previous game data");
            if (!staging.renameTo(previous)) {
                if (backup.exists()) backup.renameTo(previous);
                throw new IOException("Could not install imported game data");
            }
            deleteTree(backup);
        } finally {
            if (staging.exists()) deleteTree(staging);
        }
    }

    private static String checkedSegment(String value) throws IOException {
        if (value == null || value.isEmpty() || value.equals(".") || value.equals("..") ||
                value.contains("/") || value.contains("\\") || value.indexOf('\0') >= 0)
            throw new IOException("Unsafe game file path");
        return value;
    }

    private static File checkedChild(File root, File parent, String name) throws IOException {
        File child = new File(parent, name);
        String base = root.getCanonicalPath() + File.separator;
        if (!child.getCanonicalPath().startsWith(base))
            throw new IOException("Escaped game data directory");
        return child;
    }

    private static boolean validGameRoot(File root) {
        return new File(root, "models").isDirectory() && new File(root, "data").isDirectory();
    }

    private static void deleteTree(File file) throws IOException {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (file.isDirectory() && children != null)
            for (File child : children) deleteTree(child);
        if (!file.delete()) throw new IOException("Cannot delete temporary game data");
    }
}
