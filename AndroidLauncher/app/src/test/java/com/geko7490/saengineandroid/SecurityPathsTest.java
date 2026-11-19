package com.geko7490.saengineandroid;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SecurityPathsTest {
    private String normalize(String path) throws Exception {
        Method method = GameDataImporter.class.getDeclaredMethod("checkedPath", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, path);
    }

    private void reject(String path) throws Exception {
        try {
            normalize(path);
            fail("Unsafe ZIP entry was accepted: " + path);
        } catch (InvocationTargetException thrown) {
            assertTrue(thrown.getCause() instanceof java.io.IOException);
        }
    }

    @Test public void acceptedZipPathIsNormalized() throws Exception {
        assertEquals("models/gta3.img", normalize("models\\gta3.img"));
        assertEquals("data/main.scm", normalize("data/main.scm"));
    }

    @Test public void zipTraversalAndAbsolutePathsAreBlocked() throws Exception {
        reject("../../outside.txt");
        reject("/root/file");
        reject("models/../something");
        reject("C:/system/file");
    }

    @Test public void sha256DetectsChanges() throws Exception {
        File file = File.createTempFile("sa_engine_test", ".bin");
        try {
            Method method = ResumableDownloader.class.getDeclaredMethod("sha256", File.class);
            method.setAccessible(true);
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write("abc".getBytes(StandardCharsets.US_ASCII));
            }
            assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    method.invoke(null, file));
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write("abcd".getBytes(StandardCharsets.US_ASCII));
            }
            assertNotEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                    method.invoke(null, file));
        } finally {
            file.delete();
        }
    }
}
