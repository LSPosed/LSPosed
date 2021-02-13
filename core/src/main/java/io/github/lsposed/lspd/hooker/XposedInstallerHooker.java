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

package io.github.lsposed.lspd.hooker;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import io.github.lsposed.lspd.BuildConfig;
import io.github.lsposed.lspd.nativebridge.ConfigManager;
import io.github.lsposed.lspd.core.EdxpImpl;
import io.github.lsposed.lspd.core.Main;
import io.github.lsposed.lspd.util.Utils;

public class XposedInstallerHooker {

    public static void hookXposedInstaller(final ClassLoader classLoader) {
        final String variant;
        switch (Main.getEdxpVariant()) {
            case EdxpImpl.YAHFA:
                variant = "YAHFA";
                break;
            case EdxpImpl.SANDHOOK:
                variant = "SandHook";
                break;
            case EdxpImpl.NONE:
            default:
                variant = "Unknown";
                break;
        }

        Utils.logI("Found LSPosed Manager, hooking it");

        // LSPosed Manager R
        try {
            Class<?> ConstantsClass = XposedHelpers.findClass("io.github.lsposed.manager.Constants", classLoader);
            try {
                XposedHelpers.setStaticIntField(ConstantsClass, "xposedApiVersion", XposedBridge.getXposedVersion());
                XposedHelpers.setStaticObjectField(ConstantsClass, "xposedVersion", BuildConfig.VERSION_NAME);
                XposedHelpers.setStaticIntField(ConstantsClass, "xposedVersionCode", BuildConfig.VERSION_CODE);
                XposedHelpers.setStaticObjectField(ConstantsClass, "xposedVariant", variant);
                XposedHelpers.setStaticObjectField(ConstantsClass, "baseDir", ConfigManager.getBaseConfigPath() + "/");
                XposedHelpers.setStaticObjectField(ConstantsClass, "logDir", ConfigManager.getLogPath());
                XposedHelpers.setStaticObjectField(ConstantsClass, "miscDir", ConfigManager.getMiscPath());
                XposedHelpers.setStaticBooleanField(ConstantsClass, "permissive", ConfigManager.isPermissive());

                Utils.logI("Hooked LSPosed Manager");
                return;
            } catch (Throwable ignored) {
                // fallback
            }
            XposedHelpers.findAndHookMethod(ConstantsClass, "getXposedApiVersion", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return XposedBridge.getXposedVersion();
                }
            });
            XposedHelpers.findAndHookMethod(ConstantsClass, "getXposedVersion", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return BuildConfig.VERSION_NAME;
                }
            });
            XposedHelpers.findAndHookMethod(ConstantsClass, "getXposedVersionCode", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return BuildConfig.VERSION_CODE;
                }
            });
            XposedHelpers.findAndHookMethod(ConstantsClass, "getXposedVariant", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return variant;
                }
            });
            XposedHelpers.findAndHookMethod(ConstantsClass, "getBaseDir", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return ConfigManager.getBaseConfigPath() + "/";
                }
            });
            XposedHelpers.findAndHookMethod(ConstantsClass, "isPermissive", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return ConfigManager.isPermissive();
                }
            });
            XposedHelpers.findAndHookMethod(ConstantsClass, "getLogDir", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return ConfigManager.getLogPath();
                }
            });
            XposedHelpers.findAndHookMethod(ConstantsClass, "getMiscDir", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return ConfigManager.getMiscPath();
                }
            });
            Utils.logI("Hooked LSPosed Manager");
        } catch (Throwable t) {
            Utils.logW("Could not hook LSPosed Manager", t);
        }
    }
}
