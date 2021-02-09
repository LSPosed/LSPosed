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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package io.github.lsposed.lspd.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.ServiceManager;
import android.util.Log;
import android.ddm.DdmHandleAppName;

import io.github.lsposed.common.KeepAll;
import io.github.lsposed.lspd.service.LSPosedService;
import io.github.lsposed.lspd.util.Utils;

import java.util.concurrent.atomic.AtomicReference;

import static io.github.lsposed.lspd.service.LSPosedService.TAG;

@SuppressLint("DefaultLocale")
public class Main implements KeepAll {
    private static final AtomicReference<EdxpImpl> lspdImplRef = new AtomicReference<>(null);

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // entry points
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static void forkAndSpecializePre(int uid, int gid, int[] gids, int debugFlags,
                                            int[][] rlimits, int mountExternal, String seInfo,
                                            String niceName, int[] fdsToClose, int[] fdsToIgnore,
                                            boolean startChildZygote, String instructionSet,
                                            String appDataDir) {
        // won't be loaded
    }

    public static void forkAndSpecializePost(int pid, String appDataDir, String niceName, int variant) {
        EdxpImpl lspd = getEdxpImpl(variant);
        if (lspd == null || !lspd.isInitialized()) {
            Utils.logE("Not started up");
            return;
        }
        if (pid == 0) {
            lspd.getNormalProxy().forkAndSpecializePost(pid, appDataDir, niceName);
        }
    }

    public static void forkSystemServerPre(int uid, int gid, int[] gids, int debugFlags, int[][] rlimits,
                                           long permittedCapabilities, long effectiveCapabilities) {
        // Won't load
    }

    public static void forkSystemServerPost(int pid, int variant) {
        EdxpImpl lspd = getEdxpImpl(variant);
        if (lspd == null || !lspd.isInitialized()) {
            return;
        }
        if (pid == 0) {
            lspd.getNormalProxy().forkSystemServerPost(pid);
        }
    }

    public static synchronized boolean setEdxpImpl(EdxpImpl lspd) {
        return lspdImplRef.compareAndSet(null, lspd);
    }

    public static synchronized EdxpImpl getEdxpImpl(int variant) {
        EdxpImpl lspd = lspdImplRef.get();
        if (lspd != null) {
            return lspd;
        }
        Utils.logD("Loading variant " + variant);
        try {
            switch (variant) {
                case EdxpImpl.YAHFA:
                    Class.forName("io.github.lsposed.lspd.yahfa.core.YahfaEdxpImpl");
                    break;
                case EdxpImpl.SANDHOOK:
                    Class.forName("io.github.lsposed.lspd.sandhook.core.SandHookEdxpImpl");
                    break;
                default:
                    Utils.logE("Unsupported variant " + variant);

            }
        } catch (ClassNotFoundException e) {
            Utils.logE("loadEdxpImpls: Class not found", e);
        }
        return lspdImplRef.get();
    }

    public static synchronized EdxpImpl getEdxpImpl() {
        return lspdImplRef.get();
    }

    @EdxpImpl.Variant
    public static synchronized int getEdxpVariant() {
        return getEdxpImpl().getVariant();
    }

    private static void waitSystemService(String name) {
        while (ServiceManager.getService(name) == null) {
            try {
                Log.i(TAG, "service " + name + " is not started, wait 1s.");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.i(TAG, Log.getStackTraceString(e));
            }
        }
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--debug")) {
                DdmHandleAppName.setAppName("lspd", 0);
            }
        }
        waitSystemService("package");
        waitSystemService("activity");
        waitSystemService(Context.USER_SERVICE);
        waitSystemService(Context.APP_OPS_SERVICE);

        LSPosedService.start();
    }
}
