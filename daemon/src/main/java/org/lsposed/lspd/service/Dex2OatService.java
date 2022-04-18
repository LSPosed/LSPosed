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

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Build;
import android.os.SELinux;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class Dex2OatService {

    enum Dex2OatCompatibility {
        OK, CRASHED, MOUNT_FAILED, SELINUX_PERMISSIVE, SEPOLICY_INCORRECT
    }

    public static final String PROP_NAME = "dalvik.vm.dex2oat-flags";
    public static final String PROP_VALUE = "--inline-max-code-units=0";
    private static final String TAG = "LSPosedDex2Oat";
    private static final String DEX2OAT_32 = "/apex/com.android.art/bin/dex2oat32";
    private static final String DEX2OAT_64 = "/apex/com.android.art/bin/dex2oat64";

    private String devTmpDir;
    private LocalSocket serverSocket = null;
    private LocalServerSocket server = null;
    private FileDescriptor stockFd32 = null, stockFd64 = null;
    private Dex2OatCompatibility compatibility = Dex2OatCompatibility.OK;

    @RequiresApi(Build.VERSION_CODES.Q)
    public Dex2OatService() {
        init();
        if (!checkMount()) { // Already mounted when restart daemon
            setEnabled(true);
            if (!checkMount()) {
                setEnabled(false);
                compatibility = Dex2OatCompatibility.MOUNT_FAILED;
                return;
            }
        }

        Thread daemonThread = new Thread(() -> {
            var devPath = Paths.get(devTmpDir);
            var sockPath = devPath.resolve("dex2oat.sock");
            try {
                Log.i(TAG, "Daemon start");
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
                if (new File(DEX2OAT_32).exists()) stockFd32 = Os.open(DEX2OAT_32, OsConstants.O_RDONLY, 0);
                if (new File(DEX2OAT_64).exists()) stockFd64 = Os.open(DEX2OAT_64, OsConstants.O_RDONLY, 0);

                while (true) {
                    var client = server.accept();
                    try (var is = client.getInputStream();
                         var os = client.getOutputStream()) {
                        var lp = is.read();
                        if (lp == 32) client.setFileDescriptorsForSend(new FileDescriptor[]{stockFd32});
                        else client.setFileDescriptorsForSend(new FileDescriptor[]{stockFd64});
                        os.write(1);
                        Log.d(TAG, "Sent fd" + lp);
                    }
                }
            } catch (Throwable e) {
                Log.e(TAG, "Daemon crashed", e);
                try {
                    server.close();
                    Files.delete(sockPath);
                } catch (IOException ignored) {
                }
                try {
                    if (stockFd32 != null && stockFd32.valid()) Os.close(stockFd32);
                    if (stockFd64 != null && stockFd64.valid()) Os.close(stockFd64);
                } catch (ErrnoException ignored) {
                }
                synchronized (this) {
                    if (compatibility == Dex2OatCompatibility.OK) {
                        setEnabled(false);
                        compatibility = Dex2OatCompatibility.CRASHED;
                    }
                }
            }
        });

        Thread selinuxMonitorThread = new Thread(() -> {
            while (true) {
                try {
                    boolean enforcing = inotifySELinuxEnforce();
                    synchronized (this) {
                        if (compatibility == Dex2OatCompatibility.CRASHED) break;
                        if (!enforcing) {
                            if (compatibility == Dex2OatCompatibility.OK) setEnabled(false);
                            compatibility = Dex2OatCompatibility.SELINUX_PERMISSIVE;
                        } else if (SELinux.checkSELinuxAccess("u:r:untrusted_app:s0", "u:object_r:dex2oat_exec:s0", "file", "execute")
                                || SELinux.checkSELinuxAccess("u:r:untrusted_app:s0", "u:object_r:dex2oat_exec:s0", "file", "execute_no_trans")) {
                            if (compatibility == Dex2OatCompatibility.OK) setEnabled(false);
                            compatibility = Dex2OatCompatibility.SEPOLICY_INCORRECT;
                        } else {
                            if (compatibility != Dex2OatCompatibility.OK) {
                                setEnabled(true);
                                if (checkMount()) compatibility = Dex2OatCompatibility.OK;
                                else {
                                    setEnabled(false);
                                    compatibility = Dex2OatCompatibility.MOUNT_FAILED;
                                    break;
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "File monitor crashed", e);
                    synchronized (this) {
                        if (compatibility == Dex2OatCompatibility.OK) setEnabled(false);
                        compatibility = Dex2OatCompatibility.CRASHED;
                    }
                    break;
                }
            }
            Log.i(TAG, "SELinux monitor thread exiting");
        });

        daemonThread.start();
        selinuxMonitorThread.start();
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public int getCompatibility() {
        return compatibility.ordinal();
    }

    private native void init();

    private native boolean checkMount();

    private native boolean inotifySELinuxEnforce() throws IOException;

    private native void setEnabled(boolean enabled);

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
