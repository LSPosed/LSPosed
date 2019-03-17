package com.elderdrivers.riru.edxp.yahfa.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.yahfa.entry.hooker.StartBootstrapServicesHooker;

public class SysInnerHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            StartBootstrapServicesHooker.class.getName()
    };
}
