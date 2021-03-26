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

package org.lsposed.lspd.hooker;

import android.os.IBinder;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.util.Utils;

public class XposedInstallerHooker {

    public static void hookXposedInstaller(final ClassLoader classLoader, IBinder binder) {
        Utils.logI("Found LSPosed Manager, hooking it");
        try {
            Class<?> serviceClass = XposedHelpers.findClass("org.lsposed.manager.receivers.LSPosedManagerServiceClient", classLoader);
            XposedHelpers.setStaticObjectField(serviceClass, "binder", binder);

            Utils.logI("Hooked LSPosed Manager");
        } catch (Throwable t) {
            Utils.logW("Could not hook LSPosed Manager", t);
        }
    }
}
