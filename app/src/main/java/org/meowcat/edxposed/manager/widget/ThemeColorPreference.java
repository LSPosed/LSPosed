package org.meowcat.edxposed.manager.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.takisoft.preferencex.ColorPickerPreference;
import com.takisoft.preferencex.ColorPickerPreferenceDialogFragmentCompat;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import org.meowcat.edxposed.manager.util.CustomThemeColor;
import org.meowcat.edxposed.manager.util.CustomThemeColors;

import java.util.Objects;

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