package de.robv.android.xposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.internal.util.XmlUtils;
import com.elderdrivers.riru.edxp.util.MetaDataReader;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.services.FileResult;

/**
 * This class is basically the same as SharedPreferencesImpl from AOSP, but
 * read-only and without listeners support. Instead, it is made to be
 * compatible with all ROMs.
 */
public final class XSharedPreferences implements SharedPreferences {
    private static final String TAG = "XSharedPreferences";
    private static final HashMap<Path, PrefsData> mInstances = new HashMap<>();
    private static final Object mContent = new Object();
    private static Thread mDaemon = null;
    private static WatchService mWatcher;
    private final HashMap<OnSharedPreferenceChangeListener, Object> mListeners = new HashMap<>();
    private final File mFile;
    private final String mFilename;
    private Map<String, Object> mMap;
    private boolean mLoaded = false;
    private long mLastModified;
    private long mFileSize;

    static {
        try {
            mWatcher = new File(XposedInit.prefsBasePath).toPath().getFileSystem().newWatchService();
            Log.d(TAG, "Created WatchService instance");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create WatchService", e);
        }
    }

    /**
     * Read settings from the specified file.
     *
     * @param prefFile The file to read the preferences from.
     */
    public XSharedPreferences(File prefFile) {
        mFile = prefFile;
        mFilename = prefFile.getAbsolutePath();
        init();
    }

    /**
     * Read settings from the default preferences for a package.
     * These preferences are returned by {@link PreferenceManager#getDefaultSharedPreferences}.
     *
     * @param packageName The package name.
     */
    public XSharedPreferences(String packageName) {
        this(packageName, packageName + "_preferences");
    }

    /**
     * Read settings from a custom preferences file for a package.
     * These preferences are returned by {@link Context#getSharedPreferences(String, int)}.
     *
     * @param packageName  The package name.
     * @param prefFileName The file name without ".xml".
     */
    public XSharedPreferences(String packageName, String prefFileName) {
        boolean newModule = false;
        Set<String> modules = XposedInit.getLoadedModules();
        for (String m : modules) {
            if (m.contains("/" + packageName + "-")) {
                boolean isModule = false;
                int xposedminversion = -1;
                boolean xposedsharedprefs = false;
                try {
                    Map<String, Object> metaData = MetaDataReader.getMetaData(new File(m));
                    isModule = metaData.containsKey("xposedmodule");
                    if (isModule) {
                        Object minVersionRaw = metaData.get("xposedminversion");
                        if (minVersionRaw instanceof Integer) {
                            xposedminversion = (Integer) minVersionRaw;
                        } else if (minVersionRaw instanceof String) {
                            xposedminversion = MetaDataReader.extractIntPart((String) minVersionRaw);
                        }
                        xposedsharedprefs = metaData.containsKey("xposedsharedprefs");
                    }
                } catch (NumberFormatException | IOException e) {
                    Log.w(TAG, "Apk parser fails: " + e);
                }
                newModule = isModule && (xposedminversion > 92 || xposedsharedprefs);
            }
        }
        if (newModule && XposedInit.prefsBasePath != null) {
            mFile = new File(XposedInit.prefsBasePath, packageName + "/" + prefFileName + ".xml");
        } else {
            mFile = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/" + prefFileName + ".xml");
        }
        mFilename = mFile.getAbsolutePath();
        init();
    }

