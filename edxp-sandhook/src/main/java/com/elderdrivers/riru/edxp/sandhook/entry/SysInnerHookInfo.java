package com.elderdrivers.riru.edxp.sandhook.entry;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook.hooker.StartBootstrapServicesHooker;
import com.elderdrivers.riru.edxp.sandhook.hooker.StartBootstrapServicesHooker11;
import com.elderdrivers.riru.edxp.util.Versions;

public class SysInnerHookInfo implements KeepMembers {

    public static Class<?> getSysInnerHookerClass() {
        return Versions.hasR() ?
                StartBootstrapServicesHooker11.class :
                StartBootstrapServicesHooker.class;
    }

    public static String[] hookItemNames = {
            getSysInnerHookerClass().getName()
    };

    public static Class[] hookItems = {
            getSysInnerHookerClass()
    };
}
