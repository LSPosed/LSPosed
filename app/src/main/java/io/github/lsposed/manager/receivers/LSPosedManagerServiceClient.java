package io.github.lsposed.manager.receivers;

import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.util.List;

import io.github.lsposed.lspd.Application;
import io.github.lsposed.lspd.ILSPManagerService;
import io.github.lsposed.lspd.utils.ParceledListSlice;

public class LSPosedManagerServiceClient {

    @SuppressWarnings("FieldMayBeFinal")
    private static IBinder binder = null;
    private static ILSPManagerService service = null;

    private static void ensureService() throws NullPointerException {
        if (service == null) {
            if (binder != null) {
                service = ILSPManagerService.Stub.asInterface(binder);
            } else {
                throw new NullPointerException("binder is null");
            }
        }
    }

    public static int getXposedApiVersion() throws RemoteException, NullPointerException {
        ensureService();
        return service.getXposedApiVersion();
    }

    public static String getXposedVersionName() throws RemoteException, NullPointerException {
        ensureService();
        return service.getXposedVersionName();
    }

    public static int getXposedVersionCode() throws RemoteException, NullPointerException {
        ensureService();
        return service.getXposedVersionCode();
    }


    public static List<PackageInfo> getInstalledPackagesFromAllUsers(int flags, boolean filterNoProcess) throws RemoteException, NullPointerException {
        ensureService();
        ParceledListSlice<PackageInfo> parceledListSlice = service.getInstalledPackagesFromAllUsers(flags, false);
        //
        return parceledListSlice.getList();
    }

    public static String[] enabledModules() throws RemoteException, NullPointerException {
        ensureService();
        return service.enabledModules();
    }


    public static boolean enableModule(String packageName) throws RemoteException, NullPointerException {
        ensureService();
        return service.enableModule(packageName);
    }

    public static boolean disableModule(String packageName) throws RemoteException, NullPointerException {
        ensureService();
        return service.disableModule(packageName);
    }

    public static boolean setModuleScope(String packageName, ParceledListSlice<Application> list) throws RemoteException, NullPointerException {
        ensureService();
        return service.setModuleScope(packageName, list);
    }

    public static ParceledListSlice<Application> getModuleScope(String packageName) throws RemoteException, NullPointerException {
        ensureService();
        return service.getModuleScope(packageName);
    }

    public static boolean isResourceHook() throws RemoteException, NullPointerException {
        ensureService();
        return service.isResourceHook();
    }

    public static void setResourceHook(boolean enabled) throws RemoteException, NullPointerException {
        ensureService();
        service.setResourceHook(enabled);
    }

    public static boolean isVerboseLog() throws RemoteException, NullPointerException {
        ensureService();
        return service.isVerboseLog();
    }

    public static void setVerboseLog(boolean enabled) throws RemoteException, NullPointerException {
        ensureService();
        service.setVerboseLog(enabled);
    }

    public static int getVariant() throws RemoteException, NullPointerException {
        ensureService();
        return service.getVariant();
    }

    public static void setVariant(int variant) throws RemoteException, NullPointerException {
        ensureService();
        service.setVariant(variant);
    }

    public static boolean isPermissive() throws RemoteException, NullPointerException {
        ensureService();
        return service.isPermissive();
    }

    public static ParcelFileDescriptor getVerboseLog() throws RemoteException, NullPointerException {
        ensureService();
        return service.getVerboseLog();
    }

    public static ParcelFileDescriptor getModulesLog() throws RemoteException, NullPointerException {
        ensureService();
        return service.getModulesLog();
    }

    public static boolean clearLogs(boolean verbose) throws RemoteException, NullPointerException {
        ensureService();
        return service.clearLogs(verbose);
    }

    public static PackageInfo getPackageInfo(String packageName, int flags, int uid) throws RemoteException, NullPointerException {
        ensureService();
        return service.getPackageInfo(packageName, flags, uid);
    }
}
