package io.github.lsposed.manager.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;

import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityMainBinding;
import io.github.lsposed.manager.ui.fragment.StatusDialogBuilder;
import io.github.lsposed.manager.util.GlideHelper;
import io.github.lsposed.manager.util.ModuleUtil;
import io.github.lsposed.manager.util.NavUtil;
import io.github.lsposed.manager.util.light.Light;
import name.mikanoshi.customiuizer.holidays.HolidayHelper;
import name.mikanoshi.customiuizer.utils.Helpers;

public class MainActivity extends BaseActivity {
    ActivityMainBinding binding;

    @SuppressLint({"PrivateResource", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedScrollView, (v, insets) -> {
            Insets insets1 = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.setPadding(insets1.left, insets1.top, insets1.right, insets1.bottom);
            } else {
                v.setPadding(0, insets1.top, 0, 0);
                binding.getRoot().setPadding(insets1.left, 0, insets1.right, insets1.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
        HolidayHelper.setup(this);
        binding.status.setOnClickListener(v -> {
            if (Constants.getXposedVersionCode() != -1) {
                new StatusDialogBuilder(this)
                        .setTitle(R.string.info)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            } else {
                NavUtil.startURL(this, getString(R.string.about_source));
            }
        });
        binding.modules.setOnClickListener(new StartActivityListener(ModulesActivity.class, true));
        binding.logs.setOnClickListener(new StartActivityListener(LogsActivity.class, true));
        binding.settings.setOnClickListener(new StartActivityListener(SettingsActivity.class, false));
        binding.about.setOnClickListener(new StartActivityListener(AboutActivity.class, false));
        Glide.with(binding.appIcon)
                .load(GlideHelper.wrapApplicationInfoForIconLoader(getApplicationInfo()))
                .into(binding.appIcon);
        String installedXposedVersion = Constants.getXposedVersion();
        if (installedXposedVersion != null) {
            binding.statusTitle.setText(String.format(Locale.US, "%s %s", getString(R.string.Activated), Constants.getXposedVariant()));
            if (!Constants.isPermissive()) {
                if (Helpers.currentHoliday == Helpers.Holidays.LUNARNEWYEAR) {
                    binding.status.setCardBackgroundColor(0xfff05654);
                } else {
                    binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorNormal));
                }
                binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
                binding.statusSummary.setText(String.format(Locale.US, "%s (%d)", installedXposedVersion, Constants.getXposedVersionCode()));
            } else {
                binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorError));
                binding.statusIcon.setImageResource(R.drawable.ic_warning);
                binding.statusSummary.setText(R.string.selinux_permissive_summary);
            }
        } else {
            binding.statusTitle.setText(R.string.Install);
            binding.statusSummary.setText(R.string.InstallDetail);
            binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorInstall));
            binding.statusIcon.setImageResource(R.drawable.ic_error);
            Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
        }
    }

    private class StartActivityListener implements View.OnClickListener {
        boolean requireInstalled;
        Class<?> clazz;

        StartActivityListener(Class<?> clazz, boolean requireInstalled) {
            this.clazz = clazz;
            this.requireInstalled = requireInstalled;
        }

        @Override
        public void onClick(View v) {
            if (requireInstalled && Constants.getXposedVersion() == null) {
                Snackbar.make(binding.snackbar, R.string.lsposed_not_active, Snackbar.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, clazz);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        getWindow().getDecorView().post(() -> {
            if (Light.setLightSourceAlpha(getWindow().getDecorView(), 0.01f, 0.029f)) {
                binding.status.setElevation(24);
                binding.modules.setElevation(12);
            }
        });
        super.onResume();
        binding.modulesSummary.setText(String.format(getString(R.string.ModulesDetail), ModuleUtil.getInstance().getEnabledModules().size()));
        HolidayHelper.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HolidayHelper.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        HolidayHelper.onPause();
    }
}
