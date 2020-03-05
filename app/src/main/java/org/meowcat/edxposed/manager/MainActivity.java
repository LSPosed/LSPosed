package org.meowcat.edxposed.manager;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;

import com.google.android.material.card.MaterialCardView;

import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.RepoLoader;

public class MainActivity extends BaseActivity implements RepoLoader.RepoListener, ModuleUtil.ModuleListener {

    private RepoLoader mRepoLoader;

    @SuppressLint("PrivateResource")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRepoLoader = RepoLoader.getInstance();
        ModuleUtil.getInstance().addListener(this);
        mRepoLoader.addListener(this, false);
        findViewById(R.id.activity_main_modules).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ModulesActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.activity_main_downloads).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), DownloadActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.activity_main_apps).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), BlackListActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.activity_main_status).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), EdDownloadActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.activity_main_settings).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.activity_main_logs).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), LogsActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.activity_main_about).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), AboutActivity.class);
            startActivity(intent);
        });
        ImageView menu = findViewById(R.id.menu_more);
        TooltipCompat.setTooltipText(menu, getString(androidx.appcompat.R.string.abc_action_menu_overflow_description));
        menu.setOnClickListener(v -> {
            PopupMenu appMenu = new PopupMenu(MainActivity.this, menu);
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
        MaterialCardView cardView = findViewById(R.id.activity_main_status);
        TextView title = findViewById(R.id.activity_main_status_title);
        ImageView icon = findViewById(R.id.activity_main_status_icon);
        TextView details = findViewById(R.id.activity_main_status_summary);
        if (installedXposedVersion != null) {
            int installedXposedVersionInt = extractIntPart(installedXposedVersion);
            if (installedXposedVersionInt == XposedApp.getXposedVersion()) {
                String installedXposedVersionStr = installedXposedVersionInt + ".0";
                title.setText(R.string.Activated);
                details.setText(installedXposedVersion.replace(installedXposedVersionStr + "-", ""));
                cardView.setCardBackgroundColor(getResources().getColor(R.color.download_status_update_available));
                icon.setImageDrawable(getDrawable(R.drawable.ic_check_circle));
            } else {
                title.setText(R.string.Inactivate);
                details.setText(R.string.installed_lollipop_inactive);
                cardView.setCardBackgroundColor(getResources().getColor(R.color.amber_500));
                icon.setImageDrawable(getDrawable(R.drawable.ic_warning));
            }
        } else {
            title.setText(R.string.Install);
            details.setText(R.string.InstallDetail);
            cardView.setCardBackgroundColor(getResources().getColor(R.color.colorPrimary));
            icon.setImageDrawable(getDrawable(R.drawable.ic_error));
        }
        notifyDataSetChanged();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    private void notifyDataSetChanged() {
        runOnUiThread(() -> {
            String frameworkUpdateVersion = mRepoLoader.getFrameworkUpdateVersion();
            boolean moduleUpdateAvailable = mRepoLoader.hasModuleUpdates();
            ModuleUtil.getInstance().getEnabledModules().size();
            TextView description = findViewById(R.id.activity_main_modules_summary);
            description.setText(String.format(getString(R.string.ModulesDetail), ModuleUtil.getInstance().getEnabledModules().size()));
            if (frameworkUpdateVersion != null) {
                description = findViewById(R.id.activity_main_status_summary);
                description.setText(String.format(getString(R.string.welcome_framework_update_available), frameworkUpdateVersion));
            }
            description = findViewById(R.id.activity_main_download_summary);
            if (moduleUpdateAvailable) {
                description.setText(R.string.modules_updates_available);
            } else {
                description.setText(R.string.ModuleUptodate);
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
        mRepoLoader.removeListener(this);
    }

}
