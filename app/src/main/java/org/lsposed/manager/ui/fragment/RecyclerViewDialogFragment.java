/*
 * This file is part of LSPosed.
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
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.ui.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.lsposed.lspd.models.UserInfo;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogTitleBinding;
import org.lsposed.manager.databinding.SwiperefreshRecyclerviewBinding;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;
import org.lsposed.manager.util.ModuleUtil;

public class RecyclerViewDialogFragment extends AppCompatDialogFragment {
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        var parent = getParentFragment();
        var arguments = getArguments();
        if (!(parent instanceof ModulesFragment) || arguments == null) {
            throw new IllegalStateException();
        }
        var modulesFragment = (ModulesFragment) parent;
        var user = (UserInfo) arguments.getParcelable("userInfo");

        var pickAdaptor = modulesFragment.createPickModuleAdapter(user);
        var binding = SwiperefreshRecyclerviewBinding.inflate(LayoutInflater.from(requireActivity()), null, false);

        binding.recyclerView.setAdapter(pickAdaptor);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        pickAdaptor.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                binding.swipeRefreshLayout.setRefreshing(!pickAdaptor.isLoaded());
            }
        });
        binding.swipeRefreshLayout.setProgressViewEndTarget(true, binding.swipeRefreshLayout.getProgressViewEndOffset());
        binding.swipeRefreshLayout.setOnRefreshListener(pickAdaptor::fullRefresh);
        pickAdaptor.refresh();
        var title = DialogTitleBinding.inflate(getLayoutInflater()).getRoot();
        title.setText(getString(R.string.install_to_user, user.name));
        var dialog = new BlurBehindDialogBuilder(requireActivity(), R.style.ThemeOverlay_MaterialAlertDialog_FullWidthButtons)
                .setCustomTitle(title)
                .setView(binding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        title.setOnClickListener(s -> binding.recyclerView.smoothScrollToPosition(0));
        pickAdaptor.setOnPickListener(picked -> {
            var module = (ModuleUtil.InstalledModule) picked.getTag();
            modulesFragment.installModuleToUser(module, user);
            dialog.dismiss();
        });
        onViewCreated(binding.getRoot(), savedInstanceState);
        return dialog;
    }

    // prevent from overriding
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