    private void tryRegisterWatcher() {
        Path path = mFile.toPath();
        if (mInstances.containsKey(path)) {
            return;
        }
        try {
            path.getParent().register(mWatcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            mInstances.put(path, new PrefsData(this));
            Log.d(TAG, "Registered file watcher for " + path);
        } catch (Exception e) {
            Log.d(TAG, "Failed to register file watcher", e);
        }
    }

    private void init() {
        if (mDaemon == null || !mDaemon.isAlive()) {
            mDaemon = new Thread() {
                @Override
                public void run() {
                    Log.d(TAG, "Daemon thread started");
                    while (true) {
                        WatchKey key;
                        try {
                            key = mWatcher.take();
                        } catch (InterruptedException ignored) {
                            return;
                        }
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            Path dir = (Path)key.watchable();
                            Path path = dir.resolve((Path)event.context());
                            String pathStr = path.toString();
                            Log.v(TAG, "File " + path.toString() + " event: " + kind.name());
                            // We react to both real and backup files due to rare race conditions
                            if (pathStr.endsWith(".bak")) {
                                if (kind != StandardWatchEventKinds.ENTRY_DELETE) {
                                    continue;
                                } else {
                                    pathStr = path.getFileName().toString();
                                    path = dir.resolve(pathStr.substring(0, pathStr.length() - 4));
                                }
                            } else if (SELinuxHelper.getAppDataFileService().checkFileExists(pathStr + ".bak")) {
                                continue;
                            }
                            PrefsData data = mInstances.get(path);
                            if (data != null && data.hasChanged()) {
                                for (OnSharedPreferenceChangeListener l : data.mPrefs.mListeners.keySet()) {
                                    try {
                                        l.onSharedPreferenceChanged(data.mPrefs, null);
                                    } catch (Throwable t) {
                                        Log.e(TAG, "Fail in preference change listener", t);
                                    }
                                }
                            }
                        }
                        key.reset();
                    }
                }
            };
            mDaemon.setName(TAG + "-Daemon");
            mDaemon.setDaemon(true);
            mDaemon.start();
        }
        tryRegisterWatcher();
        startLoadFromDisk();
    }

    private static long tryGetFileSize(String filename) {
        try {
            return SELinuxHelper.getAppDataFileService().getFileSize(filename);
        } catch (IOException ignored) {
            return 0;
        }
    }

