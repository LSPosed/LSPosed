package io.github.lsposed.manager.util;

import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.text.util.Linkify;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.BaseActivity;

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
        CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(activity.getThemedColor(R.attr.colorActionBar))
                .setNavigationBarColor(activity.getThemedColor(android.R.attr.navigationBarColor))
                .setNavigationBarDividerColor(0)
                .build();
        customTabsIntent.setDefaultColorSchemeParams(params);
        boolean night = BaseActivity.isNightMode(activity.getResources().getConfiguration());
        customTabsIntent.setColorScheme(night ? CustomTabsIntent.COLOR_SCHEME_DARK : CustomTabsIntent.COLOR_SCHEME_LIGHT);
        customTabsIntent.build().launchUrl(activity, uri);
    }

    public static void startURL(BaseActivity activity, String url) {
        startURL(activity, parseURL(url));
    }
}