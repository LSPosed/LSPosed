package com.elderdrivers.riru.edxp.sandhook.entry;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook.hooker.HandleBindAppHooker;
import com.elderdrivers.riru.edxp.sandhook.hooker.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp.sandhook.hooker.SystemMainHooker;

public class SysBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            SystemMainHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName()
    };

    public static Class[] hookItems = {
            HandleBindAppHooker.class,
            SystemMainHooker.class,
            LoadedApkConstructorHooker.class,
    };
}
