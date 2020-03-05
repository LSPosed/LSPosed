package org.meowcat.edxposed.manager.widget;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.DownloadsUtil;
import org.meowcat.edxposed.manager.util.DownloadsUtil.DownloadFinishedCallback;

import java.util.Objects;

public class DownloadView extends LinearLayout {
    public static DownloadsUtil.DownloadInfo lastInfo = null;
    private final Button btnDownload;
    private final Button btnDownloadCancel;
    private final Button btnInstall;
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
                btnInstall.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                txtInfo.setVisibility(View.VISIBLE);
                txtInfo.setText(R.string.download_view_no_url);
            } else if (mInfo == null) {
                btnDownload.setVisibility(View.VISIBLE);
                btnSave.setVisibility(View.VISIBLE);
                btnDownloadCancel.setVisibility(View.GONE);
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
                        btnInstall.setVisibility(View.GONE);
                        progressBar.setVisibility(View.GONE);
                        txtInfo.setVisibility(View.VISIBLE);
                        txtInfo.setText(getContext().getString(
                                R.string.download_view_failed, mInfo.reason));
                        break;

                    case DownloadManager.STATUS_SUCCESSFUL:
                        btnDownload.setVisibility(View.GONE);
                        btnSave.setVisibility(View.VISIBLE);
                        btnDownloadCancel.setVisibility(View.GONE);
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
        btnInstall = findViewById(R.id.btnInstall);
        btnSave = findViewById(R.id.save);

        btnDownload.setOnClickListener(v -> {
            mInfo = DownloadsUtil.addModule(getContext(), mTitle, mUrl, mCallback);
            refreshViewFromUiThread();
            if (mInfo != null)
                new DownloadMonitor().start();
        });

        btnSave.setOnClickListener(v -> {
            lastInfo = mInfo;
            mInfo = DownloadsUtil.addModule(getContext(), mTitle, mUrl, (context1, info) -> {
                Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
                exportIntent.setType(DownloadsUtil.MIME_TYPE_APK);
                exportIntent.putExtra(Intent.EXTRA_TITLE, mTitle + ".apk");
                fragment.startActivityForResult(exportIntent, 42);
            });
            refreshViewFromUiThread();
            if (mInfo != null)
                new DownloadMonitor().start();
        });

        btnDownloadCancel.setOnClickListener(v -> {
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