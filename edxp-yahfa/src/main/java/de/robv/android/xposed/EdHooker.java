package de.robv.android.xposed;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import static de.robv.android.xposed.XposedBridge.disableHooks;

public class EdHooker {
    private final XposedBridge.AdditionalHookInfo additionalInfo;
    private final Member method;
    private final Method backup;
    private final boolean isStatic;

    public EdHooker(XposedBridge.AdditionalHookInfo info, Member origin, Method backup, boolean isStatic) {
        this.additionalInfo = info;
        this.method = origin;
        this.backup = backup;
        this.isStatic = isStatic;
    }

    public Object callBackup(Object thisObject, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (args == null) {
            args = new Object[0];
        }
        if (isStatic) {
            return backup.invoke(null, args);
        } else {
            Object[] newArgs = new Object[args.length + 1];
            newArgs[0] = thisObject;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            return backup.invoke(null, newArgs);
        }
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    public Object handleHookedMethod(Object[] args) throws Throwable {
        if (disableHooks) {
            return backup.invoke(null, args);
        }

        Object[] callbacksSnapshot = additionalInfo.callbacks.getSnapshot();
        final int callbacksLength = callbacksSnapshot.length;
        if (callbacksLength == 0) {
            return backup.invoke(null, args);
        }

        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();

        param.method = method;

        if (isStatic) {
            param.thisObject = null;
            param.args = args;
        } else {
            param.thisObject = args[0];
            param.args = new Object[args.length - 1];
            System.arraycopy(args, 1, param.args, 0, args.length - 1);
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
            param.setResult(callBackup(param.thisObject, param.args));
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
