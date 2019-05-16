package com.elderdrivers.riru.edxp.config;

import com.elderdrivers.riru.edxp.hook.HookProvider;

public class EdXpConfigGlobal {

    public static volatile EdXpConfig sConfig;
    public static volatile HookProvider sHookProvider;

    public static EdXpConfig getConfig() {
        if (sConfig == null) {
            throw new IllegalArgumentException("sConfig should not be null.");
        }
        return sConfig;
    }

    public static HookProvider getHookProvider() {
        if (sHookProvider == null) {
            throw new IllegalArgumentException("sHookProvider should not be null.");
        }
        return sHookProvider;
    }
}
