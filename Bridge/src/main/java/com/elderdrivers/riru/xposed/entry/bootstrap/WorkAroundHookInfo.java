package com.elderdrivers.riru.xposed.entry.bootstrap;

import com.elderdrivers.riru.common.KeepMembers;
import com.elderdrivers.riru.xposed.entry.hooker.OnePlusWorkAroundHooker;

public class WorkAroundHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            OnePlusWorkAroundHooker.class.getName()
    };
}
