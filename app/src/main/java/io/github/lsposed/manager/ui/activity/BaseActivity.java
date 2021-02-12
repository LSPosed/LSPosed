package io.github.lsposed.manager.ui.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.util.CustomThemeColor;
import io.github.lsposed.manager.util.CustomThemeColors;
import io.github.lsposed.manager.util.NavUtil;
import io.github.lsposed.manager.util.Version;
import rikka.material.app.MaterialActivity;

public class BaseActivity extends MaterialActivity {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";
    protected static SharedPreferences preferences;
    private String theme;

    static {
        preferences = App.getPreferences();
    }

    public static boolean isBlackNightTheme() {
        return preferences.getBoolean("black_dark_theme", false);
    }


    public String getTheme(Context context) {
        if (isBlackNightTheme()
                && isNightMode(context.getResources().getConfiguration()))
            return THEME_BLACK;

        return THEME_DEFAULT;
    }

    public static boolean isNightMode(Configuration configuration) {
        return (configuration.uiMode & Configuration.UI_MODE_NIGHT_YES) > 0;
    }

    @StyleRes
    public int getThemeStyleRes(Context context) {
        switch (getTheme(context)) {
            case THEME_BLACK:
                return R.style.ThemeOverlay_Black;
            case THEME_DEFAULT:
            default:
                return R.style.ThemeOverlay;
        }
    }

    @StyleRes
    private int getCustomTheme() {
        String baseThemeName = preferences.getBoolean("colorized_action_bar", false) && !preferences.getBoolean("md2", true) ?
                "ThemeOverlay.ActionBarPrimaryColor" : "ThemeOverlay";
        String customThemeName;
        String primaryColorEntryName = "colorPrimary";
        for (CustomThemeColor color : CustomThemeColors.Primary.values()) {
            if (preferences.getInt("primary_color", ContextCompat.getColor(this, R.color.colorPrimary))
                    == ContextCompat.getColor(this, color.getResourceId())) {
                primaryColorEntryName = color.getResourceEntryName();
            }
        }
        String accentColorEntryName = "colorAccent";
        for (CustomThemeColor color : CustomThemeColors.Accent.values()) {
            if (preferences.getInt("accent_color", ContextCompat.getColor(this, R.color.colorAccent))
                    == ContextCompat.getColor(this, color.getResourceId())) {
                accentColorEntryName = color.getResourceEntryName();
            }
        }
        customThemeName = baseThemeName + "." + primaryColorEntryName + "." + accentColorEntryName;
        return getResources().getIdentifier(customThemeName, "style", getPackageName());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = getTheme(this) + getCustomTheme() + preferences.getBoolean("md2", true);

        // make sure the versions are consistent
        String coreVersionStr = Constants.getXposedVersion();
        if (coreVersionStr != null) {
            Version managerVersion = new Version(BuildConfig.VERSION_NAME);
            Version coreVersion = new Version(coreVersionStr);
            if (!managerVersion.equals(coreVersion)) {
                new MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.outdated_manager)
                        .setPositiveButton(R.string.ok, (dialog, id) -> {
                            NavUtil.startURL(this, getString(R.string.about_source));
                            finish();
                        })
                        .setCancelable(false)
                        .show();
            }
        }
    }

    public int getThemedColor(int id) {
        TypedArray typedArray = getTheme().obtainStyledAttributes(new int[]{id});
        int color = typedArray.getColor(0, 0);
        typedArray.recycle();
        return color;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().setStatusBarColor(0);
        if (!Objects.equals(theme, getTheme(this) + getCustomTheme() + preferences.getBoolean("md2", true))) {
            recreate();
        }
    }

    @Override
    protected void onApplyThemeResource(@NonNull Resources.Theme theme, int resid, boolean first) {
        // apply real style and our custom style
        if (getParent() == null) {
            theme.applyStyle(resid, true);
        } else {
            try {
                theme.setTo(getParent().getTheme());
            } catch (Exception e) {
                // Empty
            }
            theme.applyStyle(resid, false);
        }
        theme.applyStyle(getCustomTheme(), true);
        if (preferences.getBoolean("md2", true) && !(this instanceof MainActivity)) {
            theme.applyStyle(R.style.ThemeOverlay_Md2, true);
        }
        if (this instanceof MainActivity) {
            theme.applyStyle(R.style.ThemeOverlay_ActivityMain, true);
        }
        theme.applyStyle(getThemeStyleRes(this), true);
        // only pass theme style to super, so styled theme will not be overwritten
        super.onApplyThemeResource(theme, R.style.ThemeOverlay, first);
    }
}
