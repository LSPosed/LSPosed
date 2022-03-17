package android.content;

import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;

public class Context {
    public IBinder getActivityToken() {
        throw new UnsupportedOperationException("STUB");
    }
    public Intent registerReceiverAsUser(BroadcastReceiver receiver, UserHandle user,
                                         IntentFilter filter, String broadcastPermission, Handler scheduler) {
        throw new UnsupportedOperationException("STUB");
    }
}
