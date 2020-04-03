package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;

import org.meowcat.edxposed.manager.adapters.AppHelper;
import org.meowcat.edxposed.manager.adapters.BlackListAdapter;
import org.meowcat.edxposed.manager.databinding.ActivityMainBinding;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;

public class MainActivity extends BaseActivity implements RepoLoader.RepoListener, ModuleUtil.ModuleListener {
    ActivityMainBinding binding;
    private RepoLoader repoLoader;

    @SuppressLint("PrivateResource")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupWindowInsets(binding.snackbar, binding.nestedScrollView);
        repoLoader = RepoLoader.getInstance();
        ModuleUtil.getInstance().addListener(this);
        repoLoader.addListener(this, false);
        binding.modules.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ModulesActivity.class);
            startActivity(intent);
        });
        binding.downloads.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), DownloadActivity.class);
            startActivity(intent);
        });
        binding.apps.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), BlackListActivity.class);
            startActivity(intent);
        });
        binding.status.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), EdDownloadActivity.class);
            startActivity(intent);
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
        binding.menuMore.setOnClickListener(v -> {
            PopupMenu appMenu = new PopupMenu(MainActivity.this, binding.menuMore);
            appMenu.inflate(R.menu.menu_installer);
            appMenu.setOnMenuItemClickListener(this::onOptionsItemSelected);
            appMenu.show();
        });
        String installedXposedVersion;
        try {
            installedXposedVersion = XposedApp.getXposedProp().getVersion();
        } catch (NullPointerException e) {
            installedXposedVersion = null;
        }
        if (installedXposedVersion != null) {
            int installedXposedVersionInt = extractIntPart(installedXposedVersion);
            if (installedXposedVersionInt == XposedApp.getXposedVersion()) {
                String installedXposedVersionStr = installedXposedVersionInt + ".0";
                binding.statusTitle.setText(R.string.Activated);
                binding.statusSummary.setText(installedXposedVersion.replace(installedXposedVersionStr + "-", ""));
                binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.download_status_update_available));
                binding.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_check_circle));
            } else {
                binding.statusTitle.setText(R.string.Inactivate);
                binding.statusSummary.setText(R.string.installed_lollipop_inactive);
                binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.amber_500));
                binding.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_warning));
            }
        } else {
            binding.statusTitle.setText(R.string.Install);
            binding.statusSummary.setText(R.string.InstallDetail);
            binding.status.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
            binding.statusIcon.setImageDrawable(getDrawable(R.drawable.ic_error));
        }
        notifyDataSetChanged();
        new Thread(() -> new BlackListAdapter(getApplicationContext(), AppHelper.isWhiteListMode(), null).generateCheckedList());
    }

    private int extractIntPart(String str) {
        int result = 0, length = str.length();
        for (int offset = 0; offset < length; offset++) {
            char c = str.charAt(offset);
            if ('0' <= c && c <= '9')
                result = result * 10 + (c - '0');
            else
                break;
        }
        return result;
    }

    private void notifyDataSetChanged() {
        runOnUiThread(() -> {
            String frameworkUpdateVersion = repoLoader.getFrameworkUpdateVersion();
            boolean moduleUpdateAvailable = repoLoader.hasModuleUpdates();
            ModuleUtil.getInstance().getEnabledModules().size();
            binding.modulesSummary.setText(String.format(getString(R.string.ModulesDetail), ModuleUtil.getInstance().getEnabledModules().size()));
            if (frameworkUpdateVersion != null) {
                binding.statusSummary.setText(String.format(getString(R.string.welcome_framework_update_available), frameworkUpdateVersion));
            }
            if (moduleUpdateAvailable) {
                binding.downloadSummary.setText(R.string.modules_updates_available);
            } else {
                binding.downloadSummary.setText(R.string.ModuleUptodate);
            }
        });
    }


    @Override
    public void onInstalledModulesReloaded(ModuleUtil moduleUtil) {
        notifyDataSetChanged();
    }

    @Override
    public void onSingleInstalledModuleReloaded(ModuleUtil moduleUtil, String packageName, ModuleUtil.InstalledModule module) {
        notifyDataSetChanged();
    }

    @Override
    public void onRepoReloaded(RepoLoader loader) {
        notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ModuleUtil.getInstance().removeListener(this);
        repoLoader.removeListener(this);
    }

}
