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

package org.lsposed.manager.ui.fragment;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.App;
import org.lsposed.manager.R;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public abstract class BaseFragment extends Fragment {

    public void navigateUp() {
        getNavController().navigateUp();
    }

    public NavController getNavController() {
        return NavHostFragment.findNavController(this);
    }

    public boolean safeNavigate(@IdRes int resId) {
        try {
            getNavController().navigate(resId);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public boolean safeNavigate(NavDirections direction) {
        try {
            getNavController().navigate(direction);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, int title) {
        setupToolbar(toolbar, tipsView, getString(title), -1);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, int title, int menu) {
        setupToolbar(toolbar, tipsView, getString(title), menu, null);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, String title, int menu) {
        setupToolbar(toolbar, tipsView, title, menu, null);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, String title, int menu, View.OnClickListener navigationOnClickListener) {
        toolbar.setNavigationOnClickListener(navigationOnClickListener == null ? (v -> navigateUp()) : navigationOnClickListener);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        toolbar.setTitle(title);
        toolbar.setTooltipText(title);
        if (tipsView != null) tipsView.setTooltipText(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            if (this instanceof MenuProvider self) {
                toolbar.setOnMenuItemClickListener(self::onMenuItemSelected);
                self.onPrepareMenu(toolbar.getMenu());
            }
        }
    }

    public void runAsync(Runnable runnable) {
        App.getExecutorService().submit(runnable);
    }

    public <T> Future<T> runAsync(Callable<T> callable) {
        return App.getExecutorService().submit(callable);
    }

    public void runOnUiThread(Runnable runnable) {
        App.getMainHandler().post(runnable);
    }

    public <T> Future<T> runOnUiThread(Callable<T> callable) {
        var task = new FutureTask<>(callable);
        runOnUiThread(task);
        return task;
    }

    public void showHint(@StringRes int res, boolean lengthShort, @StringRes int actionRes, View.OnClickListener action) {
        showHint(App.getInstance().getString(res), lengthShort, App.getInstance().getString(actionRes), action);
    }

    public void showHint(@StringRes int res, boolean lengthShort) {
        showHint(App.getInstance().getString(res), lengthShort, null, null);
    }

    public void showHint(CharSequence str, boolean lengthShort) {
        showHint(str, lengthShort, null, null);
    }

    public void showHint(CharSequence str, boolean lengthShort, CharSequence actionStr, View.OnClickListener action) {
        var container = getView();
        if (isResumed() && container != null) {
            var snackbar = Snackbar.make(container, str, lengthShort ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG);
            var fab = container.findViewById(R.id.fab);
            if (fab instanceof FloatingActionButton && ((FloatingActionButton) fab).isOrWillBeShown())
                snackbar.setAnchorView(fab);
            if (actionStr != null && action != null) snackbar.setAction(actionStr, action);
            snackbar.show();
            return;
        }
        runOnUiThread(() -> {
            try {
                Toast.makeText(App.getInstance(), str, lengthShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {
            }
        });
    }
}
