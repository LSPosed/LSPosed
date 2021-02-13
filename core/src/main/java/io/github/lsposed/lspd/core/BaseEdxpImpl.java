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

package io.github.lsposed.lspd.core;

import androidx.annotation.NonNull;

import io.github.lsposed.lspd.proxy.NormalProxy;
import io.github.lsposed.lspd.proxy.Router;

public abstract class BaseEdxpImpl implements EdxpImpl {

    protected Proxy mNormalProxy;
    protected Router mRouter;

    protected boolean mInitialized = false;

    protected void setInitialized() {
        mInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return mInitialized;
    }

    @NonNull
    @Override
    public Proxy getNormalProxy() {
        if (mNormalProxy == null) {
            mNormalProxy = createNormalProxy();
        }
        return mNormalProxy;
    }

    @NonNull
    @Override
    public Router getRouter() {
        if (mRouter == null) {
            mRouter = createRouter();
        }
        return mRouter;
    }

    protected Proxy createNormalProxy() {
        return new NormalProxy(getRouter());
    }

    protected abstract Router createRouter();
}
