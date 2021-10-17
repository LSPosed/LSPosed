package org.lsposed.manager.ui.dialog;

import android.content.Context;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.receivers.LSPManagerServiceHolder;

public class ShortcutDialogBuilder extends BlurBehindDialogBuilder {
    private static boolean shown = false;

    private ShortcutDialogBuilder(@NonNull Context context) {
        super(context);
        setTitle(R.string.parasitic_recommend);
        setMessage(R.string.parasitic_recommend_summary);
        setNegativeButton(R.string.never_show, (dialog, which) ->
                App.getPreferences().edit().putBoolean("never_show_shortcut", true).apply());
        setNeutralButton(R.string.create_shortcut, (dialog, which) -> {
            try {
                LSPManagerServiceHolder.getService().createShortcut();
            } catch (RemoteException ignored) {
            }
        });
        setPositiveButton(android.R.string.ok, null);
    }

    public static void showIfNeed(@NonNull Context context) {
        if (App.isParasitic() || !ConfigManager.isBinderAlive()) return;
        if (App.getPreferences().getBoolean("never_show_shortcut", false)) return;
        if (shown) return;
        shown = true;
        new ShortcutDialogBuilder(context).show();
    }
}
