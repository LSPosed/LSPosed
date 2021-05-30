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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;

import com.google.android.material.snackbar.Snackbar;

import org.lsposed.manager.App;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentCompileDialogBinding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;

@SuppressWarnings("deprecation")
public class CompileDialogFragment extends AppCompatDialogFragment {

    private static final String[] COMPILE_RESET_COMMAND = new String[]{"cmd", "package", "compile", "-f", "-m", "speed", ""};

    private static final String KEY_APP_INFO = "app_info";
    private ApplicationInfo appInfo;

    public static void speed(FragmentManager fragmentManager, ApplicationInfo info) {
        Bundle arguments = new Bundle();
        arguments.putParcelable(KEY_APP_INFO, info);
        CompileDialogFragment fragment = new CompileDialogFragment();
        fragment.setArguments(arguments);
        fragment.setCancelable(false);
        fragment.show(fragmentManager, "compile_dialog");
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments == null) {
            throw new IllegalStateException("arguments should not be null.");
        }
        appInfo = arguments.getParcelable(KEY_APP_INFO);
        if (appInfo == null) {
            throw new IllegalStateException("appInfo should not be null.");
        }

        FragmentCompileDialogBinding binding = FragmentCompileDialogBinding.inflate(LayoutInflater.from(requireActivity()), null, false);
        final PackageManager pm = requireContext().getPackageManager();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity())
                .setIcon(appInfo.loadIcon(pm))
                .setTitle(appInfo.loadLabel(pm))
                .setView(binding.getRoot());

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Bundle arguments = getArguments();
        if (arguments != null) {
            appInfo = arguments.getParcelable(KEY_APP_INFO);
            String[] command = COMPILE_RESET_COMMAND;
            command[6] = appInfo.packageName;
            new CompileTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, command);
        } else {
            dismissAllowingStateLoss();
        }
    }

    private static class CompileTask extends AsyncTask<String, Void, String> {

        WeakReference<CompileDialogFragment> outerRef;

        CompileTask(CompileDialogFragment fragment) {
            outerRef = new WeakReference<>(fragment);
        }

        @Override
        protected String doInBackground(String... commands) {
            try {
                Process process = Runtime.getRuntime().exec(commands);
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                int read;
                char[] buffer = new char[4096];
                StringBuilder err = new StringBuilder();
                while ((read = errorReader.read(buffer)) > 0) {
                    err.append(buffer, 0, read);
                }
                StringBuilder input = new StringBuilder();
                while ((read = inputReader.read(buffer)) > 0) {
                    input.append(buffer, 0, read);
                }
                errorReader.close();
                inputReader.close();
                process.waitFor();
                String result = "";
                if (process.exitValue() != 0) {
                    result = "Error ";
                }
                if (TextUtils.isEmpty(err)) {
                    return result + input.toString();
                } else {
                    return result + err.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error " + e.getCause();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Context context = App.getInstance();
            String text;
            if (result.length() == 0) {
                text = context.getString(R.string.compile_failed);
            } else if (result.length() >= 5 && "Error".equals(result.substring(0, 5))) {
                text = context.getString(R.string.compile_failed_with_info) + " " + result.substring(6);
            } else {
                text = context.getString(R.string.compile_done);
            }
            CompileDialogFragment fragment = outerRef.get();
            if (fragment != null) {
                fragment.dismissAllowingStateLoss();
                AppListFragment appListFragment = (AppListFragment) fragment.getParentFragment();
                if (appListFragment != null && appListFragment.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    appListFragment.makeSnackBar(text, Snackbar.LENGTH_LONG);
                    return;
                }
            }
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        }
    }
}
