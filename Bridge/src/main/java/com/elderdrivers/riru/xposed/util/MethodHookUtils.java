package com.elderdrivers.riru.xposed.util;

import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

public class MethodHookUtils {

    /**
     * FIXME
     * Some methods, for instance Application#attach(Context) in framework would be inlined,
     * which makes our hooking of them not working.
     * Actually we can append --debuggable option to dexoat args to avoid inlining,
     * but it has significant impact on app's performance.
     * So here is just a temporary workaround.
     */
    public static Member preCheck(Member target) {
        try {
            if (target instanceof Method) {
                Method method = (Method) target;
                if (method.getDeclaringClass().equals(Application.class)
                        && method.getName().equals("attach")) {
                    Utils.logW("replacing Application#attch to ContextWrapper#attachBaseContext, this is error-prone!");
                    return ContextWrapper.class.getDeclaredMethod("attachBaseContext", Context.class);
                }
            }
        } catch (Throwable throwable) {
            Utils.logE("error when preCheck " + target, throwable);
        }
        return target;
    }

}
