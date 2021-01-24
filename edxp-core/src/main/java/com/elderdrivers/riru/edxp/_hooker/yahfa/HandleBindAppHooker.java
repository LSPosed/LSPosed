package com.elderdrivers.riru.edxp._hooker.yahfa;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.impl.HandleBindApp;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public class HandleBindAppHooker implements KeepMembers {

    public static String className = "android.app.ActivityThread";
    public static String methodName = "handleBindApplication";
    public static String methodSig = "(Landroid/app/ActivityThread$AppBindData;)V";

    public static void hook(final Object thiz, final Object bindData) throws Throwable {
        final XC_MethodHook methodHook = new HandleBindApp();
        final XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam();
        param.thisObject = thiz;
        param.args = new Object[]{bindData};
        methodHook.callBeforeHookedMethod(param);
        if (!param.returnEarly) {
            backup(thiz, bindData);
        }
        methodHook.callAfterHookedMethod(param);
    }

    public static void backup(Object thiz, Object bindData) {
    }
}