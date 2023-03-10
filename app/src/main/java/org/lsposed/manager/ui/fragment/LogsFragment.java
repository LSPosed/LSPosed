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

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textview.MaterialTextView;

import org.lsposed.manager.App;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.R;
import org.lsposed.manager.databinding.FragmentPagerBinding;
import org.lsposed.manager.databinding.ItemLogTextviewBinding;
import org.lsposed.manager.databinding.SwiperefreshRecyclerviewBinding;
import org.lsposed.manager.receivers.LSPManagerServiceHolder;
import org.lsposed.manager.ui.widget.EmptyStateRecyclerView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import rikka.material.app.LocaleDelegate;
import rikka.recyclerview.RecyclerViewKt;

public class LogsFragment extends BaseFragment implements MenuProvider {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FragmentPagerBinding binding;
    private LogPageAdapter adapter;
    private MenuItem wordWrap;

    interface OptionsItemSelectListener {
        boolean onOptionsItemSelected(@NonNull MenuItem item);
    }

    private OptionsItemSelectListener optionsItemSelectListener;

    private final ActivityResultLauncher<String> saveLogsLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/zip"),
            uri -> {
                if (uri == null) return;
                runAsync(() -> {
                    var context = requireContext();
                    var contentResolver = context.getContentResolver();
                    try (var zipFd = contentResolver.openFileDescriptor(uri, "wt")) {
                        LSPManagerServiceHolder.getService().getLogs(zipFd);
                        showHint(context.getString(R.string.logs_saved), true);
                    } catch (Throwable e) {
                        var cause = e.getCause();
                        var message = cause == null ? e.getMessage() : cause.getMessage();
                        var text = context.getString(R.string.logs_save_failed2, message);
                        showHint(text, false);
                        Log.w(App.TAG, "save log", e);
                    }
                });
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPagerBinding.inflate(inflater, container, false);
        binding.appBar.setLiftable(true);
        setupToolbar(binding.toolbar, binding.clickView, R.string.Logs, R.menu.menu_logs);
        binding.toolbar.setNavigationIcon(null);
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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem item) {
        var itemId = item.getItemId();
        if (itemId == R.id.menu_save) {
            save();
            return true;
        } else if (itemId == R.id.menu_word_wrap) {
            item.setChecked(!item.isChecked());
            App.getPreferences().edit().putBoolean("enable_word_wrap", item.isChecked()).apply();
            binding.viewPager.setUserInputEnabled(item.isChecked());
            adapter.refresh();
            return true;
        }
        if (optionsItemSelectListener != null) {
            return optionsItemSelectListener.onOptionsItemSelected(item);
        }
        return false;
    }

