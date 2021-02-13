/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package de.robv.android.xposed;

import android.os.SELinux;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import de.robv.android.xposed.services.BaseService;
import de.robv.android.xposed.services.DirectAccessService;

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
		if (!sIsSELinuxEnabled) {
			return false;
		}
		boolean result = false;
		final File SELINUX_STATUS_FILE = new File("/sys/fs/selinux/enforce");
		if (SELINUX_STATUS_FILE.exists()) {
			try {
				FileInputStream fis = new FileInputStream(SELINUX_STATUS_FILE);
				int status = fis.read();
				switch (status) {
					case 49:
						result = true;
						break;
					case 48:
						result = false;
						break;
					default:
						XposedBridge.log("Unexpected byte " + status + " in /sys/fs/selinux/enforce");
				}
				fis.close();
			} catch (IOException e) {
				if (e.getMessage().contains("Permission denied")) {
					result = true;
				} else {
					XposedBridge.log("Failed to read SELinux status: " + e.getMessage());
					result = false;
				}
			}
		}
		return result;
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
	// TODO: SELinux status
	private static boolean sIsSELinuxEnabled = false;
	private static BaseService sServiceAppDataFile = new DirectAccessService(); // ed: initialized directly

	/*package*/ public static void initOnce() {
		// ed: we assume all selinux policies have been added lively using magiskpolicy
		try {
			sIsSELinuxEnabled = SELinux.isSELinuxEnabled();
		} catch (NoClassDefFoundError ignored) {}
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
