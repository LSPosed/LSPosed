package com.elderdrivers.riru.edxp.sandhook.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook.entry.hooker.HandleBindAppHooker;
import com.elderdrivers.riru.edxp.sandhook.entry.hooker.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp.sandhook.entry.hooker.OnePlusWorkAroundHooker;
import com.elderdrivers.riru.edxp.sandhook.entry.hooker.SystemMainHooker;

public class SysBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            SystemMainHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName(),
            OnePlusWorkAroundHooker.class.getName()
    };
}
