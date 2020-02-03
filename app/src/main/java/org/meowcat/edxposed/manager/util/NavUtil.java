package org.meowcat.edxposed.manager.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;

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

    public static void startURL(Activity activity, Uri uri) {
        if (!XposedApp.getPreferences().getBoolean("chrome_tabs", true)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, activity.getPackageName());
            activity.startActivity(intent);
            return;
        }

        CustomTabsIntent.Builder customTabsIntent = new CustomTabsIntent.Builder();
        customTabsIntent.setShowTitle(true);
        customTabsIntent.setToolbarColor(XposedApp.getColor(activity));
        customTabsIntent.build().launchUrl(activity, uri);
    }

    public static void startURL(Activity activity, String url) {
        startURL(activity, parseURL(url));
    }

    @AnyThread
    public static void showMessage(final @NonNull Context context, final CharSequence message) {
        XposedApp.runOnUiThread(() -> new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show());
    }

}