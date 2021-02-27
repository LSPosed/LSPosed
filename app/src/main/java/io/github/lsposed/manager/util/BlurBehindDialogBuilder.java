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

package io.github.lsposed.manager.util;

import android.content.Context;
import android.os.Build;
import android.view.SurfaceControl;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

public class BlurBehindDialogBuilder extends AlertDialog.Builder {
    public BlurBehindDialogBuilder(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(d -> setBackgroundBlurRadius(dialog.getWindow().getDecorView()));
        return dialog;
    }

    private void setBackgroundBlurRadius(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Object viewRootImpl = view.getClass().getMethod("getViewRootImpl").invoke(view);
                if (viewRootImpl == null) {
                    return;
                }
                SurfaceControl surfaceControl = (SurfaceControl) viewRootImpl.getClass().getMethod("getSurfaceControl").invoke(viewRootImpl);

                SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
                transaction.getClass().getDeclaredMethod("setBackgroundBlurRadius", SurfaceControl.class, int.class).invoke(transaction, surfaceControl, 150);
                transaction.apply();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
