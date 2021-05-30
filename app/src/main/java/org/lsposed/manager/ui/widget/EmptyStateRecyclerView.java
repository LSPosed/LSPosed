/*
 * <!--This file is part of LSPosed.
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
 * Copyright (C) 2021 LSPosed Contributors-->
 */

package org.lsposed.manager.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;

import org.lsposed.manager.R;

import rikka.core.res.ResourcesKt;
import rikka.widget.borderview.BorderRecyclerView;

public class EmptyStateRecyclerView extends BorderRecyclerView {
    private final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final String emptyText;
    private final AdapterDataObserver emptyObserver = new AdapterDataObserver() {

        @Override
        public void onChanged() {
            Adapter<?> adapter = getAdapter();
            if (adapter != null) {
                boolean newEmpty = adapter.getItemCount() == 0;
                if (empty != newEmpty) {
                    empty = newEmpty;
                    invalidate();
                }
            }
        }
    };

    private boolean empty = false;


    public EmptyStateRecyclerView(Context context) {
        this(context, null);
    }

    public EmptyStateRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyStateRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        paint.setColor(ResourcesKt.resolveColor(context.getTheme(), android.R.attr.textColorSecondary));
        paint.setTextSize(16f * dm.scaledDensity);

        emptyText = context.getString(R.string.list_empty);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        var oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(emptyObserver);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver);
            if (adapter instanceof EmptyStateAdapter && ((EmptyStateAdapter<?>) adapter).isLoaded()) {
                emptyObserver.onChanged();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (empty) {
            final int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
            final int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();

            var textLayout = new StaticLayout(emptyText, paint, width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

            canvas.save();
            canvas.translate(getPaddingLeft(), (height >> 1) + getPaddingTop() - (textLayout.getHeight() >> 1));

            textLayout.draw(canvas);

            canvas.restore();
        }
    }


    public abstract static class EmptyStateAdapter<T extends ViewHolder> extends Adapter<T> {
        abstract public boolean isLoaded();
    }

}
