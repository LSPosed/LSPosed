package com.elderdrivers.riru.edxp.art;

import com.elderdrivers.riru.common.KeepAll;
import com.elderdrivers.riru.edxp.core.Yahfa;

import java.lang.reflect.Member;
import java.util.function.Consumer;

import de.robv.android.xposed.PendingHooks;

public class ClassLinker implements KeepAll {

    public static native void setEntryPointsToInterpreter(Member method);

    public static void onPreFixupStaticTrampolines(Class clazz) {
        // remove modified native flags to let FixupStaticTrampolines fill in right entrypoints
        PendingHooks.removeNativeFlags(clazz);
    }

    public static void onPostFixupStaticTrampolines(Class clazz) {
        // native flags will be re-set in hooking logic
        PendingHooks.hookPendingMethod(clazz);
    }
}
