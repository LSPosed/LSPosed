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
import android.util.Log;
import android.view.SurfaceControl;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.lsposed.manager.App;

import java.lang.reflect.Method;

@SuppressWarnings({"JavaReflectionMemberAccess", "ConstantConditions"})
public class BlurBehindDialogBuilder extends MaterialAlertDialogBuilder {
    private static final boolean supportBlur = getSystemProperty("ro.surface_flinger.supports_background_blur", false) && !getSystemProperty("persist.sys.sf.disable_blurs", false);

    public BlurBehindDialogBuilder(@NonNull Context context) {
        super(context);
    }

    public BlurBehindDialogBuilder(@NonNull Context context, int overrideThemeResId) {
        super(context, overrideThemeResId);
    }

    @NonNull
    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        var window = dialog.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setBackgroundBlurRadius(dialog.getWindow());
        } else
            dialog.setOnShowListener(d -> setBackgroundBlurRadius(window));
        return dialog;
    }

    private void setBackgroundBlurRadius(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.getAttributes().setBlurBehindRadius(53); //android.R.styleable.Window_windowBlurBehindRadius
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            if (supportBlur) {
                View view = window.getDecorView();
                ValueAnimator animator = ValueAnimator.ofInt(1, 153);
                animator.setInterpolator(new DecelerateInterpolator());
                try {
                    Object viewRootImpl = view.getClass().getMethod("getViewRootImpl").invoke(view);
                    if (viewRootImpl == null) {
                        return;
                    }
                    SurfaceControl surfaceControl = (SurfaceControl) viewRootImpl.getClass().getMethod("getSurfaceControl").invoke(viewRootImpl);

                    @SuppressLint("BlockedPrivateApi") Method setBackgroundBlurRadius = SurfaceControl.Transaction.class.getDeclaredMethod("setBackgroundBlurRadius", SurfaceControl.class, int.class);
                    animator.addUpdateListener(animation -> {
                        try {
                            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
                            var animatedValue = animation.getAnimatedValue();
                            if (animatedValue != null) {
                                setBackgroundBlurRadius.invoke(transaction, surfaceControl, (int) animatedValue);
                            }
                            transaction.apply();
                        } catch (Throwable t) {
                            Log.e(App.TAG, "Blur behind dialog builder", t);
                        }
                    });
                } catch (Throwable t) {
                    Log.e(App.TAG, "Blur behind dialog builder", t);
                }
                view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(@NonNull View v) {
                    }

                    @Override
                    public void onViewDetachedFromWindow(@NonNull View v) {
                        animator.cancel();
                    }
                });
                animator.start();
            }
        }
    }

    public static boolean getSystemProperty(String key, boolean defaultValue) {
        boolean value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("getBoolean", String.class, boolean.class);
            value = (boolean) get.invoke(c, key, defaultValue);
        } catch (Exception e) {
            Log.e(App.TAG, "Blur behind dialog builder get system property", e);
        }
        return value;
    }
}
