package org.lsposed.lspd.impl;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.ArraySet;

import androidx.annotation.Nullable;

import org.lsposed.lspd.service.ILSPInjectedModuleService;
import org.lsposed.lspd.service.IRemotePreferenceCallback;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class LSPosedRemotePreferences implements SharedPreferences {

    private final Map<String, Object> mMap = new ConcurrentHashMap<>();

    final HashSet<OnSharedPreferenceChangeListener> mListeners = new HashSet<>();

    IRemotePreferenceCallback callback = new IRemotePreferenceCallback.Stub() {
        @Override
        synchronized public void onUpdate(Bundle bundle) {
            Set<String> changes = new ArraySet<>();
            if (bundle.containsKey("delete")) {
                var deletes = (Set<String>) bundle.getSerializable("delete");
                changes.addAll(deletes);
                for (var key : deletes) {
                    mMap.remove(key);
                }
            }
            if (bundle.containsKey("put")) {
                var puts = (Map<String, Object>) bundle.getSerializable("put");
                mMap.putAll(puts);
                changes.addAll(puts.keySet());
            }
            synchronized (mListeners) {
                for (var key : changes) {
                    mListeners.forEach(listener -> listener.onSharedPreferenceChanged(LSPosedRemotePreferences.this, key));
                }
            }
        }
    };

    public LSPosedRemotePreferences(ILSPInjectedModuleService service, String group) throws RemoteException {
        Bundle output = service.requestRemotePreferences(group, callback);
        if (output.containsKey("map")) {
            mMap.putAll((Map<String, Object>) output.getSerializable("map"));
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
            mListeners.add(listener);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }
}
