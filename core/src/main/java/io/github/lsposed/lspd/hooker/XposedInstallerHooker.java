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

package io.github.lsposed.lspd.hooker;

import android.os.IBinder;

import de.robv.android.xposed.XposedHelpers;
import io.github.lsposed.lspd.core.EdxpImpl;
import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.util.Utils;

public class XposedInstallerHooker {

    public static void hookXposedInstaller(final ClassLoader classLoader, IBinder binder) {
        final String variant;
        switch (Main.getEdxpVariant()) {
            case EdxpImpl.YAHFA:
                variant = "YAHFA";
                break;
            case EdxpImpl.SANDHOOK:
                variant = "SandHook";
                break;
            case EdxpImpl.NONE:
            default:
                variant = "Unknown";
                break;
        }

        Utils.logI("Found LSPosed Manager, hooking it");

        // LSPosed Manager R
        try {
            Class<?> serviceClass = XposedHelpers.findClass("io.github.lsposed.manager.receivers.LSPosedManagerServiceClient", classLoader);
            XposedHelpers.setStaticObjectField(serviceClass, "binder", binder);

            Utils.logI("Hooked LSPosed Manager");
        } catch (Throwable t) {
            Utils.logW("Could not hook LSPosed Manager", t);
        }
    }
}
