package com.geko7490.saengineandroid;

import org.junit.Test;
import static org.junit.Assert.*;

public final class NativeFpsModInstallerTest {
    @Test public void acceptsOnlyFourSupportedFpsTargets() {
        for (int fps : new int[]{30, 60, 90, 120})
            assertTrue(NativeFpsModInstaller.allowedFps(fps));
        for (int fps : new int[]{0, 10, 24, 59, 61, 144, 160, 255})
            assertFalse(NativeFpsModInstaller.allowedFps(fps));
    }

    @Test public void onlyAcceptsExplicitGtaSaModsFolder() {
        assertTrue(NativeFpsModInstaller.isValidTreeId(
            "primary:Android_unprotected/data/com.rockstargames.gtasa/mods"));
        assertTrue(NativeFpsModInstaller.isValidTreeId(
            "primary:Android/data/com.rockstargames.gtasa/mods"));
        assertFalse(NativeFpsModInstaller.isValidTreeId(
            "primary:Android_unprotected/data/com.other.game/mods"));
        assertFalse(NativeFpsModInstaller.isValidTreeId(
            "primary:Android/data/com.rockstargames.gtasa/files"));
        assertFalse(NativeFpsModInstaller.isValidTreeId("primary:Downloads/custom/com.rockstargames.gtasa/mods"));
        assertFalse(NativeFpsModInstaller.isValidTreeId("primary:Download"));
        assertFalse(NativeFpsModInstaller.isValidTreeId("primary:Android_unprotected/data/com.rockstargames.gtasa/mods/evil"));
        assertFalse(NativeFpsModInstaller.isValidTreeId(null));
    }
}
