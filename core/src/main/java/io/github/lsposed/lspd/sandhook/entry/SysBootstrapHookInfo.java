package io.github.lsposed.lspd.sandhook.entry;

import io.github.lsposed.common.KeepMembers;
import io.github.lsposed.lspd.sandhook.hooker.HandleBindAppHooker;
import io.github.lsposed.lspd.sandhook.hooker.LoadedApkConstructorHooker;
import io.github.lsposed.lspd.sandhook.hooker.SystemMainHooker;

public class SysBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            SystemMainHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName()
    };

    public static Class[] hookItems = {
            HandleBindAppHooker.class,
            SystemMainHooker.class,
            LoadedApkConstructorHooker.class,
    };
}
