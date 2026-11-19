package com.geko7490.saengineandroid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;

import rikka.shizuku.Shizuku;

/** Shizuku-backed local ADB shell. Shizuku's user service is NOT root by default. */
public final class ShizukuBridge {
    private static final int REQUEST = 7101;
    private final Activity host;
    private volatile IShellService service;
    private volatile String status = "Shizuku bağlanmadı";
    private final Runnable onChange;
    private boolean alive = true;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            service = IShellService.Stub.asInterface(binder);
            status = "Shizuku shell servisi bağlandı";
            notifyUpdate();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            service = null;
            status = "Shizuku bağlantısı kesildi";
            notifyUpdate();
        }
    };
    private final Shizuku.OnRequestPermissionResultListener permissionResult = (code, grant) -> {
        if (code != REQUEST) return;
        if (grant == PackageManager.PERMISSION_GRANTED) bind();
        else { status = "Shizuku izni reddedildi"; notifyUpdate(); }
    };
    private final Shizuku.OnBinderReceivedListener binderReceived = this::notifyUpdate;
    private final Shizuku.OnBinderDeadListener binderDead = () -> {
        service = null;
        status = "Shizuku servisi durdu";
        notifyUpdate();
    };

    public ShizukuBridge(Activity host, Runnable onChange) {
        this.host = host; this.onChange = onChange;
        Shizuku.addRequestPermissionResultListener(permissionResult);
        Shizuku.addBinderReceivedListenerSticky(binderReceived);
        Shizuku.addBinderDeadListener(binderDead);
    }

    public boolean ready() { return service != null; }
    public String status() { return status; }

    public void connect() {
        try {
            if (!Shizuku.pingBinder()) {
                status = "Shizuku çalışmıyor. Shizuku uygulamasından kablosuz hata ayıklama ile başlat.";
                notifyUpdate(); return;
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    status = "Shizuku izni reddedilmiş; Shizuku uygulamasından yeniden izin ver.";
                    notifyUpdate(); return;
                }
                status = "Shizuku yetkisi bekleniyor";
                notifyUpdate();
                Shizuku.requestPermission(REQUEST);
                return;
            }
            bind();
        } catch (Exception e) {
            status = "Shizuku: " + e.getMessage(); notifyUpdate();
        }
    }

    private void bind() {
        if (service != null) { notifyUpdate(); return; }
        try {
            Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                    new ComponentName(host, ShellUserService.class))
                    .daemon(false).processNameSuffix("sa-shell")
                    .debuggable(true).version(1);
            Shizuku.bindUserService(args, connection);
            status = "Shizuku shell bağlantısı kuruluyor…";
            notifyUpdate();
        } catch (Exception e) {
            status = "Shizuku bağlanamadı: " + e.getMessage();
            notifyUpdate();
        }
    }

    public int getUid() throws RemoteException {
        IShellService s = service;
        if (s == null) throw new IllegalStateException("Shizuku bağlı değil");
        return s.getUid();
    }

    /** Must be invoked on a worker thread to avoid blocking the Android UI. */
    public String exec(String command) throws RemoteException {
        IShellService s = service;
        if (s == null) throw new IllegalStateException("Shizuku bağlı değil");
        return s.execute(command);
    }

    public void close() {
        alive = false;
        Shizuku.removeRequestPermissionResultListener(permissionResult);
        Shizuku.removeBinderReceivedListener(binderReceived);
        Shizuku.removeBinderDeadListener(binderDead);
        service = null;
    }

    private void notifyUpdate() {
        if (alive) host.runOnUiThread(onChange);
    }
}
