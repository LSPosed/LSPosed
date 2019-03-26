package com.elderdrivers.riru.edxp.whale.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.whale.entry.hooker.OnePlusWorkAroundHooker;

public class WorkAroundHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            OnePlusWorkAroundHooker.class.getName()
    };
}
