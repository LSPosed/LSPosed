package org.meowcat.edxposed.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Scanner;

public class LogsActivity extends BaseActivity {
    private boolean allLog = false;
    private File mFileErrorLog = new File(XposedApp.BASE_DIR + "log/error.log");
    private File mFileErrorLogOld = new File(
            XposedApp.BASE_DIR + "log/error.log.old");
    private File mFileAllLog = new File(XposedApp.BASE_DIR + "log/all.log");
    private File mFileAllLogOld = new File(XposedApp.BASE_DIR + "log/all.log.old");
    private MenuItem mClickedMenuItem = null;
    private LogsAdapter mAdapter;
    private RecyclerView mListView;
    private Handler handler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        setupWindowInsets();

        if (!XposedApp.getPreferences().getBoolean("hide_logcat_warning", false)) {
            @SuppressLint("InflateParams") final View dontShowAgainView = getLayoutInflater().inflate(R.layout.dialog_install_warning, null);

            TextView message = dontShowAgainView.findViewById(android.R.id.message);
            message.setText(R.string.not_logcat);

            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.install_warning_title)
                    .setView(dontShowAgainView)
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        CheckBox checkBox = dontShowAgainView.findViewById(android.R.id.checkbox);
                        if (checkBox.isChecked())
                            XposedApp.getPreferences().edit().putBoolean("hide_logcat_warning", true).apply();
                    })
                    .setCancelable(false)
                    .show();
        }
        mAdapter = new LogsAdapter();
        mListView = findViewById(R.id.recyclerView);
        mListView.setAdapter(mAdapter);
        mListView.setLayoutManager(new LinearLayoutManager(this));
        TabLayout tabLayout = findViewById(R.id.sliding_tabs);
        if (XposedApp.getPreferences().getBoolean("disable_verbose_log", false)) {
            tabLayout.setVisibility(View.GONE);
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                allLog = tab.getPosition() != 0;
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
        mClickedMenuItem = item;
        switch (item.getItemId()) {
            case R.id.menu_scroll_top:
                scrollTop();
                break;
            case R.id.menu_scroll_down:
                scrollDown();
                break;
            case R.id.menu_refresh:
                reloadErrorLog();
                return true;
            case R.id.menu_send:
                try {
                    send();
                } catch (NullPointerException ignored) {
                }
                return true;
            case R.id.menu_save:
                save();
                return true;
            case R.id.menu_clear:
                clear();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollTop() {
        mListView.smoothScrollToPosition(0);
    }

    private void scrollDown() {
        mListView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
    }

    private void reloadErrorLog() {
        new LogsReader().execute(allLog ? mFileAllLog : mFileErrorLog);
    }

    private void clear() {
        try {
            new FileOutputStream(allLog ? mFileAllLog : mFileErrorLog).close();
            //noinspection ResultOfMethodCallIgnored
            (allLog ? mFileAllLogOld : mFileErrorLogOld).delete();
            mAdapter.setEmpty();
            Snackbar.make(findViewById(R.id.snackbar), R.string.logs_cleared, Snackbar.LENGTH_SHORT).show();
            reloadErrorLog();
        } catch (IOException e) {
            Snackbar.make(findViewById(R.id.snackbar), getResources().getString(R.string.logs_clear_failed) + "n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private void send() {
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", allLog ? mFileAllLog : mFileErrorLog);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        sendIntent.setType("application/html");
        startActivity(Intent.createChooser(sendIntent, getResources().getString(R.string.menuSend)));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions,
                grantResults);
        if (requestCode == XposedApp.WRITE_EXTERNAL_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mClickedMenuItem != null) {
                    new Handler().postDelayed(() -> onOptionsItemSelected(mClickedMenuItem), 500);
                }
            } else {
                Snackbar.make(findViewById(R.id.snackbar), R.string.permissionNotGranted, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void save() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, XposedApp.WRITE_EXTERNAL_PERMISSION);
            return;
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Snackbar.make(findViewById(R.id.snackbar), R.string.sdcard_not_writable, Snackbar.LENGTH_LONG).show();
            return;
        }

        Calendar now = Calendar.getInstance();
        String filename = String.format(
                "EdXposed_Verbose_%04d%02d%02d_%02d%02d%02d.log",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH), now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), now.get(Calendar.SECOND));

        File targetFile = new File(XposedApp.createFolder(), filename);

        try {
            FileInputStream in = new FileInputStream(allLog ? mFileAllLog : mFileErrorLog);
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            Snackbar.make(findViewById(R.id.snackbar), targetFile.toString(), Snackbar.LENGTH_LONG).show();
        } catch (IOException e) {
            Snackbar.make(findViewById(R.id.snackbar), getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LogsReader extends AsyncTask<File, Integer, ArrayList<String>> {
        private ProgressDialog mProgressDialog;
        private Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                mProgressDialog.show();
            }
        };

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(LogsActivity.this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setProgress(0);
            handler.postDelayed(mRunnable, 500);
        }

        @Override
        protected ArrayList<String> doInBackground(File... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            ArrayList<String> logs = new ArrayList<>();

            try {
                File logfile = log[0];
                try (Scanner scanner = new Scanner(logfile)) {
                    while (scanner.hasNextLine()) {
                        logs.add(scanner.nextLine());
                    }
                }
                return logs;
            } catch (IOException e) {
                logs.add(LogsActivity.this.getResources().getString(R.string.logs_cannot_read));
                logs.addAll(Arrays.asList(e.getMessage().split("\n")));
            }

            return logs;
        }

        @Override
        protected void onPostExecute(ArrayList<String> logs) {
            if (logs.size() == 0) {
                mAdapter.setEmpty();
            } else {
                mAdapter.setLogs(logs);
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
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
            return new LogsAdapter.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull LogsAdapter.ViewHolder holder, int position) {
            TextView view = holder.textView;
            view.setText(logs.get(position));
            view.measure(0, 0);
            int desiredWidth = view.getMeasuredWidth();
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = desiredWidth;
            if (mListView.getWidth() < desiredWidth) {
                mListView.requestLayout();
            }

        }

        void setLogs(ArrayList<String> logs) {
            this.logs.clear();
            this.logs.addAll(logs);
            notifyDataSetChanged();
            mListView.scrollToPosition(getItemCount() - 1);
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

            ViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.log);
            }
        }
    }


}
