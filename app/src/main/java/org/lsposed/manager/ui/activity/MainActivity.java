/*
 * <!--This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2021 LSPosed Contributors-->
 */

package org.lsposed.manager.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationBarView;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.ActivityMainBinding;
import org.lsposed.manager.repo.RepoLoader;
import org.lsposed.manager.ui.activity.base.BaseActivity;
import org.lsposed.manager.util.ModuleUtil;
import org.lsposed.manager.util.UpdateUtil;

import java.util.HashSet;

import rikka.core.util.ResourceUtils;

public class MainActivity extends BaseActivity implements RepoLoader.RepoListener, ModuleUtil.ModuleListener {
    private static final String KEY_PREFIX = MainActivity.class.getName() + '.';
    private static final String EXTRA_SAVED_INSTANCE_STATE = KEY_PREFIX + "SAVED_INSTANCE_STATE";

    private static final RepoLoader repoLoader = RepoLoader.getInstance();
    private static final ModuleUtil moduleUtil = ModuleUtil.getInstance();

    private boolean restarting;
    private ActivityMainBinding binding;

    public ActivityMainBinding getActivityBinding() {
        return binding;
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @NonNull
    private static Intent newIntent(@NonNull Bundle savedInstanceState, @NonNull Context context) {
        return newIntent(context)
                .putExtra(EXTRA_SAVED_INSTANCE_STATE, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            savedInstanceState = getIntent().getBundleExtra(EXTRA_SAVED_INSTANCE_STATE);
        }
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repoLoader.addListener(this);
        moduleUtil.addListener(this);

        onModulesReloaded();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        var nav = (NavigationBarView) binding.nav;
        NavigationUI.setupWithNavController(nav, navController);

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }
        NavController navController = navHostFragment.getNavController();
        var nav = (NavigationBarView) binding.nav;
        if (intent.getAction() != null && intent.getAction().equals("android.intent.action.APPLICATION_PREFERENCES")) {
            nav.setSelectedItemId(R.id.settings_fragment);
        } else if (ConfigManager.isBinderAlive()) {
            if (!TextUtils.isEmpty(intent.getDataString())) {
                switch (intent.getDataString()) {
                    case "modules":
                        nav.setSelectedItemId(R.id.modules_nav);
                        break;
                    case "logs":
                        nav.setSelectedItemId(R.id.logs_fragment);
                        break;
                    case "repo":
                        if (ConfigManager.isMagiskInstalled()) {
                            nav.setSelectedItemId(R.id.repo_nav);
                        }
                        break;
                    case "settings":
                        nav.setSelectedItemId(R.id.settings_fragment);
                        break;
                    default:
                        var data = intent.getData();
                        if (data != null && data.getScheme().equals("module")) {
                            navController.navigate(
                                    new Uri.Builder().scheme("lsposed").authority("module").appendQueryParameter("modulePackageName", data.getHost()).appendQueryParameter("moduleUserId", String.valueOf(data.getPort())).build(),
                                    new NavOptions.Builder().setEnterAnim(R.anim.fragment_enter).setExitAnim(R.anim.fragment_exit).setPopEnterAnim(R.anim.fragment_enter_pop).setPopExitAnim(R.anim.fragment_exit_pop).setLaunchSingleTop(true).setPopUpTo(navController.getGraph().getStartDestinationId(), false, true).build());
                        }
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    public void restart() {
        if (BuildCompat.isAtLeastS() || App.isParasitic()) {
            recreate();
        } else {
            try {
                Bundle savedInstanceState = new Bundle();
                onSaveInstanceState(savedInstanceState);
                finish();
                startActivity(newIntent(savedInstanceState, this));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                restarting = true;
            } catch (Throwable e) {
                recreate();
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        return restarting || super.dispatchKeyEvent(event);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyShortcutEvent(@NonNull KeyEvent event) {
        return restarting || super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        return restarting || super.dispatchTouchEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(@NonNull MotionEvent event) {
        return restarting || super.dispatchTrackballEvent(event);
    }

    @Override
    public boolean dispatchGenericMotionEvent(@NonNull MotionEvent event) {
        return restarting || super.dispatchGenericMotionEvent(event);
    }


    @Override
    public void onRepoLoaded() {
        final int[] count = new int[]{0};
        HashSet<String> processedModules = new HashSet<>();
        var modules = moduleUtil.getModules();
        if (modules == null) return;
        modules.forEach((k, v) -> {
                    if (!processedModules.contains(k.first)) {
                        var ver = repoLoader.getModuleLatestVersion(k.first);
                        if (ver != null && ver.upgradable(v.versionCode, v.versionName)) {
                            ++count[0];
                        }
                        processedModules.add(k.first);
                    }
                }
        );
        runOnUiThread(() -> {
            if (count[0] > 0 && binding != null) {
                var nav = (NavigationBarView) binding.nav;
                var badge = nav.getOrCreateBadge(R.id.repo_nav);
                badge.setVisible(true);
                badge.setNumber(count[0]);
            } else {
                onThrowable(null);
            }
        });
    }

    @Override
    public void onThrowable(Throwable t) {
        runOnUiThread(() -> {
            if (binding != null) {
                var nav = (NavigationBarView) binding.nav;
                var badge = nav.getOrCreateBadge(R.id.repo_nav);
                badge.setVisible(false);
            }
        });
    }

    @Override
    public void onModulesReloaded() {
        onRepoLoaded();
        setModulesSummary(moduleUtil.getEnabledModulesCount());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ConfigManager.isBinderAlive()) {
            setModulesSummary(moduleUtil.getEnabledModulesCount());
        } else setModulesSummary(0);
        if (binding != null) {
            var nav = (NavigationBarView) binding.nav;
            if (UpdateUtil.needUpdate()) {
                var badge = nav.getOrCreateBadge(R.id.main_fragment);
                badge.setVisible(true);
            }

            if (!ConfigManager.isBinderAlive()) {
                nav.getMenu().removeItem(R.id.logs_fragment);
                nav.getMenu().removeItem(R.id.modules_nav);
                if (!ConfigManager.isMagiskInstalled()) {
                    nav.getMenu().removeItem(R.id.repo_nav);
                }
            }
        }
    }

    private void setModulesSummary(int moduleCount) {
        runOnUiThread(() -> {
            if (binding != null) {
                var nav = (NavigationBarView) binding.nav;
                var badge = nav.getOrCreateBadge(R.id.modules_nav);
                badge.setBackgroundColor(ResourceUtils.resolveColor(getTheme(), com.google.android.material.R.attr.colorPrimary));
                badge.setBadgeTextColor(ResourceUtils.resolveColor(getTheme(), com.google.android.material.R.attr.colorOnPrimary));
                if (moduleCount > 0) {
                    badge.setVisible(true);
                    badge.setNumber(moduleCount);
                } else {
                    badge.setVisible(false);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        repoLoader.removeListener(this);
        moduleUtil.removeListener(this);
    }
}
