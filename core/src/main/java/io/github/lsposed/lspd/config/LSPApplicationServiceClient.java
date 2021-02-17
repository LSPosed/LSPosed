package io.github.lsposed.lspd.config;

import android.os.IBinder;
import android.os.RemoteException;

import java.io.File;

import io.github.lsposed.lspd.service.ILSPApplicationService;
import io.github.lsposed.lspd.util.Utils;

public class LSPApplicationServiceClient implements ILSPApplicationService {
    static ILSPApplicationService service = null;
    static IBinder serviceBinder = null;

    static String baseCachePath = null;
    static String basePrefsPath = null;

    public static LSPApplicationServiceClient serviceClient = null;

    public static void Init(IBinder binder) {
        if (serviceClient == null && binder != null && serviceBinder == null && service == null) {
            serviceBinder = binder;
            try {
                serviceBinder.linkToDeath(
                        new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                serviceBinder.unlinkToDeath(this, 0);
                                serviceBinder = null;
                                service = null;
                            }
                        }, 0);
            } catch (RemoteException e) {
                Utils.logE("link to death error: ", e);
            }
            service = ILSPApplicationService.Stub.asInterface(binder);
            serviceClient = new LSPApplicationServiceClient();
        }
    }

    @Override
    public void registerHeartBeat(IBinder handle) {
        if (service == null || serviceBinder == null) {
            Utils.logE("Register Failed: service is null");
        }
        try {
            service.registerHeartBeat(handle);
        } catch (RemoteException e) {
            Utils.logE("register heart beat failed", e);
        }
    }

    @Override
    public IBinder requestModuleBinder() {
        try {
            return service.requestModuleBinder();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public IBinder requestManagerBinder() {
        try {
            return service.requestManagerBinder();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public int getVariant() {
        try {
            return service.getVariant();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return -1;
    }

    @Override
    public boolean isResourcesHookEnabled() {
        try {
            return service.isResourcesHookEnabled();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return false;
    }

    @Override
    public String[] getModulesList() {
        try {
            return service.getModulesList();
        } catch (RemoteException | NullPointerException ignored) {
        }
        return new String[0];
    }

    @Override
    public String getPrefsPath(String packageName) {
        try {
            if (basePrefsPath == null)
                basePrefsPath = service.getPrefsPath("");
            return basePrefsPath + File.separator + packageName;
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public String getCachePath(String fileName) {
        try {
            if (baseCachePath == null)
                baseCachePath = service.getCachePath("");
            return baseCachePath + File.separator + fileName;
        } catch (RemoteException | NullPointerException ignored) {
        }
        return null;
    }

    @Override
    public IBinder asBinder() {
        return serviceBinder;
    }
}
