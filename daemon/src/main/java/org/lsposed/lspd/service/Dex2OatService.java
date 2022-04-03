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
import android.os.SELinux;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class Dex2OatService {

    private static final String TAG = "Dex2OatService";
    private static final String DEX2OAT_32 = "/apex/com.android.art/bin/dex2oat32";
    private static final String DEX2OAT_64 = "/apex/com.android.art/bin/dex2oat64";

    private Thread thread = null;
    private LocalSocket serverSocket = null;
    private LocalServerSocket server = null;
    private FileDescriptor stockFd32 = null, stockFd64 = null;

    public void start() {
        thread = new Thread(() -> {
            try {
                Log.i(TAG, "dex2oat daemon start");
                if (setSocketCreateContext("u:r:dex2oat:s0")) {
                    Log.d(TAG, "set socket context to u:r:dex2oat:s0");
                } else {
                    Log.e(TAG, "failed to set socket context");
                }
                var devPath = getDevPath();
                var sockPath = devPath + "/dex2oat.sock";
                Files.createDirectories(Paths.get(devPath));
                Log.d(TAG, "dev path: " + devPath);

                serverSocket = new LocalSocket(LocalSocket.SOCKET_STREAM);
                serverSocket.bind(new LocalSocketAddress(sockPath, LocalSocketAddress.Namespace.FILESYSTEM));
                server = new LocalServerSocket(serverSocket.getFileDescriptor());
                SELinux.setFileContext(sockPath, "u:object_r:magisk_file:s0");
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
                        Log.d(TAG, "sent fd" + lp);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "dex2oat daemon crashed", e);
            }
        });
        thread.start();
    }

    public boolean isAlive() {
        return thread.isAlive();
    }

    private static native String getDevPath();

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
