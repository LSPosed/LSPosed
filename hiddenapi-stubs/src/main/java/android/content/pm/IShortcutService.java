package android.content.pm;

import android.content.IntentSender;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IShortcutService extends IInterface {
    boolean isRequestPinItemSupported(int user, int requestType) throws RemoteException;
    ParceledListSlice<ShortcutInfo> getShortcuts(String packageName, int matchFlags, int userId) throws RemoteException;
    ParceledListSlice<ShortcutInfo> getPinnedShortcuts(String packageName, int userId) throws RemoteException;
    boolean requestPinShortcut(String packageName, ShortcutInfo shortcut,
                               IntentSender resultIntent, int userId) throws RemoteException;

    boolean updateShortcuts(String packageName, ParceledListSlice<ShortcutInfo> shortcuts, int userId) throws RemoteException;

    abstract class Stub extends Binder implements IShortcutService {

        public static IShortcutService asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
    }
}
