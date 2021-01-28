package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import java.util.List;

public interface IPackageManager extends IInterface {

    void forceDexOpt(String packageName) throws RemoteException;
    boolean performDexOptMode(String packageName, boolean checkProfiles,
                              String targetCompilerFilter, boolean force, boolean bootComplete, String splitName) throws RemoteException;
    void clearApplicationProfileData(String packageName) throws RemoteException;
    List<String> getAllPackages() throws RemoteException;
    boolean runBackgroundDexoptJob(List<String> packageNames) throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
