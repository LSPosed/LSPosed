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

import android.app.Activity;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogItemBinding;
import org.lsposed.manager.databinding.DialogWarningBinding;
import org.lsposed.manager.util.chrome.LinkTransformationMethod;

public class WarningDialogBuilder extends BlurBehindDialogBuilder {

    public WarningDialogBuilder(@NonNull Context context) {
        super(context);
        Activity activity = (Activity) context;
        setTitle(R.string.partial_activated);

        LayoutInflater inflater = LayoutInflater.from(context);
        DialogWarningBinding binding = DialogWarningBinding.inflate(inflater, null, false);

        if (!ConfigManager.isSepolicyLoaded()) {
            DialogItemBinding item = DialogItemBinding.inflate(inflater, binding.container, true);
            item.title.setText(R.string.selinux_policy_not_loaded_summary);
            item.value.setText(HtmlCompat.fromHtml(activity.getString(R.string.selinux_policy_not_loaded), HtmlCompat.FROM_HTML_MODE_LEGACY));
            item.value.setMovementMethod(LinkMovementMethod.getInstance());
            item.value.setTransformationMethod(new LinkTransformationMethod(activity));
        }
        if (!ConfigManager.systemServerRequested()) {
            DialogItemBinding item = DialogItemBinding.inflate(inflater, binding.container, true);
            item.title.setText(R.string.system_inject_fail_summary);
            item.value.setText(HtmlCompat.fromHtml(activity.getString(R.string.system_inject_fail), HtmlCompat.FROM_HTML_MODE_LEGACY));
            item.value.setMovementMethod(LinkMovementMethod.getInstance());
            item.value.setTransformationMethod(new LinkTransformationMethod(activity));
        }
        if (!ConfigManager.dex2oatFlagsLoaded()) {
            DialogItemBinding item = DialogItemBinding.inflate(inflater, binding.container, true);
            item.title.setText(R.string.system_prop_incorrect_summary);
            item.value.setText(HtmlCompat.fromHtml(activity.getString(R.string.system_prop_incorrect), HtmlCompat.FROM_HTML_MODE_LEGACY));
            item.value.setMovementMethod(LinkMovementMethod.getInstance());
            item.value.setTransformationMethod(new LinkTransformationMethod(activity));
        }

        setView(binding.getRoot());
        setPositiveButton(android.R.string.ok, null);
    }
}
