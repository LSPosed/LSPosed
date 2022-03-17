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
import androidx.core.view.ScrollingView;
import androidx.viewpager2.widget.ViewPager2;

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
        if (clampedX || clampedY) {
            var viewParent = findViewParentIfNeeds(this);
            if (viewParent != null) viewParent.requestDisallowInterceptTouchEvent(false);
        }
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }

    private static ViewParent findViewParentIfNeeds(View v) {
        var parent = v.getParent();
        if (parent == null) return null;
        if (parent instanceof ViewPager2 || parent instanceof ScrollingView) {
            return parent;
        } else if (parent instanceof View) {
            return findViewParentIfNeeds((View) parent);
        } else {
            return parent;
        }
    }
}
