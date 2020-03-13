package org.meowcat.edxposed.manager.receivers;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import org.meowcat.edxposed.manager.util.DownloadsUtil;

public class DownloadReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        try {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                DownloadsUtil.triggerDownloadFinishedCallback(context, downloadId);
            }
        } catch (Exception e) {//Flyme
            e.printStackTrace();
            Toast.makeText(context, "shit flyme boom", Toast.LENGTH_LONG).show();
        }

    }
}