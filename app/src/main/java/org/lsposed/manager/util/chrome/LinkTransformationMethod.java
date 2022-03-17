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

package org.lsposed.manager.util.chrome;

import android.app.Activity;
import android.graphics.Rect;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.TransformationMethod;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

public class LinkTransformationMethod implements TransformationMethod {

    private final Activity activity;

    public LinkTransformationMethod(Activity activity) {
        this.activity = activity;
    }

    @Override
    public CharSequence getTransformation(CharSequence source, View view) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            if (textView.getText() == null || !(textView.getText() instanceof Spannable)) {
                return source;
            }
            Spannable text = (Spannable) textView.getText();
            URLSpan[] spans = text.getSpans(0, textView.length(), URLSpan.class);
            for (int i = spans.length - 1; i >= 0; i--) {
                URLSpan oldSpan = spans[i];
                int start = text.getSpanStart(oldSpan);
                int end = text.getSpanEnd(oldSpan);
                String url = oldSpan.getURL();
                text.removeSpan(oldSpan);
                text.setSpan(new CustomTabsURLSpan(activity, url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return text;
        }
        return source;
    }

    @Override
    public void onFocusChanged(View view, CharSequence sourceText, boolean focused, int direction, Rect previouslyFocusedRect) {
    }
}
