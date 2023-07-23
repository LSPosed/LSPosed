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
 * Copyright (C) 2023 LSPosed Contributors
 */

package org.lsposed.manager.services;

import android.os.IBinder;

import org.lsposed.lspd.ILSPManagerDispatchService;
import org.lsposed.manager.App;
import org.lsposed.manager.adapters.AppHelper;
import org.lsposed.manager.util.ModuleUtil;

public class LSPManagerDispatchService extends ILSPManagerDispatchService.Stub {

    @Override
    public boolean reloadSingleModule(String packageName, int userId, boolean packageRemovedForAllUsers) {
        ModuleUtil.getInstance().reloadSingleModule(packageName, userId, packageRemovedForAllUsers);
        return true;
    }

    @Override
    public boolean reloadInstalledModules() {
        App.getExecutorService().submit(() -> ModuleUtil.getInstance().reloadInstalledModules());
        return true;
    }

    @Override
    public boolean refreshAppList(boolean force) {
        App.getExecutorService().submit(() -> AppHelper.getAppList(force));
        return true;
    }

    @Override
    public IBinder asBinder() {
        return this;
    }
}
