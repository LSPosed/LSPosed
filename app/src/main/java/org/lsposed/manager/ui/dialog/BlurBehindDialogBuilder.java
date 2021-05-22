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

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.SurfaceControl;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.lang.reflect.Method;

@SuppressWarnings({"JavaReflectionMemberAccess", "ConstantConditions"})
@SuppressLint("PrivateApi")
public class BlurBehindDialogBuilder extends AlertDialog.Builder {
    private static final boolean supportBlur = getSystemProperty("ro.surface_flinger.supports_background_blur", false) && !getSystemProperty("persist.sys.sf.disable_blurs", false);

    public BlurBehindDialogBuilder(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        dialog.setOnShowListener(d -> setBackgroundBlurRadius(dialog));
        return dialog;
    }

    private void setBackgroundBlurRadius(AlertDialog dialog) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && supportBlur) {
            ValueAnimator animator = ValueAnimator.ofInt(1, 150);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(150);
            View view = dialog.getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || Build.VERSION.SDK_INT == Build.VERSION_CODES.R && Build.VERSION.PREVIEW_SDK_INT != 0) {
                animator.addUpdateListener(animation -> dialog.getWindow().setBackgroundBlurRadius((Integer) animation.getAnimatedValue()));
            } else {
                try {
                    Object viewRootImpl = view.getClass().getMethod("getViewRootImpl").invoke(view);
                    if (viewRootImpl == null) {
                        return;
                    }
                    SurfaceControl surfaceControl = (SurfaceControl) viewRootImpl.getClass().getMethod("getSurfaceControl").invoke(viewRootImpl);


                    Method setBackgroundBlurRadius = SurfaceControl.Transaction.class.getDeclaredMethod("setBackgroundBlurRadius", SurfaceControl.class, int.class);
                    animator.addUpdateListener(animation -> {
                        try {
                            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
                            setBackgroundBlurRadius.invoke(transaction, surfaceControl, animation.getAnimatedValue());
                            transaction.apply();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    });
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {

                }

                @Override
                public void onViewDetachedFromWindow(View v) {
                    animator.cancel();
                }
            });
            animator.start();
        }
    }

    public static boolean getSystemProperty(String key, boolean defaultValue) {
        boolean value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("getBoolean", String.class, boolean.class);
            value = (boolean) get.invoke(c, key, defaultValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }
}
