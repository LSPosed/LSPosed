package com.elderdrivers.riru.edxp.sandhook.entry;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.edxp.sandhook.hooker.OnePlusWorkAroundHooker;

public class WorkAroundHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            OnePlusWorkAroundHooker.class.getName()
    };

    public static Class[] hookItems = {
            OnePlusWorkAroundHooker.class
    };
}
