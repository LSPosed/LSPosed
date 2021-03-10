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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

import com.takisoft.preferencex.PreferenceFragmentCompat;

import org.lsposed.manager.R;

public class ThemeColorPreference extends DialogPreference {

    static {
        PreferenceFragmentCompat.registerPreferenceFragment(ThemeColorPreference.class,
                ThemeColorPreferenceDialogFragmentCompat.class);
    }

    private final ThemeUtil.CustomThemeColors[] colors;
    private ThemeUtil.CustomThemeColors color;

    private ImageView colorWidget;

    public ThemeColorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        colors = ThemeUtil.CustomThemeColors.values();

        setWidgetLayoutResource(R.layout.preference_widget_color_swatch);
    }

    public ThemeColorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @SuppressLint("RestrictedApi")
    public ThemeColorPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.dialogPreferenceStyle,
                android.R.attr.dialogPreferenceStyle));
    }

    public ThemeColorPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        colorWidget = (ImageView) holder.findViewById(R.id.color_picker_widget);
        setColorOnWidget(color);
    }

    private void setColorOnWidget(ThemeUtil.CustomThemeColors color) {
        if (colorWidget == null) {
            return;
        }
        Drawable drawable = getContext().getDrawable(R.drawable.colorpickerpreference_pref_swatch);
        drawable.setTint(getContext().getColor(color.getResourceId()));
        colorWidget.setImageDrawable(drawable);
    }

    public ThemeUtil.CustomThemeColors[] getColors() {
        return colors;
    }

    private void setInternalColor(ThemeUtil.CustomThemeColors color, boolean force) {
        ThemeUtil.CustomThemeColors oldColor = ThemeUtil.CustomThemeColors.valueOf(getPersistedString("COLOR_PRIMARY"));

        boolean changed = !oldColor.equals(color);

        if (changed || force) {
            this.color = color;

            persistString(color.toString());

            setColorOnWidget(color);

            notifyChanged();
        }
    }

    public ThemeUtil.CustomThemeColors getColor() {
        return color;
    }

    public void setColor(ThemeUtil.CustomThemeColors color) {
        setInternalColor(color, false);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValueObj) {
        setInternalColor(ThemeUtil.CustomThemeColors.valueOf(getPersistedString(ThemeUtil.CustomThemeColors.COLOR_PRIMARY.toString())), true);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.color = color.toString();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setColor(ThemeUtil.CustomThemeColors.valueOf(myState.color));
    }

    private static class SavedState extends BaseSavedState {
        private String color;

        public SavedState(Parcel source) {
            super(source);
            color = source.readString();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(color);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
