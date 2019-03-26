package com.elderdrivers.riru.edxp.whale.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.whale.entry.hooker.HandleBindAppHooker;
import com.elderdrivers.riru.edxp.whale.entry.hooker.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp.whale.entry.hooker.OnePlusWorkAroundHooker;

public class AppBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName(),
            OnePlusWorkAroundHooker.class.getName()
    };
}
