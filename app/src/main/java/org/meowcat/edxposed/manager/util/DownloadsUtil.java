package org.meowcat.edxposed.manager.util;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.XposedApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DownloadsUtil {
    public static final String MIME_TYPE_APK = "application/vnd.android.package-archive";
    private static final Map<String, DownloadFinishedCallback> callbacks = new HashMap<>();
    private static final SharedPreferences pref = XposedApp.getInstance().getSharedPreferences("download_cache", Context.MODE_PRIVATE);

    private static DownloadInfo add(Builder b) {
        Context context = b.context;
        removeAllForUrl(context, b.url);

        synchronized (callbacks) {
            callbacks.put(b.url, b.callback);
        }

        Request request = new Request(Uri.parse(b.url));
        request.setTitle(b.title);
        request.setMimeType(b.mimeType.toString());
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE);
        File destination = new File(context.getExternalCacheDir(), "/downloads/" + b.title + b.mimeType.getExtension());
        removeAllForLocalFile(context, destination);
        request.setDestinationUri(Uri.fromFile(destination));
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long id = dm.enqueue(request);
        return getById(context, id);
    }

    public static DownloadInfo addModule(Context context, String title, String url, DownloadFinishedCallback callback) {
        return new Builder(context)
                .setTitle(title)
                .setUrl(url)
                .setCallback(callback)
                .setModule(true)
                .setMimeType(MIME_TYPES.APK)
                .download();
    }

    /*
        public static ModuleVersion getStableVersion(Module m) {
            for (int i = 0; i < m.versions.size(); i++) {
                ModuleVersion mvTemp = m.versions.get(i);

                if (mvTemp.relType == ReleaseType.STABLE) {
                    return mvTemp;
                }
            }
            return null;
        }
    */
    public static DownloadInfo getById(Context context, long id) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor c = dm.query(new Query().setFilterById(id));
        if (!c.moveToFirst()) {
            c.close();
            return null;
        }

        int columnUri = c.getColumnIndexOrThrow(DownloadManager.COLUMN_URI);
        int columnTitle = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
        int columnLastMod = c.getColumnIndexOrThrow(
                DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
        int columnLocalUri = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
        int columnStatus = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
        int columnTotalSize = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
        int columnBytesDownloaded = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
        int columnReason = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON);

        int status = c.getInt(columnStatus);
        String localFilename;
        try {
            localFilename = getFilenameFromUri(c.getString(columnLocalUri));
        } catch (UnsupportedOperationException e) {
            Toast.makeText(context, "An error occurred. Restart app and try again.\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
        if (status == DownloadManager.STATUS_SUCCESSFUL && !new File(localFilename).isFile()) {
            dm.remove(id);
            c.close();
            return null;
        }

        DownloadInfo info = new DownloadInfo(id, c.getString(columnUri),
                c.getString(columnTitle), c.getLong(columnLastMod),
                localFilename, status,
                c.getInt(columnTotalSize), c.getInt(columnBytesDownloaded),
                c.getInt(columnReason));
        c.close();
        return info;
    }

    public static DownloadInfo getLatestForUrl(Context context, String url) {
        List<DownloadInfo> all;
        try {
            all = getAllForUrl(context, url);
        } catch (Throwable throwable) {
            return null;
        }
        return Objects.requireNonNull(all).isEmpty() ? null : all.get(0);
    }

    private static List<DownloadInfo> getAllForUrl(Context context, String url) {
        DownloadManager dm = (DownloadManager) context
                .getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor c = dm.query(new Query());
        int columnId = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
        int columnUri = c.getColumnIndexOrThrow(DownloadManager.COLUMN_URI);
        int columnTitle = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE);
        int columnLastMod = c.getColumnIndexOrThrow(
                DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
        int columnLocalUri = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);
        int columnStatus = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS);
        int columnTotalSize = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
        int columnBytesDownloaded = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
        int columnReason = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON);

        List<DownloadInfo> downloads = new ArrayList<>();
        while (c.moveToNext()) {
            if (!url.equals(c.getString(columnUri)))
                continue;

            int status = c.getInt(columnStatus);
            String localFilename;
            try {
                localFilename = getFilenameFromUri(c.getString(columnLocalUri));
            } catch (UnsupportedOperationException e) {
                Toast.makeText(context, "An error occurred. Restart app and try again.\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
            if (status == DownloadManager.STATUS_SUCCESSFUL && !new File(localFilename).isFile()) {
                dm.remove(c.getLong(columnId));
                continue;
            }

            downloads.add(new DownloadInfo(c.getLong(columnId),
                    c.getString(columnUri), c.getString(columnTitle),
                    c.getLong(columnLastMod), localFilename,
                    status, c.getInt(columnTotalSize),
                    c.getInt(columnBytesDownloaded), c.getInt(columnReason)));
        }
        c.close();

        Collections.sort(downloads);
        return downloads;
    }

    public static void removeById(Context context, long id) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        dm.remove(id);
    }

    private static void removeAllForUrl(Context context, String url) {
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor c = dm.query(new Query());
        int columnId = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
        int columnUri = c.getColumnIndexOrThrow(DownloadManager.COLUMN_URI);

        List<Long> idsList = new ArrayList<>(1);
        while (c.moveToNext()) {
            if (url.equals(c.getString(columnUri)))
                idsList.add(c.getLong(columnId));
        }
        c.close();

        if (idsList.isEmpty())
            return;

        long[] ids = new long[idsList.size()];
        for (int i = 0; i < ids.length; i++)
            ids[i] = idsList.get(i);

        dm.remove(ids);
    }

    private static void removeAllForLocalFile(Context context, File file) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();

        String filename;
        try {
            filename = file.getCanonicalPath();
        } catch (IOException e) {
            Log.w(XposedApp.TAG, "Could not resolve path for " + file.getAbsolutePath(), e);
            return;
        }

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor c = dm.query(new Query());
        int columnId = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
        int columnLocalUri = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI);

        List<Long> idsList = new ArrayList<>(1);
        while (c.moveToNext()) {
            String itemFilename;
            try {
                itemFilename = getFilenameFromUri(c.getString(columnLocalUri));
            } catch (UnsupportedOperationException e) {
                Toast.makeText(context, "An error occurred. Restart app and try again.\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
                itemFilename = null;
            }
            if (itemFilename != null) {
                if (filename.equals(itemFilename)) {
                    idsList.add(c.getLong(columnId));
                } else {
                    try {
                        if (filename.equals(new File(itemFilename).getCanonicalPath())) {
                            idsList.add(c.getLong(columnId));
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        c.close();

        if (idsList.isEmpty())
            return;

        long[] ids = new long[idsList.size()];
        for (int i = 0; i < ids.length; i++)
            ids[i] = idsList.get(i);

        dm.remove(ids);
    }

//    public static void removeOutdated(Context context, long cutoff) {
//        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
//        Cursor c = dm.query(new Query());
//        int columnId = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID);
//        int columnLastMod = c.getColumnIndexOrThrow(
//                DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP);
//
//        List<Long> idsList = new ArrayList<>();
//        while (c.moveToNext()) {
//            if (c.getLong(columnLastMod) < cutoff)
//                idsList.add(c.getLong(columnId));
//        }
//        c.close();
//
//        if (idsList.isEmpty())
//            return;
//
//        long[] ids = new long[idsList.size()];
//        for (int i = 0; i < ids.length; i++)
//            ids[i] = idsList.get(0);
//
//        dm.remove(ids);
//    }

    public static void triggerDownloadFinishedCallback(Context context, long id) {
        DownloadInfo info = getById(context, id);
        if (info == null || info.status != DownloadManager.STATUS_SUCCESSFUL)
            return;

        DownloadFinishedCallback callback;
        synchronized (callbacks) {
            callback = callbacks.get(info.url);
        }

        if (callback == null)
            return;

        // Hack to reset stat information.
        //noinspection ResultOfMethodCallIgnored
        new File(info.localFilename).setExecutable(false);
        callback.onDownloadFinished(context, info);
    }

    private static String getFilenameFromUri(String uriString) {
        if (uriString == null) {
            return null;
        }
        Uri uri = Uri.parse(uriString);
        if (Objects.requireNonNull(uri.getScheme()).equals("file")) {
            return uri.getPath();
        } else if (uri.getScheme().equals("content")) {
            Context context = XposedApp.getInstance();
            try (Cursor c = context.getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns.DATA}, null, null, null)) {
                Objects.requireNonNull(c).moveToFirst();
                return c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
            }
        } else {
            throw new UnsupportedOperationException("Unexpected URI: " + uriString);
        }
    }

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
                            XposedApp.getInstance().getString(R.string.repo_download_failed_http,
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
                    XposedApp.getInstance().getString(R.string.repo_download_failed, url,
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

    public enum MIME_TYPES {
        APK {
            @NonNull
            public String toString() {
                return MIME_TYPE_APK;
            }

            public String getExtension() {
                return ".apk";
            }
        };
//        ZIP {
//            public String toString() {
//                return MIME_TYPE_ZIP;
//            }
//
//            public String getExtension() {
//                return ".zip";
//            }
//        };

        public String getExtension() {
            return null;
        }
    }

    public interface DownloadFinishedCallback {
        void onDownloadFinished(Context context, DownloadInfo info);
    }

    public static class Builder {
        private final Context context;
        boolean module = false;
        private String title = null;
        private String url = null;
        private DownloadFinishedCallback callback = null;
        private MIME_TYPES mimeType = MIME_TYPES.APK;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder setCallback(DownloadFinishedCallback callback) {
            this.callback = callback;
            return this;
        }

        Builder setMimeType(MIME_TYPES mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder setModule(boolean module) {
            this.module = module;
            return this;
        }

        public DownloadInfo download() {
            return add(this);
        }
    }

    public static class DownloadInfo implements Comparable<DownloadInfo> {
        public final long id;
        public final String url;
        public final String title;
        public final String localFilename;
        public final int status;
        public final int totalSize;
        public final int bytesDownloaded;
        public final int reason;
        final long lastModification;

        private DownloadInfo(long id, String url, String title, long lastModification, String localFilename, int status, int totalSize, int bytesDownloaded, int reason) {
            this.id = id;
            this.url = url;
            this.title = title;
            this.lastModification = lastModification;
            this.localFilename = localFilename;
            this.status = status;
            this.totalSize = totalSize;
            this.bytesDownloaded = bytesDownloaded;
            this.reason = reason;
        }

        @Override
        public int compareTo(@NonNull DownloadInfo another) {
            int compare = (int) (another.lastModification
                    - this.lastModification);
            if (compare != 0)
                return compare;
            return this.url.compareTo(another.url);
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