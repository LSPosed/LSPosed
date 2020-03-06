package org.meowcat.edxposed.manager.widget;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.databinding.DownloadViewBinding;
import org.meowcat.edxposed.manager.util.DownloadsUtil;
import org.meowcat.edxposed.manager.util.DownloadsUtil.DownloadFinishedCallback;

public class DownloadView extends LinearLayout {
    public static DownloadsUtil.DownloadInfo lastInfo = null;
    public Fragment fragment;
    private DownloadsUtil.DownloadInfo mInfo = null;
    private String mUrl = null;
    private String mTitle = null;
    private DownloadFinishedCallback mCallback = null;
    private DownloadViewBinding binding;
    private final Runnable refreshViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (mUrl == null) {
                binding.btnDownload.setVisibility(View.GONE);
                binding.btnSave.setVisibility(View.GONE);
                binding.btnDownloadCancel.setVisibility(View.GONE);
                binding.btnInstall.setVisibility(View.GONE);
                binding.progress.setVisibility(View.GONE);
                binding.txtInfo.setVisibility(View.VISIBLE);
                binding.txtInfo.setText(R.string.download_view_no_url);
            } else if (mInfo == null) {
                binding.btnDownload.setVisibility(View.VISIBLE);
                binding.btnSave.setVisibility(View.VISIBLE);
                binding.btnDownloadCancel.setVisibility(View.GONE);
                binding.btnInstall.setVisibility(View.GONE);
                binding.progress.setVisibility(View.GONE);
                binding.txtInfo.setVisibility(View.GONE);
            } else {
                switch (mInfo.status) {
                    case DownloadManager.STATUS_PENDING:
                    case DownloadManager.STATUS_PAUSED:
                    case DownloadManager.STATUS_RUNNING:
                        binding.btnDownload.setVisibility(View.GONE);
                        binding.btnSave.setVisibility(View.GONE);
                        binding.btnDownloadCancel.setVisibility(View.VISIBLE);
                        binding.btnInstall.setVisibility(View.GONE);
                        binding.progress.setVisibility(View.VISIBLE);
                        binding.txtInfo.setVisibility(View.VISIBLE);
                        if (mInfo.totalSize <= 0 || mInfo.status != DownloadManager.STATUS_RUNNING) {
                            binding.progress.setIndeterminate(true);
                            binding.txtInfo.setText(R.string.download_view_waiting);
                        } else {
                            binding.progress.setIndeterminate(false);
                            binding.progress.setMax(mInfo.totalSize);
                            binding.progress.setProgress(mInfo.bytesDownloaded);
                            binding.txtInfo.setText(getContext().getString(
                                    R.string.download_view_running,
                                    mInfo.bytesDownloaded / 1024,
                                    mInfo.totalSize / 1024));
                        }
                        break;

                    case DownloadManager.STATUS_FAILED:
                        binding.btnDownload.setVisibility(View.VISIBLE);
                        binding.btnSave.setVisibility(View.VISIBLE);
                        binding.btnDownloadCancel.setVisibility(View.GONE);
                        binding.btnInstall.setVisibility(View.GONE);
                        binding.progress.setVisibility(View.GONE);
                        binding.txtInfo.setVisibility(View.VISIBLE);
                        binding.txtInfo.setText(getContext().getString(
                                R.string.download_view_failed, mInfo.reason));
                        break;

                    case DownloadManager.STATUS_SUCCESSFUL:
                        binding.btnDownload.setVisibility(View.GONE);
                        binding.btnSave.setVisibility(View.VISIBLE);
                        binding.btnDownloadCancel.setVisibility(View.GONE);
                        binding.btnInstall.setVisibility(View.VISIBLE);
                        binding.progress.setVisibility(View.GONE);
                        binding.txtInfo.setVisibility(View.VISIBLE);
                        binding.txtInfo.setText(R.string.download_view_successful);
                        break;
                }
            }
        }
    };

    public DownloadView(Context context, final AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        setOrientation(LinearLayout.VERTICAL);

        binding = DownloadViewBinding.inflate(LayoutInflater.from(context), this);

        binding.btnDownload.setOnClickListener(v -> {
            mInfo = DownloadsUtil.addModule(getContext(), mTitle, mUrl, mCallback);
            refreshViewFromUiThread();
            if (mInfo != null)
                new DownloadMonitor().start();
        });

        binding.btnSave.setOnClickListener(v -> {
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

        binding.btnDownloadCancel.setOnClickListener(v -> {
            if (mInfo == null)
                return;

            DownloadsUtil.removeById(getContext(), mInfo.id);
            // UI update will happen automatically by the DownloadMonitor
        });

        binding.btnInstall.setOnClickListener(v -> {
            if (mCallback == null)
                return;

            mCallback.onDownloadFinished(getContext(), mInfo);
        });

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