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
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class InstallerVerifier {
    public static boolean verifyInstallerSignature(ApplicationInfo appInfo) {
        ApkVerifier verifier = new ApkVerifier.Builder(new File(appInfo.sourceDir))
                .setMinCheckedPlatformVersion(27)
                .build();
        try {
            ApkVerifier.Result result = verifier.verify();
            if (!result.isVerified()) {
                return false;
            }
            return Arrays.equals(result.getSignerCertificates().get(0).getEncoded(), CERTIFICATE);
        } catch (Throwable t) {
            Utils.logE("verifyInstallerSignature: ", t);
            return false;
        }
    }

    public static void hookXposedInstaller(final ClassLoader classLoader) {
        try {
            Class<?> ConstantsClass = XposedHelpers.findClass("org.lsposed.manager.Constants", classLoader);
            XposedHelpers.findAndHookMethod(android.app.Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        XposedHelpers.callStaticMethod(ConstantsClass, "showErrorToast", 0);
                    } catch (Throwable t) {
                        Utils.logW("showErrorToast: ", t);
                        Toast.makeText((Context) param.thisObject, "This application has been destroyed, please make sure you download it from the official source.", Toast.LENGTH_LONG).show();
                    }
                    new Handler().postDelayed(() -> System.exit(0), 50);
                }
            });
        } catch (Throwable t) {
            Utils.logW("hookXposedInstaller: ", t);
        }
    }
}
