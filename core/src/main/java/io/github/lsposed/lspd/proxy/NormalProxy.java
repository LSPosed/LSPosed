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

package io.github.lsposed.lspd.proxy;

import io.github.lsposed.lspd.nativebridge.ConfigManager;
import io.github.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import io.github.lsposed.lspd.util.Utils;

import de.robv.android.xposed.SELinuxHelper;
import de.robv.android.xposed.XposedInit;

import static io.github.lsposed.lspd.util.FileUtils.getDataPathPrefix;

public class NormalProxy extends BaseProxy {

    public NormalProxy(Router router) {
        super(router);
    }

    public void forkAndSpecializePost(String appDataDir, String niceName) {
        forkPostCommon(false, appDataDir, niceName);
    }

    public void forkSystemServerPost() {
        forkPostCommon(true,
                getDataPathPrefix() + "android", "system_server");
    }


    private void forkPostCommon(boolean isSystem, String appDataDir, String niceName) {
        SELinuxHelper.initOnce();
        mRouter.initResourcesHook();
        mRouter.prepare(isSystem);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        ConfigManager.appDataDir = appDataDir;
        ConfigManager.niceName = niceName;
        mRouter.installBootstrapHooks(isSystem);
        XposedInit.prefsBasePath = ConfigManager.getPrefsPath("");
        mRouter.onEnterChildProcess();
        Utils.logI("Loading modules for " + niceName);
        mRouter.loadModulesSafely(true);
    }

}
