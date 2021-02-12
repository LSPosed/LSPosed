package io.github.lsposed.manager.util;

import android.net.Uri;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

import io.github.lsposed.manager.R;
import io.github.lsposed.manager.ui.activity.BaseActivity;

public final class NavUtil {

    public static void startURL(BaseActivity activity, Uri uri) {
        CustomTabsIntent.Builder customTabsIntent = new CustomTabsIntent.Builder();
        customTabsIntent.setShowTitle(true);
        CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(activity.getThemedColor(R.attr.toolbarColor))
                .setNavigationBarColor(activity.getThemedColor(android.R.attr.navigationBarColor))
                .setNavigationBarDividerColor(0)
                .build();
        customTabsIntent.setDefaultColorSchemeParams(params);
        boolean night = BaseActivity.isNightMode(activity.getResources().getConfiguration());
        customTabsIntent.setColorScheme(night ? CustomTabsIntent.COLOR_SCHEME_DARK : CustomTabsIntent.COLOR_SCHEME_LIGHT);
        customTabsIntent.build().launchUrl(activity, uri);
    }

    public static void startURL(BaseActivity activity, String url) {
        startURL(activity, Uri.parse(url));
    }
}