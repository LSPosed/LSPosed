package com.elderdrivers.riru.xposed.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.xposed.entry.hooker.HandleBindAppHooker;
import com.elderdrivers.riru.xposed.entry.hooker.LoadedApkConstructorHooker;

public class AppBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName()
    };
}
