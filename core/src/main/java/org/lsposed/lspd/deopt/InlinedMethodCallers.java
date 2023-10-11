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

package org.lsposed.lspd.deopt;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.util.HashMap;

/**
 * Providing a whitelist of methods which are the callers of the target methods we want to hook.
 * Because the target methods are inlined into the callers, we deoptimize the callers to
 * run in intercept mode to make target methods hookable.
 * <p>
 * Only for methods which are included in pre-compiled framework codes.
 * TODO recompile system apps and priv-apps since their original dex files are available
 */
public class InlinedMethodCallers {

    public static final String KEY_BOOT_IMAGE = "boot_image";
    public static final String KEY_BOOT_IMAGE_MIUI_RES = "boot_image_miui_res";
    public static final String KEY_SYSTEM_SERVER = "system_server";

    /**
     * Key should be {@link #KEY_BOOT_IMAGE}, {@link #KEY_SYSTEM_SERVER}, or a package name
     * of system apps or priv-apps i.e. com.android.systemui
     */
    private static final HashMap<String, Object[][]> CALLERS = new HashMap<>();

    /**
     * format for each row: {className, methodName, methodSig}
     */
    private static final Object[][] BOOT_IMAGE = {
            // callers of Application#attach(Context)
            {"android.app.Instrumentation", "newApplication", ClassLoader.class, String.class, Context.class},
            {"android.app.Instrumentation", "newApplication", ClassLoader.class, Context.class},
            {"android.app.ContextImpl", "getSharedPreferencesPath", String.class}
    };

    // TODO deprecate this
    private static final Object[][] BOOT_IMAGE_FOR_MIUI_RES = {
            // for MIUI resources hooking
            {"android.content.res.MiuiResources", "init", String.class},
            {"android.content.res.MiuiResources", "updateMiuiImpl"},
            {"android.content.res.MiuiResources", "setImpl", "android.content.res.ResourcesImpl"},
            {"android.content.res.MiuiResources", "loadOverlayValue", TypedValue.class, int.class},
            {"android.content.res.MiuiResources", "getThemeString", CharSequence.class},
            {"android.content.res.MiuiResources", "<init>", ClassLoader.class},
            {"android.content.res.MiuiResources", "<init>"},
            {"android.content.res.MiuiResources", "<init>", AssetManager.class, DisplayMetrics.class, Configuration.class},
            {"android.miui.ResourcesManager", "initMiuiResource", Resources.class, String.class},
            {"android.app.LoadedApk", "getResources", Resources.class},
            {"android.content.res.Resources", "getSystem", Resources.class},
            {"android.app.ApplicationPackageManager", "getResourcesForApplication", ApplicationInfo.class},
            {"android.app.ContextImpl", "setResources", Resources.class},
    };

    private static final Object[][] SYSTEM_SERVER = {};

    private static final Object[][] SYSTEM_UI = {};

    static {
        CALLERS.put(KEY_BOOT_IMAGE, BOOT_IMAGE);
        CALLERS.put(KEY_BOOT_IMAGE_MIUI_RES, BOOT_IMAGE_FOR_MIUI_RES);
        CALLERS.put(KEY_SYSTEM_SERVER, SYSTEM_SERVER);
        CALLERS.put("com.android.systemui", SYSTEM_UI);
    }

    public static HashMap<String, Object[][]> getAll() {
        return CALLERS;
    }

    public static Object[][] get(String where) {
        return CALLERS.get(where);
    }
}
