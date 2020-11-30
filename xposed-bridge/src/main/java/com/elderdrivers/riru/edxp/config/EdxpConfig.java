package com.elderdrivers.riru.edxp.config;

public interface EdxpConfig {

    String getConfigPath(String suffix);

    String getDataPathPrefix();

    String getInstallerPackageName();

    String getXposedPropPath();

    String getLibSandHookName();

    boolean isNoModuleLogEnabled();

    boolean isResourcesHookEnabled();

    boolean isBlackWhiteListMode();

    String getModulesList();
}
