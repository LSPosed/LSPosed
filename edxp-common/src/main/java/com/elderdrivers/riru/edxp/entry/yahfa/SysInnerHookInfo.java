package com.elderdrivers.riru.edxp.entry.yahfa;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.yahfa.StartBootstrapServicesHooker;
import com.elderdrivers.riru.edxp._hooker.yahfa.StartBootstrapServicesHooker11;
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
}
