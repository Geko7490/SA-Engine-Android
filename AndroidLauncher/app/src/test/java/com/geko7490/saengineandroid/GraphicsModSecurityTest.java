package com.geko7490.saengineandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class GraphicsModSecurityTest {
    @Test public void zipEntryValidationRejectsTraversal() {
        assertTrue(GraphicsArchive.allowedPath("timecyc.dat"));
        assertTrue(GraphicsArchive.allowedPath("mod/data/timecyc.dat"));
        assertFalse(GraphicsArchive.allowedPath("../timecyc.dat"));
        assertFalse(GraphicsArchive.allowedPath("data/../../timecyc.dat"));
        assertFalse(GraphicsArchive.allowedPath("C:/Windows/evil.dat"));
        assertFalse(GraphicsArchive.allowedPath("/data/user/0/file"));
        assertFalse(GraphicsArchive.allowedPath("data\\timecyc.dat"));
        assertFalse(GraphicsArchive.allowedPath("timecyc.dat\u0000"));
    }
    @Test public void shellQuotingEscapesMetacharacters() {
        assertEquals("'file name.dat'", GraphicsInstaller.shellQuote("file name.dat"));
        assertEquals("'a'\\''b'", GraphicsInstaller.shellQuote("a'b"));
        assertEquals("'foo; rm -rf /'", GraphicsInstaller.shellQuote("foo; rm -rf /"));
    }
    @Test public void shellSuccessRequiresExplicitZeroExitStatus() {
        assertTrue(GraphicsInstaller.success("exit=0\nSUCCESS"));
        assertFalse(GraphicsInstaller.success("exit=1\nPermission denied"));
        assertFalse(GraphicsInstaller.success("ERROR: service disconnected"));
    }
}