    private static byte[] tryGetFileHash(String filename) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream is = SELinuxHelper.getAppDataFileService().getFileInputStream(filename)) {
                byte[] buf = new byte[4096];
                int read;
                while ((read = is.read(buf)) != -1) {
                    md.update(buf, 0, read);
                }
            }
            return md.digest();
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    /**
     * Tries to make the preferences file world-readable.
     *
     * <p><strong>Warning:</strong> This is only meant to work around permission "fix" functions that are part
     * of some recoveries. It doesn't replace the need to open preferences with {@code MODE_WORLD_READABLE}
     * in the module's UI code. Otherwise, Android will set stricter permissions again during the next save.
     *
     * <p>This will only work if executed as root (e.g. {@code initZygote()}) and only if SELinux is disabled.
     *
     * @return {@code true} in case the file could be made world-readable.
     */
    @SuppressLint("SetWorldReadable")
    public boolean makeWorldReadable() {
        if (!SELinuxHelper.getAppDataFileService().hasDirectFileAccess())
            return false; // It doesn't make much sense to make the file readable if we wouldn't be able to access it anyway.

        if (!mFile.exists()) // Just in case - the file should never be created if it doesn't exist.
            return false;

        if (!mFile.setReadable(true, false))
            return false;

        tryRegisterWatcher();
        return true;
    }

    /**
     * Returns the file that is backing these preferences.
     *
     * <p><strong>Warning:</strong> The file might not be accessible directly.
     */
    public File getFile() {
        return mFile;
    }

    private void startLoadFromDisk() {
        synchronized (this) {
            mLoaded = false;
        }
        new Thread("XSharedPreferences-load") {
            @Override
            public void run() {
                synchronized (XSharedPreferences.this) {
                    loadFromDiskLocked();
                }
            }
        }.start();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void loadFromDiskLocked() {
        if (mLoaded) {
            return;
        }

        Map map = null;
        FileResult result = null;
        try {
            result = SELinuxHelper.getAppDataFileService().getFileInputStream(mFilename, mFileSize, mLastModified);
            if (result.stream != null) {
                map = XmlUtils.readMapXml(result.stream);
                result.stream.close();
            } else {
                // The file is unchanged, keep the current values
                map = mMap;
            }
        } catch (XmlPullParserException e) {
            Log.w(TAG, "getSharedPreferences failed for: " + mFilename, e);
        } catch (FileNotFoundException ignored) {
            // SharedPreferencesImpl has a canRead() check, so it doesn't log anything in case the file doesn't exist
        } catch (IOException e) {
            Log.w(TAG, "getSharedPreferences failed for: " + mFilename, e);
        } finally {
            if (result != null && result.stream != null) {
                try {
                    result.stream.close();
                } catch (RuntimeException rethrown) {
                    throw rethrown;
                } catch (Exception ignored) {
                }
            }
        }

        mLoaded = true;
        if (map != null) {
            mMap = map;
            mLastModified = result.mtime;
            mFileSize = result.size;
        } else {
            mMap = new HashMap<>();
        }
        notifyAll();
    }

    /**
     * Reload the settings from file if they have changed.
     *
     * <p><strong>Warning:</strong> With enforcing SELinux, this call might be quite expensive.
     */
    public synchronized void reload() {
        if (hasFileChanged()) {
            init();
        }
    }

    /**
     * Check whether the file has changed since the last time it has been loaded.
     *
     * <p><strong>Warning:</strong> With enforcing SELinux, this call might be quite expensive.
     */
    public synchronized boolean hasFileChanged() {
        try {
            FileResult result = SELinuxHelper.getAppDataFileService().statFile(mFilename);
            return mLastModified != result.mtime || mFileSize != result.size;
        } catch (FileNotFoundException ignored) {
            // SharedPreferencesImpl doesn't log anything in case the file doesn't exist
            return true;
        } catch (IOException e) {
            Log.w(TAG, "hasFileChanged", e);
            return true;
        }
    }

    private void awaitLoadedLocked() {
        while (!mLoaded) {
            try {
                wait();
            } catch (InterruptedException unused) {
            }
        }
    }

    /**
     * @hide
     */
    @Override
    public Map<String, ?> getAll() {
        synchronized (this) {
            awaitLoadedLocked();
            return new HashMap<>(mMap);
        }
    }

    /**
     * @hide
     */
    @Override
    public String getString(String key, String defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            String v = (String) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    /**
     * @hide
     */
    @Override
    @SuppressWarnings("unchecked")
    public Set<String> getStringSet(String key, Set<String> defValues) {
        synchronized (this) {
            awaitLoadedLocked();
            Set<String> v = (Set<String>) mMap.get(key);
            return v != null ? v : defValues;
        }
    }

    /**
     * @hide
     */
    @Override
    public int getInt(String key, int defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Integer v = (Integer) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    /**
     * @hide
     */
    @Override
    public long getLong(String key, long defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Long v = (Long) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    /**
     * @hide
     */
    @Override
    public float getFloat(String key, float defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Float v = (Float) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    /**
     * @hide
     */
    @Override
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (this) {
            awaitLoadedLocked();
            Boolean v = (Boolean) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    /**
     * @hide
     */
    @Override
    public boolean contains(String key) {
        synchronized (this) {
            awaitLoadedLocked();
            return mMap.containsKey(key);
        }
    }

    /**
     * @deprecated Not supported by this implementation.
     */
    @Deprecated
    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("read-only implementation");
    }

    @Deprecated
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
            mListeners.put(listener, mContent);
        }
    }

    @Deprecated
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
            mListeners.remove(listener);
        }
    }

    private static class PrefsData {
        public final XSharedPreferences mPrefs;
        private long mSize;
        private byte[] mHash;

        public PrefsData(XSharedPreferences prefs) {
            mPrefs = prefs;
            mSize = tryGetFileSize(prefs.mFilename);
            mHash = tryGetFileHash(prefs.mFilename);
        }

        public boolean hasChanged() {
            long size = tryGetFileSize(mPrefs.mFilename);
            if (size < 1) {
                Log.d(TAG, "Ignoring empty prefs file");
                return false;
            }
            if (size != mSize) {
                mSize = size;
                mHash = tryGetFileHash(mPrefs.mFilename);
                Log.d(TAG, "Prefs file size changed");
                return true;
            }
            byte[] hash = tryGetFileHash(mPrefs.mFilename);
            if (!Arrays.equals(hash, mHash)) {
                mHash = hash;
                Log.d(TAG, "Prefs file hash changed");
                return true;
            }
            Log.d(TAG, "Prefs file not changed");
            return false;
        }
    }
}
