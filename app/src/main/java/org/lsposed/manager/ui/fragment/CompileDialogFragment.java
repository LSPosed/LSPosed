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
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package org.lsposed.manager.ui.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;

import org.lsposed.manager.App;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentCompileDialogBinding;
import org.lsposed.manager.receivers.LSPManagerServiceHolder;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;

import java.lang.ref.WeakReference;

@SuppressWarnings("deprecation")
public class CompileDialogFragment extends AppCompatDialogFragment {
    private ApplicationInfo appInfo;

    public static void speed(FragmentManager fragmentManager, ApplicationInfo info) {
        CompileDialogFragment fragment = new CompileDialogFragment();
        fragment.setCancelable(false);
        var bundle = new Bundle();
        bundle.putParcelable("appInfo", info);
        fragment.setArguments(bundle);
        fragment.show(fragmentManager, "compile_dialog");
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        var arguments = getArguments();
        appInfo = arguments != null ? arguments.getParcelable("appInfo") : null;
        if (appInfo == null) {
            throw new IllegalStateException("appInfo should not be null.");
        }

        FragmentCompileDialogBinding binding = FragmentCompileDialogBinding.inflate(LayoutInflater.from(requireActivity()), null, false);
        final PackageManager pm = requireContext().getPackageManager();
        var builder = new BlurBehindDialogBuilder(requireActivity())
                .setTitle(appInfo.loadLabel(pm))
                .setView(binding.getRoot());

        return builder.create();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new CompileTask(this).executeOnExecutor(App.getExecutorService(), appInfo.packageName);
    }

    private static class CompileTask extends AsyncTask<String, Void, Throwable> {

        WeakReference<CompileDialogFragment> outerRef;

        CompileTask(CompileDialogFragment fragment) {
            outerRef = new WeakReference<>(fragment);
        }

        @Override
        protected Throwable doInBackground(String... commands) {
            try {
                if (LSPManagerServiceHolder.getService().performDexOptMode(commands[0])) {
                    return null;
                } else {
                    return new UnknownError();
                }
            } catch (Throwable e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Throwable result) {
            Context context = App.getInstance();
            String text;
            if (result != null) {
                if (result instanceof UnknownError) {
                    text = context.getString(R.string.compile_failed);
                } else {
                    text = context.getString(R.string.compile_failed_with_info) + result.toString();
                }
            } else {
                text = context.getString(R.string.compile_done);
            }
            CompileDialogFragment fragment = outerRef.get();
            if (fragment != null) {
                fragment.dismissAllowingStateLoss();
                var parent = fragment.getParentFragment();
                if (parent instanceof BaseFragment) {
                    ((BaseFragment) parent).showHint(text, true);
                }
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    }
}
