package com.geko7490.saengineandroid;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * V0.5 read-only game file bridge. Only the installed engine package UID can read
 * files; launcher grants that package prefix URI access at launch.
 *
 * List:  content://com.geko7490.saengineandroid.gamedata/list/<directory>?offset=0
 * File:  content://com.geko7490.saengineandroid.gamedata/files/<relativeFile>
 * No file system path is shared with another application.
 */
public final class GameDataProvider extends ContentProvider {
    public static final String AUTHORITY = "com.geko7490.saengineandroid.gamedata";
    public static final Uri LIST_ROOT = Uri.parse("content://" + AUTHORITY + "/list");
    public static final Uri FILE_ROOT = Uri.parse("content://" + AUTHORITY + "/files");
    public static final int PAGE_SIZE = 150;

    @Override public boolean onCreate() { return true; }

    private void checkCaller() {
        int uid = Binder.getCallingUid();
        if (uid == android.os.Process.myUid()) return;
        if (getContext() == null) throw new SecurityException("No provider context");
        String[] packages = getContext().getPackageManager().getPackagesForUid(uid);
        if (packages != null) for (String name : packages)
            if (EngineInstaller.ENGINE_PACKAGE.equals(name)) return;
        throw new SecurityException("Only the installed SA Engine Game may access game files");
    }

    private File resolve(Uri uri, String mode) throws FileNotFoundException {
        List<String> parts = uri.getPathSegments();
        if (parts.isEmpty() || !mode.equals(parts.get(0)))
            throw new FileNotFoundException("Invalid game URI");
        File directory;
        try {
            directory = new GameDataImporter(getContext()).getGameRoot();
            if (!GameDataImporter.isValidRoot(directory))
                throw new FileNotFoundException("GTA SA data has not been imported");
            File target = directory;
            for (int i = 1; i < parts.size(); i++) {
                String part = parts.get(i);
                if (part.equals(".") || part.equals("..") || part.contains("/") ||
                        part.contains("\\") || part.isEmpty())
                    throw new FileNotFoundException("Unsafe file name");
                target = new File(target, part);
            }
            String rootPath = directory.getCanonicalPath();
            String path = target.getCanonicalPath();
            if (!(path.equals(rootPath) || path.startsWith(rootPath + File.separator)))
                throw new FileNotFoundException("Path escapes game directory");
            return target;
        } catch (IOException e) {
            throw new FileNotFoundException(e.getMessage());
        }
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        checkCaller();
        try {
            File dir = resolve(uri, "list");
            if (!dir.isDirectory()) throw new FileNotFoundException("Not a directory");
            File[] entries = dir.listFiles();
            if (entries == null) throw new FileNotFoundException("Directory unavailable");
            Arrays.sort(entries, Comparator.comparing(File::getName));
            int offset = 0;
            try { offset = Math.max(0, Integer.parseInt(uri.getQueryParameter("offset"))); }
            catch (RuntimeException ignored) { }
            MatrixCursor result = new MatrixCursor(new String[]{"name", "is_directory", "size"});
            for (int i = offset; i < entries.length && i < offset + PAGE_SIZE; i++) {
                File f = entries[i];
                if (f.isFile() || f.isDirectory())
                    result.addRow(new Object[]{f.getName(), f.isDirectory() ? 1 : 0, f.length()});
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot enumerate game data: " + e.getMessage());
        }
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        checkCaller();
        if (!"r".equals(mode)) throw new FileNotFoundException("Read-only game bridge");
        File file = resolve(uri, "files");
        if (!file.isFile()) throw new FileNotFoundException("File unavailable");
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) { return "application/octet-stream"; }
    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Read-only");
    }
    @Override public int update(Uri uri, ContentValues values, String s, String[] a) {
        throw new UnsupportedOperationException("Read-only");
    }
    @Override public int delete(Uri uri, String s, String[] a) {
        throw new UnsupportedOperationException("Read-only");
    }
}
