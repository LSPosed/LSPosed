package io.github.libxposed.service;

import io.github.xposed.xposedservice.IXposedService;

public abstract class XposedService extends IXposedService.Stub {

    public static final int API = 100;
    public static final String AUTHORITY_SUFFIX = ".XposedService";
    public static final String SEND_BINDER = "SendBinder";

    @Override
    public final int getVersion() {
        return API;
    }
}
