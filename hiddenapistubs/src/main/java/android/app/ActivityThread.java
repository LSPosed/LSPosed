package android.app;

import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;

public final class ActivityThread {
	public static ActivityThread currentActivityThread() {
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
}
