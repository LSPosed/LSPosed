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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import org.lsposed.manager.R;

public class ExpandableTextView extends TextView {
    private final TimeInterpolator expandInterpolator;
    private final TimeInterpolator collapseInterpolator;
    private static final int DEFAULT_ANIM_DURATION = 200;
    private final int maxLines;
    private final long animationDuration;
    private boolean animating;
    private boolean expanded;
    private int collapsedHeight;

    public ExpandableTextView(Context context) {
        this(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        var attributes = context.obtainStyledAttributes(attrs, R.styleable.ExpandableTextView, defStyle, 0);
        animationDuration = attributes.getInt(R.styleable.ExpandableTextView_animation_duration, DEFAULT_ANIM_DURATION);
        attributes.recycle();
        maxLines = getMaxLines();
        getViewTreeObserver().addOnDrawListener(() -> {
            if (getLineCount() > maxLines) {
                setOnClickListener(v -> toggle());
            }
        });

        expandInterpolator = new AccelerateDecelerateInterpolator();
        collapseInterpolator = new AccelerateDecelerateInterpolator();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // if this TextView is collapsed and maxLines = 0,
        // than make its height equals to zero
        if (this.maxLines == 0 && !this.expanded && !this.animating) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void toggle() {
        if (expanded) {
            collapse();
        } else {
            expand();
        }
    }

    private void expand() {
        if (!expanded && !animating && maxLines >= 0) {
            // measure collapsed height
            measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            collapsedHeight = getMeasuredHeight();
            animating = true;

            // set maxLines to MAX Integer, so we can calculate the expanded height
            setMaxLines(Integer.MAX_VALUE);

            // measure expanded height
            measure(MeasureSpec.makeMeasureSpec(this.getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            var expandedHeight = getMeasuredHeight();

            // animate from collapsed height to expanded height
            var valueAnimator = ValueAnimator.ofInt(this.collapsedHeight, expandedHeight);
            valueAnimator.addUpdateListener(animation -> setHeight((int) animation.getAnimatedValue()));

            // wait for the animation to end
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // reset min & max height (previously set with setHeight() method)
                    setMaxHeight(Integer.MAX_VALUE);
                    setMinHeight(0);

                    // if fully expanded, set height to WRAP_CONTENT, because when rotating the device
                    // the height calculated with this ValueAnimator isn't correct anymore
                    var layoutParams = getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    setLayoutParams(layoutParams);
                    expanded = true;
                    animating = false;
                }
            });
            valueAnimator.setInterpolator(expandInterpolator);
            valueAnimator.setDuration(animationDuration).start();
        }
    }

    private void collapse() {
        if (expanded && !animating && maxLines >= 0) {
            // measure expanded height
            var expandedHeight = getMeasuredHeight();
            animating = true;

            // animate from expanded height to collapsed height
            var valueAnimator = ValueAnimator.ofInt(expandedHeight, collapsedHeight);
            valueAnimator.addUpdateListener(animation -> setHeight((int) animation.getAnimatedValue()));

            // wait for the animation to end
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(final Animator animation) {
                    expanded = false;
                    animating = false;

                    // set maxLines back to original value
                    setMaxLines(maxLines);

                    // if fully collapsed, set height back to WRAP_CONTENT, because when rotating the device
                    // the height previously calculated with this ValueAnimator isn't correct anymore
                    var layoutParams = getLayoutParams();
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    setLayoutParams(layoutParams);
                }
            });
            valueAnimator.setInterpolator(collapseInterpolator);
            valueAnimator.setDuration(animationDuration).start();
        }
    }
}
