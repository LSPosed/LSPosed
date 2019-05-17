package com.elderdrivers.riru.edxp.whale.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.whale._hooker.StartBootstrapServicesHooker;

public class SysInnerHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            StartBootstrapServicesHooker.class.getName()
    };
}
