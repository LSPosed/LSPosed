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
import static java.lang.Math.max;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentLogsBinding;
import org.lsposed.manager.databinding.ItemLogBinding;
import org.lsposed.manager.util.SimpleStatefulAdaptor;

import java.io.BufferedReader;
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
    private final SharedPreferences preferences = App.getPreferences();
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
        setupToolbar(binding.toolbar, R.string.Logs, R.menu.menu_logs);

        binding.slidingTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                verbose = tab.getPosition() == 1;
                reloadLogs();
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
        reloadLogs();
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
                binding.recyclerView.smoothScrollToPosition(max(adapter.getItemCount() - 1, 0));
            }
        } else if (itemId == R.id.menu_refresh) {
            reloadLogs();
            return true;
        } else if (itemId == R.id.menu_save) {
            save();
            return true;
        } else if (itemId == R.id.menu_clear) {
            clear();
            return true;
        } else if (itemId == R.id.item_word_wrap) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("enable_word_wrap", item.isChecked()).apply();
            reloadLogs();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.item_word_wrap).setChecked(preferences.getBoolean("enable_word_wrap", false));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    private void reloadLogs() {
        var parcelFileDescriptor = ConfigManager.getLog(verbose);
        if (parcelFileDescriptor != null)
            new LogsReader().execute(parcelFileDescriptor);
    }

    private void clear() {
        if (ConfigManager.clearLogs(verbose)) {
            Snackbar.make(binding.snackbar, R.string.logs_cleared, Snackbar.LENGTH_SHORT).show();
            adapter.clearLogs();
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

        var now = LocalDateTime.now();
        var name = "app_" + now.toString() + ".log";
        try (var is = new ProcessBuilder("logcat", "-d").start().getInputStream()) {
            os.putNextEntry(new ZipEntry(name));
            FileUtils.copy(is, os);
            os.closeEntry();
        } catch (IOException e) {
            Log.w(TAG, name, e);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class LogsReader extends AsyncTask<ParcelFileDescriptor, Integer, List<String>> {
        private AlertDialog mProgressDialog;
        private final Runnable mRunnable = () -> {
            synchronized (LogsReader.this) {
                if (!requireActivity().isFinishing()) {
                    mProgressDialog.show();
                }
            }
        };

        @Override
        synchronized protected void onPreExecute() {
            mProgressDialog = new MaterialAlertDialogBuilder(requireActivity()).create();
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setCancelable(false);
            handler.postDelayed(mRunnable, 300);
        }

        @Override
        protected List<String> doInBackground(ParcelFileDescriptor... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            List<String> logs = new ArrayList<>();

            try (var pfd = log[0]; InputStream inputStream = new FileInputStream(pfd.getFileDescriptor()); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.add(line);
                }
            } catch (Exception e) {
                logs.add(requireActivity().getResources().getString(R.string.logs_cannot_read));
                logs.addAll(Arrays.asList(Log.getStackTraceString(e).split("\n")));
            }

            return logs;
        }

        @Override
        synchronized protected void onPostExecute(List<String> logs) {
            adapter.setLogs(logs);

            handler.removeCallbacks(mRunnable);
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(LogsFragment.class.getName() + "." + "tab", binding.slidingTabs.getSelectedTabPosition());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            var tabPosition = savedInstanceState.getInt(LogsFragment.class.getName() + "." + "tab", 0);
            if (tabPosition < binding.slidingTabs.getTabCount())
                binding.slidingTabs.selectTab(binding.slidingTabs.getTabAt(tabPosition));
        }
        super.onViewStateRestored(savedInstanceState);
    }

    private class LogsAdapter extends SimpleStatefulAdaptor<LogsAdapter.ViewHolder> {
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
            int desiredWidth = (preferences.getBoolean("enable_word_wrap", false)) ? layoutManager.getWidth() : view.getMeasuredWidth();
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

        void clearLogs() {
            notifyItemRangeRemoved(0, logs.size());
            logs.clear();
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
