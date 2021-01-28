package org.meowcat.edxposed.manager.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.CustomThemeColor;
import org.meowcat.edxposed.manager.util.CustomThemeColors;
import org.meowcat.edxposed.manager.util.NavUtil;
import org.meowcat.edxposed.manager.util.RebootUtil;

import java.util.Objects;

public class BaseActivity extends AppCompatActivity {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";
    protected static SharedPreferences preferences;
    private String theme;

    static {
        preferences = App.getPreferences();
    }

    public static boolean isBlackNightTheme() {
        return preferences.getBoolean("black_dark_theme", false) || preferences.getBoolean("md2", false);
    }

    public static String getTheme(Context context) {
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
        String baseThemeName = preferences.getBoolean("colorized_action_bar", false) && !preferences.getBoolean("md2", false) ?
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
        theme = getTheme(this) + getCustomTheme() + preferences.getBoolean("md2", false);
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
        }
        if (!Objects.equals(theme, getTheme(this) + getCustomTheme() + preferences.getBoolean("md2", false))) {
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
        if (preferences.getBoolean("md2", false) && !(this instanceof MainActivity)) {
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
            areYouSure(R.string.take_while_cannot_resore, (dialog, which) -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.dexopt_now)
                        .setMessage(R.string.this_may_take_a_while)
                        .setCancelable(false)
                        .show();
                new Thread("dexopt") {
                    @Override
                    public void run() {
                        if (!Shell.rootAccess()) {
                            dialog.dismiss();
                            NavUtil.showMessage(BaseActivity.this, getString(R.string.root_failed));
                            return;
                        }

                        Shell.su("cmd package bg-dexopt-job").exec();

                        dialog.dismiss();
                        App.runOnUiThread(() -> Toast.makeText(BaseActivity.this, R.string.done, Toast.LENGTH_LONG).show());
                    }
                }.start();
            });
        } else if (itemId == R.id.speed_all) {
            areYouSure(R.string.take_while_cannot_resore, (dialog, which) -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.speed_now)
                        .setMessage(R.string.this_may_take_a_while)
                        .setCancelable(false)
                        .show();
                new Thread("dex2oat") {
                    @Override
                    public void run() {
                        if (!Shell.rootAccess()) {
                            dialog.dismiss();
                            NavUtil.showMessage(BaseActivity.this, getString(R.string.root_failed));
                            return;
                        }

                        Shell.su("cmd package compile -m speed -a").exec();

                        dialog.dismiss();
                        App.runOnUiThread(() -> Toast.makeText(BaseActivity.this, R.string.done, Toast.LENGTH_LONG).show());
                    }
                };
            });
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
