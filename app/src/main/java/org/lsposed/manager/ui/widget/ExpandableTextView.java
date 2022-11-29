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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.textview.MaterialTextView;

import org.lsposed.manager.R;

public class ExpandableTextView extends MaterialTextView {
    private CharSequence text = null;
    private int nextLines = 0;
    private final int maxLines;
    private final SpannableString collapse;
    private final SpannableString expand;
    private final SpannableStringBuilder sb = new SpannableStringBuilder();
    private int lineCount = 0;

    public ExpandableTextView(Context context) {
        this(context, null);
    }

    public ExpandableTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        maxLines = getMaxLines();
        collapse = new SpannableString(context.getString(R.string.collapse));
        ClickableSpan span = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                TransitionManager.beginDelayedTransition((ViewGroup) getParent());
                setMaxLines(nextLines);
                ExpandableTextView.super.setText(text);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setTypeface(Typeface.DEFAULT_BOLD);
            }
        };
        collapse.setSpan(span, 0, collapse.length(), 0);
        expand = new SpannableString(context.getString(R.string.expand));
        expand.setSpan(span, 0, expand.length(), 0);
        setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        this.text = text;
        super.setText(text, type);
    }

    @Override
    public boolean onPreDraw() {
        this.getViewTreeObserver().removeOnPreDrawListener(this);
        if (lineCount == 0) {
            lineCount = getLayout().getLineCount();
        }
        if (lineCount > maxLines) {
            int hintTextOffsetEnd;
            if (maxLines == getMaxLines()) {
                nextLines = lineCount + 1;
                hintTextOffsetEnd = getLayout().getLineStart(getMaxLines() - 1);
                setTextWithSpan(text, hintTextOffsetEnd - 1, expand);
            } else if (nextLines == getMaxLines()) {
                nextLines = maxLines;
                hintTextOffsetEnd = getLayout().getLineStart(getMaxLines() - 1);
                setTextWithSpan(text, hintTextOffsetEnd, collapse);
            }
        }
        return super.onPreDraw();
    }

    private void setTextWithSpan(CharSequence text, int textOffsetEnd,
                                 SpannableString sbStr) {
        sb.clearSpans();
        sb.clear();
        sb.append(text, 0, textOffsetEnd);
        sb.append("\n");
        sb.append(sbStr);
        super.setText(sb, BufferType.NORMAL);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getLayout() != null) {
            lineCount = getLayout().getLineCount();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        Layout layout = this.getLayout();
        if (layout != null) {
            int line = layout.getLineForVertical((int) event.getY());
            int offset = layout.getOffsetForHorizontal(line, event.getX());

            if (getText() instanceof Spanned) {
                Spanned spanned = (Spanned) getText();

                ClickableSpan[] links = spanned.getSpans(offset, offset, ClickableSpan.class);

                if (links.length == 0) {
                    return false;
                } else {
                    return super.onTouchEvent(event);
                }
            }
        }

        return false;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putInt("maxLines", getMaxLines());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            setMaxLines(bundle.getInt("maxLines"));
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }

}
