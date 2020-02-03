package org.meowcat.edxposed.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Objects;

public class LogsActivity extends BaseActivity {
    private boolean errorLog = false;
    private File mFileErrorLog = new File(XposedApp.BASE_DIR + "log/error.log");
    private File mFileErrorLogOld = new File(
            XposedApp.BASE_DIR + "log/error.log.old");
    private File mFileErrorLogError = new File(XposedApp.BASE_DIR + "log/all.log");
    private File mFileErrorLogOldError = new File(XposedApp.BASE_DIR + "log/all.log.old");
    private TextView mTxtLog;
    private ScrollView mSVLog;
    private HorizontalScrollView mHSVLog;
    private MenuItem mClickedMenuItem = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        mTxtLog = findViewById(R.id.txtLog);
        mTxtLog.setTextIsSelectable(true);
        mSVLog = findViewById(R.id.svLog);
        mHSVLog = findViewById(R.id.hsvLog);

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
            case R.id.menu_logs:
                item.setChecked(true);
                errorLog = false;
                reloadErrorLog();
                break;
            case R.id.menu_logs_err:
                item.setChecked(true);
                errorLog = true;
                reloadErrorLog();
                scrollDown();
                break;
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
        mSVLog.post(() -> mSVLog.scrollTo(0, 0));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void scrollDown() {
        mSVLog.post(() -> mSVLog.scrollTo(0, mTxtLog.getHeight()));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void reloadErrorLog() {
        new LogsReader().execute(errorLog ? mFileErrorLogError : mFileErrorLog);
        mSVLog.post(() -> mSVLog.scrollTo(0, mTxtLog.getHeight()));
        mHSVLog.post(() -> mHSVLog.scrollTo(0, 0));
    }

    private void clear() {
        try {
            new FileOutputStream(errorLog ? mFileErrorLogError : mFileErrorLog).close();
            (errorLog ? mFileErrorLogOldError : mFileErrorLogOld).delete();
            mTxtLog.setText(R.string.log_is_empty);
            Toast.makeText(this, R.string.logs_cleared,
                    Toast.LENGTH_SHORT).show();
            reloadErrorLog();
        } catch (IOException e) {
            Toast.makeText(this, getResources().getString(R.string.logs_clear_failed) + "n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void send() {
        Uri uri = FileProvider.getUriForFile(Objects.requireNonNull(this), "org.meowcat.edxposed.manager.fileprovider", errorLog ? mFileErrorLogError : mFileErrorLog);
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
                Toast.makeText(this, R.string.permissionNotGranted, Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void save() {
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(this), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, XposedApp.WRITE_EXTERNAL_PERMISSION);
            return;
        }

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, R.string.sdcard_not_writable, Toast.LENGTH_LONG).show();
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
            FileInputStream in = new FileInputStream(errorLog ? mFileErrorLogError : mFileErrorLog);
            FileOutputStream out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.close();

            Toast.makeText(this, targetFile.toString(),
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, getResources().getString(R.string.logs_save_failed) + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class LogsReader extends AsyncTask<File, Integer, String> {

        private static final int MAX_LOG_SIZE = 1000 * 1024; // 1000 KB
        private ProgressDialog mProgressDialog;

        private long skipLargeFile(BufferedReader is, long length) throws IOException {
            if (length < MAX_LOG_SIZE)
                return 0;

            long skipped = length - MAX_LOG_SIZE;
            long yetToSkip = skipped;
            do {
                yetToSkip -= is.skip(yetToSkip);
            } while (yetToSkip > 0);

            int c;
            do {
                c = is.read();
                if (c == -1)
                    break;
                skipped++;
            } while (c != '\n');

            return skipped;

        }

        @Override
        protected void onPreExecute() {
            mTxtLog.setText("");
            mProgressDialog = new ProgressDialog(LogsActivity.this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(File... log) {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY + 2);

            StringBuilder llog = new StringBuilder(15 * 10 * 1024);

            if (XposedApp.getPreferences().getBoolean(
                    "disable_verbose_log", false) && errorLog) {
                llog.append(LogsActivity.this.getResources().getString(R.string.logs_verbose_disabled));
                return llog.toString();
            }
            try {
                File logfile = log[0];
                BufferedReader br;
                br = new BufferedReader(new FileReader(logfile));
                long skipped = skipLargeFile(br, logfile.length());
                if (skipped > 0) {
                    llog.append(LogsActivity.this.getResources().getString(R.string.logs_too_long));
                    llog.append("\n-----------------\n");
                }

                char[] temp = new char[1024];
                int read;
                while ((read = br.read(temp)) > 0) {
                    llog.append(temp, 0, read);
                }
                br.close();
            } catch (IOException e) {
                llog.append(LogsActivity.this.getResources().getString(R.string.logs_cannot_read));
                llog.append(e.getMessage());
            }

            return llog.toString();
        }

        @Override
        protected void onPostExecute(String llog) {
            mProgressDialog.dismiss();
            mTxtLog.setText(llog);

            if (llog.length() == 0)
                mTxtLog.setText(R.string.log_is_empty);
        }

    }
}
