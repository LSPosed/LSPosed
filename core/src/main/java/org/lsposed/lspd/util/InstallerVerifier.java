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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.widget.Toast;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class InstallerVerifier {

    private static final byte[] CERTIFICATE = {48, -126, 2, -61, 48, -126, 1, -85, -96, 3, 2, 1, 2, 2, 4, 36, 13, 10, 11, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 48, 18, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 76, 83, 80, 111, 115, 101, 100, 48, 30, 23, 13, 50, 49, 48, 51, 48, 49, 49, 55, 49, 49, 48, 55, 90, 23, 13, 52, 54, 48, 50, 50, 51, 49, 55, 49, 49, 48, 55, 90, 48, 18, 49, 16, 48, 14, 6, 3, 85, 4, 10, 19, 7, 76, 83, 80, 111, 115, 101, 100, 48, -126, 1, 34, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, 15, 0, 48, -126, 1, 10, 2, -126, 1, 1, 0, -122, -20, -29, -82, 21, 0, 37, -64, -36, 21, 88, -73, 39, -107, 72, -34, -126, 30, 106, 8, 99, 62, -108, 126, 124, 6, -7, 12, -90, -8, -55, -24, -88, -108, 7, -109, 54, 12, -111, 85, -72, 119, -37, -112, -106, -106, 6, -120, 43, -120, 8, -8, 57, 102, -104, -28, -39, -120, 58, -124, -56, 67, 118, 55, 31, 124, -21, 101, 110, 59, 109, 119, 78, -103, 90, 101, -85, -67, 52, -42, -63, 74, 34, -11, 50, -55, 4, 7, -101, -45, -64, 87, -9, 0, -60, -125, -16, 68, -115, -87, 108, -110, 10, 3, -95, 15, -103, 56, -47, -25, 4, 24, -74, 102, 86, 2, 30, -86, -22, 25, 124, -4, -2, 43, -45, 63, 66, -93, 115, 14, 9, -82, 68, 19, -6, -120, 62, 71, 61, -119, 33, 115, -80, 23, -99, 13, 91, 99, -104, -105, -16, 47, -40, 69, 48, -87, 100, 8, -111, 93, 54, 83, 106, -49, -78, -42, 104, 44, 70, -36, 101, -27, -10, -92, -44, 90, -84, -52, -79, 120, -102, 27, -95, -71, 91, -98, -116, 35, 105, 53, 120, -61, -99, 89, 72, -18, -120, 31, 48, 104, -10, -22, -10, 121, 107, -38, -122, -7, -111, -88, -98, 36, 55, 82, -10, -80, -23, -10, 67, -62, 87, 96, 80, 4, 79, 116, 12, -20, -92, 14, -118, -11, -28, 12, -100, 79, 79, 4, -43, 33, -5, 28, -45, 39, -91, 50, -119, 62, -103, -101, 11, 23, 13, 79, -60, -99, 2, 3, 1, 0, 1, -93, 33, 48, 31, 48, 29, 6, 3, 85, 29, 14, 4, 22, 4, 20, -12, 0, 95, -84, 54, 105, -5, 73, 14, -74, -128, 118, 112, 113, 67, -21, -92, -37, 13, -73, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 11, 5, 0, 3, -126, 1, 1, 0, 24, 12, 70, -87, -94, -63, 25, -29, -57, 108, 117, 95, -12, -50, 23, 40, -95, -105, 38, -112, -75, 68, 109, -2, 127, 28, 45, -115, 37, 44, -15, 68, 77, -33, 20, 30, 23, 63, 60, -42, 7, -14, -76, 9, -79, 57, 47, 67, -30, 8, 89, 95, -108, -1, -40, -115, 21, -61, -35, -111, -95, -2, -22, -35, -107, 92, -122, -72, 47, 78, 24, 35, -127, 107, -101, 64, -65, -54, 38, 46, 76, -90, 50, 60, 63, -106, -73, 3, -6, -85, 103, -29, -17, 1, -104, 15, -4, 125, -60, 25, 105, -94, -38, -58, 103, 117, -89, 95, -85, 14, 7, 120, -75, 106, -38, -67, -44, 42, 42, -93, 12, -47, 102, -55, -105, 116, -117, 69, -127, -68, 105, -27, 15, -86, -75, -58, -90, 119, -4, 1, 107, -66, -86, 48, -53, -39, 29, -54, -84, 92, 115, -45, -31, -50, 100, -75, -106, 13, 101, -101, 37, -25, -82, 63, -100, 22, 52, 57, 54, 96, 95, -19, -16, 46, -58, -55, 48, 22, 68, -28, 109, 78, 112, 123, 32, -4, -51, 41, -79, -97, 41, 7, -11, 85, -112, 0, -40, -50, 106, -104, 32, 69, 46, 68, -120, -17, -112, -72, -90, -103, 120, -95, 28, 85, 12, 35, 48, -66, 47, 84, -38, 31, -18, 6, -73, -62, 126, 60, -15, -67, 49, 22, 103, 96, -33, -27, -114, 81, -68, 123, 62, 115, 46, -106, 121, -61, -89, -28, -40, 115, 32, -13, 20, -10, 89, 87};

    public static boolean verifyInstallerSignature(ApplicationInfo appInfo) {
        if ((appInfo.flags & ApplicationInfo.FLAG_TEST_ONLY) != 0) {
            return true;
        }
        ApkVerifier verifier = new ApkVerifier.Builder(new File(appInfo.sourceDir))
                .setMinCheckedPlatformVersion(26)
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
                    System.exit(0);
                }
            });
        } catch (Throwable t) {
            Utils.logW("hookXposedInstaller: ", t);
        }
    }
}
