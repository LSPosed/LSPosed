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

import de.robv.android.xposed.services.BaseService;
import de.robv.android.xposed.services.DirectAccessService;

/**
 * A helper to work with (or without) SELinux, abstracting much of its big complexity.
 */
public final class SELinuxHelper {
    private SELinuxHelper() {
    }

    /**
     * Determines whether SELinux is disabled or enabled.
     *
     * @return A boolean indicating whether SELinux is enabled.
     */
    public static boolean isSELinuxEnabled() {
        // lsp: always enabled
        return true;
    }

    /**
     * Determines whether SELinux is permissive or enforcing.
     *
     * @return A boolean indicating whether SELinux is enforcing.
     */
    public static boolean isSELinuxEnforced() {
        // lsp: always enforcing
        return true;
    }

    /**
     * Gets the security context of the current process.
     *
     * @return A String representing the security context of the current process.
     */
    public static String getContext() {
        return null;
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
        return sServiceAppDataFile;
    }

    private static final BaseService sServiceAppDataFile = new DirectAccessService(); // ed: initialized directly

}
