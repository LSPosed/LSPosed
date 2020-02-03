package org.meowcat.edxposed.manager.widget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.DownloadsUtil;
import org.meowcat.edxposed.manager.util.DownloadsUtil.DownloadFinishedCallback;

import java.util.Objects;

import static org.meowcat.edxposed.manager.XposedApp.WRITE_EXTERNAL_PERMISSION;

public class DownloadView extends LinearLayout {
    @SuppressLint("StaticFieldLeak")
    public static Button mClickedButton;
    private final Button btnDownload;
    private final Button btnDownloadCancel;
    private final Button btnInstall;
    private final Button btnRemove;
    private final Button btnSave;
    private final ProgressBar progressBar;
    private final TextView txtInfo;
    public Fragment fragment;
    private DownloadsUtil.DownloadInfo mInfo = null;
    private String mUrl = null;
    private final Runnable refreshViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (mUrl == null) {
                btnDownload.setVisibility(View.GONE);
                btnSave.setVisibility(View.GONE);
                btnDownloadCancel.setVisibility(View.GONE);
                btnRemove.setVisibility(View.GONE);
                btnInstall.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                txtInfo.setVisibility(View.VISIBLE);
                txtInfo.setText(R.string.download_view_no_url);
            } else if (mInfo == null) {
                btnDownload.setVisibility(View.VISIBLE);
                btnSave.setVisibility(View.VISIBLE);
                btnDownloadCancel.setVisibility(View.GONE);
                btnRemove.setVisibility(View.GONE);
                btnInstall.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                txtInfo.setVisibility(View.GONE);
            } else {
                switch (mInfo.status) {
                    case DownloadManager.STATUS_PENDING:
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_RUNNING:
                        btnDownload.setVisibility(View.GONE);
                        btnSave.setVisibility(View.GONE);
                        btnDownloadCancel.setVisibility(View.VISIBLE);
                        btnRemove.setVisibility(View.GONE);
                        btnInstall.setVisibility(View.GONE);
                        progressBar.setVisibility(View.VISIBLE);
                        txtInfo.setVisibility(View.VISIBLE);
                        if (mInfo.totalSize <= 0 || mInfo.status != DownloadManager.STATUS_RUNNING) {
                            progressBar.setIndeterminate(true);
                            txtInfo.setText(R.string.download_view_waiting);
                        } else {
                            progressBar.setIndeterminate(false);
                            progressBar.setMax(mInfo.totalSize);
                            progressBar.setProgress(mInfo.bytesDownloaded);
                            txtInfo.setText(getContext().getString(
                                    R.string.download_view_running,
                                    mInfo.bytesDownloaded / 1024,
                                    mInfo.totalSize / 1024));
                        }
                        break;

                    case DownloadManager.STATUS_FAILED:
                        btnDownload.setVisibility(View.VISIBLE);
                        btnSave.setVisibility(View.VISIBLE);
                        btnDownloadCancel.setVisibility(View.GONE);
                        btnRemove.setVisibility(View.GONE);
                        btnInstall.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        txtInfo.setVisibility(View.VISIBLE);
                        txtInfo.setText(getContext().getString(
                                R.string.download_view_failed, mInfo.reason));
                        break;

                    case DownloadManager.STATUS_SUCCESSFUL:
                        btnDownload.setVisibility(View.GONE);
                        btnSave.setVisibility(View.GONE);
                        btnDownloadCancel.setVisibility(View.GONE);
                        btnRemove.setVisibility(View.VISIBLE);
                        btnInstall.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        txtInfo.setVisibility(View.VISIBLE);
                        txtInfo.setText(R.string.download_view_successful);
                        break;
                }
            }
        }
    };
    private String mTitle = null;
    private DownloadFinishedCallback mCallback = null;

    public DownloadView(Context context, final AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        setOrientation(LinearLayout.VERTICAL);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Objects.requireNonNull(inflater).inflate(R.layout.download_view, this, true);

        btnDownload = findViewById(R.id.btnDownload);
        btnDownloadCancel = findViewById(R.id.btnDownloadCancel);
        btnRemove = findViewById(R.id.btnRemove);
        btnInstall = findViewById(R.id.btnInstall);
        btnSave = findViewById(R.id.save);

        btnDownload.setOnClickListener(v -> {
            mClickedButton = btnDownload;

            mInfo = DownloadsUtil.addModule(getContext(), mTitle, mUrl, false, mCallback);
            refreshViewFromUiThread();

            if (mInfo != null)
                new DownloadMonitor().start();
        });

        btnSave.setOnClickListener(v -> {
            mClickedButton = btnSave;

            if (checkPermissions())
                return;

            mInfo = DownloadsUtil.addModule(getContext(), mTitle, mUrl, true, (context1, info) -> Toast.makeText(context1, context1.getString(R.string.module_saved, info.localFilename), Toast.LENGTH_SHORT).show());
        });

        btnDownloadCancel.setOnClickListener(v -> {
            if (mInfo == null)
                return;

            DownloadsUtil.removeById(getContext(), mInfo.id);
            // UI update will happen automatically by the DownloadMonitor
        });

        btnRemove.setOnClickListener(v -> {
            if (mInfo == null)
                return;

            DownloadsUtil.removeById(getContext(), mInfo.id);
            // UI update will happen automatically by the DownloadMonitor
        });

        btnInstall.setOnClickListener(v -> {
            if (mCallback == null)
                return;

            mCallback.onDownloadFinished(getContext(), mInfo);
        });

        progressBar = findViewById(R.id.progress);
        txtInfo = findViewById(R.id.txtInfo);

        refreshViewFromUiThread();
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this.getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            fragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_PERMISSION);
            return true;
        }
        return false;
    }

    private void refreshViewFromUiThread() {
        refreshViewRunnable.run();
    }

    private void refreshView() {
        post(refreshViewRunnable);
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        mUrl = url;

        if (mUrl != null)
            mInfo = DownloadsUtil.getLatestForUrl(getContext(), mUrl);
        else
            mInfo = null;

        refreshView();
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    @SuppressWarnings("unused")
    public DownloadFinishedCallback getDownloadFinishedCallback() {
        return mCallback;
    }

    public void setDownloadFinishedCallback(DownloadFinishedCallback downloadFinishedCallback) {
        this.mCallback = downloadFinishedCallback;
    }

    private class DownloadMonitor extends Thread {
        DownloadMonitor() {
            super("DownloadMonitor");
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }

                try {
                    mInfo = DownloadsUtil.getById(getContext(), mInfo.id);
                } catch (NullPointerException ignored) {
                }

                refreshView();
                if (mInfo == null)
                    return;

                if (mInfo.status != DownloadManager.STATUS_PENDING
                        && mInfo.status != DownloadManager.STATUS_PAUSED
                        && mInfo.status != DownloadManager.STATUS_RUNNING)
                    return;
            }
        }
    }
}