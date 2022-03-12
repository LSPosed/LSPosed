package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface IPackageInstaller extends IInterface {

    void uninstall(android.content.pm.VersionedPackage versionedPackage, java.lang.String callerPackageName, int flags, android.content.IntentSender statusReceiver, int userId) throws android.os.RemoteException;

    abstract class Stub extends Binder implements IPackageInstaller {
        public static IPackageInstaller asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
