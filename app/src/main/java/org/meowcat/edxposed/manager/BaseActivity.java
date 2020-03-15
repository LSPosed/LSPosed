package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.topjohnwu.superuser.Shell;

import org.meowcat.edxposed.manager.util.CustomThemeColor;
import org.meowcat.edxposed.manager.util.CustomThemeColors;
import org.meowcat.edxposed.manager.util.NavUtil;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";
    private String theme;

    public static boolean isBlackNightTheme() {
        return XposedApp.getPreferences().getBoolean("black_dark_theme", false) || XposedApp.getPreferences().getBoolean("md2", false);
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

    @Override
    public void setContentView(View view) {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
            int padding = (displayMetrics.widthPixels - displayMetrics.heightPixels) / 2;
            frameLayout.setPadding(padding, 0, padding, 0);
        }
        frameLayout.addView(view);
        super.setContentView(frameLayout);
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
        String baseThemeName = XposedApp.getPreferences().getBoolean("colorized_action_bar", false) && !XposedApp.getPreferences().getBoolean("md2", false) ?
                "ThemeOverlay.ActionBarPrimaryColor" : "ThemeOverlay";
        String customThemeName;
        String primaryColorEntryName = "colorPrimary";
        for (CustomThemeColor color : CustomThemeColors.Primary.values()) {
            if (XposedApp.getPreferences().getInt("primary_color", ContextCompat.getColor(this, R.color.colorPrimary))
                    == ContextCompat.getColor(this, color.getResourceId())) {
                primaryColorEntryName = color.getResourceEntryName();
            }
        }
        String accentColorEntryName = "colorAccent";
        for (CustomThemeColor color : CustomThemeColors.Accent.values()) {
            if (XposedApp.getPreferences().getInt("accent_color", ContextCompat.getColor(this, R.color.colorAccent))
                    == ContextCompat.getColor(this, color.getResourceId())) {
                accentColorEntryName = color.getResourceEntryName();
            }
        }
        customThemeName = baseThemeName + "." + primaryColorEntryName + "." + accentColorEntryName;
        return getResources().getIdentifier(customThemeName, "style", getPackageName());
    }

    protected void setupWindowInsets(View rootView, View secondView) {
        rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            if (secondView != null && insets.getTappableElementInsets().bottom != insets.getSystemWindowInsetBottom()) {
                secondView.setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
            }
            rootView.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getTappableElementInsets().bottom);
            return insets;
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(XposedApp.getPreferences().getInt("theme", 0));
        theme = getTheme(this) + getCustomTheme() + XposedApp.getPreferences().getBoolean("md2", false);
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
            if (XposedApp.getPreferences().getBoolean("transparent_status_bar", false)) {
                getWindow().setStatusBarColor(getThemedColor(R.attr.colorActionBar));
            } else {
                getWindow().setStatusBarColor(getThemedColor(R.attr.colorPrimaryDark));
            }
        }
        if (!Objects.equals(theme, getTheme(this) + getCustomTheme() + XposedApp.getPreferences().getBoolean("md2", false))) {
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
        if (XposedApp.getPreferences().getBoolean("md2", false) && !(this instanceof MainActivity)) {
            theme.applyStyle(R.style.ThemeOverlay_Md2, true);
        }
        theme.applyStyle(getThemeStyleRes(this), true);
        // only pass theme style to super, so styled theme will not be overwritten
        super.onApplyThemeResource(theme, R.style.ThemeOverlay, first);
    }

    private void areYouSure(int contentTextId, DialogInterface.OnClickListener listener) {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.areyousure)
                .setMessage(contentTextId)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    void softReboot() {
        if (!Shell.rootAccess()) {
            showAlert(getString(R.string.root_failed));
            return;
        }

        List<String> messages = new LinkedList<>();
        Shell.Result result = Shell.su("setprop ctl.restart surfaceflinger; setprop ctl.restart zygote").exec();
        if (result.getCode() != 0) {
            messages.add(result.getOut().toString());
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
    }

    void showAlert(final String result) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            runOnUiThread(() -> showAlert(result));
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setMessage(result)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    void reboot(String mode) {
        if (!Shell.rootAccess()) {
            showAlert(getString(R.string.root_failed));
            return;
        }

        List<String> messages = new LinkedList<>();

        String command = "/system/bin/svc power reboot";
        if (mode != null) {
            command += " " + mode;
            if (mode.equals("recovery"))
                // create a flag used by some kernels to boot into recovery
                Shell.su("touch /cache/recovery/boot").exec();
        }
        Shell.Result result = Shell.su(command).exec();
        if (result.getCode() != 0) {
            messages.add(result.getOut().toString());
            messages.add("");
            messages.add(getString(R.string.reboot_failed));
            showAlert(TextUtils.join("\n", messages).trim());
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dexopt_all:
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
                            XposedApp.runOnUiThread(() -> Toast.makeText(BaseActivity.this, R.string.done, Toast.LENGTH_LONG).show());
                        }
                    }.start();
                });
                break;
            case R.id.speed_all:
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
                            XposedApp.runOnUiThread(() -> Toast.makeText(BaseActivity.this, R.string.done, Toast.LENGTH_LONG).show());
                        }
                    };
                });
                break;
            case R.id.reboot:
                areYouSure(R.string.reboot, (dialog, which) -> reboot(null));
                break;
            case R.id.soft_reboot:
                areYouSure(R.string.soft_reboot, (dialog, which) -> softReboot());
                break;
            case R.id.reboot_recovery:
                reboot("recovery");
                break;
            case R.id.reboot_bootloader:
                reboot("bootloader");
                break;
            case R.id.reboot_download:
                reboot("download");
                break;
            case R.id.reboot_edl:
                reboot("edl");
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
