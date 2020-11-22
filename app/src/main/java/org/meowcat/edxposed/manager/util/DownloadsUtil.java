package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.content.SharedPreferences;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class DownloadsUtil {
    private static final SharedPreferences pref = App.getInstance().getSharedPreferences("download_cache", Context.MODE_PRIVATE);

    static SyncDownloadInfo downloadSynchronously(String url, File target) {
        final boolean useNotModifiedTags = target.exists();

        URLConnection connection = null;
        InputStream in = null;
        FileOutputStream out = null;
        try {
            connection = new URL(url).openConnection();
            connection.setDoOutput(false);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            if (connection instanceof HttpURLConnection) {
                // Disable transparent gzip encoding for gzipped files
                if (url.endsWith(".gz")) {
                    connection.addRequestProperty("Accept-Encoding", "identity");
                }

                if (useNotModifiedTags) {
                    String modified = pref.getString("download_" + url + "_modified", null);
                    String etag = pref.getString("download_" + url + "_etag", null);

                    if (modified != null) {
                        connection.addRequestProperty("If-Modified-Since", modified);
                    }
                    if (etag != null) {
                        connection.addRequestProperty("If-None-Match", etag);
                    }
                }
            }

            connection.connect();

            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    return new SyncDownloadInfo(SyncDownloadInfo.STATUS_NOT_MODIFIED, null);
                } else if (responseCode < 200 || responseCode >= 300) {
                    return new SyncDownloadInfo(SyncDownloadInfo.STATUS_FAILED,
                            App.getInstance().getString(R.string.repo_download_failed_http,
                                    url, responseCode,
                                    httpConnection.getResponseMessage()));
                }
            }

            in = connection.getInputStream();
            out = new FileOutputStream(target);
            byte[] buf = new byte[1024];
            int read;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
            }

            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                String modified = httpConnection.getHeaderField("Last-Modified");
                String etag = httpConnection.getHeaderField("ETag");

                pref.edit()
                        .putString("download_" + url + "_modified", modified)
                        .putString("download_" + url + "_etag", etag).apply();
            }

            return new SyncDownloadInfo(SyncDownloadInfo.STATUS_SUCCESS, null);

        } catch (Throwable t) {
            return new SyncDownloadInfo(SyncDownloadInfo.STATUS_FAILED,
                    App.getInstance().getString(R.string.repo_download_failed, url,
                            t.getMessage()));

        } finally {
            if (connection instanceof HttpURLConnection)
                ((HttpURLConnection) connection).disconnect();
            if (in != null)
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            if (out != null)
                try {
                    out.close();
                } catch (IOException ignored) {
                }
        }
    }

    static void clearCache(String url) {
        if (url != null) {
            pref.edit().remove("download_" + url + "_modified")
                    .remove("download_" + url + "_etag").apply();
        } else {
            pref.edit().clear().apply();
        }
    }

    public static class SyncDownloadInfo {
        static final int STATUS_SUCCESS = 0;
        static final int STATUS_NOT_MODIFIED = 1;
        static final int STATUS_FAILED = 2;

        public final int status;
        final String errorMessage;

        private SyncDownloadInfo(int status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }
    }
}