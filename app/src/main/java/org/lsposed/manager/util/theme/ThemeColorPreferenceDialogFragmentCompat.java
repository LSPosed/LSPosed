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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.takisoft.colorpicker.ColorPickerDialog;
import com.takisoft.colorpicker.OnColorSelectedListener;

public class ThemeColorPreferenceDialogFragmentCompat extends PreferenceDialogFragmentCompat implements OnColorSelectedListener {

    private int pickedColor;
    ThemeUtil.CustomThemeColors[] themeColors;
    private int[] colors;

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

        ColorPickerDialog.Params params = new ColorPickerDialog.Params.Builder(activity)
                .setSelectedColor(selectedColor)
                .setColors(colors)
                .setSize(ColorPickerDialog.SIZE_SMALL)
                .setSortColors(false)
                .setColumns(0)
                .build();

        ColorPickerDialog dialog = new ColorPickerDialog(activity, this, params);
        dialog.setTitle(pref.getDialogTitle());

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
