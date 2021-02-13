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
import io.github.lsposed.lspd.service.BridgeService;
import io.github.lsposed.lspd.util.Utils;

public class XposedInstallerHooker {

    public static void hookXposedInstaller(final ClassLoader classLoader) {
        String variant_;
        switch (Main.getEdxpVariant()) {
            case EdxpImpl.YAHFA:
                variant_ = "YAHFA";
                break;
            case EdxpImpl.SANDHOOK:
                variant_ = "SandHook";
                break;
            case EdxpImpl.NONE:
            default:
                variant_ = "Unknown";
                break;
        }
        final String variant = variant_;

        Utils.logI("Found LSPosed Manager, hooking it");

        // LSPosed Manager R
        try {
            Class<?> ConstantsClass = XposedHelpers.findClass("io.github.lsposed.manager.Constants", classLoader);
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
            XposedHelpers.findAndHookMethod(ConstantsClass, "getXposedApiVersion", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) {
                    return XposedBridge.getXposedVersion();
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
            Class<?> serviceClass = XposedHelpers.findClass("io.github.xposed.xposedservice.XposedService", classLoader);
            XposedHelpers.setStaticObjectField(serviceClass, "serviceBinder", BridgeService.requireBinder());

            Utils.logI("Hooked LSPosed Manager");
        } catch (Throwable t) {
            Utils.logW("Could not hook LSPosed Manager", t);
        }
    }
}
