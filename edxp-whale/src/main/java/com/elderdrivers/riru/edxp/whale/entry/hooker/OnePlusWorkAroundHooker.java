package com.elderdrivers.riru.edxp.whale.entry.hooker;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.Main;
import com.elderdrivers.riru.edxp.whale.entry.Router;

import de.robv.android.xposed.XposedBridge;

/**
 * On OnePlus stock roms (Android Pie), {@link dalvik.system.BaseDexClassLoader#findClass(String)}
 * will open /dev/binder to communicate with PackageManagerService to check whether
 * current package name inCompatConfigList, which is an OnePlus OEM feature enabled only when
 * system prop "persist.sys.oem.region" set to "CN".(detail of related source code:
 * https://gist.github.com/solohsu/ecc07141759958fc096ba0781fac0a5f)
 * If we invoke intZygoteCallbacks in
 * {@link Main#forkAndSpecializePre}, where in zygote process,
 * we would get a chance to invoke findclass, leaving fd of /dev/binder open in zygote process,
 * which is not allowed because /dev/binder is not in predefined whitelist here:
 * http://androidxref.com/9.0.0_r3/xref/frameworks/base/core/jni/fd_utils.cpp#35
 * So we just hook BaseDexClassLoader#inCompatConfigList to return false to prevent
 * open of /dev/binder and we haven't found side effects yet.
 * Other roms might share the same problems but not reported too.
 */
public class OnePlusWorkAroundHooker implements KeepMembers {

    public static String className = "dalvik.system.BaseDexClassLoader";
    public static String methodName = "inCompatConfigList";
    public static String methodSig = "(ILjava/lang/String;)Z";

    public static boolean hook(int type, String packageName) {
        if (XposedBridge.disableHooks || Router.forkCompleted) {
            return backup(type, packageName);
        }
        Router.logD("BaseDexClassLoader#inCompatConfigList() starts");
        return false;
    }

    public static boolean backup(int type, String packageName) {
        return false;
    }
}