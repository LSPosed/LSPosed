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

import de.robv.android.xposed.callbacks.XC_InitZygote;
import de.robv.android.xposed.callbacks.XCallback;

/**
 * Hook the initialization of Zygote process(es), from which all the apps are forked.
 *
 * <p>Implement this interface in your module's main class in order to be notified when Android is
 * starting up. In {@link IXposedHookZygoteInit}, you can modify objects and place hooks that should
 * be applied for every app. Only the Android framework/system classes are available at that point
 * in time. Use {@code null} as class loader for {@link XposedHelpers#findAndHookMethod(String, ClassLoader, String, Object...)}
 * and its variants.
 *
 * <p>If you want to hook one/multiple specific apps, use {@link IXposedHookLoadPackage} instead.
 */
public interface IXposedHookZygoteInit extends IXposedMod {
    /**
     * Called very early during startup of Zygote.
     *
     * @param startupParam Details about the module itself and the started process.
     * @throws Throwable everything is caught, but will prevent further initialization of the module.
     */
    void initZygote(StartupParam startupParam) throws Throwable;

    /**
     * Data holder for {@link #initZygote}.
     */
    final class StartupParam extends XCallback.Param {
        /*package*/ StartupParam() {
        }

        /**
         * @param callbacks
         * @hide
         */
        public StartupParam(XposedBridge.CopyOnWriteSortedSet<? extends XCallback> callbacks) {
            super(callbacks);
        }

        /**
         * The path to the module's APK.
         */
        public String modulePath;

        /**
         * Always {@code true} on 32-bit ROMs. On 64-bit, it's only {@code true} for the primary
         * process that starts the system_server.
         */
        public boolean startsSystemServer;
    }

    /**
     * @hide
     */
    final class Wrapper extends XC_InitZygote {
        private final IXposedHookZygoteInit instance;
        private final StartupParam startupParam;

        public Wrapper(IXposedHookZygoteInit instance, StartupParam startupParam) {
            this.instance = instance;
            this.startupParam = startupParam;
        }

        @Override
        public void initZygote(StartupParam startupParam) throws Throwable {
            // NOTE: parameter startupParam not used
            // cause startupParam info is generated and saved along with instance here
            instance.initZygote(this.startupParam);
        }

        @Override
        public String getApkPath() {
            return startupParam.modulePath;
        }
    }
}
