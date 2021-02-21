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

package io.github.lsposed.lspd.sandhook.core;

import android.os.Build;

import io.github.lsposed.lspd.core.BaseImpl;
import io.github.lsposed.lspd.core.Impl;
import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.nativebridge.SandHook;
import io.github.lsposed.lspd.nativebridge.Yahfa;

import com.swift.sandhook.ClassNeverCall;
import com.swift.sandhook.xposedcompat.methodgen.SandHookXposedBridge;

public class SandHookImpl extends BaseImpl {

    static {
        final Impl lspdImpl = new SandHookImpl();
        if (Main.setImpl(lspdImpl)) {
            lspdImpl.init();
        }
    }

    @Override
    protected io.github.lsposed.lspd.proxy.Router createRouter() {
        return new SandHookRouter();
    }

    @Variant
    @Override
    public int getVariant() {
        return SANDHOOK;
    }

    @Override
    public void init() {
        SandHook.init(com.swift.sandhook.SandHook.class, ClassNeverCall.class);
        int sdkVersion = Build.VERSION.SDK_INT;
        if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            sdkVersion += 1;
        }
        Yahfa.init(sdkVersion);
        getRouter().injectConfig();
        SandHookXposedBridge.init();
        setInitialized();
    }
}
