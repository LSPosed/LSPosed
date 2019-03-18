package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge.CopyOnWriteSortedSet;

/**
 * This class is only used for internal purposes, except for the {@link LoadPackageParam}
 * subclass.
 */
public abstract class XC_LoadPackage extends XCallback implements IXposedHookLoadPackage {
	/**
	 * Creates a new callback with default priority.
	 * @hide
	 */
	@SuppressWarnings("deprecation")
	public XC_LoadPackage() {
		super();
	}

	/**
	 * Creates a new callback with a specific priority.
	 *
	 * @param priority See {@link XCallback#priority}.
	 * @hide
	 */
	public XC_LoadPackage(int priority) {
		super(priority);
	}

	/**
	 * Wraps information about the app being loaded.
	 */
	public static final class LoadPackageParam extends XCallback.Param {
		/** @hide */
		public LoadPackageParam(CopyOnWriteSortedSet<XC_LoadPackage> callbacks) {
			super(callbacks);
		}

		/** The name of the package being loaded. */
		public String packageName;

		/** The process in which the package is executed. */
		public String processName;

		/** The ClassLoader used for this package. */
		public ClassLoader classLoader;

		/** More information about the application being loaded. */
		public ApplicationInfo appInfo;

		/** Set to {@code true} if this is the first (and main) application for this process. */
		public boolean isFirstApplication;
	}

	/** @hide */
	@Override
	protected void call(Param param) throws Throwable {
		if (param instanceof LoadPackageParam)
			handleLoadPackage((LoadPackageParam) param);
	}
}
