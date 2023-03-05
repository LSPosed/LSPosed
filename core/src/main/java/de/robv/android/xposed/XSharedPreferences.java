/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 - 2022 LSPosed Contributors
 */

package de.robv.android.xposed;

import static org.lsposed.lspd.core.ApplicationServiceClient.serviceClient;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.internal.util.XmlUtils;

import org.lsposed.lspd.core.BuildConfig;
import org.lsposed.lspd.util.MetaDataReader;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.robv.android.xposed.services.FileResult;

/**
 * This class is basically the same as SharedPreferencesImpl from AOSP, but
 * read-only and without listeners support. Instead, it is made to be
 * compatible with all ROMs.
 */
public final class XSharedPreferences implements SharedPreferences {
    private static final String TAG = "XSharedPreferences";
    private static final HashMap<WatchKey, PrefsData> sWatcherKeyInstances = new HashMap<>();
    private static final Object sContent = new Object();
    private static Thread sWatcherDaemon = null;
    private static WatchService sWatcher;

    private final HashMap<OnSharedPreferenceChangeListener, Object> mListeners = new HashMap<>();
    private final File mFile;
    private final String mFilename;
    private Map<String, Object> mMap;
    private boolean mLoaded = false;
    private long mLastModified;
    private long mFileSize;
    private WatchKey mWatchKey;

    private static void initWatcherDaemon() {
        sWatcherDaemon = new Thread() {
            @Override
            public void run() {
                if (BuildConfig.DEBUG) Log.d(TAG, "Watcher daemon thread started");
                while (true) {
                    WatchKey key;
                    try {
                        key = sWatcher.take();
                    } catch (ClosedWatchServiceException ignored) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Watcher daemon thread finished");
                        sWatcher = null;
                        return;
                    } catch (InterruptedException ignored) {
                        return;
                    }
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }
                        Path dir = (Path) key.watchable();
                        Path path = dir.resolve((Path) event.context());
                        String pathStr = path.toString();
                        if (BuildConfig.DEBUG)
                            Log.v(TAG, "File " + path.toString() + " event: " + kind.name());
                        // We react to both real and backup files due to rare race conditions
                        if (pathStr.endsWith(".bak")) {
                            if (kind != StandardWatchEventKinds.ENTRY_DELETE) {
                                continue;
                            }
                        } else if (SELinuxHelper.getAppDataFileService().checkFileExists(pathStr + ".bak")) {
                            continue;
                        }
                        PrefsData data = sWatcherKeyInstances.get(key);
                        if (data != null && data.hasChanged()) {
                            for (OnSharedPreferenceChangeListener l : data.mPrefs.mListeners.keySet()) {
                                try {
                                    l.onSharedPreferenceChanged(data.mPrefs, null);
                                } catch (Throwable t) {
                                    if (BuildConfig.DEBUG)
                                        Log.e(TAG, "Fail in preference change listener", t);
                                }
                            }
                        }
                    }
                    key.reset();
                }
            }
        };
        sWatcherDaemon.setName(TAG + "-Daemon");
        sWatcherDaemon.setDaemon(true);
        sWatcherDaemon.start();
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
        var m = XposedInit.getLoadedModules().getOrDefault(packageName, Optional.empty());
        if (m.isPresent()) {
            boolean isModule = false;
            int xposedminversion = -1;
            boolean xposedsharedprefs = false;
            try {
                Map<String, Object> metaData = MetaDataReader.getMetaData(new File(m.get()));
                isModule = metaData.containsKey("xposedminversion");
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
        if (newModule) {
            mFile = new File(serviceClient.getPrefsPath(packageName), prefFileName + ".xml");
        } else {
            mFile = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/" + prefFileName + ".xml");
        }
        mFilename = mFile.getAbsolutePath();
        init();
    }

    private void tryRegisterWatcher() {
        if (mWatchKey != null && mWatchKey.isValid()) {
            return;
        }

        synchronized (sWatcherKeyInstances) {
            Path path = mFile.toPath();
            try {
                if (sWatcher == null) {
                    sWatcher = new File(serviceClient.getPrefsPath("")).toPath().getFileSystem().newWatchService();
                    if (BuildConfig.DEBUG) Log.d(TAG, "Created WatchService instance");
                }
                mWatchKey = path.getParent().register(sWatcher, StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                sWatcherKeyInstances.put(mWatchKey, new PrefsData(this));
                if (sWatcherDaemon == null || !sWatcherDaemon.isAlive()) {
                    initWatcherDaemon();
                }
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "tryRegisterWatcher: registered file watcher for " + path);
            } catch (AccessDeniedException accDeniedEx) {
                if (BuildConfig.DEBUG) Log.e(TAG, "tryRegisterWatcher: access denied to " + path);
            } catch (Exception e) {
                Log.e(TAG, "tryRegisterWatcher: failed to register file watcher", e);
            }
        }
    }

    private void tryUnregisterWatcher() {
        synchronized (sWatcherKeyInstances) {
            if (mWatchKey != null) {
                sWatcherKeyInstances.remove(mWatchKey);
                mWatchKey.cancel();
                mWatchKey = null;
            }
            boolean atLeastOneValid = false;
            for (WatchKey key : sWatcherKeyInstances.keySet()) {
                atLeastOneValid |= key.isValid();
            }
            if (!atLeastOneValid) {
                try {
                    sWatcher.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    private void init() {
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

        // Watcher service needs read access to parent directory (looks like execute is not enough)
        if (mFile.getParentFile() != null) {
            mFile.getParentFile().setReadable(true, false);
        }

        if (!mListeners.isEmpty()) {
            tryRegisterWatcher();
        }

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

    /**
     * Registers a callback to be invoked when a change happens to a preference file.<br>
     * Note that it is not possible to determine which preference changed exactly and thus
     * preference key in callback invocation will always be null.
     *
     * @param listener The callback that will run.
     * @see #unregisterOnSharedPreferenceChangeListener
     */
    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("listener cannot be null");

        synchronized (this) {
            if (mListeners.put(listener, sContent) == null) {
                tryRegisterWatcher();
            }
        }
    }

    /**
     * Unregisters a previous callback.
     *
     * @param listener The callback that should be unregistered.
     * @see #registerOnSharedPreferenceChangeListener
     */
    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (this) {
            if (mListeners.remove(listener) != null && mListeners.isEmpty()) {
                tryUnregisterWatcher();
            }
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
                if (BuildConfig.DEBUG) Log.d(TAG, "Ignoring empty prefs file");
                return false;
            }
            if (size != mSize) {
                mSize = size;
                mHash = tryGetFileHash(mPrefs.mFilename);
                if (BuildConfig.DEBUG) Log.d(TAG, "Prefs file size changed");
                return true;
            }
            byte[] hash = tryGetFileHash(mPrefs.mFilename);
            if (!Arrays.equals(hash, mHash)) {
                mHash = hash;
                if (BuildConfig.DEBUG) Log.d(TAG, "Prefs file hash changed");
                return true;
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "Prefs file not changed");
            return false;
        }
    }
}
