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

package org.lsposed.manager.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import rikka.widget.borderview.BorderRecyclerView;

public class ScrollWebView extends WebView {
    public ScrollWebView(@NonNull Context context) {
        super(context);
    }

    public ScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ScrollWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            var viewParent = findViewParentIfNeeds(this);
            if (viewParent != null) viewParent.requestDisallowInterceptTouchEvent(true);
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        if (clampedX) {
            var viewParent = findViewParentIfNeeds(this);
            if (viewParent != null) viewParent.requestDisallowInterceptTouchEvent(false);
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    private static ViewParent findViewParentIfNeeds(View v) {
        var parent = v.getParent();
        if (parent == null) return null;
        if (parent instanceof RecyclerView && !(parent instanceof BorderRecyclerView)) {
            return parent;
        } else if (parent instanceof View) {
            return findViewParentIfNeeds((View) parent);
        } else {
            return parent;
        }
    }
}
