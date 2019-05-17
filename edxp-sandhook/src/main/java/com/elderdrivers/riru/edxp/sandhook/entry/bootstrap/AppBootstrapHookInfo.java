package com.elderdrivers.riru.edxp.sandhook.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook._hooker.HandleBindAppHooker;
import com.elderdrivers.riru.edxp.sandhook._hooker.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp.sandhook._hooker.OnePlusWorkAroundHooker;

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