    @Override
    public void onPrepareMenu(@NonNull Menu menu) {
        wordWrap = menu.findItem(R.id.menu_word_wrap);
        wordWrap.setChecked(App.getPreferences().getBoolean("enable_word_wrap", false));
        binding.viewPager.setUserInputEnabled(wordWrap.isChecked());
    }

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }

    private void save() {
        LocalDateTime now = LocalDateTime.now();
        String filename = String.format(LocaleDelegate.getDefaultLocale(), "LSPosed_%s.zip", now.toString());
        try {
            saveLogsLauncher.launch(filename);
        } catch (ActivityNotFoundException e) {
            showHint(R.string.enable_documentui, true);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    public static class LogFragment extends BaseFragment {
        public static final int SCROLL_THRESHOLD = 500;
        protected boolean verbose;
        protected SwiperefreshRecyclerviewBinding binding;
        protected LogAdaptor adaptor;
        protected LinearLayoutManager layoutManager;

        class LogAdaptor extends EmptyStateRecyclerView.EmptyStateAdapter<LogAdaptor.ViewHolder> {
            private List<CharSequence> log = Collections.emptyList();
            private boolean isLoaded = false;

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new ViewHolder(ItemLogTextviewBinding.inflate(getLayoutInflater(), parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                holder.item.setText(log.get(position));
            }

            @Override
            public int getItemCount() {
                return log.size();
            }

            @SuppressLint("NotifyDataSetChanged")
            void refresh(List<CharSequence> log) {
                runOnUiThread(() -> {
                    isLoaded = true;
                    this.log = log;
                    notifyDataSetChanged();
                });
            }

            void fullRefresh() {
                runAsync(() -> {
                    isLoaded = false;
                    List<CharSequence> tmp;
                    try (var parcelFileDescriptor = ConfigManager.getLog(verbose);
                         var br = new BufferedReader(new InputStreamReader(new FileInputStream(parcelFileDescriptor != null ? parcelFileDescriptor.getFileDescriptor() : null)))) {
                        tmp = br.lines().parallel().collect(Collectors.toList());
                    } catch (Throwable e) {
                        tmp = Arrays.asList(Log.getStackTraceString(e).split("\n"));
                    }
                    refresh(tmp);
                });
            }

            @Override
            public boolean isLoaded() {
                return isLoaded;
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                final MaterialTextView item;

                public ViewHolder(ItemLogTextviewBinding binding) {
                    super(binding.getRoot());
                    item = binding.logItem;
                }
            }
        }

        protected LogAdaptor createAdaptor() {
            return new LogAdaptor();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            binding = SwiperefreshRecyclerviewBinding.inflate(getLayoutInflater(), container, false);
            var arguments = getArguments();
            if (arguments == null) return null;
            verbose = arguments.getBoolean("verbose");
            adaptor = createAdaptor();
            binding.recyclerView.setAdapter(adaptor);
            layoutManager = new LinearLayoutManager(requireActivity());
            binding.recyclerView.setLayoutManager(layoutManager);
            // ltr even for rtl languages because of log format
            binding.recyclerView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            binding.swipeRefreshLayout.setProgressViewEndTarget(true, binding.swipeRefreshLayout.getProgressViewEndOffset());
            RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
            binding.swipeRefreshLayout.setOnRefreshListener(adaptor::fullRefresh);
            adaptor.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    binding.swipeRefreshLayout.setRefreshing(!adaptor.isLoaded());
                }
            });
            adaptor.fullRefresh();
            return binding.getRoot();
        }

        public void scrollToTop(LogsFragment logsFragment) {
            logsFragment.binding.appBar.setExpanded(true, true);
            if (layoutManager.findFirstVisibleItemPosition() > SCROLL_THRESHOLD) {
                binding.recyclerView.scrollToPosition(0);
            } else {
                binding.recyclerView.smoothScrollToPosition(0);
            }
        }

        public void scrollToBottom(LogsFragment logsFragment) {
            logsFragment.binding.appBar.setExpanded(false, true);
            var end = Math.max(adaptor.getItemCount() - 1, 0);
            if (adaptor.getItemCount() - layoutManager.findLastVisibleItemPosition() > SCROLL_THRESHOLD) {
                binding.recyclerView.scrollToPosition(end);
            } else {
                binding.recyclerView.smoothScrollToPosition(end);
            }
        }

        void attachListeners() {
            var parent = getParentFragment();
            if (parent instanceof LogsFragment) {
                var logsFragment = (LogsFragment) parent;
                logsFragment.binding.appBar.setLifted(!binding.recyclerView.getBorderViewDelegate().isShowingTopBorder());
                binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> logsFragment.binding.appBar.setLifted(!top));
                logsFragment.setOptionsItemSelectListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_scroll_top) {
                        scrollToTop(logsFragment);
                    } else if (itemId == R.id.menu_scroll_down) {
                        scrollToBottom(logsFragment);
                    } else if (itemId == R.id.menu_clear) {
                        if (ConfigManager.clearLogs(verbose)) {
                            logsFragment.showHint(R.string.logs_cleared, true);
                            adaptor.fullRefresh();
                        } else {
                            logsFragment.showHint(R.string.logs_clear_failed_2, true);
                        }
                        return true;
                    }
                    return false;
                });

                View.OnClickListener l = v -> scrollToTop(logsFragment);
                logsFragment.binding.clickView.setOnClickListener(l);
                logsFragment.binding.toolbar.setOnClickListener(l);
            }
        }

        void detachListeners() {
            binding.recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener(null);
        }

        @Override
        public void onStart() {
            super.onStart();
            attachListeners();
        }

        @Override
        public void onResume() {
            super.onResume();
            attachListeners();
        }


        @Override
        public void onPause() {
            super.onPause();
            detachListeners();
        }

        @Override
        public void onStop() {
            super.onStop();
            detachListeners();
        }
    }

    public static class UnwrapLogFragment extends LogFragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            var root = super.onCreateView(inflater, container, savedInstanceState);
            binding.swipeRefreshLayout.removeView(binding.recyclerView);
            HorizontalScrollView horizontalScrollView = new HorizontalScrollView(getContext());
            horizontalScrollView.setFillViewport(true);
            horizontalScrollView.setHorizontalScrollBarEnabled(false);
            horizontalScrollView.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            binding.swipeRefreshLayout.addView(horizontalScrollView);
            horizontalScrollView.addView(binding.recyclerView);
            binding.recyclerView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
            return root;
        }

        @Override
        protected LogAdaptor createAdaptor() {
            return new LogAdaptor() {
                @Override
                public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                    super.onBindViewHolder(holder, position);
                    var view = holder.item;
                    view.measure(0, 0);
                    int desiredWidth = view.getMeasuredWidth();
                    ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                    layoutParams.width = desiredWidth;
                    if (binding.recyclerView.getWidth() < desiredWidth) {
                        binding.recyclerView.requestLayout();
                    }
                }
            };
        }
    }

    class LogPageAdapter extends FragmentStateAdapter {

        public LogPageAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            var bundle = new Bundle();
            bundle.putBoolean("verbose", verbose(position));
            var f = getItemViewType(position) == 0 ? new LogFragment() : new UnwrapLogFragment();
            f.setArguments(bundle);
            return f;
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        @Override
        public long getItemId(int position) {
            return verbose(position) ? R.string.nav_item_logs_verbose : R.string.nav_item_logs_module;
        }

        @Override
        public boolean containsItem(long itemId) {
            return itemId == R.string.nav_item_logs_verbose || itemId == R.string.nav_item_logs_module;
        }

        public boolean verbose(int position) {
            return position != 0;
        }

        @Override
        public int getItemViewType(int position) {
            return wordWrap.isChecked() ? 0 : 1;
        }

        public void refresh() {
            runOnUiThread(this::notifyDataSetChanged);
        }
    }
}
