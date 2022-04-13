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

import android.app.Dialog;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.receivers.LSPManagerServiceHolder;

public class ShortcutDialog extends DialogFragment {
    private static boolean shown = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BlurBehindDialogBuilder(requireContext(), R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons)
                .setTitle(R.string.parasitic_recommend)
                .setMessage(R.string.parasitic_recommend_summary)
                .setNegativeButton(R.string.never_show, (dialog, which) ->
                        App.getPreferences().edit().putBoolean("never_show_shortcut", true).apply())
                .setNeutralButton(R.string.create_shortcut, (dialog, which) -> {
                    try {
                        LSPManagerServiceHolder.getService().createShortcut();
                    } catch (RemoteException ignored) {
                    }
                })
                .setPositiveButton(android.R.string.ok, null).create();
    }

    public static void showIfNeed(FragmentManager fm) {
        if (App.isParasitic() || !ConfigManager.isBinderAlive()) return;
        if (App.getPreferences().getBoolean("never_show_shortcut", false)) return;
        if (shown) return;
        shown = true;
        new ShortcutDialog().show(fm, "shortcut");
    }
}
