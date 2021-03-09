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
 * Copyright (C) 2021 LSPosed Contributors
 */

package io.github.lsposed.manager.ui.dialog;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.io.File;
import java.util.Locale;

import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.ConfigManager;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.DialogInfoBinding;
import rikka.core.util.ClipboardUtils;

public class StatusDialogBuilder extends BlurBehindDialogBuilder {

    public StatusDialogBuilder(@NonNull Context context) {
        super(context);
        DialogInfoBinding binding = DialogInfoBinding.inflate(LayoutInflater.from(context), null, false);

        binding.apiVersion.setText(String.valueOf(ConfigManager.getXposedApiVersion()));
        binding.frameworkVersion.setText(String.format(Locale.US, "%s (%s)", ConfigManager.getXposedVersionName(), ConfigManager.getXposedVersionCode()));
        binding.managerVersion.setText(String.format(Locale.US, "%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        if (Build.VERSION.PREVIEW_SDK_INT != 0) {
            binding.systemVersion.setText(String.format(Locale.US, "%1$s Preview (API %2$d)", Build.VERSION.CODENAME, Build.VERSION.SDK_INT));
        } else {
            binding.systemVersion.setText(String.format(Locale.US, "%1$s (API %2$d)", Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        }

        binding.device.setText(getUIFramework());
        binding.systemAbi.setText(Build.SUPPORTED_ABIS[0]);

        if (ConfigManager.isPermissive()) {
            binding.selinux.setVisibility(View.VISIBLE);
            binding.selinux.setText(HtmlCompat.fromHtml(context.getString(R.string.selinux_permissive), HtmlCompat.FROM_HTML_MODE_LEGACY));
        }

        setView(binding.getRoot());

        setPositiveButton(android.R.string.ok, null);
        setNeutralButton(android.R.string.copy, (dialog, which) -> ClipboardUtils.put(context,
                String.format(Locale.US, "%s\n%s\n\n%s\n%s\n\n%s\n%s\n\n%s\n%s\n\n%s\n%s\n\n%s\n%s",
                        context.getString(R.string.info_api_version), binding.apiVersion.getText(),
                        context.getString(R.string.info_framework_version), binding.frameworkVersion.getText(),
                        context.getString(R.string.info_manager_version), binding.managerVersion.getText(),
                        context.getString(R.string.info_system_version), binding.systemVersion.getText(),
                        context.getString(R.string.info_device), binding.device.getText(),
                        context.getString(R.string.info_system_abi), binding.systemAbi.getText())));
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