package io.github.lsposed.manager.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import io.github.lsposed.manager.BuildConfig;
import io.github.lsposed.manager.Constants;
import io.github.lsposed.manager.R;
import io.github.lsposed.manager.databinding.ActivityLogsBinding;
import io.github.lsposed.manager.databinding.DialogInstallWarningBinding;
import io.github.lsposed.manager.databinding.ItemLogBinding;
import io.github.lsposed.manager.util.LinearLayoutManagerFix;
import rikka.recyclerview.RecyclerViewKt;

public class LogsActivity extends BaseActivity {
    private int logType = 0;
    private final Path modulesLog = Paths.get(Constants.getLogDir(), "modules.log");
    private final Path allLog = Paths.get(Constants.getLogDir(), "all.log");
    private LogsAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ActivityLogsBinding binding;
    private LinearLayoutManagerFix layoutManager;

    private Path getLogFile() {
        switch (logType) {
            case 0:
            default:
                return modulesLog;
            case 1:
                return allLog;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }

        if (!preferences.getBoolean("hide_logcat_warning", false)) {
            DialogInstallWarningBinding binding = DialogInstallWarningBinding.inflate(getLayoutInflater());
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_warning_title)
                    .setMessage(R.string.not_logcat)
                    .setView(binding.getRoot())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        if (binding.checkbox.isChecked())
                            preferences.edit().putBoolean("hide_logcat_warning", true).apply();
                    })
                    .setCancelable(false)
                    .show();
        }
        adapter = new LogsAdapter();
        RecyclerViewKt.fixEdgeEffect(binding.recyclerView, false, true);
        binding.recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManagerFix(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        setupRecyclerViewInsets(binding.recyclerView, binding.getRoot());
        try {
            if (Files.readAllBytes(Paths.get(Constants.getMiscDir(), "disable_verbose_log"))[0] == 49) {
                binding.slidingTabs.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        binding.slidingTabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                logType = tab.getPosition();
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

    @Override
    public void onResume() {
        super.onResume();
        reloadErrorLog();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_logs, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_scroll_top) {
            Log.e("Test", adapter.getItemCount() + "");
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
        new LogsReader().execute(getLogFile());
    }

    private void clear() {
        try {
            Files.write(getLogFile(), new byte[0]);
            adapter.setEmpty();
            Snackbar.make(binding.snackbar, R.string.logs_cleared, Snackbar.LENGTH_SHORT).show();
            reloadErrorLog();
        } catch (IOException e) {
            Snackbar.make(binding.snackbar, getResources().getString(R.string.logs_clear_failed) + "n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void send() {
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", getLogFile().toFile());
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setDataAndType(uri, "text/plain");
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.menuSend)));
    }

    @SuppressLint("DefaultLocale")
    private void save() {
        Calendar now = Calendar.getInstance();
        String filename = String.format(
                "LSPosed_Verbose_%04d%02d%02d_%02d%02d%02d.log",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));

        Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
        exportIntent.setType("text/*");
        exportIntent.putExtra(Intent.EXTRA_TITLE, filename);
        startActivityForResult(exportIntent, 42);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == 42) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        OutputStream os = getContentResolver().openOutputStream(uri);
                        if (os != null) {
                            Files.copy(getLogFile(), os);
                        }
                    } catch (Exception e) {
                        Snackbar.make(binding.snackbar, getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class LogsReader extends AsyncTask<Path, Integer, List<String>> {
        private AlertDialog mProgressDialog;
        private final Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    mProgressDialog.show();
                }
            }
        };

        @Override
        protected void onPreExecute() {
            mProgressDialog = new MaterialAlertDialogBuilder(LogsActivity.this).create();
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setCancelable(false);
            handler.postDelayed(mRunnable, 300);
        }

        @Override
        protected List<String> doInBackground(Path... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            List<String> logs = new ArrayList<>();

            try {
                logs = Files.readAllLines(log[0]);
                return logs;
            } catch (IOException e) {
                logs.add(LogsActivity.this.getResources().getString(R.string.logs_cannot_read));
                if (e.getMessage() != null) {
                    logs.addAll(Arrays.asList(e.getMessage().split("\n")));
                }
            }

            return logs;
        }

        @Override
        protected void onPostExecute(List<String> logs) {
            if (logs.size() == 0) {
                adapter.setEmpty();
            } else {
                adapter.setLogs(logs);
            }
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

        void setEmpty() {
            logs.clear();
            logs.add(getString(R.string.log_is_empty));
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
