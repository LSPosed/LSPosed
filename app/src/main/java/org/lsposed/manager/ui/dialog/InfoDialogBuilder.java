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

package org.lsposed.manager.ui.dialog;

import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import java.util.Locale;

import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogInfoBinding;
import rikka.core.util.ClipboardUtils;

public class InfoDialogBuilder extends BlurBehindDialogBuilder {

    public InfoDialogBuilder(@NonNull Context context) {
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

        binding.device.setText(getDevice());
        binding.systemAbi.setText(Build.SUPPORTED_ABIS[0]);

        if (ConfigManager.isPermissive()) {
            binding.selinux.setVisibility(View.VISIBLE);
            binding.selinux.setText(HtmlCompat.fromHtml(context.getString(R.string.selinux_permissive), HtmlCompat.FROM_HTML_MODE_LEGACY));
        } else if (!ConfigManager.isSepolicyLoaded()) {
            binding.selinux.setVisibility(View.VISIBLE);
            binding.selinux.setText(HtmlCompat.fromHtml(context.getString(R.string.selinux_policy_not_loaded), HtmlCompat.FROM_HTML_MODE_LEGACY));
        }

        setView(binding.getRoot());

        setPositiveButton(android.R.string.ok, null);
        String info = context.getString(R.string.info_api_version) +
                "\n" +
                binding.apiVersion.getText() +
                "\n\n" +
                context.getString(R.string.info_framework_version) +
                "\n" +
                binding.frameworkVersion.getText() +
                "\n\n" +
                context.getString(R.string.info_manager_version) +
                "\n" +
                binding.managerVersion.getText() +
                "\n\n" +
                context.getString(R.string.info_system_version) +
                "\n" +
                binding.systemVersion.getText() +
                "\n\n" +
                context.getString(R.string.info_device) +
                "\n" +
                binding.device.getText() +
                "\n\n" +
                context.getString(R.string.info_system_abi) +
                "\n" +
                binding.systemAbi.getText();
        setNeutralButton(android.R.string.copy, (dialog, which) -> ClipboardUtils.put(context, info));
    }

    private String getDevice() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        return manufacturer;
    }
}
