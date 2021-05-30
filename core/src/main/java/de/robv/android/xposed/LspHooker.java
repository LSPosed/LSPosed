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

package de.robv.android.xposed;

import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class LspHooker {
    private final XposedBridge.AdditionalHookInfo additionalInfo;
    private final Executable method;
    private final Method backup;

    public LspHooker(XposedBridge.AdditionalHookInfo info, Executable origin, Method backup) {
        this.additionalInfo = info;
        this.method = origin;
        this.backup = backup;
        this.backup.setAccessible(true);
    }

    public Object invokeOriginalMethod(Object thisObject, Object[] args) throws InvocationTargetException, IllegalAccessException {
        return backup.invoke(thisObject, args);
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public Object handleHookedMethod(Object[] args) throws Throwable {
        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();

        param.method = method;

        if (Modifier.isStatic(method.getModifiers())) {
            param.thisObject = null;
            param.args = args;
        } else {
            param.thisObject = args[0];
            param.args = new Object[args.length - 1];
            System.arraycopy(args, 1, param.args, 0, args.length - 1);
        }

        Object[] callbacksSnapshot = additionalInfo.callbacks.getSnapshot();
        final int callbacksLength = callbacksSnapshot.length;
        if (callbacksLength == 0) {
            try {
                return backup.invoke(param.thisObject, param.args);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
        }

        // call "before method" callbacks
        int beforeIdx = 0;
        do {
            try {
                ((XC_MethodHook) callbacksSnapshot[beforeIdx]).beforeHookedMethod(param);
            } catch (Throwable t) {
                XposedBridge.log(t);

                // reset result (ignoring what the unexpectedly exiting callback did)
                param.setResult(null);
                param.returnEarly = false;
                continue;
            }

            if (param.returnEarly) {
                // skip remaining "before" callbacks and corresponding "after" callbacks
                beforeIdx++;
                break;
            }
        } while (++beforeIdx < callbacksLength);

        // call original method if not requested otherwise
        if (!param.returnEarly) {
            try {
                param.setResult(backup.invoke(param.thisObject, param.args));
            } catch (InvocationTargetException e) {
                param.setThrowable(e.getCause());
            }
        }

        // call "after method" callbacks
        int afterIdx = beforeIdx - 1;
        do {
            Object lastResult = param.getResult();
            Throwable lastThrowable = param.getThrowable();

            try {
                ((XC_MethodHook) callbacksSnapshot[afterIdx]).afterHookedMethod(param);
            } catch (Throwable t) {
                XposedBridge.log(t);

                // reset to last result (ignoring what the unexpectedly exiting callback did)
                if (lastThrowable == null)
                    param.setResult(lastResult);
                else
                    param.setThrowable(lastThrowable);
            }
        } while (--afterIdx >= 0);

        // return
        if (param.hasThrowable())
            throw param.getThrowable();
        else {
            var result = param.getResult();
            if (method instanceof Method) {
                var returnType = ((Method) method).getReturnType();
                if (!returnType.isPrimitive())
                    return returnType.cast(result);
            }
            return result;
        }
    }

}
