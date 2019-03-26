package com.elderdrivers.riru.edxp.whale.entry.hooker;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;

import com.elderdrivers.riru.edxp.util.Utils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.elderdrivers.riru.edxp.config.InstallerChooser.INSTALLER_PACKAGE_NAME;
import static com.elderdrivers.riru.edxp.util.FileUtils.IS_USING_PROTECTED_STORAGE;

public class XposedBlackListHooker {

    public static final String BLACK_LIST_PACKAGE_NAME = "com.flarejune.xposedblacklist";
    private static final String BLACK_LIST_PREF_NAME = "list";
    private static final String PREF_KEY_BLACK_LIST = "blackList";
    public static final String PREF_FILE_PATH = (IS_USING_PROTECTED_STORAGE ? "/data/user_de/0/" : "/data/data")
            + BLACK_LIST_PACKAGE_NAME + "/shared_prefs/" + BLACK_LIST_PREF_NAME + ".xml";
    private static final XSharedPreferences PREFERENCES = new XSharedPreferences(new File(PREF_FILE_PATH));
    // always white list. empty string is to make sure blackList does not contain empty packageName
    private static final List<String> WHITE_LIST = Arrays.asList(INSTALLER_PACKAGE_NAME, BLACK_LIST_PACKAGE_NAME, "");

    static {
        try {
            PREFERENCES.makeWorldReadable();
        } catch (Throwable throwable) {
            Utils.logE("error making pref worldReadable", throwable);
        }
    }

    public static boolean shouldDisableHooks(String packageName) {
        return XposedBridge.disableHooks || getBlackList().contains(packageName);
    }

    public static Set<String> getBlackList() {
        try {
            PREFERENCES.reload();
            Set<String> result = PREFERENCES.getStringSet(PREF_KEY_BLACK_LIST, new HashSet<String>());
            if (result != null) result.removeAll(WHITE_LIST);
            return result;
        } catch (Throwable throwable) {
            Utils.logE("error when reading black list", throwable);
            return new HashSet<>();
        }
    }

    public static void hook(ClassLoader classLoader) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(ContextWrapper.class, "getSharedPreferences", String.class, int.class, new XC_MethodHook() {
                @TargetApi(Build.VERSION_CODES.N)
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        String prefName = (String) param.args[0];
                        if (!prefName.equals(BLACK_LIST_PREF_NAME)) {
                            return;
                        }
                        Activity activity = (Activity) param.thisObject;
                        Context context = activity.createDeviceProtectedStorageContext();
                        context.moveSharedPreferencesFrom(activity, prefName);
                        param.setResult(context.getSharedPreferences(prefName, (int) param.args[1]));
                    } catch (Throwable throwable) {
                        Utils.logE("error hooking Xposed BlackList", throwable);
                    }
                }
            });
        } catch (Throwable throwable) {
            Utils.logE("error hooking Xposed BlackList", throwable);
        }
    }
}
