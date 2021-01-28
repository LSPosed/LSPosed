package io.github.lsposed.lspd.sandhook.entry;

import io.github.lsposed.common.KeepMembers;
import io.github.lsposed.lspd.sandhook.hooker.HandleBindAppHooker;
import io.github.lsposed.lspd.sandhook.hooker.LoadedApkConstructorHooker;

public class AppBootstrapHookInfo implements KeepMembers {
    public static String[] hookItemNames = {
            HandleBindAppHooker.class.getName(),
            LoadedApkConstructorHooker.class.getName(),
    };

    public static Class[] hookItems = {
            HandleBindAppHooker.class,
            LoadedApkConstructorHooker.class,
    };
}
