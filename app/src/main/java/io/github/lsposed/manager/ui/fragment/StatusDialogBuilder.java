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

package io.github.lsposed.manager.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.ConfigManager;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.StatusInstallerBinding;

@SuppressLint("StaticFieldLeak")
public class StatusDialogBuilder extends AlertDialog.Builder {
    private static final String CPU_ABI;
    private static final String CPU_ABI2;

    static {
        String[] abiList = Build.SUPPORTED_64_BIT_ABIS;
        if (abiList.length == 0) {
            abiList = Build.SUPPORTED_32_BIT_ABIS;
        }
        CPU_ABI = abiList[0];
        if (abiList.length > 1) {
            CPU_ABI2 = abiList[1];
        } else {
            CPU_ABI2 = "";
        }
    }

    public StatusDialogBuilder(@NonNull Context context) {
        super(context);
        StatusInstallerBinding binding = StatusInstallerBinding.inflate(LayoutInflater.from(context), null, false);

        String installXposedVersion = ConfigManager.getXposedVersionName();
        String mAppVer = String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        binding.manager.setText(mAppVer);

        if (installXposedVersion != null) {
            binding.api.setText(String.format(Locale.US, "%s.0", ConfigManager.getXposedApiVersion()));
            binding.framework.setText(String.format(Locale.US, "%s (%s)", installXposedVersion, ConfigManager.getXposedVersionCode()));
        }

        if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            binding.androidVersion.setText(context.getString(R.string.android_sdk_preview, Build.VERSION.CODENAME));
        } else {
            binding.androidVersion.setText(context.getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        }

        binding.manufacturer.setText(getUIFramework());
        binding.cpu.setText(getCompleteArch());

        if (ConfigManager.isPermissive()) {
            binding.selinux.setVisibility(View.VISIBLE);
            binding.selinux.setText(HtmlCompat.fromHtml(context.getString(R.string.selinux_permissive), HtmlCompat.FROM_HTML_MODE_LEGACY));
        }
        setView(binding.getRoot());
    }

    private static String getCompleteArch() {
        String info = "";

        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text;
            while ((text = br.readLine()) != null) {
                if (!text.startsWith("processor")) break;
            }
            br.close();
            String[] array = text != null ? text.split(":\\s+", 2) : new String[0];
            if (array.length >= 2) {
                info += array[1] + " ";
            }
        } catch (IOException ignored) {
        }

        info += Build.SUPPORTED_ABIS[0];
        return info + " (" + getArch() + ")";
    }

    public static String getArch() {
        if (CPU_ABI.equals("arm64-v8a")) {
            return "arm64";
        } else if (CPU_ABI.equals("x86_64")) {
            return "x86_64";
        } else if (CPU_ABI.equals("mips64")) {
            return "mips64";
        } else if (CPU_ABI.startsWith("x86") || CPU_ABI2.startsWith("x86")) {
            return "x86";
        } else if (CPU_ABI.startsWith("mips")) {
            return "mips";
        } else if (CPU_ABI.startsWith("armeabi-v5") || CPU_ABI.startsWith("armeabi-v6")) {
            return "armv5";
        } else {
            return "arm";
        }
    }

    private String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
            case 27:
                return "Oreo";
            case 28:
                return "Pie";
            case 29:
                return "Q";
            case 30:
                return "R";
            case 31:
                return "S";
        }
        return "Unknown";
    }

    private String getUIFramework() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        if (new File("/system/framework/framework-miui-res.apk").exists() || new File("/system/app/miui/miui.apk").exists() || new File("/system/app/miuisystem/miuisystem.apk").exists()) {
            manufacturer += "(MIUI)";
        } else if (new File("/system/priv-app/oneplus-framework-res/oneplus-framework-res.apk").exists()) {
            manufacturer += "(Hydrogen/Oxygen OS)";
        } else if (new File("/system/framework/oppo-framework.jar").exists() || new File("/system/framework/oppo-framework-res.apk").exists() || new File("/system/framework/coloros-framework.jar").exists() || new File("/system/framework/coloros.services.jar").exists() || new File("/system/framework/oppo-services.jar").exists() || new File("/system/framework/coloros-support-wrapper.jar").exists()) {
            manufacturer += "(Color OS)";
        } else if (new File("/system/framework/hwEmui.jar").exists() || new File("/system/framework/hwcustEmui.jar").exists() || new File("/system/framework/hwframework.jar").exists() || new File("/system/framework/framework-res-hwext.apk").exists() || new File("/system/framework/hwServices.jar").exists() || new File("/system/framework/hwcustframework.jar").exists()) {
            manufacturer += "(EMUI)";
        } else if (new File("/system/framework/com.samsung.device.jar").exists() || new File("/system/framework/sec_platform_library.jar").exists()) {
            manufacturer += "(One UI)";
        } else if (new File("/system/priv-app/CarbonDelta/CarbonDelta.apk").exists()) {
            manufacturer += "(Carbon OS)";
        } else if (new File("/system/framework/flyme-framework.jar").exists() || new File("/system/framework/flyme-res").exists() || new File("/system/framework/flyme-telephony-common.jar").exists()) {
            manufacturer += "(Flyme)";
        } else if (new File("/system/framework/org.lineageos.platform-res.apk").exists() || new File("/system/framework/org.lineageos.platform.jar").exists()) {
            manufacturer += "(Lineage OS Based ROM)";
        } else if (new File("/system/framework/twframework.jar").exists() || new File("/system/framework/samsung-services.jar").exists()) {
            manufacturer += "(TouchWiz)";
        } else if (new File("/system/framework/core.jar.jex").exists()) {
            manufacturer += "(Aliyun OS)";
        }
        return manufacturer;
    }
}