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

package org.lsposed.manager.ui.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

public class LinkifyTextView extends androidx.appcompat.widget.AppCompatTextView {

    private ClickableSpan mCurrentSpan;

    public LinkifyTextView(Context context) {
        super(context);
    }

    public LinkifyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkifyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ClickableSpan getCurrentSpan() {
        return mCurrentSpan;
    }

    public void clearCurrentSpan() {
        mCurrentSpan = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        // Let the parent or grandparent of TextView to handles click action.
        // Otherwise click effect like ripple will not work, and if touch area
        // do not contain a url, the TextView will still get MotionEvent.
        // onTouchEven must be called with MotionEvent.ACTION_DOWN for each touch
        // action on it, so we analyze touched url here.
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mCurrentSpan = null;

            if (getText() instanceof Spanned) {
                // Get this code from android.text.method.LinkMovementMethod.
                // Work fine !
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();

                x += getScrollX();
                y += getScrollY();

                Layout layout = getLayout();
                if (null != layout) {
                    int line = layout.getLineForVertical(y);
                    int off = layout.getOffsetForHorizontal(line, x);

                    ClickableSpan[] spans = ((Spanned) getText()).getSpans(off, off, ClickableSpan.class);

                    if (spans.length > 0) {
                        mCurrentSpan = spans[0];
                    }
                }
            }
        }

        return super.onTouchEvent(event);
    }
}
