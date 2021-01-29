package io.github.lsposed.manager.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import io.github.lsposed.manager.App;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.util.CompileUtil;
import io.github.lsposed.manager.util.CustomThemeColor;
import io.github.lsposed.manager.util.CustomThemeColors;
import io.github.lsposed.manager.util.RebootUtil;

public class BaseActivity extends AppCompatActivity {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";
    protected static SharedPreferences preferences;
    private String theme;

    static {
        preferences = App.getPreferences();
    }

    public static boolean isBlackNightTheme() {
        return preferences.getBoolean("black_dark_theme", false) || preferences.getBoolean("md2", true);
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
        if (this instanceof AboutActivity) {
            return R.style.ThemeOverlay_Black;
        }
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
        AppCompatDelegate.setDefaultNightMode(preferences.getInt("theme", -1));
        theme = getTheme(this) + getCustomTheme() + preferences.getBoolean("md2", true);
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
        if (!(this instanceof MainActivity)) {
            if (preferences.getBoolean("transparent_status_bar", false)) {
                getWindow().setStatusBarColor(getThemedColor(R.attr.colorActionBar));
            } else {
                getWindow().setStatusBarColor(getThemedColor(R.attr.colorPrimaryDark));
            }
        } else {
            getWindow().setStatusBarColor(getThemedColor(android.R.attr.colorBackground));
        }
        if (!Objects.equals(theme, getTheme(this) + getCustomTheme() + preferences.getBoolean("md2", true))) {
            recreate();
        }
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
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

    private void areYouSure(int contentTextId, DialogInterface.OnClickListener listener) {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.areyousure)
                .setMessage(contentTextId)
                .setPositiveButton(android.R.string.ok, listener)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.dexopt_all) {
            CompileUtil.compileAllDexopt(this);
        } else if (itemId == R.id.speed_all) {
            CompileUtil.compileAllSpeed(this);
        } else if (itemId == R.id.reboot) {
            areYouSure(R.string.reboot, (dialog, which) -> RebootUtil.reboot(RebootUtil.RebootType.NORMAL));
        } else if (itemId == R.id.soft_reboot) {
            areYouSure(R.string.soft_reboot, (dialog, which) -> RebootUtil.reboot(RebootUtil.RebootType.USERSPACE));
        } else if (itemId == R.id.reboot_recovery) {
            areYouSure(R.string.reboot_recovery, (dialog, which) -> RebootUtil.reboot(RebootUtil.RebootType.RECOVERY));
        } else if (itemId == R.id.reboot_bootloader) {
            areYouSure(R.string.reboot_bootloader, (dialog, which) -> RebootUtil.reboot(RebootUtil.RebootType.BOOTLOADER));
        } else if (itemId == R.id.reboot_download) {
            areYouSure(R.string.reboot_download, (dialog, which) -> RebootUtil.reboot(RebootUtil.RebootType.DOWNLOAD));
        } else if (itemId == R.id.reboot_edl) {
            areYouSure(R.string.reboot_edl, (dialog, which) -> RebootUtil.reboot(RebootUtil.RebootType.EDL));
        }

        return super.onOptionsItemSelected(item);
    }
}
