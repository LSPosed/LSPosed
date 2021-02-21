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

package io.github.lsposed.lspd.yahfa.core;

import android.os.Build;

import io.github.lsposed.lspd.core.BaseImpl;
import io.github.lsposed.lspd.core.Impl;
import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.core.Proxy;
import io.github.lsposed.lspd.nativebridge.Yahfa;
import io.github.lsposed.lspd.proxy.NormalProxy;
import io.github.lsposed.lspd.proxy.Router;

public class YahfaImpl extends BaseImpl {

    static {
        final Impl lspdImpl = new YahfaImpl();
        if (Main.setImpl(lspdImpl)) {
            lspdImpl.init();
        }
    }

    @Variant
    @Override
    public int getVariant() {
        return YAHFA;
    }

    @Override
    public void init() {
        int sdkVersion = Build.VERSION.SDK_INT;
        if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            sdkVersion += 1;
        }
        Yahfa.init(sdkVersion);
        getRouter().injectConfig();
        setInitialized();
    }

    @Override
    protected Proxy createNormalProxy() {
        return new NormalProxy(getRouter());
    }

    @Override
    protected Router createRouter() {
        return new YahfaRouter();
    }
}
