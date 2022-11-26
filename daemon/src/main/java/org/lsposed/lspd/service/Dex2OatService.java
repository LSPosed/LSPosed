/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2022 LSPosed Contributors
 */

package org.lsposed.lspd.service;

import static org.lsposed.lspd.ILSPManagerService.DEX2OAT_CRASHED;
import static org.lsposed.lspd.ILSPManagerService.DEX2OAT_MOUNT_FAILED;
import static org.lsposed.lspd.ILSPManagerService.DEX2OAT_OK;
import static org.lsposed.lspd.ILSPManagerService.DEX2OAT_SELINUX_PERMISSIVE;
import static org.lsposed.lspd.ILSPManagerService.DEX2OAT_SEPOLICY_INCORRECT;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.FileObserver;
import android.os.SELinux;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class Dex2OatService {

    public static final String PROP_NAME = "dalvik.vm.dex2oat-flags";
    public static final String PROP_VALUE = "--inline-max-code-units=0";
    private static final String TAG = "LSPosedDex2Oat";

    private String devTmpDir, magiskPath, fakeBin32, fakeBin64;
    private String[] dex2oatBinaries;
    private FileDescriptor[] stockFds;
    private LocalSocket serverSocket = null;
    private LocalServerSocket server = null;
    private int compatibility = DEX2OAT_OK;

    private final FileObserver selinuxObserver = new FileObserver("/sys/fs/selinux/enforce", FileObserver.MODIFY) {
        @Override
        public void onEvent(int i, @Nullable String s) {
            Log.d(TAG, "SELinux status changed");
            synchronized (this) {
                if (compatibility == DEX2OAT_CRASHED) stopWatching();
                boolean enforcing = false;
                try (var is = Files.newInputStream(Paths.get("/sys/fs/selinux/enforce"))) {
                    enforcing = is.read() == '1';
                } catch (IOException ignored) {
                }
                if (!enforcing) {
                    if (compatibility == DEX2OAT_OK) setEnabled(false);
                    compatibility = DEX2OAT_SELINUX_PERMISSIVE;
                } else if (SELinux.checkSELinuxAccess("u:r:untrusted_app:s0", "u:object_r:dex2oat_exec:s0", "file", "execute")
                        || SELinux.checkSELinuxAccess("u:r:untrusted_app:s0", "u:object_r:dex2oat_exec:s0", "file", "execute_no_trans")) {
                    if (compatibility == DEX2OAT_OK) setEnabled(false);
                    compatibility = DEX2OAT_SEPOLICY_INCORRECT;
                } else {
                    if (compatibility != DEX2OAT_OK) {
                        setEnabled(true);
                        if (checkMount()) compatibility = DEX2OAT_OK;
                        else {
                            setEnabled(false);
                            compatibility = DEX2OAT_MOUNT_FAILED;
                            stopWatching();
                        }
                    }
                }
            }
        }

        @Override
        public void stopWatching() {
            super.stopWatching();
            Log.w(TAG, "SELinux observer stopped");
        }
    };

    @RequiresApi(Build.VERSION_CODES.Q)
    public Dex2OatService() {
        initNative();
        try {
            Files.walk(Paths.get(magiskPath).resolve("dex2oat")).forEach(path -> SELinux.setFileContext(path.toString(), "u:object_r:magisk_file:s0"));
        } catch (IOException e) {
            Log.e(TAG, "Error setting sepolicy", e);
        }
        if (Arrays.stream(dex2oatBinaries).noneMatch(Objects::nonNull)) {
            Log.e(TAG, "Failed to find dex2oat binaries");
            compatibility = DEX2OAT_MOUNT_FAILED;
            return;
        }

        if (!checkMount()) { // Already mounted when restart daemon
            setEnabled(true);
            if (!checkMount()) {
                setEnabled(false);
                compatibility = DEX2OAT_MOUNT_FAILED;
                return;
            }
        }

        Thread daemonThread = new Thread(() -> {
            var devPath = Paths.get(devTmpDir);
            var sockPath = devPath.resolve("dex2oat.sock");
            try {
                Log.i(TAG, "Dex2oat wrapper daemon start");
                if (setSocketCreateContext("u:r:dex2oat:s0")) {
                    Log.d(TAG, "Set socket context to u:r:dex2oat:s0");
                } else {
                    throw new IOException("Failed to set socket context");
                }
                Files.createDirectories(devPath);
                Log.d(TAG, "Dev path: " + devPath);

                serverSocket = new LocalSocket(LocalSocket.SOCKET_STREAM);
                serverSocket.bind(new LocalSocketAddress(sockPath.toString(), LocalSocketAddress.Namespace.FILESYSTEM));
                server = new LocalServerSocket(serverSocket.getFileDescriptor());
                SELinux.setFileContext(sockPath.toString(), "u:object_r:magisk_file:s0");
                stockFds = new FileDescriptor[dex2oatBinaries.length];
                for (int i = 0; i < dex2oatBinaries.length; i++) {
                    if (dex2oatBinaries[i] != null) {
                        stockFds[i] = Os.open(dex2oatBinaries[i], OsConstants.O_RDONLY, 0);
                    }
                }

                while (true) {
                    var client = server.accept();
                    try (var is = client.getInputStream();
                         var os = client.getOutputStream()) {
                        var id = is.read();
                        client.setFileDescriptorsForSend(new FileDescriptor[]{stockFds[id]});
                        os.write(1);
                        Log.d(TAG, String.format("Sent stock fd: is64 = %b, isDebug = %b", (id & 0b10) != 0, (id & 0b01) != 0));
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "Dex2oat wrapper daemon crashed", e);
                try {
                    server.close();
                    Files.delete(sockPath);
                } catch (IOException ignored) {
                }
                try {
                    for (var fd : stockFds) {
                        if (fd != null && fd.valid()) Os.close(fd);
                    }
                } catch (ErrnoException ignored) {
                }
                synchronized (this) {
                    selinuxObserver.stopWatching();
                    if (compatibility == DEX2OAT_OK) {
                        setEnabled(false);
                        compatibility = DEX2OAT_CRASHED;
                    }
                }
            }
        });

        daemonThread.start();
        selinuxObserver.startWatching();
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public int getCompatibility() {
        return compatibility;
    }

    private native void initNative();

    private native void setEnabled(boolean enabled);

    private boolean checkMount() {
        for (int i = 0; i < dex2oatBinaries.length; i++) {
            var bin = dex2oatBinaries[i];
            if (bin == null) continue;
            try {
                var apex = Os.stat("/proc/1/root" + bin);
                var fake = Os.stat(i < 2 ? fakeBin32 : fakeBin64);
                if (apex.st_ino != fake.st_ino) {
                    Log.w(TAG, "Check mount failed for " + bin);
                    return false;
                }
            } catch (ErrnoException e) {
                Log.e(TAG, "Check mount failed for " + bin, e);
                return false;
            }
        }
        Log.d(TAG, "Check mount succeeded");
        return true;
    }

    private boolean setSocketCreateContext(String context) {
        FileDescriptor fd = null;
        try {
            fd = Os.open("/proc/thread-self/attr/sockcreate", OsConstants.O_RDWR, 0);
        } catch (ErrnoException e) {
            if (e.errno == OsConstants.ENOENT) {
                int tid = Os.gettid();
                try {
                    fd = Os.open(String.format(Locale.ENGLISH, "/proc/self/task/%d/attr/sockcreate", tid), OsConstants.O_RDWR, 0);
                } catch (ErrnoException ignored) {
                }
            }
        }

        if (fd == null) {
            return false;
        }

        byte[] bytes;
        int length;
        int remaining;
        if (!TextUtils.isEmpty(context)) {
            byte[] stringBytes = context.getBytes();
            bytes = new byte[stringBytes.length + 1];
            System.arraycopy(stringBytes, 0, bytes, 0, stringBytes.length);
            bytes[stringBytes.length] = '\0';

            length = bytes.length;
            remaining = bytes.length;
        } else {
            bytes = null;
            length = 0;
            remaining = 0;
        }

        do {
            try {
                remaining -= Os.write(fd, bytes, length - remaining, remaining);
                if (remaining <= 0) {
                    break;
                }
            } catch (ErrnoException e) {
                break;
            } catch (InterruptedIOException e) {
                remaining -= e.bytesTransferred;
            }
        } while (true);

        try {
            Os.close(fd);
        } catch (ErrnoException e) {
            Log.w(TAG, Log.getStackTraceString(e));
        }
        return true;
    }
}
