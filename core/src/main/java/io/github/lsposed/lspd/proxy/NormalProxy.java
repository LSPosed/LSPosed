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

import android.os.Environment;

import java.io.File;

import io.github.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import io.github.lsposed.lspd.nativebridge.ModuleLogger;
import io.github.lsposed.lspd.util.Utils;

import static io.github.lsposed.lspd.config.LSPApplicationServiceClient.serviceClient;

public class NormalProxy extends BaseProxy {

    public NormalProxy(Router router) {
        super(router);
    }

    public void forkAndSpecializePost(String appDataDir, String niceName) {
        forkPostCommon(false, appDataDir, niceName);
    }

    public void forkSystemServerPost() {
        forkPostCommon(true,
                new File(Environment.getDataDirectory(), "android").toString(), "system_server");
    }


    private void forkPostCommon(boolean isSystem, String appDataDir, String niceName) {
        // init logger
        ModuleLogger.initLogger(serviceClient.getModuleLogger());
        mRouter.initResourcesHook();
        mRouter.prepare(isSystem);
        PrebuiltMethodsDeopter.deoptBootMethods(); // do it once for secondary zygote
        mRouter.installBootstrapHooks(isSystem, appDataDir);
        mRouter.onEnterChildProcess();
        Utils.logI("Loading modules for " + niceName);
        mRouter.loadModulesSafely(true);
    }

}
