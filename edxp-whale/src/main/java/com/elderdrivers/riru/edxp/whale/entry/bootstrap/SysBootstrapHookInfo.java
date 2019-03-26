package com.elderdrivers.riru.edxp.whale.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.whale.entry.hooker.HandleBindAppHooker;
import com.elderdrivers.riru.edxp.whale.entry.hooker.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp.whale.entry.hooker.OnePlusWorkAroundHooker;
import com.elderdrivers.riru.edxp.whale.entry.hooker.SystemMainHooker;

public class SysBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            SystemMainHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName(),
            OnePlusWorkAroundHooker.class.getName()
    };
}
