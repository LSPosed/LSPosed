package de.robv.android.xposed;

import static org.lsposed.lspd.core.ApplicationServiceClient.serviceClient;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.github.xposed.xposedservice.IXRemotePreferenceCallback;

@SuppressWarnings("unchecked")
public class XRemotePreference implements SharedPreferences {

    private Map<String, Object> mMap = new ConcurrentHashMap<>();

    private static final Object CONTENT = new Object();
    final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners = new WeakHashMap<>();

    IXRemotePreferenceCallback callback = new IXRemotePreferenceCallback.Stub() {
        @Override
        synchronized public void onUpdate(Bundle bundle) {
            if (bundle.containsKey("map"))
                mMap = (ConcurrentHashMap<String, Object>) bundle.getSerializable("map");
            if (bundle.containsKey("diff")) {
                for (var key : bundle.getStringArrayList("diff")) {
                    synchronized (mListeners) {
                        mListeners.forEach((listener, __) -> {
                            listener.onSharedPreferenceChanged(XRemotePreference.this, key);
                        });
                    }
                }
            }
        }
    };

    public XRemotePreference(String packageName) {
        this(packageName, 0);
    }

    public XRemotePreference(String packageName, int userId) {
        try {
            Bundle output = serviceClient.requestRemotePreference(packageName, userId, callback.asBinder());
            callback.onUpdate(output);
        } catch (RemoteException e) {
            XposedBridge.log(e);
        }
    }

    @Override
    public Map<String, ?> getAll() {
        return new TreeMap<>(mMap);
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        var v = (String) mMap.getOrDefault(key, defValue);
        if (v != null) return v;
        return defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        var v = (Set<String>) mMap.getOrDefault(key, defValues);
        if (v != null) return v;
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        var v = (Integer) mMap.getOrDefault(key, defValue);
        if (v != null) return v;
        return defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        var v = (Long) mMap.getOrDefault(key, defValue);
        if (v != null) return v;
        return defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        var v = (Float) mMap.getOrDefault(key, defValue);
        if (v != null) return v;
        return defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        var v = (Boolean) mMap.getOrDefault(key, defValue);
        if (v != null) return v;
        return defValue;
    }

    @Override
    public boolean contains(String key) {
        return mMap.containsKey(key);
    }

    @Override
    public Editor edit() {
        throw new UnsupportedOperationException("Read only implementation");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (mListeners) {
            mListeners.put(listener, CONTENT);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }

    }
}
