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
 * Copyright (C) 2022 LSPosed Contributors
 */

package org.lsposed.manager.util;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.StatefulAdapter;

import java.util.HashMap;
import java.util.List;

public abstract class SimpleStatefulAdaptor<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> implements StatefulAdapter {
    HashMap<Long, SparseArray<Parcelable>> states = new HashMap<>();
    protected RecyclerView rv = null;

    public SimpleStatefulAdaptor() {
        setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
    }

    @Override
    @CallSuper
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        rv = recyclerView;
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onViewRecycled(@NonNull T holder) {
        saveStateOf(holder);
        super.onViewRecycled(holder);
    }

    @CallSuper
    @Override
    public final void onBindViewHolder(@NonNull T holder, int position, @NonNull List<Object> payloads) {
        var state = states.remove(holder.getItemId());
        if (state != null) {
            holder.itemView.restoreHierarchyState(state);
        }
        onBindViewHolder(holder, position);
    }

    private void saveStateOf(@NonNull RecyclerView.ViewHolder holder) {
        var state = new SparseArray<Parcelable>();
        holder.itemView.saveHierarchyState(state);
        states.put(holder.getItemId(), state);
    }

    @NonNull
    public Parcelable saveState() {
        for (int childCount = rv.getChildCount(), i = 0; i < childCount; ++i) {
            saveStateOf(rv.getChildViewHolder(rv.getChildAt(i)));
        }

        var out = new Bundle();
        for (var state : states.entrySet()) {
            var item = new Bundle();
            for (int i = 0; i < state.getValue().size(); ++i) {
                item.putParcelable(String.valueOf(state.getValue().keyAt(i)), state.getValue().valueAt(i));
            }
            out.putParcelable(String.valueOf(state.getKey()), item);
        }
        return out;
    }

    @Override
    public void restoreState(@NonNull Parcelable savedState) {
        if (savedState instanceof Bundle) {
            for (var stateKey : ((Bundle) savedState).keySet()) {
                var array = new SparseArray<Parcelable>();
                var state = ((Bundle) savedState).getParcelable(stateKey);
                if (state instanceof Bundle) {
                    for (var itemKey : ((Bundle) state).keySet()) {
                        var item = ((Bundle) state).getParcelable(itemKey);
                        array.put(Integer.parseInt(itemKey), item);
                    }
                }
                states.put(Long.parseLong(stateKey), array);
            }
        }
    }

}
