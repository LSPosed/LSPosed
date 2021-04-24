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

package de.robv.android.xposed;

import android.app.Application;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

/**
 * Get notified when an app ("Android package") is loaded.
 * This is especially useful to hook some app-specific methods.
 *
 * <p>This interface should be implemented by the module's main class. Xposed will take care of
 * registering it as a callback automatically.
 */
public interface IXposedHookLoadPackage extends IXposedMod {
    /**
     * This method is called when an app is loaded. It's called very early, even before
     * {@link Application#onCreate} is called.
     * Modules can set up their app-specific hooks here.
     *
     * @param lpparam Information about the app.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    void handleLoadPackage(LoadPackageParam lpparam) throws Throwable;

    /**
     * @hide
     */
    final class Wrapper extends XC_LoadPackage {
        private final IXposedHookLoadPackage instance;
        private final String apkPath;

        public Wrapper(IXposedHookLoadPackage instance, String apkPath) {
            this.instance = instance;
            this.apkPath = apkPath;
        }

        @Override
        public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
            instance.handleLoadPackage(lpparam);
        }

        @Override
        public String getApkPath() {
            return apkPath;
        }
    }
}
