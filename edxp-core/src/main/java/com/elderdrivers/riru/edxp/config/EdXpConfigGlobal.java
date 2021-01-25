package com.elderdrivers.riru.edxp.config;

import com.elderdrivers.riru.edxp.hook.HookProvider;

public class EdXpConfigGlobal {

    public static volatile HookProvider sHookProvider;

    public static HookProvider getHookProvider() {
        if (sHookProvider == null) {
            throw new IllegalArgumentException("sHookProvider should not be null.");
        }
        return sHookProvider;
    }
}
