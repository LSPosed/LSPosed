package org.meowcat.edxposed.manager.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

public class ListPreferenceSummaryFix extends ListPreference {
    public ListPreferenceSummaryFix(Context context) {
        super(context);
    }

    public ListPreferenceSummaryFix(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        notifyChanged();
    }
}