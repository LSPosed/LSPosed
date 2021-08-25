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

import static org.lsposed.manager.App.TAG;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentLogsBinding;
import org.lsposed.manager.databinding.ItemLogBinding;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import rikka.core.os.FileUtils;
import rikka.recyclerview.RecyclerViewKt;

@SuppressLint("NotifyDataSetChanged")
public class LogsFragment extends BaseFragment {
    private boolean verbose = false;
    private LogsAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FragmentLogsBinding binding;
    private LinearLayoutManager layoutManager;
    private final ActivityResultLauncher<String> saveLogsLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri == null) return;
                runAsync(() -> {
                    try (var os = new ZipOutputStream(requireContext().getContentResolver().openOutputStream(uri))) {
                        os.setLevel(Deflater.BEST_COMPRESSION);
                        zipLogs(os);
                        os.finish();
                    } catch (IOException e) {
                        var text = App.getInstance().getString(R.string.logs_save_failed2, e.getMessage());
                        if (binding != null && isResumed()) {
                            Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(App.getInstance(), text, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLogsBinding.inflate(inflater, container, false);
        binding.getRoot().bringChildToFront(binding.appBar);
        setupToolbar(binding.toolbar, R.string.Logs, R.menu.menu_logs);
        binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> binding.appBar.setRaised(!top));


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

        adapter = new LogsAdapter();
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        binding.recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(requireActivity());
        binding.recyclerView.setLayoutManager(layoutManager);
        return binding.getRoot();
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
        } else if (itemId == R.id.menu_save) {
            save();
            return true;
        } else if (itemId == R.id.menu_clear) {
            clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    private void reloadErrorLog() {
        ParcelFileDescriptor parcelFileDescriptor = ConfigManager.getLog(verbose);
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

    private void save() {
        LocalDateTime now = LocalDateTime.now();
        String filename = String.format(Locale.ROOT, "LSPosed_%s.zip", now.toString());
        saveLogsLauncher.launch(filename);
    }

    private static void zipLogs(ZipOutputStream os) {
        var logs = ConfigManager.getLogs();
        logs.forEach((name, fd) -> {
            try (var is = new FileInputStream(fd.getFileDescriptor())) {
                os.putNextEntry(new ZipEntry(name));
                FileUtils.copy(is, os);
                os.closeEntry();
            } catch (IOException e) {
                Log.w(TAG, name, e);
            }
        });

        try (var is = Runtime.getRuntime().exec("getprop").getInputStream()) {
            os.putNextEntry(new ZipEntry("system_props.txt"));
            FileUtils.copy(is, os);
            os.closeEntry();
        } catch (IOException e) {
            Log.w(TAG, "system_props.txt", e);
        }

        var now = LocalDateTime.now();
        var name = "app_" + now.toString() + ".txt";
        try (var is = Runtime.getRuntime().exec("logcat -d").getInputStream()) {
            os.putNextEntry(new ZipEntry(name));
            FileUtils.copy(is, os);
            os.closeEntry();
        } catch (IOException e) {
            Log.w(TAG, name, e);
        }
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
