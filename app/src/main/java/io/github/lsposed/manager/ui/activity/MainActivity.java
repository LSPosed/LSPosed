package io.github.lsposed.manager.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.Locale;

import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.adapters.AppHelper;
import io.github.lsposed.manager.databinding.ActivityMainBinding;
import io.github.lsposed.manager.ui.fragment.StatusDialogBuilder;
import io.github.lsposed.manager.util.GlideHelper;
import io.github.lsposed.manager.util.ModuleUtil;
import io.github.lsposed.manager.util.NavUtil;
import io.github.lsposed.manager.util.light.Light;

public class MainActivity extends BaseActivity {
    ActivityMainBinding binding;

    @SuppressLint({"PrivateResource", "ClickableViewAccessibility"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().getDecorView().post(() -> {
            if (Light.setLightSourceAlpha(getWindow().getDecorView(), 0.01f, 0.029f)) {
                binding.status.setElevation(24);
                binding.modules.setElevation(12);
                binding.apps.setElevation(12);
            }
        });
        binding.modules.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ModulesActivity.class);
            startActivity(intent);
        });
        binding.apps.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), AppListActivity.class);
            startActivity(intent);
        });
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
        binding.settings.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        });
        binding.logs.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), LogsActivity.class);
            startActivity(intent);
        });
        binding.about.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), AboutActivity.class);
            startActivity(intent);
        });
        TooltipCompat.setTooltipText(binding.menuMore, getString(androidx.appcompat.R.string.abc_action_menu_overflow_description));
        PopupMenu appMenu = new PopupMenu(MainActivity.this, binding.menuMore);
        appMenu.inflate(R.menu.menu_installer);
        appMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
        binding.menuMore.setOnTouchListener(appMenu.getDragToOpenListener());
        binding.menuMore.setOnClickListener(v -> appMenu.show());
        Glide.with(binding.appIcon)
                .load(GlideHelper.wrapApplicationInfoForIconLoader(getApplicationInfo()))
                .into(binding.appIcon);
        String installedXposedVersion = Constants.getXposedVersion();
        if (installedXposedVersion != null) {
            binding.statusTitle.setText(String.format(Locale.US, "%s %s", getString(R.string.Activated), Constants.getXposedVariant()));
            binding.statusSummary.setText(String.format(Locale.US, "%s (%d)", installedXposedVersion, Constants.getXposedVersionCode()));
            binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.download_status_update_available));
            binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
        } else {
            binding.statusTitle.setText(R.string.Install);
            binding.statusSummary.setText(R.string.InstallDetail);
            binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            binding.statusIcon.setImageResource(R.drawable.ic_error);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.modulesSummary.setText(String.format(getString(R.string.ModulesDetail), ModuleUtil.getInstance().getEnabledModules().size()));
        binding.appsTitle.setText(R.string.Apps);
        int count = AppHelper.getAppList().size();
        binding.appsSummary.setText(getString(R.string.whitelist_summary, count));
    }
}
