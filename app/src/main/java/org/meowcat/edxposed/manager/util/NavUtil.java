package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.BaseActivity;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;

public final class NavUtil {

    public static Uri parseURL(String str) {
        if (str == null || str.isEmpty())
            return null;

        Spannable spannable = new SpannableString(str);
        Linkify.addLinks(spannable, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);
        return (spans.length > 0) ? Uri.parse(spans[0].getURL()) : null;
    }

    public static void startURL(BaseActivity activity, Uri uri) {
        CustomTabsIntent.Builder customTabsIntent = new CustomTabsIntent.Builder();
        customTabsIntent.setShowTitle(true);
        customTabsIntent.setToolbarColor(activity.getThemedColor(R.attr.colorActionBar));
        customTabsIntent.build().launchUrl(activity, uri);
    }

    public static void startURL(BaseActivity activity, String url) {
        startURL(activity, parseURL(url));
    }

    @AnyThread
    public static void showMessage(final @NonNull Context context, final CharSequence message) {
        XposedApp.runOnUiThread(() -> new MaterialAlertDialogBuilder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }

}