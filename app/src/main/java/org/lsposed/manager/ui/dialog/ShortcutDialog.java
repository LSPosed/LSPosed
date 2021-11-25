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
        return new BlurBehindDialogBuilder(requireContext())
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
