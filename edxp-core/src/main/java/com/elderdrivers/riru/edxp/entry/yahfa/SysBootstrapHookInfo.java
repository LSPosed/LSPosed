package com.elderdrivers.riru.edxp.entry.yahfa;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp._hooker.yahfa.HandleBindAppHooker;
import com.elderdrivers.riru.edxp._hooker.yahfa.LoadedApkConstructorHooker;
import com.elderdrivers.riru.edxp._hooker.yahfa.SystemMainHooker;

public class SysBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            SystemMainHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName(),
    };
}
