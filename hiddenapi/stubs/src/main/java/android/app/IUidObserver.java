package android.app;

import android.os.Binder;

public interface IUidObserver {

    void onUidGone(int uid, boolean disabled);

    void onUidActive(int uid);

    void onUidIdle(int uid, boolean disabled);

    void onUidCachedChanged(int uid, boolean cached);

    abstract class Stub extends Binder implements IUidObserver {
    }
}
