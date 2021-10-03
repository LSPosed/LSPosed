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

package org.lsposed.manager.util.theme;

import static com.google.android.material.theme.overlay.MaterialThemeOverlay.wrap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.InsetDialogOnTouchListener;
import com.google.android.material.dialog.MaterialDialogs;
import com.google.android.material.resources.MaterialAttributes;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.takisoft.colorpicker.ColorPickerDialog;
import com.takisoft.colorpicker.OnColorSelectedListener;

@SuppressLint("RestrictedApi")
public class ThemeColorPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat implements OnColorSelectedListener {

    private int pickedColor;
    ThemeUtil.CustomThemeColors[] themeColors;
    private int[] colors;
    @AttrRes
    private static final int DEF_STYLE_ATTR = com.google.android.material.R.attr.alertDialogStyle;
    @SuppressLint("PrivateResource")
    @StyleRes
    private static final int DEF_STYLE_RES = com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents;
    @AttrRes
    private static final int MATERIAL_ALERT_DIALOG_THEME_OVERLAY = com.google.android.material.R.attr.materialAlertDialogTheme;

    private static int getMaterialAlertDialogThemeOverlay(@NonNull Context context) {
        TypedValue materialAlertDialogThemeOverlay =
                MaterialAttributes.resolve(context, MATERIAL_ALERT_DIALOG_THEME_OVERLAY);
        if (materialAlertDialogThemeOverlay == null) {
            return 0;
        }
        return materialAlertDialogThemeOverlay.data;
    }

    private static Context createMaterialAlertDialogThemedContext(@NonNull Context context) {
        int themeOverlayId = getMaterialAlertDialogThemeOverlay(context);
        Context themedContext = wrap(context, null, DEF_STYLE_ATTR, DEF_STYLE_RES);
        if (themeOverlayId == 0) {
            return themedContext;
        }
        return new ContextThemeWrapper(themedContext, themeOverlayId);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ThemeColorPreference pref = getColorPickerPreference();
        Activity activity = getActivity();
        assert activity != null;
        int selectedColor = activity.getColor(pref.getColor().getResourceId());
        themeColors = pref.getColors();
        colors = new int[themeColors.length];
        for (int i = 0; i < themeColors.length; i++) {
            colors[i] = activity.getColor(themeColors[i].getResourceId());
        }

        Context context = createMaterialAlertDialogThemedContext(activity);

        ColorPickerDialog.Params params = new ColorPickerDialog.Params.Builder(context)
                .setSelectedColor(selectedColor)
                .setColors(colors)
                .setSize(ColorPickerDialog.SIZE_SMALL)
                .setSortColors(false)
                .setColumns(0)
                .build();

        Resources.Theme theme = context.getTheme();

        Rect backgroundInsets = MaterialDialogs.getDialogBackgroundInsets(context, DEF_STYLE_ATTR, DEF_STYLE_RES);

        int surfaceColor =
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, getClass().getCanonicalName());
        MaterialShapeDrawable materialShapeDrawable =
                new MaterialShapeDrawable(context, null, DEF_STYLE_ATTR, DEF_STYLE_RES);
        materialShapeDrawable.initializeElevationOverlay(context);
        materialShapeDrawable.setFillColor(ColorStateList.valueOf(surfaceColor));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            TypedValue dialogCornerRadiusValue = new TypedValue();
            theme.resolveAttribute(android.R.attr.dialogCornerRadius, dialogCornerRadiusValue, true);
            float dialogCornerRadius =
                    dialogCornerRadiusValue.getDimension(activity.getResources().getDisplayMetrics());
            if (dialogCornerRadiusValue.type == TypedValue.TYPE_DIMENSION && dialogCornerRadius >= 0) {
                materialShapeDrawable.setCornerSize(dialogCornerRadius);
            }
        }

        ColorPickerDialog dialog = new ColorPickerDialog(context, this, params);
        dialog.setTitle(pref.getDialogTitle());

        Window window = dialog.getWindow();
        View decorView = window.getDecorView();
        materialShapeDrawable.setElevation(ViewCompat.getElevation(decorView));

        Drawable insetDrawable = MaterialDialogs.insetDrawable(materialShapeDrawable, backgroundInsets);
        window.setBackgroundDrawable(insetDrawable);
        decorView.setOnTouchListener(new InsetDialogOnTouchListener(dialog, backgroundInsets));

        return dialog;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        ThemeColorPreference preference = getColorPickerPreference();

        if (positiveResult && preference.callChangeListener(pickedColor)) {
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == pickedColor) {
                    preference.setColor(themeColors[i]);
                }
            }
        }
    }

    @Override
    public void onColorSelected(int color) {
        this.pickedColor = color;

        super.onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
    }

    ThemeColorPreference getColorPickerPreference() {
        return (ThemeColorPreference) getPreference();
    }
}
