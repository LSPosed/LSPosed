package org.lsposed.lspd.config;

import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import org.lsposed.lspd.service.ILSPApplicationService;

import java.util.List;
import java.util.Map;

abstract public class ApplicationServiceClient implements ILSPApplicationService {

    public static ApplicationServiceClient serviceClient = null;

    @Override
    abstract public IBinder requestModuleBinder();

    @Override
    abstract public boolean requestManagerBinder(String packageName, String path, List<IBinder> binder);

    @Override
    abstract public boolean isResourcesHookEnabled();

    @Override
    abstract public Map getModulesList(String processName);

    abstract public Map<String, String> getModulesList();

    @Override
    abstract public String getPrefsPath(String packageName);

    @Override
    abstract public ParcelFileDescriptor getModuleLogger();
}
