package hidden;

import android.content.res.AssetManager;
import android.os.Binder;
import android.os.IBinder;

public class HiddenApiBridge {
    public static int AssetManager_addAssetPath(AssetManager am, String path) {
        return am.addAssetPath(path);
    }

    public static IBinder Binder_allowBlocking(IBinder binder) {
        return Binder.allowBlocking(binder);
    }
}
