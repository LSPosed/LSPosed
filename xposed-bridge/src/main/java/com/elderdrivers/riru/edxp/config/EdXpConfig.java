package com.elderdrivers.riru.edxp.config;

public interface EdXpConfig {

    String getInstallerBaseDir();

    String getBlackListModulePackageName();

    boolean isDynamicModulesMode();

    boolean isResourcesHookEnabled();

}
