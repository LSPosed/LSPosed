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

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.App;
import org.lsposed.manager.R;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class BaseFragment extends Fragment {
    public void navigateUp() {
        getNavController().navigateUp();
    }

    public NavController getNavController() {
        return NavHostFragment.findNavController(this);
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
            toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
            onPrepareOptionsMenu(toolbar.getMenu());
        }
    }

    public void runAsync(Runnable runnable) {
        App.getExecutorService().submit(runnable);
    }

    public <T> Future<T> runAsync(Callable<T> callable) {
        return App.getExecutorService().submit(callable);
    }

    public void runOnUiThread(Runnable runnable) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(runnable);
        }
    }

    public <T> Future<T> runOnUiThread(Callable<T> callable) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            var task = new FutureTask<>(callable);
            activity.runOnUiThread(task);
            return task;
        } else {
            return new FutureTask<>(() -> null);
        }
    }

    public void showHint(@StringRes int res, boolean lengthShort, @StringRes int actionRes, View.OnClickListener action) {
        showHint(getString(res), lengthShort, getString(actionRes), action);
    }

    public void showHint(@StringRes int res, boolean lengthShort) {
        showHint(getString(res), lengthShort, null, null);
    }

    public void showHint(CharSequence str, boolean lengthShort) {
        showHint(str, lengthShort, null, null);
    }

    public void showHint(CharSequence str, boolean lengthShort, CharSequence actionStr, View.OnClickListener action) {
        if (isResumed()) {
            var container = requireActivity().findViewById(R.id.container);
            if (container != null) {
                var snackbar = Snackbar.make(container, str, lengthShort ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG);
                if (actionStr != null && action != null) snackbar.setAction(actionStr, action);
                snackbar.show();
                return;
            }
        }
        Toast.makeText(requireContext(), str, lengthShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
    }

}
