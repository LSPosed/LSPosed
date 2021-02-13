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

package io.github.lsposed.lspd.deopt;

import java.util.HashMap;

import de.robv.android.xposed.annotation.ApiSensitive;
import de.robv.android.xposed.annotation.Level;

/**
 * Providing a whitelist of methods which are the callers of the target methods we want to hook.
 * Because the target methods are inlined into the callers, we deoptimize the callers to
 * run in intercept mode to make target methods hookable.
 * <p>
 * Only for methods which are included in pre-compiled framework codes.
 * TODO recompile system apps and priv-apps since their original dex files are available
 */
@ApiSensitive(Level.MIDDLE)
public class InlinedMethodCallers {

    public static final String KEY_BOOT_IMAGE = "boot_image";
    public static final String KEY_BOOT_IMAGE_MIUI_RES = "boot_image_miui_res";
    public static final String KEY_SYSTEM_SERVER = "system_server";

    /**
     * Key should be {@link #KEY_BOOT_IMAGE}, {@link #KEY_SYSTEM_SERVER}, or a package name
     * of system apps or priv-apps i.e. com.android.systemui
     */
    private static final HashMap<String, String[][]> CALLERS = new HashMap<>();

    /**
     * format for each row: {className, methodName, methodSig}
     */
    private static final String[][] BOOT_IMAGE = {
            // callers of Application#attach(Context)
            {"android.app.Instrumentation", "newApplication", "(Ljava/lang/ClassLoader;Ljava/lang/String;Landroid/content/Context;)Landroid/app/Application;"},
            {"android.app.Instrumentation", "newApplication", "(Ljava/lang/ClassLoader;Landroid/content/Context;)Landroid/app/Application;"},
    };

    // TODO deprecate this
    private static final String[][] BOOT_IMAGE_FOR_MIUI_RES = {
            // for MIUI resources hooking
            {"android.content.res.MiuiResources", "init", "(Ljava/lang/String;)V"},
            {"android.content.res.MiuiResources", "updateMiuiImpl", "()V"},
            {"android.content.res.MiuiResources", "setImpl", "(Landroid/content/res/ResourcesImpl;)V"},
            {"android.content.res.MiuiResources", "loadOverlayValue", "(Landroid/util/TypedValue;I)V"},
            {"android.content.res.MiuiResources", "getThemeString", "(I)Ljava/lang/CharSequence;"},
            {"android.content.res.MiuiResources", "<init>", "(Ljava/lang/ClassLoader;)V"},
            {"android.content.res.MiuiResources", "<init>", "()V"},
            {"android.content.res.MiuiResources", "<init>", "(Landroid/content/res/AssetManager;Landroid/util/DisplayMetrics;Landroid/content/res/Configuration;)V"},
            {"android.miui.ResourcesManager", "initMiuiResource", "(Landroid/content/res/Resources;Ljava/lang/String;)V"},
            {"android.app.LoadedApk", "getResources", "()Landroid/content/res/Resources;"},
            {"android.content.res.Resources", "getSystem", "()Landroid/content/res/Resources;"},
            {"android.app.ApplicationPackageManager", "getResourcesForApplication", "(Landroid/content/pm/ApplicationInfo;)Landroid/content/res/Resources;"},
            {"android.app.ContextImpl", "setResources", "(Landroid/content/res/Resources;)V"},
    };

    private static final String[][] SYSTEM_SERVER = {};

    private static final String[][] SYSTEM_UI = {};

    static {
        CALLERS.put(KEY_BOOT_IMAGE, BOOT_IMAGE);
        CALLERS.put(KEY_BOOT_IMAGE_MIUI_RES, BOOT_IMAGE_FOR_MIUI_RES);
        CALLERS.put(KEY_SYSTEM_SERVER, SYSTEM_SERVER);
        CALLERS.put("com.android.systemui", SYSTEM_UI);
    }

    public static HashMap<String, String[][]> getAll() {
        return CALLERS;
    }

    public static String[][] get(String where) {
        return CALLERS.get(where);
    }
}
