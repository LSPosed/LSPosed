package org.lsposed.lspd.config;

import android.os.IBinder;

import org.lsposed.lspd.models.Module;
import org.lsposed.lspd.service.ILSPApplicationService;

import java.util.List;

abstract public class ApplicationServiceClient implements ILSPApplicationService {

    public static ApplicationServiceClient serviceClient = null;

    @Override
    abstract public IBinder requestModuleBinder(String name);

    @Override
    abstract public List<Module> getModulesList(String processName);

    abstract public List<Module> getModulesList();

    @Override
    abstract public String getPrefsPath(String packageName);
}
