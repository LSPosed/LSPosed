package de.robv.android.xposed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static de.robv.android.xposed.XposedBridge.disableHooks;

public class LspHooker {
    private final XposedBridge.AdditionalHookInfo additionalInfo;
    private final Member method;
    private final Method backup;

    public LspHooker(XposedBridge.AdditionalHookInfo info, Member origin, Method backup) {
        this.additionalInfo = info;
        this.method = origin;
        this.backup = backup;
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

        if (disableHooks) {
            try {
                return backup.invoke(param.thisObject, param.args);
            } catch (InvocationTargetException ite) {
                throw ite.getCause();
            }
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
        else
            return param.getResult();
    }

}
