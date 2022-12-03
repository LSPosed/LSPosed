package android.app;

import android.content.pm.ParceledListSlice;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;

public interface INotificationManager extends IInterface {
    void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id,
                                    Notification notification, int userId) throws RemoteException;

    void cancelNotificationWithTag(String pkg, String tag, int id, int userId) throws RemoteException;

    @RequiresApi(30)
    void cancelNotificationWithTag(String pkg, String opPkg, String tag, int id, int userId) throws RemoteException;

    void createNotificationChannelsForPackage(String pkg, int uid, ParceledListSlice<NotificationChannel> channelsList) throws RemoteException;

    void updateNotificationChannelForPackage(String pkg, int uid, NotificationChannel channel);

    @RequiresApi(30)
    NotificationChannel getNotificationChannelForPackage(String pkg, int uid, String channelId, String conversationId, boolean includeDeleted) throws RemoteException;

    NotificationChannel getNotificationChannelForPackage(String pkg, int uid, String channelId, boolean includeDeleted) throws RemoteException;

    abstract class Stub extends Binder implements INotificationManager {
        public static INotificationManager asInterface(IBinder obj) {
            throw new UnsupportedOperationException();
        }
    }
}
