package android.app;

import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;

public final class ActivityThread {
    public static ActivityThread currentActivityThread() {
        throw new UnsupportedOperationException("STUB");
    }

    public ApplicationThread getApplicationThread() {
        throw new UnsupportedOperationException("STUB");
    }


    public static Application currentApplication() {
        throw new UnsupportedOperationException("STUB");
    }

    public static String currentPackageName() {
        throw new UnsupportedOperationException("STUB");
    }

    public final LoadedApk getPackageInfoNoCheck(ApplicationInfo ai, CompatibilityInfo compatInfo) {
        throw new UnsupportedOperationException("STUB");
    }

    public static String currentProcessName() {
        throw new UnsupportedOperationException("STUB");
    }

    public ContextImpl getSystemContext() {
        throw new UnsupportedOperationException("STUB");
    }

    public static ActivityThread systemMain() {
        throw new UnsupportedOperationException("STUB");
    }

    private class ApplicationThread extends IApplicationThread.Stub {
        @Override
        public IBinder asBinder() {
            return null;
        }
    }

    public static final class ActivityClientRecord {
        Bundle state;
        PersistableBundle persistentState;
    }
}
