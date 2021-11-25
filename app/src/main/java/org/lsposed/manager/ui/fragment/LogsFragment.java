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
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentLogsBinding;
import org.lsposed.manager.ui.dialog.BlurBehindDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import rikka.core.os.FileUtils;

public class LogsFragment extends BaseFragment {
    private boolean verbose = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FragmentLogsBinding binding;
    private final ActivityResultLauncher<String> saveLogsLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri == null) return;
                runAsync(() -> {
                    var context = requireContext();
                    var contentResolver = context.getContentResolver();
                    try (var os = new ZipOutputStream(contentResolver.openOutputStream(uri))) {
                        os.setLevel(Deflater.BEST_COMPRESSION);
                        zipLogs(os);
                        os.finish();
                    } catch (IOException e) {
                        var text = context.getString(R.string.logs_save_failed2, e.getMessage());
                        if (binding != null && isResumed()) {
                            Snackbar.make(binding.snackbar, text, Snackbar.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
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
            binding.scrollView.fullScroll(ScrollView.FOCUS_UP);
        } else if (itemId == R.id.menu_scroll_down) {
            binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
        } else if (itemId == R.id.menu_refresh) {
            reloadLogs();
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

    private void reloadLogs() {
        var parcelFileDescriptor = ConfigManager.getLog(verbose);
        if (parcelFileDescriptor != null)
            new LogsReader().execute(parcelFileDescriptor);
    }

    private void clear() {
        if (ConfigManager.clearLogs(verbose)) {
            Snackbar.make(binding.snackbar, R.string.logs_cleared, Snackbar.LENGTH_SHORT).show();
            binding.body.setText("");
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
    private class LogsReader extends AsyncTask<ParcelFileDescriptor, Integer, String> {
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
            mProgressDialog = new BlurBehindDialogBuilder(requireActivity()).create();
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setCancelable(false);
            handler.postDelayed(mRunnable, 300);
        }

        @Override
        protected String doInBackground(ParcelFileDescriptor... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);
            try (var pfd = log[0];
                 var inputStream = new FileInputStream(pfd.getFileDescriptor())) {
                int size = Math.toIntExact(pfd.getStatSize()); // max 4MiB
                var logs = new ByteArrayOutputStream(size);
                FileUtils.copy(inputStream, logs);
                return logs.toString();
            } catch (IOException e) {
                return requireActivity().getResources().getString(R.string.logs_cannot_read)
                        + "\n" + Log.getStackTraceString(e);
            }
        }

        @Override
        synchronized protected void onPostExecute(String logs) {
            binding.body.setText(logs);

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
}
