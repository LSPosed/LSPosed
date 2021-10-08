package org.lsposed.manager.ui.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import org.lsposed.manager.R;

public class FlashDialogBuilder extends BlurBehindDialogBuilder {
    public FlashDialogBuilder(@NonNull Context context, @NonNull String zipPath) {
        super(context);
        setTitle(R.string.update_lsposed);
        setMessage(R.string.update_lsposed_msg);
        setNegativeButton(android.R.string.cancel, null);
        setPositiveButton(R.string.install, (dialog, which) -> flash(zipPath));
    }

    void flash(String zipPath) {
        //TODO: ConfigManager.flashZip(zipPath, fd);
    }
}
