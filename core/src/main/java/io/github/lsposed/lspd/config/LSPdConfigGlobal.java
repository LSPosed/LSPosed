package io.github.lsposed.lspd.config;

import io.github.lsposed.lspd.hook.HookProvider;

public class LSPdConfigGlobal {

    public static volatile HookProvider sHookProvider;

    public static HookProvider getHookProvider() {
        if (sHookProvider == null) {
            throw new IllegalArgumentException("sHookProvider should not be null.");
        }
        return sHookProvider;
    }
}
