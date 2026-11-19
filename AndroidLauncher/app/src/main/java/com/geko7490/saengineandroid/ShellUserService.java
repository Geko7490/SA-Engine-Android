package com.geko7490.saengineandroid;

import android.os.Process;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** Isolated Shizuku UserService, running as Android's shell UID after user authorization. */
public final class ShellUserService extends IShellService.Stub {
    private static final int MAX_OUTPUT = 100 * 1024;
    public ShellUserService() { }

    @Override public int getUid() { return Process.myUid(); }

    @Override public String execute(String command) {
        if (command == null || command.trim().isEmpty()) return "Empty command";
        if (command.length() > 4096) return "Command too long";
        java.lang.Process proc = null;
        try {
            // Only user-originated commands are executed. No untrusted mod archives
            // or downloaded catalogue descriptions become shell commands.
            proc = new ProcessBuilder("/system/bin/sh", "-c", command)
                    .redirectErrorStream(true).start();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            InputStream in = proc.getInputStream();
            byte[] part = new byte[4096];
            int n;
            long start = System.nanoTime();
            boolean truncated = false;
            while ((n = in.read(part)) != -1) {
                int room = MAX_OUTPUT - buffer.size();
                if (room > 0) buffer.write(part, 0, Math.min(n, room));
                else truncated = true;
                if (System.nanoTime() - start > TimeUnit.SECONDS.toNanos(45)) {
                    proc.destroyForcibly();
                    return "Timeout after 45 s\n" + buffer.toString("UTF-8");
                }
            }
            if (!proc.waitFor(5, TimeUnit.SECONDS)) {
                proc.destroyForcibly();
                return "Command timed out\n" + buffer.toString("UTF-8");
            }
            return "exit=" + proc.exitValue() + "\n" +
                    buffer.toString("UTF-8") + (truncated ? "\n[output truncated]" : "");
        } catch (Exception e) {
            return "ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } finally {
            if (proc != null) proc.destroy();
        }
    }

    @Override public void destroy() { System.exit(0); }
}
