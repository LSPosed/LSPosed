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

package org.lsposed.lspd.util;

import static org.lsposed.lspd.util.SignInfo.CERTIFICATE;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class InstallerVerifier {
    public static boolean verifyInstallerSignature(String path) {
        ApkVerifier verifier = new ApkVerifier.Builder(new File(path))
                .setMinCheckedPlatformVersion(27)
                .build();
        try {
            ApkVerifier.Result result = verifier.verify();
            if (!result.isVerified()) {
                return false;
            }
            boolean ret = Arrays.equals(result.getSignerCertificates().get(0).getEncoded(), CERTIFICATE);
            Utils.logI("verifyInstallerSignature: " + ret);
            return ret;
        } catch (Throwable t) {
            Utils.logE("verifyInstallerSignature: ", t);
            return false;
        }
    }

    public static boolean sendBinderToManager(final ClassLoader classLoader, IBinder binder) {
        Utils.logI("Found LSPosed Manager");
        try {
            var clazz = XposedHelpers.findClass("org.lsposed.manager.Constants", classLoader);
            var ret = (boolean) XposedHelpers.callStaticMethod(clazz, "setBinder",
                    new Class[]{IBinder.class}, binder);
            Utils.logI("Send binder to LSPosed Manager: " + ret);
            return ret;
        } catch (Throwable t) {
            Utils.logW("Could not send binder to LSPosed Manager", t);
            return false;
        }
    }
}
