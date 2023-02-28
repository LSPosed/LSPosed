package org.lsposed.manager.util;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import rikka.core.util.ResourceUtils;

public class StyleUtils {
    public static void setHintSpanColor(Context context, SpannableStringBuilder spannableStringBuilder, int start, int end, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final TypefaceSpan typefaceSpan = new TypefaceSpan(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            spannableStringBuilder.setSpan(typefaceSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        } else {
            final StyleSpan styleSpan = new StyleSpan(Typeface.BOLD);
            spannableStringBuilder.setSpan(styleSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
        if (color != -1) {
            final ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(ResourceUtils.resolveColor(context.getTheme(), color));
            spannableStringBuilder.setSpan(foregroundColorSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        }
    }
}
