package com.elderdrivers.riru.edxp.hooker;

import android.os.StrictMode;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

@ApiSensitive(Level.LOW)
public class SliceProviderFix {

    public static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";

    public static void hook() {
        XposedHelpers.findAndHookMethod(StrictMode.ThreadPolicy.Builder.class, "build", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(param.thisObject, "permitAll");
            }
        });
    }

}
