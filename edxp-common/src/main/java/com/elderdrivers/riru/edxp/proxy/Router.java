package com.elderdrivers.riru.edxp.proxy;

public interface Router {

    void initResourcesHook();

    void prepare(boolean isSystem);

    String parsePackageName(String appDataDir);

    void installBootstrapHooks(boolean isSystem);

    void loadModulesSafely(boolean callInitZygote);

    void startBootstrapHook(boolean isSystem);

    void startSystemServerHook();

    void onEnterChildProcess();

    void injectConfig();
}
