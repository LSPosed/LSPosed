package io.github.lsposed.lspd.service;

import android.os.IBinder;

import de.robv.android.xposed.XposedBridge;
import io.github.xposed.xposedservice.IXposedService;

public class LSPModuleService extends IXposedService.Stub {

    public LSPModuleService() {
    }

    @Override
    public IBinder asBinder() {
        return this;
    }

    @Override
    public int getVersion() {
        return XposedBridge.getXposedVersion();
    }
}
