package io.github.lsposed.lspd.sandhook.entry;

import io.github.lsposed.common.KeepMembers;
import io.github.lsposed.lspd.sandhook.hooker.StartBootstrapServicesHooker;
import io.github.lsposed.lspd.sandhook.hooker.StartBootstrapServicesHooker11;
import io.github.lsposed.lspd.util.Versions;

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
