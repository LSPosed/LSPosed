package com.elderdrivers.riru.edxp.proxy;

public interface Router {

    void initResourcesHook();

    void prepare(boolean isSystem);

    String parsePackageName(String appDataDir);

    void installBootstrapHooks(boolean isSystem);

    void loadModulesSafely(boolean isInZygote);

    void startBootstrapHook(boolean isSystem);

    void startSystemServerHook();

    void startWorkAroundHook();

    void onForkStart();

    void onForkFinish();

    void onEnterChildProcess();

    void injectConfig();

    boolean isForkCompleted();
}
