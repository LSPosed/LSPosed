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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentItemLogBinding;
import org.lsposed.manager.databinding.FragmentPagerBinding;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import rikka.core.os.FileUtils;

public class LogsFragment extends BaseFragment {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FragmentPagerBinding binding;
    private LogPageAdapter adapter;

    interface OptionsItemSelectListener {
        boolean onOptionsItemSelected(@NonNull MenuItem item);
    }

    private OptionsItemSelectListener optionsItemSelectListener;

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
                        showHint(text, false);
                    }
                });
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPagerBinding.inflate(inflater, container, false);
        binding.appBar.setLiftable(true);
        setupToolbar(binding.toolbar, R.string.Logs, R.menu.menu_logs);
        binding.toolbar.setSubtitle(ConfigManager.isVerboseLogEnabled() ? R.string.enabled_verbose_log : R.string.disabled_verbose_log);
        adapter = new LogPageAdapter(this);
        binding.viewPager.setAdapter(adapter);
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> tab.setText((int) adapter.getItemId(position))).attach();

        binding.tabLayout.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            ViewGroup vg = (ViewGroup) binding.tabLayout.getChildAt(0);
            int tabLayoutWidth = IntStream.range(0, binding.tabLayout.getTabCount()).map(i -> vg.getChildAt(i).getWidth()).sum();
            if (tabLayoutWidth <= binding.getRoot().getWidth()) {
                binding.tabLayout.setTabMode(TabLayout.MODE_FIXED);
                binding.tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            }
        });

        return binding.getRoot();
    }

    public void setOptionsItemSelectListener(OptionsItemSelectListener optionsItemSelectListener) {
        this.optionsItemSelectListener = optionsItemSelectListener;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        var itemId = item.getItemId();
        if (itemId == R.id.menu_save) {
            save();
            return true;
        } else if (itemId == R.id.item_word_wrap) {
            item.setChecked(!item.isChecked());
            App.getPreferences().edit().putBoolean("enable_word_wrap", item.isChecked()).apply();
            binding.viewPager.setUserInputEnabled(item.isChecked());
        }
        if (optionsItemSelectListener != null) {
            if (optionsItemSelectListener.onOptionsItemSelected(item))
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        var checked = App.getPreferences().getBoolean("enable_word_wrap", false);
        menu.findItem(R.id.item_word_wrap).setChecked(checked);
        binding.viewPager.setUserInputEnabled(checked);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
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
                Log.w(App.TAG, name, e);
            }
        });

        var now = LocalDateTime.now();
        var name = "app_" + now.toString() + ".log";
        try (var is = new ProcessBuilder("logcat", "-d").start().getInputStream()) {
            os.putNextEntry(new ZipEntry(name));
            FileUtils.copy(is, os);
            os.closeEntry();
        } catch (IOException e) {
            Log.w(App.TAG, name, e);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public static class LogFragment extends BaseFragment {
        private boolean verbose;
        private FragmentItemLogBinding binding;
        private String log = null;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = FragmentItemLogBinding.inflate(getLayoutInflater(), container, false);
            var arguments = getArguments();
            if (arguments == null) return null;
            verbose = arguments.getBoolean("verbose");
            binding.swipeRefreshLayout.setOnRefreshListener(this::refreshLog);
            binding.swipeRefreshLayout.setProgressViewEndTarget(true, binding.swipeRefreshLayout.getProgressViewEndOffset());
            return binding.getRoot();
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshLog();
            var parent = getParentFragment();
            if (parent instanceof LogsFragment) {
                var logsFragment = (LogsFragment) parent;
                logsFragment.binding.appBar.setLifted(!binding.scrollView.getBorderViewDelegate().isShowingTopBorder());
                binding.scrollView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> logsFragment.binding.appBar.setLifted(!top));
                logsFragment.setOptionsItemSelectListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_scroll_top) {
                        binding.scrollView.fullScroll(ScrollView.FOCUS_UP);
                    } else if (itemId == R.id.menu_scroll_down) {
                        logsFragment.binding.appBar.setExpanded(false, true);
                        binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    } else if (itemId == R.id.menu_clear) {
                        if (ConfigManager.clearLogs(verbose)) {
                            logsFragment.showHint(R.string.logs_cleared, true);
                            refreshLog();
                        } else {
                            logsFragment.showHint(R.string.logs_clear_failed_2, true);
                        }
                        return true;
                    } else if (itemId == R.id.item_word_wrap) {
                        refreshLog();
                    }
                    return false;
                });
                logsFragment.binding.toolbar.setOnClickListener(v -> {
                    logsFragment.binding.appBar.setExpanded(true, true);
                    binding.scrollView.fullScroll(ScrollView.FOCUS_UP);
                });
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            binding.scrollView.getBorderViewDelegate().setBorderVisibilityChangedListener(null);
        }

        void refreshLog() {
            runAsync(() -> {
                try (var parcelFileDescriptor = ConfigManager.getLog(verbose);
                     var bis = new BufferedInputStream(new FileInputStream(parcelFileDescriptor != null ? parcelFileDescriptor.getFileDescriptor() : null));
                     ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                    for (int result = bis.read(); result != -1; result = bis.read()) {
                        buf.write((byte) result);
                    }
                    log = buf.toString();
                } catch (Throwable e) {
                    log = e.getLocalizedMessage();
                } finally {
                    runOnUiThread(this::finishLoad);
                }
            });
        }

        void finishLoad() {
            runAsync(() -> {
                var precomputedTextCompat = PrecomputedTextCompat.create(log, binding.body.getTextMetricsParamsCompat());
                runOnUiThread(() -> {
                    var wrap = App.getPreferences().getBoolean("enable_word_wrap", false);
                    if (wrap) {
                        binding.wrapBody.setVisibility(View.VISIBLE);
                        binding.horizontalScrollView.setVisibility(View.GONE);
                    } else {
                        binding.wrapBody.setVisibility(View.GONE);
                        binding.horizontalScrollView.setVisibility(View.VISIBLE);
                    }
                    TextViewCompat.setPrecomputedText(binding.body, precomputedTextCompat);
                    TextViewCompat.setPrecomputedText(binding.wrapBody, precomputedTextCompat);
                    binding.swipeRefreshLayout.setRefreshing(false);
                });
            });
        }
    }

    static class LogPageAdapter extends FragmentStateAdapter {

        public LogPageAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            var bundle = new Bundle();
            bundle.putBoolean("verbose", verbose(position));
            var f = new LogFragment();
            f.setArguments(bundle);
            return f;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @Override
        public long getItemId(int position) {
            return verbose(position) ? R.string.nav_item_logs_lsp : R.string.nav_item_logs_module;
        }

        @Override
        public boolean containsItem(long itemId) {
            return itemId == R.string.nav_item_logs_lsp || itemId == R.string.nav_item_logs_module;
        }

        public boolean verbose(int position) {
            return position != 0;
        }
    }
}
