package de.robv.android.xposed;

import android.os.SELinux;

import de.robv.android.xposed.services.BaseService;
import de.robv.android.xposed.services.BinderService;
import de.robv.android.xposed.services.DirectAccessService;
import de.robv.android.xposed.services.ZygoteService;

/**
 * A helper to work with (or without) SELinux, abstracting much of its big complexity.
 */
public final class SELinuxHelper {
	private SELinuxHelper() {}

	/**
	 * Determines whether SELinux is disabled or enabled.
	 *
	 * @return A boolean indicating whether SELinux is enabled.
	 */
	public static boolean isSELinuxEnabled() {
		return sIsSELinuxEnabled;
	}

	/**
	 * Determines whether SELinux is permissive or enforcing.
	 *
	 * @return A boolean indicating whether SELinux is enforcing.
	*/
	public static boolean isSELinuxEnforced() {
		return sIsSELinuxEnabled && SELinux.isSELinuxEnforced();
	}

	/**
	 * Gets the security context of the current process.
	 *
	 * @return A String representing the security context of the current process.
	 */
	public static String getContext() {
		return sIsSELinuxEnabled ? SELinux.getContext() : null;
	}

	/**
	 * Retrieve the service to be used when accessing files in {@code /data/data/*}.
	 *
	 * <p class="caution"><strong>IMPORTANT:</strong> If you call this from the Zygote process,
	 * don't re-use the result in different process!
	 *
	 * @return An instance of the service.
	 */
	public static BaseService getAppDataFileService() {
		if (sServiceAppDataFile != null)
			return sServiceAppDataFile;
		throw new UnsupportedOperationException();
	}


	// ----------------------------------------------------------------------------
	private static boolean sIsSELinuxEnabled = false;
	private static BaseService sServiceAppDataFile = new DirectAccessService(); // ed: initialized directly

	/*package*/ static void initOnce() {
		// ed: we assume all selinux policies have been added lively using magiskpolicy
//		try {
//			sIsSELinuxEnabled = SELinux.isSELinuxEnabled();
//		} catch (NoClassDefFoundError ignored) {}
	}

	/*package*/ static void initForProcess(String packageName) {
		// ed: sServiceAppDataFile has been initialized with default value
//		if (sIsSELinuxEnabled) {
//			if (packageName == null) {  // Zygote
//				sServiceAppDataFile = new ZygoteService();
//			} else if (packageName.equals("android")) {  //system_server
//				sServiceAppDataFile = BinderService.getService(BinderService.TARGET_APP);
//			} else {  // app
//				sServiceAppDataFile = new DirectAccessService();
//			}
//		} else {
//			sServiceAppDataFile = new DirectAccessService();
//		}
	}
}
