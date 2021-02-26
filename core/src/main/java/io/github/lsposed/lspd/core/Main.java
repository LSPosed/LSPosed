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
import android.ddm.DdmHandleAppName;
import android.os.IBinder;

import java.util.concurrent.atomic.AtomicReference;

import io.github.lsposed.lspd.config.LSPApplicationServiceClient;
import io.github.lsposed.lspd.sandhook.core.SandHookImpl;
import io.github.lsposed.lspd.service.ServiceManager;
import io.github.lsposed.lspd.util.Utils;
import io.github.lsposed.lspd.yahfa.core.YahfaImpl;

import static io.github.lsposed.lspd.config.LSPApplicationServiceClient.serviceClient;

@SuppressLint("DefaultLocale")
public class Main {
    private static final AtomicReference<Impl> lspdImplRef = new AtomicReference<>(null);

    public static void forkAndSpecializePost(String appDataDir, String niceName, IBinder binder) {
        LSPApplicationServiceClient.Init(binder, niceName);
        final int variant = serviceClient.getVariant();
        Impl lspd = getImpl(variant);
        if (lspd == null || !lspd.isInitialized()) {
            Utils.logE("Not started up");
            return;
        }
        lspd.getNormalProxy().forkAndSpecializePost(appDataDir, niceName);
    }

    public static void forkSystemServerPost(IBinder binder) {
        LSPApplicationServiceClient.Init(binder, "android");
        final int variant = serviceClient.getVariant();
        Impl lspd = getImpl(variant);
        if (lspd == null || !lspd.isInitialized()) {
            return;
        }
        lspd.getNormalProxy().forkSystemServerPost();
    }

    public static synchronized Impl getImpl(int variant) {
        Impl lspd = lspdImplRef.get();
        if (lspd != null) {
            return lspd;
        }
        Utils.logD("Loading variant " + variant);
        Impl impl = null;
        switch (variant) {
            case Impl.YAHFA:
                impl = new YahfaImpl();
                break;
            case Impl.SANDHOOK:
                impl = new SandHookImpl();
                break;
            default:
                Utils.logE("Unsupported variant " + variant);

        }
        if (impl != null && lspdImplRef.compareAndSet(null, impl)) {
            impl.init();
        }
        return impl;
    }

    public static synchronized Impl getImpl() {
        return lspdImplRef.get();
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--debug")) {
                DdmHandleAppName.setAppName("lspd", 0);
            }
        }
        ServiceManager.start();
    }
}
