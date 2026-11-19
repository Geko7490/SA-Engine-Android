package com.geko7490.saengineandroid;

import android.os.Process;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** Isolated Shizuku UserService: commands execute under UID 2000 (ADB) or 0 (root Shizuku). */
public final class ShellUserService extends IShellService.Stub {
    private static final int MAX_OUTPUT = 100 * 1024;
    public ShellUserService() { }

    @Override public int getUid() { return Process.myUid(); }

    @Override public String execute(String command) {
        if (command == null || command.trim().isEmpty()) return "Empty command";
        if (command.length() > 8192) return "Command too long";
        java.lang.Process proc = null;
        try {
            // Commands originate from explicit UI events, never from downloaded
            // mods or web pages. The Android shell user is not automatically root.
            proc = new ProcessBuilder("/system/bin/sh", "-c", command)
                    .redirectErrorStream(true).start();
            final java.lang.Process running = proc;
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            final boolean[] truncated = {false};
            Thread reader = new Thread(() -> {
                try (InputStream in = running.getInputStream()) {
                    byte[] part = new byte[4096];
                    int n;
                    while ((n = in.read(part)) != -1) {
                        int remaining = MAX_OUTPUT - buffer.size();
                        if (remaining > 0) buffer.write(part, 0, Math.min(n, remaining));
                        if (n > remaining) truncated[0] = true;
                    }
                } catch (Exception ignored) { }
            }, "SAEngineShellOutput");
            reader.setDaemon(true);
            reader.start();
            boolean finished = proc.waitFor(45, TimeUnit.SECONDS);
            if (!finished) proc.destroyForcibly();
            reader.join(3000);
            String data = buffer.toString("UTF-8");
            if (!finished) return "TIMEOUT: 45 seconds\n" + data;
            return "exit=" + proc.exitValue() + "\n" + data +
                    (truncated[0] ? "\n[output truncated]" : "");
        } catch (Exception e) {
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    @Override public void destroy() { System.exit(0); }
}
