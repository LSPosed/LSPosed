package com.elderdrivers.riru.edxp.sandhook.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook._hooker.StartBootstrapServicesHooker;

public class SysInnerHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            StartBootstrapServicesHooker.class.getName()
    };

    public static Class[] hookItems = {
            StartBootstrapServicesHooker.class
    };
}
