package io.github.lsposed.lspd.nativebridge;

import io.github.lsposed.common.KeepAll;

import java.lang.reflect.Member;

import de.robv.android.xposed.PendingHooks;

public class ClassLinker implements KeepAll {

    public static native void setEntryPointsToInterpreter(Member method);

    public static void onPostFixupStaticTrampolines(Class clazz) {
        // native flags will be re-set in hooking logic
        PendingHooks.hookPendingMethod(clazz);
    }
}
