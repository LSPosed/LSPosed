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

package io.github.lsposed.lspd.nativebridge;

public class ConfigManager {

    public static String appDataDir = "";
    public static String niceName = "";
    public static String appProcessName = "";

    public static native boolean isResourcesHookEnabled();

    public static native String getInstallerPackageName();

    public static native String getPrefsPath(String suffix);

    public static native String getCachePath(String suffix);

    public static native String getLogPath();

    public static native String getMiscPath();

    public static native String getBaseConfigPath();

    public static native String getDataPathPrefix();

    public static native String getModulesList();

    public static native boolean isPermissive();
}
