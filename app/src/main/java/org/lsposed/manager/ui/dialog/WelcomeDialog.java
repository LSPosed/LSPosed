package org.lsposed.manager.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.util.ShortcutUtil;

public class WelcomeDialog extends DialogFragment {
    private static boolean shown = false;

    private Dialog parasiticDialog(BlurBehindDialogBuilder builder) {
        var shortcutSupported = ShortcutUtil.isRequestPinShortcutSupported(requireContext());
        builder
                .setTitle(R.string.parasitic_welcome)
                .setMessage(shortcutSupported ? R.string.parasitic_welcome_summary :
                        R.string.parasitic_welcome_summary_no_shortcut_support)
                .setNegativeButton(R.string.never_show, (dialog, which) ->
                        App.getPreferences().edit().putBoolean("never_show_welcome", true).apply())
                .setPositiveButton(android.R.string.ok, null);
        if (shortcutSupported)
            builder.setNeutralButton(R.string.create_shortcut, (dialog, which) ->
                    ShortcutUtil.requestPinLaunchShortcut(() ->
                            App.getPreferences().edit().putBoolean("never_show_welcome",
                                    true).apply()));
        return builder.create();
    }

    private Dialog appDialog(BlurBehindDialogBuilder builder) {

        return builder
                .setTitle(R.string.app_welcome)
                .setMessage(R.string.app_welcome_summary)
                .setNegativeButton(R.string.never_show, (d, w) ->
                        App.getPreferences().edit().putBoolean("never_show_welcome", true).apply())
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        var builder = new BlurBehindDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons);
        if (App.isParasitic()) {
            return parasiticDialog(builder);
        } else {
            return appDialog(builder);
        }
    }

    public static void showIfNeed(FragmentManager fm) {
        if (shown) return;
        if (!ConfigManager.isBinderAlive()) return;
        if (App.getPreferences().getBoolean("never_show_welcome", false)) return;
        new WelcomeDialog().show(fm, "welcome");
        shown = true;
    }
}
