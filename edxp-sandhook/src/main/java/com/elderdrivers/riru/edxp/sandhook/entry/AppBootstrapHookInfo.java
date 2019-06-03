package com.elderdrivers.riru.edxp.sandhook.entry;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook.hooker.HandleBindAppHooker;
import com.elderdrivers.riru.edxp.sandhook.hooker.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp.sandhook.hooker.OnePlusWorkAroundHooker;

public class AppBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            OnePlusWorkAroundHooker.class.getName(),
            HandleBindAppHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName(),
    };

    public static Class[] hookItems = {
            HandleBindAppHooker.class,
            LoadedApkConstructorHooker.class,
            OnePlusWorkAroundHooker.class
    };
}
