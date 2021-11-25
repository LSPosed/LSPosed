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

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import org.lsposed.manager.App;
import org.lsposed.manager.R;

import java.util.concurrent.Future;

public class BaseFragment extends Fragment {
    public void navigateUp() {
        getNavController().navigateUp();
    }

    public NavController getNavController() {
        return NavHostFragment.findNavController(this);
    }

    public void setupToolbar(Toolbar toolbar, int title) {
        setupToolbar(toolbar, getString(title), -1);
    }

    public void setupToolbar(Toolbar toolbar, int title, int menu) {
        setupToolbar(toolbar, getString(title), menu, null);
    }

    public void setupToolbar(Toolbar toolbar, String title, int menu) {
        setupToolbar(toolbar, title, menu, null);
    }

    public void setupToolbar(Toolbar toolbar, String title, int menu, View.OnClickListener navigationOnClickListener) {
        toolbar.setNavigationOnClickListener(navigationOnClickListener == null ? (v -> navigateUp()) : navigationOnClickListener);
        toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24);
        toolbar.setTitle(title);
        toolbar.setTooltipText(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            toolbar.setOnMenuItemClickListener(this::onOptionsItemSelected);
            onPrepareOptionsMenu(toolbar.getMenu());
        }
    }

    public Future<?> runAsync(Runnable runnable) {
        return App.getExecutorService().submit(runnable);
    }

    public void runOnUiThread(Runnable runnable) {
        Activity activity = getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(runnable);
        }
    }
}
