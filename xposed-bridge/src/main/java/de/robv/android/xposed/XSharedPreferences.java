package de.robv.android.xposed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
	private final File mFile;
	private final String mFilename;
	private Map<String, Object> mMap;
	private boolean mLoaded = false;
	private long mLastModified;
	private long mFileSize;

	/**
	 * Read settings from the specified file.
	 * @param prefFile The file to read the preferences from.
	 */
	public XSharedPreferences(File prefFile) {
		mFile = prefFile;
		mFilename = mFile.getAbsolutePath();
		startLoadFromDisk();
	}

	/**
	 * Read settings from the default preferences for a package.
	 * These preferences are returned by {@link PreferenceManager#getDefaultSharedPreferences}.
	 * @param packageName The package name.
	 */
	public XSharedPreferences(String packageName) {
		this(packageName, packageName + "_preferences");
	}

	/**
	 * Read settings from a custom preferences file for a package.
	 * These preferences are returned by {@link Context#getSharedPreferences(String, int)}.
	 * @param packageName The package name.
	 * @param prefFileName The file name without ".xml".
	 */
	public XSharedPreferences(String packageName, String prefFileName) {
		mFile = new File(Environment.getDataDirectory(), "data/" + packageName + "/shared_prefs/" + prefFileName + ".xml");
		mFilename = mFile.getAbsolutePath();
		startLoadFromDisk();
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

		return mFile.setReadable(true, false);
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
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
			Log.w(TAG, "getSharedPreferences", e);
		} catch (FileNotFoundException ignored) {
			// SharedPreferencesImpl has a canRead() check, so it doesn't log anything in case the file doesn't exist
		} catch (IOException e) {
			Log.w(TAG, "getSharedPreferences", e);
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
		if (hasFileChanged())
			startLoadFromDisk();
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

	/** @hide */
	@Override
	public Map<String, ?> getAll() {
		synchronized (this) {
			awaitLoadedLocked();
			return new HashMap<>(mMap);
		}
	}

	/** @hide */
	@Override
	public String getString(String key, String defValue) {
		synchronized (this) {
			awaitLoadedLocked();
			String v = (String)mMap.get(key);
			return v != null ? v : defValue;
		}
	}

	/** @hide */
	@Override
	@SuppressWarnings("unchecked")
	public Set<String> getStringSet(String key, Set<String> defValues) {
		synchronized (this) {
			awaitLoadedLocked();
			Set<String> v = (Set<String>) mMap.get(key);
			return v != null ? v : defValues;
		}
	}

	/** @hide */
	@Override
	public int getInt(String key, int defValue) {
		synchronized (this) {
			awaitLoadedLocked();
			Integer v = (Integer)mMap.get(key);
			return v != null ? v : defValue;
		}
	}

	/** @hide */
	@Override
	public long getLong(String key, long defValue) {
		synchronized (this) {
			awaitLoadedLocked();
			Long v = (Long)mMap.get(key);
			return v != null ? v : defValue;
		}
	}

	/** @hide */
	@Override
	public float getFloat(String key, float defValue) {
		synchronized (this) {
			awaitLoadedLocked();
			Float v = (Float)mMap.get(key);
			return v != null ? v : defValue;
		}
	}

	/** @hide */
	@Override
	public boolean getBoolean(String key, boolean defValue) {
		synchronized (this) {
			awaitLoadedLocked();
			Boolean v = (Boolean)mMap.get(key);
			return v != null ? v : defValue;
		}
	}

	/** @hide */
	@Override
	public boolean contains(String key) {
		synchronized (this) {
			awaitLoadedLocked();
			return mMap.containsKey(key);
		}
	}

	/** @deprecated Not supported by this implementation. */
	@Deprecated
	@Override
	public Editor edit() {
		throw new UnsupportedOperationException("read-only implementation");
	}

	/** @deprecated Not supported by this implementation. */
	@Deprecated
	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		throw new UnsupportedOperationException("listeners are not supported in this implementation");
	}

	/** @deprecated Not supported by this implementation. */
	@Deprecated
	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		throw new UnsupportedOperationException("listeners are not supported in this implementation");
	}

}
