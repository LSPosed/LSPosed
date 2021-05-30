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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.lsposed.manager.App;
import org.lsposed.manager.BuildConfig;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.DialogInstallWarningBinding;
import org.lsposed.manager.databinding.FragmentLogsBinding;
import org.lsposed.manager.databinding.ItemLogBinding;
import org.lsposed.manager.util.LinearLayoutManagerFix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import rikka.core.os.FileUtils;
import rikka.core.res.ResourcesKt;
import rikka.insets.WindowInsetsHelperKt;
import rikka.recyclerview.RecyclerViewKt;

@SuppressLint("NotifyDataSetChanged")
public class LogsFragment extends BaseFragment {
    private boolean verbose = false;
    private LogsAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FragmentLogsBinding binding;
    private LinearLayoutManagerFix layoutManager;
    ActivityResultLauncher<String> saveLogsLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        // grantUriPermission might throw RemoteException on MIUI
                        requireContext().grantUriPermission(BuildConfig.APPLICATION_ID, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
                        try (var os = requireContext().getContentResolver().openOutputStream(uri)) {
                            ParcelFileDescriptor parcelFileDescriptor = ConfigManager.getLogs(verbose);
                            if (parcelFileDescriptor == null) {
                                return;
                            }
                            try (var is = new FileInputStream(parcelFileDescriptor.getFileDescriptor())) {
                                FileUtils.copy(is, os);
                            }
                        } catch (Exception e) {
                            Snackbar.make(binding.snackbar, getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogsBinding.inflate(inflater, container, false);
        binding.getRoot().bringChildToFront(binding.appBar);
        setupToolbar(binding.toolbar, R.string.Logs, R.menu.menu_logs);
        binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));


        if (!ConfigManager.isVerboseLogEnabled()) {
            WindowInsetsHelperKt.setInitialPadding(binding.recyclerView, 0, ResourcesKt.resolveDimensionPixelOffset(requireActivity().getTheme(), R.attr.actionBarSize, 0), 0, 0);
            binding.slidingTabs.setVisibility(View.GONE);
        } else {
            binding.slidingTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    verbose = tab.getPosition() == 1;
                    reloadErrorLog();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        }

        adapter = new LogsAdapter();
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        binding.recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManagerFix(requireActivity());
        binding.recyclerView.setLayoutManager(layoutManager);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!App.getPreferences().getBoolean("hide_logcat_warning", false)) {
            DialogInstallWarningBinding binding = DialogInstallWarningBinding.inflate(getLayoutInflater());
            binding.getRoot().setOnClickListener(v -> binding.checkbox.toggle());
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.not_logcat_2)
                    .setView(binding.getRoot())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (binding.checkbox.isChecked()) {
                            App.getPreferences().edit().putBoolean("hide_logcat_warning", true).apply();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        reloadErrorLog();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_scroll_top) {
            if (layoutManager.findFirstVisibleItemPosition() > 1000) {
                binding.recyclerView.scrollToPosition(0);
            } else {
                binding.recyclerView.smoothScrollToPosition(0);
            }
            binding.recyclerView.smoothScrollToPosition(0);
        } else if (itemId == R.id.menu_scroll_down) {
            if (adapter.getItemCount() - layoutManager.findLastVisibleItemPosition() > 1000) {
                binding.recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            } else {
                binding.recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        } else if (itemId == R.id.menu_refresh) {
            reloadErrorLog();
            return true;
        } else if (itemId == R.id.menu_send) {
            try {
                send();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (itemId == R.id.menu_save) {
            save();
            return true;
        } else if (itemId == R.id.menu_clear) {
            clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadErrorLog() {
        ParcelFileDescriptor parcelFileDescriptor = ConfigManager.getLogs(verbose);
        if (parcelFileDescriptor != null) {
            new LogsReader().execute(parcelFileDescriptor.getFileDescriptor());
        } else {
            binding.slidingTabs.selectTab(binding.slidingTabs.getTabAt(0));
            new AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.verbose_log_not_avaliable)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void clear() {
        if (ConfigManager.clearLogs(verbose)) {
            Snackbar.make(binding.snackbar, R.string.logs_cleared, Snackbar.LENGTH_SHORT).show();
            reloadErrorLog();
        } else {
            Snackbar.make(binding.snackbar, R.string.logs_clear_failed_2, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void send() {
        ParcelFileDescriptor parcelFileDescriptor = ConfigManager.getLogs(verbose);
        if (parcelFileDescriptor == null) {
            return;
        }
        Calendar now = Calendar.getInstance();
        String filename = String.format(Locale.US,
                "LSPosed_%s_%04d%02d%02d_%02d%02d%02d.log",
                verbose ? "Verbose" : "Modules",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        File cacheFile = new File(requireActivity().getCacheDir(), filename);
        try (var os = new FileOutputStream(cacheFile); var is = new FileInputStream(parcelFileDescriptor.getFileDescriptor())) {
            FileUtils.copy(is, os);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Uri uri = FileProvider.getUriForFile(requireActivity(), BuildConfig.APPLICATION_ID + ".fileprovider", cacheFile);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setDataAndType(uri, "text/plain");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.menuSend)));
    }

    private void save() {
        Calendar now = Calendar.getInstance();
        String filename = String.format(Locale.US,
                "LSPosed_%s_%04d%02d%02d_%02d%02d%02d.log",
                verbose ? "Verbose" : "Modules",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        saveLogsLauncher.launch(filename);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class LogsReader extends AsyncTask<FileDescriptor, Integer, List<String>> {
        private AlertDialog mProgressDialog;
        private final Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                if (!requireActivity().isFinishing()) {
                    mProgressDialog.show();
                }
            }
        };

        @Override
        protected void onPreExecute() {
            mProgressDialog = new AlertDialog.Builder(requireActivity()).create();
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setCancelable(false);
            handler.postDelayed(mRunnable, 300);
        }

        @Override
        protected List<String> doInBackground(FileDescriptor... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            List<String> logs = new ArrayList<>();

            try (InputStream inputStream = new FileInputStream(log[0]); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                }
            } catch (IOException e) {
                logs.add(requireActivity().getResources().getString(R.string.logs_cannot_read));
                if (e.getMessage() != null) {
                    logs.addAll(Arrays.asList(e.getMessage().split("\n")));
                }
            }

            return logs;
        }

        @Override
        protected void onPostExecute(List<String> logs) {
            adapter.setLogs(logs);

            handler.removeCallbacks(mRunnable);//It loaded so fast that no need to show progress
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    private class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.ViewHolder> {
        ArrayList<String> logs = new ArrayList<>();

        @NonNull
        @Override
        public LogsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemLogBinding binding = ItemLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new LogsAdapter.ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull LogsAdapter.ViewHolder holder, int position) {
            TextView view = holder.textView;
            view.setText(logs.get(position));
            view.measure(0, 0);
            int desiredWidth = view.getMeasuredWidth();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = desiredWidth;
            if (binding.recyclerView.getWidth() < desiredWidth) {
                binding.recyclerView.requestLayout();
            }
        }

        void setLogs(List<String> logs) {
            this.logs.clear();
            this.logs.addAll(logs);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(ItemLogBinding binding) {
                super(binding.getRoot());
                textView = binding.log;
            }
        }
    }
}
