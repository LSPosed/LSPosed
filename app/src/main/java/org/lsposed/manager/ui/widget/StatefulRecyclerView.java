package org.lsposed.manager.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.adapter.StatefulAdapter;

import rikka.widget.borderview.BorderRecyclerView;

public class StatefulRecyclerView extends BorderRecyclerView {
    public StatefulRecyclerView(@NonNull Context context) {
        super(context);
    }

    public StatefulRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public StatefulRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        var adapter = getAdapter();
        if (adapter instanceof StatefulAdapter) {
            bundle.putParcelable("adaptor", ((StatefulAdapter) adapter).saveState());
        }
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            super.onRestoreInstanceState(bundle.getParcelable("superState"));
            var adapter = getAdapter();
            if (adapter instanceof StatefulAdapter) {
                ((StatefulAdapter) adapter).restoreState(bundle.getParcelable("adaptor"));
            }
        } else {
            super.onRestoreInstanceState(state);
        }
    }
}
