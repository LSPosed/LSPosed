package com.elderdrivers.riru.xposed.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.xposed.entry.hooker.StartBootstrapServicesHooker;

public class SysInnerHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            StartBootstrapServicesHooker.class.getName()
    };
}
