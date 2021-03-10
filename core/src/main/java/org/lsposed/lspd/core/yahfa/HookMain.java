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

package org.lsposed.lspd.core.yahfa;

import org.lsposed.lspd.nativebridge.Yahfa;
import org.lsposed.lspd.util.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

public class HookMain {
    public static void backupAndHook(Executable target, Method hook, Method backup) {
        Utils.logD(String.format("target=%s, hook=%s, backup=%s", target, hook, backup));
        if (target == null) {
            throw new IllegalArgumentException("null target method");
        }
        if (hook == null) {
            throw new IllegalArgumentException("null hook method");
        }

        if (!Modifier.isStatic(hook.getModifiers())) {
            throw new IllegalArgumentException("Hook must be a static method: " + hook);
        }
        checkCompatibleMethods(target, hook, "Hook");
        if (backup != null) {
            if (!Modifier.isStatic(backup.getModifiers())) {
                throw new IllegalArgumentException("Backup must be a static method: " + backup);
            }
            // backup is just a placeholder and the constraint could be less strict
            checkCompatibleMethods(target, backup, "Backup");
        }
        if(!Yahfa.backupAndHookNative(target, hook, backup)){
            throw new RuntimeException("Failed to hook " + target + " with " + hook);
        } else {
            Yahfa.recordHooked(target);
        }
    }

    private static void checkCompatibleMethods(Executable original, Method replacement, String replacementName) {
        ArrayList<Class<?>> originalParams;
        if (original instanceof Method) {
            originalParams = new ArrayList<>(Arrays.asList(((Method) original).getParameterTypes()));
        } else if (original instanceof Constructor) {
            originalParams = new ArrayList<>(Arrays.asList(((Constructor<?>) original).getParameterTypes()));
        } else {
            throw new IllegalArgumentException("Type of target method is wrong");
        }

        ArrayList<Class<?>> replacementParams = new ArrayList<>(Arrays.asList(replacement.getParameterTypes()));

        if (original instanceof Method
                && !Modifier.isStatic(((Method) original).getModifiers())) {
            originalParams.add(0, ((Method) original).getDeclaringClass());
        } else if (original instanceof Constructor) {
            originalParams.add(0, ((Constructor<?>) original).getDeclaringClass());
        }


        if (!Modifier.isStatic(replacement.getModifiers())) {
            replacementParams.add(0, replacement.getDeclaringClass());
        }

        if (original instanceof Method
                && !replacement.getReturnType().isAssignableFrom(((Method) original).getReturnType())) {
            throw new IllegalArgumentException("Incompatible return types. " + "Original" + ": " + ((Method) original).getReturnType() + ", " + replacementName + ": " + replacement.getReturnType());
        } else if (original instanceof Constructor) {
            if (replacement.getReturnType().equals(Void.class)) {
                throw new IllegalArgumentException("Incompatible return types. " + "<init>" + ": " + "V" + ", " + replacementName + ": " + replacement.getReturnType());
            }
        }

        if (originalParams.size() != replacementParams.size()) {
            throw new IllegalArgumentException("Number of arguments don't match. " + "Original" + ": " + originalParams.size() + ", " + replacementName + ": " + replacementParams.size());
        }

        for (int i = 0; i < originalParams.size(); i++) {
            if (!replacementParams.get(i).isAssignableFrom(originalParams.get(i))) {
                throw new IllegalArgumentException("Incompatible argument #" + i + ": " + "Original" + ": " + originalParams.get(i) + ", " + replacementName + ": " + replacementParams.get(i));
            }
        }
    }
}
