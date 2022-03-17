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
 * Copyright (C) 2022 LSPosed Contributors
 */

package org.lsposed.lspd.core;

import android.os.IBinder;

import org.lsposed.lspd.BuildConfig;
import org.lsposed.lspd.config.LSPApplicationServiceClient;
import org.lsposed.lspd.util.ParasiticManagerHooker;
import org.lsposed.lspd.util.Utils;

public class Main {

    public static void forkCommon(boolean isSystem, String niceName, IBinder binder) {
        LSPApplicationServiceClient.Init(binder, niceName);
        Startup.initXposed(isSystem);
        if ((niceName.equals(BuildConfig.MANAGER_INJECTED_PKG_NAME) || niceName.equals(BuildConfig.DEFAULT_MANAGER_PACKAGE_NAME))
                && ParasiticManagerHooker.start()) {
            Utils.logI("Loaded manager, skipping next steps");
            return;
        }
        Startup.bootstrapXposed(isSystem, niceName);
    }
}
