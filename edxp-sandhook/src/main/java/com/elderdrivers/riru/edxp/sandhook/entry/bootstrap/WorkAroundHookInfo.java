package com.elderdrivers.riru.edxp.sandhook.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook.entry.hooker.OnePlusWorkAroundHooker;

public class WorkAroundHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            OnePlusWorkAroundHooker.class.getName()
    };

    public static Class[] hookItems = {
            OnePlusWorkAroundHooker.class
    };
}
