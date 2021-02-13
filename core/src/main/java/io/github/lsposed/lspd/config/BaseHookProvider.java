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

package io.github.lsposed.lspd.config;

import io.github.lsposed.lspd.nativebridge.Yahfa;
import io.github.lsposed.lspd.deopt.PrebuiltMethodsDeopter;
import io.github.lsposed.lspd.hook.HookProvider;

import java.lang.reflect.Member;

public abstract class BaseHookProvider implements HookProvider {

    @Override
    public void unhookMethod(Member method) {

    }

    public Member findMethodNative(Member hookMethod) {
        return hookMethod;
    }

    public long getMethodId(Member member) {
        return 0;
    }

    public Object findMethodNative(Class clazz, String methodName, String methodSig) {
        return null;
    }

    public void deoptMethods(String packageName, ClassLoader classLoader) {
        PrebuiltMethodsDeopter.deoptMethods(packageName, classLoader);
    }

    @Override
    public void deoptMethodNative(Object method) {

    }

    @Override
    public boolean initXResourcesNative() {
        return false;
    }

    @Override
    public boolean methodHooked(Member target) {
        return Yahfa.isHooked(target);
    }

    @Override
    public Object invokeOriginalMethod(Member method, long methodId, Object thisObject, Object[] args) throws Throwable {
        return null;
    }
}
