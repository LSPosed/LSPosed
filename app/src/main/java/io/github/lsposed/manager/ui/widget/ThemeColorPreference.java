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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package io.github.lsposed.manager.ui.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.takisoft.preferencex.ColorPickerPreference;
import com.takisoft.preferencex.ColorPickerPreferenceDialogFragmentCompat;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import java.util.Objects;

import io.github.lsposed.manager.util.CustomThemeColor;
import io.github.lsposed.manager.util.CustomThemeColors;

public class ThemeColorPreference extends ColorPickerPreference {

    static {
        PreferenceFragmentCompat.registerPreferenceFragment(ThemeColorPreference.class,
                ColorPickerPreferenceDialogFragmentCompat.class);
    }

    public ThemeColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public ThemeColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ThemeColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThemeColorPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        String key = getKey();
        Context context = getContext();
        CustomThemeColor[] colors;
        if (Objects.equals(key, "primary_color")) {
            colors = CustomThemeColors.Primary.values();
        } else if (Objects.equals(key, "accent_color")) {
            colors = CustomThemeColors.Accent.values();
        } else {
            throw new IllegalArgumentException("Unknown custom theme color preference key: " + key);
        }
        int[] mEntryValues = new int[colors.length];
        for (int i = 0; i < colors.length; ++i) {
            CustomThemeColor color = colors[i];
            mEntryValues[i] = ContextCompat.getColor(context, color.getResourceId());
        }
        setColors(mEntryValues);
    }
}