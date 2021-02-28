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

package io.github.lsposed.lspd.yahfa.config;

import io.github.lsposed.lspd.nativebridge.ClassLinker;
import io.github.lsposed.lspd.config.BaseHookProvider;
import io.github.lsposed.lspd.nativebridge.ResourcesHook;
import io.github.lsposed.lspd.nativebridge.Yahfa;
import io.github.lsposed.lspd.yahfa.dexmaker.DynamicBridge;

import java.lang.reflect.Member;

import de.robv.android.xposed.XposedBridge;

import static io.github.lsposed.lspd.util.ClassUtils.shouldDelayHook;

public class YahfaHookProvider extends BaseHookProvider {

    @Override
    public void hookMethod(Member method, XposedBridge.AdditionalHookInfo additionalInfo) {
        DynamicBridge.hookMethod(method, additionalInfo);
    }

    @Override
    public Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args) throws Throwable {
        return DynamicBridge.invokeOriginalMethod(method, thisObject, args);
    }

    @Override
    public Member findMethodNative(Member hookMethod) {
        return shouldDelayHook(hookMethod) ? null : hookMethod;
    }

    @Override
    public Object findMethodNative(Class clazz, String methodName, String methodSig) {
        return Yahfa.findMethodNative(clazz, methodName, methodSig);
    }

    @Override
    public void deoptMethodNative(Object method) {
        ClassLinker.setEntryPointsToInterpreter((Member) method);
    }

    @Override
    public boolean initXResourcesNative() {
        return ResourcesHook.initXResourcesNative();
    }

    @Override
    public boolean removeFinalFlagNative(Class clazz) {
        return ResourcesHook.removeFinalFlagNative(clazz);
    }
}
